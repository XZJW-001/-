package com.meeting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeting.common.exception.BusinessException;
import com.meeting.entity.Meeting;
import com.meeting.mapper.MeetingMapper;
import com.meeting.utils.QrcodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class InnovationExperienceService {

    private static final DateTimeFormatter TRIGGER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern OWNER_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,4})(?:负责|跟进|完成)");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2}月\\d{1,2}日|\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|本周[一二三四五六日]|下周[一二三四五六日]?)");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final MeetingMapper meetingMapper;
    private final InnovationSecurityService securityService;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> liveEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long meetingId) {
        ensureMeeting(meetingId);
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        liveEmitters.computeIfAbsent(meetingId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> removeEmitter(meetingId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("snapshot").data(getLiveSnapshot(meetingId)));
        } catch (Exception e) {
            cleanup.run();
        }
        return emitter;
    }

    public void publishLiveUpdate(Long meetingId) {
        List<SseEmitter> emitters = liveEmitters.get(meetingId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        Map<String, Object> snapshot = getLiveSnapshot(meetingId);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
            } catch (Exception e) {
                removeEmitter(meetingId, emitter);
            }
        }
    }

    public Map<String, Object> getLiveSnapshot(Long meetingId) {
        Meeting meeting = ensureMeeting(meetingId);
        Map<String, Object> counts = jdbcTemplate.queryForMap("""
                SELECT COUNT(*) total,
                  COALESCE(SUM(CASE WHEN status IN (1,2) THEN 1 ELSE 0 END),0) signed,
                  COALESCE(SUM(CASE WHEN status=2 THEN 1 ELSE 0 END),0) late,
                  COALESCE(SUM(CASE WHEN status=0 THEN 1 ELSE 0 END),0) pending,
                  COALESCE(SUM(CASE WHEN status=3 THEN 1 ELSE 0 END),0) absent
                FROM meeting_attendee WHERE meeting_id=?
                """, meetingId);
        int total = intValue(counts.get("total"));
        int signed = intValue(counts.get("signed"));
        List<Map<String, Object>> latest = jdbcTemplate.query("""
                SELECT c.id, c.user_id, u.real_name, u.avatar, c.sign_time, c.sign_method, c.sign_status,
                       COALESCE(r.risk_level, 'LOW') risk_level
                FROM check_in_record c
                LEFT JOIN sys_user u ON u.id=c.user_id
                LEFT JOIN checkin_risk r ON r.record_id=c.id
                WHERE c.meeting_id=? ORDER BY c.sign_time DESC LIMIT 12
                """, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("userId", rs.getLong("user_id"));
            item.put("realName", rs.getString("real_name"));
            item.put("avatar", rs.getString("avatar"));
            item.put("signTime", rs.getTimestamp("sign_time").toLocalDateTime());
            item.put("signMethod", rs.getString("sign_method"));
            item.put("signStatus", rs.getInt("sign_status"));
            item.put("riskLevel", rs.getString("risk_level"));
            return item;
        }, meetingId);

        Integer guestCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM meeting_guest WHERE meeting_id=?", Integer.class, meetingId);
        Integer highRisk = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkin_risk WHERE meeting_id=? AND risk_level='HIGH' AND review_status=0",
                Integer.class, meetingId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("meetingId", meetingId);
        result.put("title", meeting.getTitle());
        result.put("location", meeting.getLocation());
        result.put("startTime", meeting.getStartTime());
        result.put("endTime", meeting.getEndTime());
        result.put("status", meeting.getStatus());
        result.put("total", total);
        result.put("signed", signed);
        result.put("late", intValue(counts.get("late")));
        result.put("pending", intValue(counts.get("pending")));
        result.put("absent", intValue(counts.get("absent")));
        result.put("guestCount", guestCount == null ? 0 : guestCount);
        result.put("highRiskCount", highRisk == null ? 0 : highRisk);
        result.put("attendanceRate", total == 0 ? 0 : Math.round(signed * 10000.0 / total) / 100.0);
        result.put("latest", latest);
        result.put("serverTime", LocalDateTime.now());
        return result;
    }

    @Transactional
    public Map<String, Object> sendReminders(Long meetingId, String type, boolean onlyUnsigned) {
        Meeting meeting = ensureMeeting(meetingId);
        String reminderType = hasText(type) ? type.toUpperCase(Locale.ROOT) : "MANUAL";
        String triggerKey = "manual:" + meetingId + ":" + LocalDateTime.now().format(TRIGGER_TIME);
        String content = reminderText(meeting, reminderType);
        String statusFilter = onlyUnsigned ? " AND ma.status=0" : "";
        int created = jdbcTemplate.update("""
                INSERT IGNORE INTO smart_reminder
                  (meeting_id, user_id, reminder_type, content, trigger_key)
                SELECT ma.meeting_id, ma.user_id, ?, ?, ?
                FROM meeting_attendee ma WHERE ma.meeting_id=?
                """ + statusFilter, reminderType, content, triggerKey, meetingId);
        return Map.of("meetingId", meetingId, "createdCount", created, "content", content);
    }

    public List<Map<String, Object>> getMyReminders(Long userId, boolean unreadOnly) {
        String sql = """
                SELECT r.id, r.meeting_id, m.title meeting_title, r.reminder_type, r.content,
                       r.is_read, r.create_time, m.start_time, m.location
                FROM smart_reminder r LEFT JOIN meeting m ON m.id=r.meeting_id
                WHERE r.user_id=?
                """ + (unreadOnly ? " AND r.is_read=0" : "") + " ORDER BY r.create_time DESC LIMIT 100";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("meetingId", rs.getLong("meeting_id"));
            item.put("meetingTitle", rs.getString("meeting_title"));
            item.put("type", rs.getString("reminder_type"));
            item.put("content", rs.getString("content"));
            item.put("read", rs.getInt("is_read") == 1);
            item.put("createTime", rs.getTimestamp("create_time").toLocalDateTime());
            item.put("startTime", rs.getTimestamp("start_time") == null ? null : rs.getTimestamp("start_time").toLocalDateTime());
            item.put("location", rs.getString("location"));
            return item;
        }, userId);
    }

    public void markReminderRead(Long reminderId, Long userId) {
        int updated = jdbcTemplate.update(
                "UPDATE smart_reminder SET is_read=1, read_time=NOW() WHERE id=? AND user_id=?",
                reminderId, userId);
        if (updated == 0) {
            throw new BusinessException("提醒不存在");
        }
    }

    public void processAutomaticReminders() {
        List<Map<String, Object>> meetings = jdbcTemplate.queryForList("""
                SELECT m.id, m.title, m.location, m.start_time,
                       COALESCE(c.reminder_minutes,30) reminder_minutes
                FROM meeting m
                LEFT JOIN meeting_feature_config c ON c.meeting_id=m.id
                WHERE m.status IN (0,1,2)
                  AND COALESCE(c.reminder_enabled,1)=1
                  AND m.start_time BETWEEN DATE_SUB(NOW(), INTERVAL 10 MINUTE) AND DATE_ADD(NOW(), INTERVAL 1 DAY)
                """);
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> row : meetings) {
            Long meetingId = ((Number) row.get("id")).longValue();
            LocalDateTime start = ((java.sql.Timestamp) row.get("start_time")).toLocalDateTime();
            long minutes = Duration.between(now, start).toMinutes();
            int configured = intValue(row.get("reminder_minutes"));
            if (minutes >= 0 && minutes <= configured) {
                insertAutomaticReminder(meetingId, "PRE_START", "pre:" + meetingId,
                        "会议《" + row.get("title") + "》即将开始，请提前到达 " + row.get("location"));
            } else if (minutes < 0 && minutes >= -10) {
                insertAutomaticReminder(meetingId, "CHECKIN_OPEN", "open:" + meetingId,
                        "会议《" + row.get("title") + "》已开始，您尚未签到，请尽快完成签到");
            }
        }
    }

    @Transactional
    public Map<String, Object> createGuestInvite(Long meetingId, Long creatorId, Integer validHours) {
        Meeting meeting = ensureMeeting(meetingId);
        int hours = validHours == null ? 24 : Math.max(1, Math.min(validHours, 168));
        LocalDateTime expire = meeting.getEndTime() != null && meeting.getEndTime().isAfter(LocalDateTime.now())
                ? meeting.getEndTime().plusHours(1) : LocalDateTime.now().plusHours(hours);
        String token = QrcodeUtil.generateToken();
        jdbcTemplate.update("""
                INSERT INTO meeting_guest_invite(meeting_id, invite_token, expire_time, creator_id)
                VALUES (?, ?, ?, ?)
                """, meetingId, token, expire, creatorId);
        String content = "MEETING_GUEST:" + token;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("meetingId", meetingId);
        result.put("meetingTitle", meeting.getTitle());
        result.put("token", token);
        result.put("content", content);
        result.put("expireTime", expire);
        try {
            result.put("qrcodeImage", "data:image/png;base64," + QrcodeUtil.generateQrcodeBase64(content, 320, 320));
        } catch (Exception e) {
            throw new BusinessException("访客二维码生成失败");
        }
        return result;
    }

    public Map<String, Object> getGuestInvite(String token) {
        Map<String, Object> row = findGuestInvite(token);
        Meeting meeting = ensureMeeting(((Number) row.get("meeting_id")).longValue());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("meetingId", meeting.getId());
        result.put("title", meeting.getTitle());
        result.put("location", meeting.getLocation());
        result.put("startTime", meeting.getStartTime());
        result.put("endTime", meeting.getEndTime());
        result.put("description", meeting.getDescription());
        result.put("inviteToken", token);
        result.put("expireTime", ((java.sql.Timestamp) row.get("expire_time")).toLocalDateTime());
        return result;
    }

    @Transactional
    public Map<String, Object> guestCheckIn(String token, Map<String, Object> body, String ipAddress) {
        Map<String, Object> invite = findGuestInvite(token);
        String name = string(body.get("guestName"));
        if (!hasText(name)) {
            throw new BusinessException("请填写访客姓名");
        }
        Long inviteId = ((Number) invite.get("id")).longValue();
        Long meetingId = ((Number) invite.get("meeting_id")).longValue();
        String phone = string(body.get("phone"));
        try {
            jdbcTemplate.update("""
                    INSERT INTO meeting_guest
                      (meeting_id, invite_id, guest_name, organization, phone, sign_time, device_info, ip_address)
                    VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)
                    """, meetingId, inviteId, name, string(body.get("organization")),
                    hasText(phone) ? phone : null, string(body.get("deviceInfo")), ipAddress);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BusinessException("该手机号已经完成访客签到");
        }
        publishLiveUpdate(meetingId);
        return Map.of("meetingId", meetingId, "guestName", name, "signTime", LocalDateTime.now());
    }

    public List<Map<String, Object>> getGuests(Long meetingId) {
        ensureMeeting(meetingId);
        return jdbcTemplate.query("""
                SELECT id, guest_name, organization, phone, sign_time, device_info, ip_address
                FROM meeting_guest WHERE meeting_id=? ORDER BY sign_time DESC, id DESC
                """, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("guestName", rs.getString("guest_name"));
            item.put("organization", rs.getString("organization"));
            item.put("phone", rs.getString("phone"));
            item.put("signTime", rs.getTimestamp("sign_time").toLocalDateTime());
            item.put("deviceInfo", rs.getString("device_info"));
            item.put("ipAddress", rs.getString("ip_address"));
            return item;
        }, meetingId);
    }

    public Map<String, Object> getAttendanceProfile(Long userId) {
        Map<String, Object> counts = jdbcTemplate.queryForMap("""
                SELECT COUNT(*) total,
                  COALESCE(SUM(CASE WHEN status=1 THEN 1 ELSE 0 END),0) normal_count,
                  COALESCE(SUM(CASE WHEN status=2 THEN 1 ELSE 0 END),0) late_count,
                  COALESCE(SUM(CASE WHEN status=3 THEN 1 ELSE 0 END),0) absent_count,
                  COALESCE(SUM(CASE WHEN status IN (1,2) THEN 1 ELSE 0 END),0) attended_count
                FROM meeting_attendee WHERE user_id=?
                """, userId);
        int total = intValue(counts.get("total"));
        int normal = intValue(counts.get("normal_count"));
        int late = intValue(counts.get("late_count"));
        int absent = intValue(counts.get("absent_count"));
        int attended = intValue(counts.get("attended_count"));
        double attendanceRate = total == 0 ? 0 : Math.round(attended * 10000.0 / total) / 100.0;
        int reliability = total == 0 ? 100 : Math.max(0, (int) Math.round(attendanceRate - late * 2.0 - absent * 5.0));

        List<Integer> statuses = jdbcTemplate.query("""
                SELECT ma.status FROM meeting_attendee ma
                JOIN meeting m ON m.id=ma.meeting_id
                WHERE ma.user_id=? AND m.start_time<=NOW()
                ORDER BY m.start_time DESC
                """, (rs, rowNum) -> rs.getInt(1), userId);
        int streak = 0;
        for (Integer status : statuses) {
            if (status != null && status == 1) streak++; else break;
        }

        List<Map<String, Object>> methodRows = jdbcTemplate.queryForList("""
                SELECT sign_method, COUNT(*) count FROM meeting_attendee
                WHERE user_id=? AND sign_method IS NOT NULL GROUP BY sign_method ORDER BY count DESC LIMIT 1
                """, userId);
        String preferredMethod = methodRows.isEmpty() ? "qrcode" : String.valueOf(methodRows.get(0).get("sign_method"));
        List<Map<String, Object>> monthly = jdbcTemplate.query("""
                SELECT DATE_FORMAT(m.start_time,'%Y-%m') month, COUNT(*) total,
                       SUM(CASE WHEN ma.status IN (1,2) THEN 1 ELSE 0 END) attended
                FROM meeting_attendee ma JOIN meeting m ON m.id=ma.meeting_id
                WHERE ma.user_id=? AND m.start_time>=DATE_SUB(NOW(), INTERVAL 6 MONTH)
                GROUP BY DATE_FORMAT(m.start_time,'%Y-%m') ORDER BY month
                """, (rs, rowNum) -> Map.of(
                "month", rs.getString("month"),
                "total", rs.getInt("total"),
                "attended", rs.getInt("attended")
        ), userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("totalMeetings", total);
        result.put("normalCount", normal);
        result.put("lateCount", late);
        result.put("absenceCount", absent);
        result.put("attendanceRate", attendanceRate);
        result.put("reliabilityScore", reliability);
        result.put("continuousAttendance", streak);
        result.put("preferredMethod", preferredMethod);
        result.put("monthlyTrend", monthly);
        return result;
    }

    public Map<String, Object> getAlerts() {
        List<Map<String, Object>> meetingWarnings = new ArrayList<>();
        List<Map<String, Object>> upcoming = jdbcTemplate.queryForList("""
                SELECT id, title, group_id, start_time FROM meeting
                WHERE start_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 14 DAY)
                ORDER BY start_time LIMIT 20
                """);
        for (Map<String, Object> meeting : upcoming) {
            Long groupId = meeting.get("group_id") == null ? null : ((Number) meeting.get("group_id")).longValue();
            Double predicted = 100.0;
            if (groupId != null) {
                predicted = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(AVG(CASE WHEN ma.status IN (1,2) THEN 100.0 ELSE 0 END),100)
                        FROM meeting_attendee ma JOIN meeting m ON m.id=ma.meeting_id
                        WHERE m.group_id=? AND m.status=3
                        """, Double.class, groupId);
            }
            if (predicted != null && predicted < 80) {
                Map<String, Object> warning = new LinkedHashMap<>();
                warning.put("meetingId", meeting.get("id"));
                warning.put("title", meeting.get("title"));
                warning.put("startTime", ((java.sql.Timestamp) meeting.get("start_time")).toLocalDateTime());
                warning.put("predictedAttendanceRate", Math.round(predicted * 100.0) / 100.0);
                warning.put("level", predicted < 60 ? "HIGH" : "MEDIUM");
                warning.put("suggestion", "建议提前发送提醒并确认缺席原因");
                meetingWarnings.add(warning);
            }
        }

        List<Map<String, Object>> userWarnings = jdbcTemplate.query("""
                SELECT u.id user_id, u.real_name, COUNT(*) absent_count
                FROM meeting_attendee ma JOIN sys_user u ON u.id=ma.user_id
                WHERE ma.status=3 GROUP BY u.id,u.real_name HAVING COUNT(*)>=2
                ORDER BY absent_count DESC LIMIT 20
                """, (rs, rowNum) -> Map.of(
                "userId", rs.getLong("user_id"),
                "realName", rs.getString("real_name"),
                "absenceCount", rs.getInt("absent_count"),
                "level", rs.getInt("absent_count") >= 4 ? "HIGH" : "MEDIUM",
                "suggestion", "建议了解参会困难并优化提醒时间"
        ));
        Integer highRisk = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkin_risk WHERE risk_level='HIGH' AND review_status=0", Integer.class);
        return Map.of(
                "meetingWarnings", meetingWarnings,
                "userWarnings", userWarnings,
                "unreviewedHighRiskCount", highRisk == null ? 0 : highRisk,
                "generatedAt", LocalDateTime.now()
        );
    }

    @Transactional
    public Map<String, Object> generateMinutes(Long meetingId, Long userId, String sourceText) {
        Meeting meeting = ensureMeeting(meetingId);
        if (!hasText(sourceText)) {
            throw new BusinessException("请填写会议记录或转写文本");
        }
        List<String> sentences = Arrays.stream(sourceText.replace('\r', '\n').split("[。！？!?\\n]+"))
                .map(String::trim).filter(this::hasText).distinct().toList();
        String summary = sentences.stream().limit(4).reduce((a, b) -> a + "；" + b).orElse("暂无可提取内容");
        if (summary.length() > 600) summary = summary.substring(0, 600) + "...";

        List<Map<String, Object>> actionItems = new ArrayList<>();
        for (String sentence : sentences) {
            if (!(sentence.contains("负责") || sentence.contains("跟进") || sentence.contains("完成") ||
                    sentence.contains("待办") || sentence.contains("截止") || sentence.contains("行动"))) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("task", sentence);
            Matcher owner = OWNER_PATTERN.matcher(sentence);
            item.put("owner", owner.find() ? owner.group(1) : "待分配");
            Matcher date = DATE_PATTERN.matcher(sentence);
            item.put("dueDate", date.find() ? date.group(1) : "待确认");
            item.put("status", "TODO");
            actionItems.add(item);
            if (actionItems.size() >= 10) break;
        }
        if (actionItems.isEmpty()) {
            actionItems.add(new LinkedHashMap<>(Map.of(
                    "task", "整理并确认《" + meeting.getTitle() + "》会议决议",
                    "owner", "待分配", "dueDate", "待确认", "status", "TODO"
            )));
        }
        jdbcTemplate.update("""
                INSERT INTO meeting_minutes(meeting_id, source_text, summary, action_items, created_by)
                VALUES (?, ?, ?, ?, ?)
                """, meetingId, sourceText, summary, json(actionItems), userId);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("meetingId", meetingId);
        result.put("meetingTitle", meeting.getTitle());
        result.put("summary", summary);
        result.put("actionItems", actionItems);
        result.put("mode", "LOCAL_EXTRACTIVE");
        result.put("createTime", LocalDateTime.now());
        return result;
    }

    public List<Map<String, Object>> getMinutes(Long meetingId) {
        ensureMeeting(meetingId);
        return jdbcTemplate.query("""
                SELECT mm.*, u.real_name FROM meeting_minutes mm
                LEFT JOIN sys_user u ON u.id=mm.created_by
                WHERE mm.meeting_id=? ORDER BY mm.create_time DESC, mm.id DESC
                """, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("meetingId", rs.getLong("meeting_id"));
            item.put("sourceText", rs.getString("source_text"));
            item.put("summary", rs.getString("summary"));
            item.put("actionItems", parseActionItems(rs.getString("action_items")));
            item.put("creatorName", rs.getString("real_name"));
            item.put("createTime", rs.getTimestamp("create_time").toLocalDateTime());
            return item;
        }, meetingId);
    }

    private void insertAutomaticReminder(Long meetingId, String type, String triggerKey, String content) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO smart_reminder
                  (meeting_id, user_id, reminder_type, content, trigger_key)
                SELECT ma.meeting_id, ma.user_id, ?, ?, ?
                FROM meeting_attendee ma WHERE ma.meeting_id=? AND ma.status=0
                """, type, content, triggerKey, meetingId);
    }

    private Map<String, Object> findGuestInvite(String token) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM meeting_guest_invite
                WHERE invite_token=? AND status=1 AND expire_time>NOW()
                """, token);
        if (rows.isEmpty()) {
            throw new BusinessException("访客邀请无效或已过期");
        }
        return rows.get(0);
    }

    private String reminderText(Meeting meeting, String type) {
        return switch (type) {
            case "CHECKIN_OPEN" -> "会议《" + meeting.getTitle() + "》签到已开放，请及时完成签到";
            case "MATERIAL" -> "会议《" + meeting.getTitle() + "》资料已更新，请会前查看";
            default -> "会议《" + meeting.getTitle() + "》将于 " + meeting.getStartTime() + " 在 " + meeting.getLocation() + " 举行";
        };
    }

    private Meeting ensureMeeting(Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) throw new BusinessException("会议不存在");
        return meeting;
    }

    private void removeEmitter(Long meetingId, SseEmitter emitter) {
        List<SseEmitter> emitters = liveEmitters.get(meetingId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) liveEmitters.remove(meetingId);
        }
    }

    private List<Map<String, Object>> parseActionItems(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    private int intValue(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

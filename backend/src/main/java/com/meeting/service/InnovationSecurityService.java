package com.meeting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeting.common.exception.BusinessException;
import com.meeting.dto.CheckInRequest;
import com.meeting.entity.CheckInRecord;
import com.meeting.entity.Meeting;
import com.meeting.mapper.MeetingMapper;
import com.meeting.utils.QrcodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InnovationSecurityService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final MeetingMapper meetingMapper;

    @Value("${app.innovation.signing-secret:meeting-innovation-signing-secret-change-me}")
    private String signingSecret;

    public Map<String, Object> getConfig(Long meetingId) {
        ensureMeeting(meetingId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM meeting_feature_config WHERE meeting_id = ?", meetingId);
        if (rows.isEmpty()) {
            return defaultConfig(meetingId);
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> result = defaultConfig(meetingId);
        result.put("dynamicQrEnabled", bool(row.get("dynamic_qr_enabled")));
        result.put("qrRefreshSeconds", intValue(row.get("qr_refresh_seconds"), 20));
        result.put("requireLocation", bool(row.get("require_location")));
        result.put("requirePhoto", bool(row.get("require_photo")));
        result.put("venueLatitude", row.get("venue_latitude"));
        result.put("venueLongitude", row.get("venue_longitude"));
        result.put("radiusMeters", intValue(row.get("radius_meters"), 300));
        result.put("offlineAllowed", bool(row.get("offline_allowed")));
        result.put("offlineMaxMinutes", intValue(row.get("offline_max_minutes"), 120));
        result.put("reminderEnabled", bool(row.get("reminder_enabled")));
        result.put("reminderMinutes", intValue(row.get("reminder_minutes"), 30));
        return result;
    }

    @Transactional
    public Map<String, Object> saveConfig(Long meetingId, Long userId, Map<String, Object> body) {
        ensureMeeting(meetingId);
        int refreshSeconds = clamp(intValue(body.get("qrRefreshSeconds"), 20), 10, 300);
        int radiusMeters = clamp(intValue(body.get("radiusMeters"), 300), 50, 5000);
        int offlineMinutes = clamp(intValue(body.get("offlineMaxMinutes"), 120), 5, 1440);
        int reminderMinutes = clamp(intValue(body.get("reminderMinutes"), 30), 5, 1440);

        jdbcTemplate.update("""
                INSERT INTO meeting_feature_config (
                  meeting_id, dynamic_qr_enabled, qr_refresh_seconds,
                  require_location, require_photo, venue_latitude, venue_longitude,
                  radius_meters, offline_allowed, offline_max_minutes,
                  reminder_enabled, reminder_minutes, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  dynamic_qr_enabled = VALUES(dynamic_qr_enabled),
                  qr_refresh_seconds = VALUES(qr_refresh_seconds),
                  require_location = VALUES(require_location),
                  require_photo = VALUES(require_photo),
                  venue_latitude = VALUES(venue_latitude),
                  venue_longitude = VALUES(venue_longitude),
                  radius_meters = VALUES(radius_meters),
                  offline_allowed = VALUES(offline_allowed),
                  offline_max_minutes = VALUES(offline_max_minutes),
                  reminder_enabled = VALUES(reminder_enabled),
                  reminder_minutes = VALUES(reminder_minutes),
                  updated_by = VALUES(updated_by)
                """,
                meetingId, flag(body.get("dynamicQrEnabled")), refreshSeconds,
                flag(body.get("requireLocation")), flag(body.get("requirePhoto")),
                decimal(body.get("venueLatitude")), decimal(body.get("venueLongitude")),
                radiusMeters, flagDefault(body.get("offlineAllowed"), true), offlineMinutes,
                flagDefault(body.get("reminderEnabled"), true), reminderMinutes, userId);
        return getConfig(meetingId);
    }

    public Map<String, Object> issueDynamicQrcode(Long meetingId) {
        Meeting meeting = ensureMeeting(meetingId);
        Map<String, Object> config = getConfig(meetingId);
        int seconds = (Integer) config.get("qrRefreshSeconds");
        long slot = System.currentTimeMillis() / 1000L / seconds;
        String ticket = signPayload("qr:" + meetingId + ":" + slot);
        String content = "MEETING_DYNAMIC:" + ticket;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("meetingId", meetingId);
        result.put("meetingTitle", meeting.getTitle());
        result.put("ticket", ticket);
        result.put("qrcodeContent", content);
        result.put("refreshSeconds", seconds);
        result.put("expiresAt", LocalDateTime.now().plusSeconds(seconds * 2L));
        try {
            result.put("qrcodeImage", "data:image/png;base64," + QrcodeUtil.generateQrcodeBase64(content, 320, 320));
        } catch (Exception e) {
            throw new BusinessException("动态二维码生成失败");
        }
        return result;
    }

    public Map<String, Object> previewDynamicTicket(String ticket) {
        ParsedTicket parsed = parseTicket(ticket, "qr");
        Long meetingId = parsed.meetingId();
        Meeting meeting = ensureMeeting(meetingId);
        validateDynamicTicket(ticket, meetingId, getConfig(meetingId));
        Map<String, Object> result = meetingSummary(meeting);
        result.put("dynamicTicket", ticket);
        result.put("featureConfig", getConfig(meetingId));
        return result;
    }

    public Map<String, Object> issueOfflinePermit(Long meetingId, Long userId) {
        ensureMeeting(meetingId);
        Map<String, Object> config = getConfig(meetingId);
        if (!Boolean.TRUE.equals(config.get("offlineAllowed"))) {
            throw new BusinessException("该会议未开启弱网签到");
        }
        int minutes = (Integer) config.get("offlineMaxMinutes");
        long expires = LocalDateTime.now().plusMinutes(minutes).toEpochSecond(ZoneOffset.ofHours(8));
        String permit = signPayload("offline:" + meetingId + ":" + userId + ":" + expires);
        return Map.of(
                "meetingId", meetingId,
                "permit", permit,
                "expiresAt", LocalDateTime.now().plusMinutes(minutes),
                "maxMinutes", minutes
        );
    }

    public void validateRules(Meeting meeting, Long userId, CheckInRequest request) {
        Map<String, Object> config = getConfig(meeting.getId());
        if (Boolean.TRUE.equals(config.get("dynamicQrEnabled")) && "qrcode".equals(request.getSignMethod())) {
            validateDynamicTicket(request.getDynamicTicket(), meeting.getId(), config);
        }

        if (request.getOfflineSignedAt() != null || hasText(request.getOfflinePermit())) {
            validateOfflinePermit(meeting.getId(), userId, request, config);
        }

        if (Boolean.TRUE.equals(config.get("requirePhoto"))) {
            Object photo = request.getVerifyData() == null ? null : request.getVerifyData().get("photoData");
            if (photo == null || !hasText(String.valueOf(photo))) {
                throw new BusinessException("该会议要求同时提交现场照片");
            }
        }

        if (Boolean.TRUE.equals(config.get("requireLocation"))) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new BusinessException("该会议要求同时获取现场定位");
            }
            Double venueLat = doubleValue(config.get("venueLatitude"));
            Double venueLng = doubleValue(config.get("venueLongitude"));
            if (venueLat == null || venueLng == null) {
                throw new BusinessException("会议尚未配置会场坐标，请联系管理员");
            }
            double distance = distanceMeters(venueLat, venueLng, request.getLatitude(), request.getLongitude());
            if (distance > (Integer) config.get("radiusMeters")) {
                throw new BusinessException("当前位置距离会场约 " + Math.round(distance) + " 米，超出签到范围");
            }
        }
    }

    @Transactional
    public Map<String, Object> assessAndSaveRisk(CheckInRecord record, CheckInRequest request) {
        Map<String, Object> config = getConfig(record.getMeetingId());
        int score = 0;
        List<String> reasons = new ArrayList<>();
        String device = hasText(request.getDeviceId()) ? request.getDeviceId() : request.getDeviceInfo();

        if (!hasText(device)) {
            score += 15;
            reasons.add("缺少稳定设备标识");
        } else {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM check_in_record
                    WHERE meeting_id = ? AND user_id <> ? AND device_info = ?
                    """, Integer.class, record.getMeetingId(), record.getUserId(), device);
            if (count != null && count > 0) {
                score += 55;
                reasons.add("同一设备为多名参会者签到");
            }
        }

        if (hasText(request.getIpAddress())) {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM check_in_record
                    WHERE meeting_id = ? AND user_id <> ? AND ip_address = ?
                    """, Integer.class, record.getMeetingId(), record.getUserId(), request.getIpAddress());
            if (count != null && count >= 3) {
                score += 20;
                reasons.add("同一网络短时间内出现多账号签到");
            }
        }

        if (request.getOfflineSignedAt() != null) {
            long delay = Math.max(0, Duration.between(request.getOfflineSignedAt(), LocalDateTime.now()).toMinutes());
            score += delay > 30 ? 20 : 10;
            reasons.add("离线凭证延迟 " + delay + " 分钟后同步");
        }

        if (request.getLatitude() != null && request.getLongitude() != null) {
            score += addImpossibleTravelRisk(record, request, reasons);
            Double venueLat = doubleValue(config.get("venueLatitude"));
            Double venueLng = doubleValue(config.get("venueLongitude"));
            if (!Boolean.TRUE.equals(config.get("requireLocation")) && venueLat != null && venueLng != null) {
                double distance = distanceMeters(venueLat, venueLng, request.getLatitude(), request.getLongitude());
                if (distance > (Integer) config.get("radiusMeters")) {
                    score += 25;
                    reasons.add("签到位置偏离会场约 " + Math.round(distance) + " 米");
                }
            }
        }

        score = Math.min(score, 100);
        String level = score >= 60 ? "HIGH" : score >= 30 ? "MEDIUM" : "LOW";
        String reasonJson = json(reasons);
        jdbcTemplate.update("""
                INSERT INTO checkin_risk (record_id, meeting_id, user_id, risk_score, risk_level, reasons)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE risk_score=VALUES(risk_score), risk_level=VALUES(risk_level), reasons=VALUES(reasons)
                """, record.getId(), record.getMeetingId(), record.getUserId(), score, level, reasonJson);
        return Map.of("riskScore", score, "riskLevel", level, "riskReasons", reasons);
    }

    public List<Map<String, Object>> getRisks(Long meetingId, String level) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.id, r.record_id, r.meeting_id, r.user_id, r.risk_score, r.risk_level,
                       r.reasons, r.review_status, r.review_remark, r.create_time,
                       u.real_name, u.username, c.sign_time, c.sign_method, c.device_info, c.ip_address
                FROM checkin_risk r
                LEFT JOIN sys_user u ON u.id = r.user_id
                LEFT JOIN check_in_record c ON c.id = r.record_id
                WHERE r.meeting_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(meetingId);
        if (hasText(level)) {
            sql.append(" AND r.risk_level = ?");
            args.add(level.toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY r.risk_score DESC, r.create_time DESC, r.id DESC LIMIT 100");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("recordId", rs.getLong("record_id"));
            item.put("meetingId", rs.getLong("meeting_id"));
            item.put("userId", rs.getLong("user_id"));
            item.put("realName", rs.getString("real_name"));
            item.put("username", rs.getString("username"));
            item.put("riskScore", rs.getInt("risk_score"));
            item.put("riskLevel", rs.getString("risk_level"));
            item.put("reasons", parseStringList(rs.getString("reasons")));
            item.put("reviewStatus", rs.getInt("review_status"));
            item.put("reviewRemark", rs.getString("review_remark"));
            item.put("signTime", rs.getTimestamp("sign_time") == null ? null : rs.getTimestamp("sign_time").toLocalDateTime());
            item.put("signMethod", rs.getString("sign_method"));
            item.put("deviceInfo", rs.getString("device_info"));
            item.put("ipAddress", rs.getString("ip_address"));
            return item;
        }, args.toArray());
    }

    public void reviewRisk(Long riskId, Long reviewerId, Integer status, String remark) {
        int updated = jdbcTemplate.update("""
                UPDATE checkin_risk SET review_status=?, reviewer_id=?, review_remark=?, review_time=NOW()
                WHERE id=?
                """, status == null ? 1 : status, reviewerId, remark, riskId);
        if (updated == 0) {
            throw new BusinessException("风险记录不存在");
        }
    }

    public Map<String, Object> findOfflineReceipt(String clientRequestId, Long userId) {
        if (!hasText(clientRequestId)) {
            return null;
        }
        List<String> rows = jdbcTemplate.query(
                "SELECT result_json FROM offline_checkin_receipt WHERE client_request_id=? AND user_id=?",
                (rs, rowNum) -> rs.getString(1), clientRequestId, userId);
        if (rows.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(rows.get(0), new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }

    public void saveOfflineReceipt(CheckInRequest request, Long meetingId, Long userId,
                                   Long recordId, Map<String, Object> result) {
        if (!hasText(request.getClientRequestId())) {
            return;
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO offline_checkin_receipt
                    (client_request_id, meeting_id, user_id, record_id, result_json)
                    VALUES (?, ?, ?, ?, ?)
                    """, request.getClientRequestId(), meetingId, userId, recordId, json(result));
        } catch (DuplicateKeyException ignored) {
            log.debug("Offline receipt already exists: {}", request.getClientRequestId());
        }
    }

    private void validateDynamicTicket(String ticket, Long meetingId, Map<String, Object> config) {
        if (!hasText(ticket)) {
            throw new BusinessException("请扫描实时动态二维码");
        }
        ParsedTicket parsed = parseTicket(ticket, "qr");
        if (!meetingId.equals(parsed.meetingId())) {
            throw new BusinessException("动态二维码与会议不匹配");
        }
        int seconds = (Integer) config.get("qrRefreshSeconds");
        long currentSlot = System.currentTimeMillis() / 1000L / seconds;
        if (Math.abs(currentSlot - parsed.value()) > 1) {
            throw new BusinessException("动态二维码已刷新，请重新扫码");
        }
    }

    private void validateOfflinePermit(Long meetingId, Long userId, CheckInRequest request,
                                       Map<String, Object> config) {
        if (!Boolean.TRUE.equals(config.get("offlineAllowed"))) {
            throw new BusinessException("该会议未开启弱网签到");
        }
        if (!hasText(request.getOfflinePermit()) || request.getOfflineSignedAt() == null) {
            throw new BusinessException("离线签到凭证不完整");
        }
        ParsedTicket parsed = parseTicket(request.getOfflinePermit(), "offline");
        if (!meetingId.equals(parsed.meetingId()) || !userId.equals(parsed.userId())) {
            throw new BusinessException("离线签到凭证与当前用户不匹配");
        }
        long nowEpoch = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8));
        if (parsed.value() < nowEpoch) {
            throw new BusinessException("离线签到凭证已过期");
        }
        long delay = Math.abs(Duration.between(request.getOfflineSignedAt(), LocalDateTime.now()).toMinutes());
        if (delay > (Integer) config.get("offlineMaxMinutes")) {
            throw new BusinessException("离线签到超过允许同步时限");
        }
    }

    private ParsedTicket parseTicket(String ticket, String expectedType) {
        try {
            String[] parts = ticket.split("\\.");
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }
            String payload = new String(URL_DECODER.decode(parts[0]), StandardCharsets.UTF_8);
            byte[] actual = URL_DECODER.decode(parts[1]);
            byte[] expected = hmac(payload);
            if (!MessageDigest.isEqual(actual, expected)) {
                throw new IllegalArgumentException();
            }
            String[] values = payload.split(":");
            if (!expectedType.equals(values[0])) {
                throw new IllegalArgumentException();
            }
            if ("offline".equals(expectedType)) {
                return new ParsedTicket(values[0], Long.valueOf(values[1]), Long.valueOf(values[2]), Long.parseLong(values[3]));
            }
            return new ParsedTicket(values[0], Long.valueOf(values[1]), null, Long.parseLong(values[2]));
        } catch (Exception e) {
            throw new BusinessException("签到票据无效或已被篡改");
        }
    }

    private String signPayload(String payload) {
        return URL_ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." +
                URL_ENCODER.encodeToString(hmac(payload));
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign innovation ticket", e);
        }
    }

    private int addImpossibleTravelRisk(CheckInRecord record, CheckInRequest request, List<String> reasons) {
        List<Map<String, Object>> previous = jdbcTemplate.queryForList("""
                SELECT latitude, longitude, sign_time FROM check_in_record
                WHERE user_id=? AND id<>? AND latitude IS NOT NULL AND longitude IS NOT NULL
                ORDER BY sign_time DESC LIMIT 1
                """, record.getUserId(), record.getId());
        if (previous.isEmpty()) {
            return 0;
        }
        Map<String, Object> row = previous.get(0);
        LocalDateTime signTime = ((java.sql.Timestamp) row.get("sign_time")).toLocalDateTime();
        long minutes = Math.abs(Duration.between(signTime, record.getSignTime()).toMinutes());
        double distance = distanceMeters(
                ((Number) row.get("latitude")).doubleValue(),
                ((Number) row.get("longitude")).doubleValue(),
                request.getLatitude(), request.getLongitude());
        if (minutes <= 120 && distance >= 50_000) {
            reasons.add("两小时内签到位置跨越约 " + Math.round(distance / 1000) + " 公里");
            return 40;
        }
        return 0;
    }

    private Meeting ensureMeeting(Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        return meeting;
    }

    private Map<String, Object> meetingSummary(Meeting meeting) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("meetingId", meeting.getId());
        result.put("title", meeting.getTitle());
        result.put("description", meeting.getDescription());
        result.put("location", meeting.getLocation());
        result.put("startTime", meeting.getStartTime());
        result.put("endTime", meeting.getEndTime());
        result.put("checkinStartTime", meeting.getCheckinStartTime());
        result.put("checkinEndTime", meeting.getCheckinEndTime());
        result.put("signMethods", meeting.getSignMethods());
        result.put("status", meeting.getStatus());
        return result;
    }

    private Map<String, Object> defaultConfig(Long meetingId) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("meetingId", meetingId);
        config.put("dynamicQrEnabled", false);
        config.put("qrRefreshSeconds", 20);
        config.put("requireLocation", false);
        config.put("requirePhoto", false);
        config.put("venueLatitude", null);
        config.put("venueLongitude", null);
        config.put("radiusMeters", 300);
        config.put("offlineAllowed", true);
        config.put("offlineMaxMinutes", 120);
        config.put("reminderEnabled", true);
        config.put("reminderMinutes", 30);
        return config;
    }

    private List<String> parseStringList(String value) {
        if (!hasText(value)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of(value);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    private boolean bool(Object value) {
        return value instanceof Boolean b ? b : value != null && ((Number) value).intValue() == 1;
    }

    private int flag(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value)) ? 1 : 0;
    }

    private int flagDefault(Object value, boolean defaultValue) {
        return value == null ? (defaultValue ? 1 : 0) : flag(value);
    }

    private int intValue(Object value, int defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) return defaultValue;
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private Double decimal(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : Double.valueOf(String.valueOf(value));
    }

    private Double doubleValue(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6_371_000;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private record ParsedTicket(String type, Long meetingId, Long userId, long value) {}
}

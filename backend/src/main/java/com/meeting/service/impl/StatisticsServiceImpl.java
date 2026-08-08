package com.meeting.service.impl;

import com.meeting.entity.CheckInRecord;
import com.meeting.entity.Meeting;
import com.meeting.entity.MeetingAttendee;
import com.meeting.mapper.CheckInRecordMapper;
import com.meeting.mapper.GroupMemberMapper;
import com.meeting.mapper.MeetingAttendeeMapper;
import com.meeting.mapper.MeetingMapper;
import com.meeting.mapper.SysUserMapper;
import com.meeting.entity.SysUser;
import com.meeting.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final MeetingMapper meetingMapper;
    private final MeetingAttendeeMapper attendeeMapper;
    private final CheckInRecordMapper recordMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final SysUserMapper userMapper;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取总会议数
            Long totalMeetings = meetingMapper.selectCount(null);
            result.put("totalMeetings", totalMeetings);
            
            // 获取进行中会议数
            Long ongoingMeetings = meetingMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Meeting>()
                    .eq("status", 1)
            );
            result.put("ongoingMeetings", ongoingMeetings);
            
            // 获取总用户数
            Long totalUsers = userMapper.selectCount(null);
            result.put("totalUsers", totalUsers);
            
            // 获取今日签到数
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            Map<String, Object> todayStats = recordMapper.statisticsByTimeRange(todayStart, LocalDateTime.now());
            result.put("todaySignCount", todayStats.get("totalSignCount") != null ? todayStats.get("totalSignCount") : 0);
            result.put("todayAttendanceRate", todayStats.get("attendanceRate") != null ? todayStats.get("attendanceRate") : 0);
            
            // 总签到数
            Map<String, Object> totalStats = recordMapper.statisticsByTimeRange(
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.now().plusYears(1)
            );
            result.put("totalSignCount", totalStats.get("totalSignCount") != null ? totalStats.get("totalSignCount") : 0);
            result.put("totalCheckins", totalStats.get("totalSignCount") != null ? totalStats.get("totalSignCount") : 0);
            
            // 总出勤率
            result.put("attendanceRate", totalStats.get("attendanceRate") != null ? totalStats.get("attendanceRate") : 0);
            
        } catch (Exception e) {
            log.error("获取概览统计失败", e);
            result.put("totalMeetings", 0);
            result.put("ongoingMeetings", 0);
            result.put("totalUsers", 0);
            result.put("todaySignCount", 0);
            result.put("todayAttendanceRate", 0);
            result.put("totalSignCount", 0);
            result.put("totalCheckins", 0);
            result.put("attendanceRate", 0);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getMeetingStats(Long meetingId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Meeting meeting = meetingMapper.selectById(meetingId);
            if (meeting == null) {
                result.put("meetingId", meetingId);
                result.put("meetingTitle", "未知会议");
                result.put("totalCount", 0);
                result.put("signedCount", 0);
                result.put("lateCount", 0);
                result.put("makeupCount", 0);
                result.put("proxyCount", 0);
                result.put("absentCount", 0);
                result.put("attendanceRate", 0);
                return result;
            }
            
            // 获取参会人员列表
            List<MeetingAttendee> attendees = attendeeMapper.findByMeetingIdWithUser(meetingId);
            
            int total = attendees.size();
            // 如果会议关联群聊，总人数以群成员数为准
            if (meeting.getGroupId() != null) {
                int memberCount = groupMemberMapper.countMembers(meeting.getGroupId());
                if (memberCount > total) {
                    total = memberCount;
                }
            }
            
            int signed = 0;
            int late = 0;
            int makeup = 0;
            int proxy = 0;
            int absent = 0;
            
            for (MeetingAttendee attendee : attendees) {
                switch (attendee.getStatus()) {
                    case 1: signed++; break;
                    case 2: late++; break;
                    case 3: absent++; break;
                    default: break;
                }
                // 检查签到方式是否为补签或代签
                String method = attendee.getSignMethod();
                if (method != null) {
                    if (method.equals("makeup")) {
                        makeup++;
                    }
                    if (method.equals("proxy")) {
                        proxy++;
                    }
                }
            }
            
            double attendanceRate = total > 0 ? (double) (signed + late) / total * 100 : 0;
            
            result.put("meetingId", meetingId);
            result.put("meetingTitle", meeting.getTitle());
            result.put("totalCount", total);
            result.put("signedCount", signed);
            result.put("lateCount", late);
            result.put("makeupCount", makeup);
            result.put("proxyCount", proxy);
            result.put("absentCount", absent);
            result.put("attendanceRate", Math.round(attendanceRate * 100.0) / 100.0);
            
            // 获取签到方式分布
            Map<String, Integer> methodDistribution = new HashMap<>();
            methodDistribution.put("qrcode", 0);
            methodDistribution.put("makeup", 0);
            methodDistribution.put("proxy", 0);
            methodDistribution.put("photo", 0);
            methodDistribution.put("gesture", 0);
            methodDistribution.put("location", 0);
            
            for (MeetingAttendee attendee : attendees) {
                if (attendee.getSignMethod() != null) {
                    String method = attendee.getSignMethod();
                    if (methodDistribution.containsKey(method)) {
                        methodDistribution.put(method, methodDistribution.get(method) + 1);
                    }
                }
            }
            result.put("methodDistribution", methodDistribution);
            
        } catch (Exception e) {
            log.error("获取会议统计失败: meetingId={}", meetingId, e);
            result.put("meetingId", meetingId);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getMeetingStatistics(Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            throw new com.meeting.common.exception.BusinessException("会议不存在");
        }

        // 获取参会人员统计
        Map<String, Object> statusCount = attendeeMapper.countByMeetingId(meetingId);
        
        // 获取签到记录统计
        Map<String, Object> signCount = recordMapper.statisticsByMeetingId(meetingId);

        // 如果会议关联群聊，应到人数以群成员数为准
        Object totalCountObj = statusCount.get("totalCount");
        int totalCount = totalCountObj != null ? ((Number) totalCountObj).intValue() : 0;
        
        if (meeting.getGroupId() != null) {
            int memberCount = groupMemberMapper.countMembers(meeting.getGroupId());
            if (memberCount > totalCount) {
                totalCount = memberCount;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("meetingId", meetingId);
        result.put("meetingTitle", meeting.getTitle());
        result.put("totalCount", totalCount);
        result.put("signedCount", statusCount.get("signedCount"));
        result.put("lateCount", statusCount.get("lateCount"));
        result.put("notSignedCount", statusCount.get("notSignedCount"));
        result.put("attendanceRate", signCount.get("attendanceRate"));

        return result;
    }

    @Override
    public Map<String, Object> getStatusDistribution(Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        List<MeetingAttendee> attendees = attendeeMapper.findByMeetingIdWithUser(meetingId);
        
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("signed", 0);
        distribution.put("late", 0);
        distribution.put("notSigned", 0);
        distribution.put("absent", 0);

        for (MeetingAttendee attendee : attendees) {
            switch (attendee.getStatus()) {
                case 1: distribution.put("signed", distribution.get("signed") + 1); break;
                case 2: distribution.put("late", distribution.get("late") + 1); break;
                case 3: distribution.put("absent", distribution.get("absent") + 1); break;
                default: distribution.put("notSigned", distribution.get("notSigned") + 1);
            }
        }

        int total = attendees.size();
        // 如果会议关联群聊，总人数以群成员数为准
        if (meeting != null && meeting.getGroupId() != null) {
            int memberCount = groupMemberMapper.countMembers(meeting.getGroupId());
            if (memberCount > total) {
                // 将未签到的空缺填充到 notSigned
                int diff = memberCount - total;
                distribution.put("notSigned", distribution.get("notSigned") + diff);
                total = memberCount;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("meetingId", meetingId);
        result.put("distribution", distribution);
        result.put("total", total);

        return result;
    }

    @Override
    public Map<String, Object> getMakeUpStatistics(Long meetingId) {
        Map<String, Object> result = new HashMap<>();
        result.put("meetingId", meetingId);
        result.put("makeupCount", 0);
        result.put("pendingCount", 0);
        result.put("approvedCount", 0);

        return result;
    }

    @Override
    public Map<String, Object> getProxyStatistics(Long meetingId) {
        Map<String, Object> result = new HashMap<>();
        result.put("meetingId", meetingId);
        result.put("proxyCount", 0);
        result.put("proxyUsers", new ArrayList<>());

        return result;
    }

    @Override
    public Map<String, Object> getUserStatistics(Long userId) {
        Map<String, Object> statistics = recordMapper.statisticsByUserId(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("totalMeetings", statistics.get("totalMeetings"));
        result.put("signedCount", statistics.get("signedCount"));
        result.put("lateCount", statistics.get("lateCount"));
        result.put("absenceCount", statistics.get("absenceCount"));
        result.put("attendanceRate", statistics.get("attendanceRate"));

        return result;
    }

    @Override
    public Map<String, Object> getTimeRangeStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> statistics = recordMapper.statisticsByTimeRange(startTime, endTime);

        Map<String, Object> result = new HashMap<>();
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("totalSignCount", statistics.get("totalSignCount"));
        result.put("signedCount", statistics.get("signedCount"));
        result.put("lateCount", statistics.get("lateCount"));
        result.put("attendanceRate", statistics.get("attendanceRate"));

        return result;
    }

    @Override
    public List<Map<String, Object>> getDeptStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        return result;
    }

    @Override
    public byte[] exportMeetingReport(Long meetingId, String format) {
        List<Map<String, Object>> reportData = new ArrayList<>();
        
        List<MeetingAttendee> attendees = attendeeMapper.findByMeetingIdWithUser(meetingId);
        
        StringBuilder csv = new StringBuilder();
        csv.append("参会人姓名,部门,职位,签到状态,签到时间,签到方式\n");
        
        for (MeetingAttendee attendee : attendees) {
            String statusText = switch (attendee.getStatus()) {
                case 1 -> "已签到";
                case 2 -> "迟到";
                case 3 -> "缺勤";
                default -> "未签到";
            };
            
            String signTime = attendee.getSignTime() != null 
                    ? attendee.getSignTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) 
                    : "-";
            
            csv.append(String.format("%s,%s,%s,%s,%s,%s\n",
                    attendee.getUser() != null ? attendee.getUser().getRealName() : "-",
                    attendee.getUser() != null && attendee.getUser().getDeptId() != null ? "部门" + attendee.getUser().getDeptId() : "-",
                    attendee.getUser() != null && attendee.getUser().getPosition() != null ? attendee.getUser().getPosition() : "-",
                    statusText,
                    signTime,
                    attendee.getSignMethod() != null ? attendee.getSignMethod() : "-"
            ));
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public List<Map<String, Object>> getMeetingCheckInLocations(Long meetingId) {
        List<CheckInRecord> records = recordMapper.findByMeetingId(meetingId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CheckInRecord record : records) {
            // 只返回有经纬度的定位签到记录
            if (record.getLatitude() == null || record.getLongitude() == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("userId", record.getUserId());
            item.put("signTime", record.getSignTime());
            item.put("signStatus", record.getSignStatus());
            item.put("latitude", record.getLatitude());
            item.put("longitude", record.getLongitude());
            item.put("location", record.getLocation());
            item.put("signMethod", record.getSignMethod());

            SysUser user = userMapper.selectById(record.getUserId());
            if (user != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("realName", user.getRealName());
                userMap.put("username", user.getUsername());
                userMap.put("avatar", user.getAvatar());
                item.put("user", userMap);
            }
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMeetingCheckInPhotos(Long meetingId) {
        // 使用专用查询：仅查 photo 方式记录，含 verify_data
        List<CheckInRecord> records = recordMapper.findPhotoRecordsByMeetingId(meetingId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CheckInRecord record : records) {
            Map<String, Object> verifyData = record.getVerifyData();
            if (verifyData == null || !verifyData.containsKey("photoData")) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("userId", record.getUserId());
            item.put("signTime", record.getSignTime());
            item.put("signStatus", record.getSignStatus());
            item.put("signMethod", record.getSignMethod());
            item.put("photoData", verifyData.get("photoData"));

            SysUser user = userMapper.selectById(record.getUserId());
            if (user != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("realName", user.getRealName());
                userMap.put("username", user.getUsername());
                userMap.put("avatar", user.getAvatar());
                userMap.put("position", user.getPosition());
                item.put("user", userMap);
            }
            result.add(item);
        }
        return result;
    }
}

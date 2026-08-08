package com.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meeting.common.exception.BusinessException;
import com.meeting.config.JwtConfig;
import com.meeting.dto.MeetingCreateRequest;
import com.meeting.entity.CheckInRecord;
import com.meeting.entity.GroupMember;
import com.meeting.entity.Meeting;
import com.meeting.entity.MeetingAttendee;
import com.meeting.mapper.CheckInRecordMapper;
import com.meeting.mapper.GroupMemberMapper;
import com.meeting.mapper.MeetingAttendeeMapper;
import com.meeting.mapper.MeetingMapper;
import com.meeting.service.MeetingService;
import com.meeting.utils.QrcodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 会议服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingMapper meetingMapper;
    private final MeetingAttendeeMapper attendeeMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final CheckInRecordMapper checkInRecordMapper;
    private final JwtConfig jwtConfig;

    @Value("${app.qrcode.default-expire:120}")
    private int defaultExpire;

    @Value("${app.qrcode.default-width:300}")
    private int defaultWidth;

    @Value("${app.qrcode.default-height:300}")
    private int defaultHeight;

    @Value("${app.storage.path:./uploads}")
    private String storagePath;

    @Override
    @Transactional
    public Meeting createMeeting(Long creatorId, MeetingCreateRequest request) {
        // 验证时间
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessException("会议开始时间必须早于结束时间");
        }
        
        // 如果未设置签到时间，自动根据会议时间生成
        LocalDateTime checkinStartTime = request.getCheckinStartTime();
        LocalDateTime checkinEndTime = request.getCheckinEndTime();
        if (checkinStartTime == null) {
            // 默认签到开始时间 = 会议开始时间前30分钟
            checkinStartTime = request.getStartTime().minusMinutes(30);
        }
        if (checkinEndTime == null) {
            // 默认签到截止时间 = 会议开始时间后15分钟
            checkinEndTime = request.getStartTime().plusMinutes(15);
        }
        // 确保签到截止时间不早于签到开始时间
        if (!checkinStartTime.isBefore(checkinEndTime)) {
            checkinEndTime = checkinStartTime.plusMinutes(30);
        }

        // 创建会议
        Meeting meeting = new Meeting();
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setLocation(request.getLocation());
        meeting.setStartTime(request.getStartTime());
        meeting.setEndTime(request.getEndTime());
        meeting.setCheckinStartTime(checkinStartTime);
        meeting.setCheckinEndTime(checkinEndTime);
        meeting.setLateTime(request.getLateTime() != null ? request.getLateTime() : 15);
        meeting.setStatus(0); // 草稿状态
        meeting.setCreatorId(creatorId);

        // 设置所属群聊
        if (request.getGroupId() != null) {
            meeting.setGroupId(request.getGroupId());
        }

        // 设置支持的签到方式
        List<String> signMethods = request.getSignMethods();
        if (signMethods == null || signMethods.isEmpty()) {
            signMethods = Arrays.asList("qrcode");
        }
        meeting.setSignMethods(signMethods);

        // 设置手势签到密码（仅当支持手势签到时）
        if (signMethods.contains("gesture")) {
            String gesturePwd = request.getGesturePassword();
            if (gesturePwd == null || gesturePwd.trim().isEmpty()) {
                throw new BusinessException("手势签到方式需要设置手势密码");
            }
            meeting.setGesturePassword(gesturePwd.trim());
        }

        // 生成二维码Token
        String token = QrcodeUtil.generateToken();
        meeting.setQrcodeToken(token);

        meetingMapper.insert(meeting);
        log.info("创建会议成功: id={}, title={}", meeting.getId(), meeting.getTitle());

        // 添加参会人员
        List<Long> finalAttendeeIds = new ArrayList<>();

        // 如果有指定参会人员，优先使用
        if (request.getAttendeeIds() != null && !request.getAttendeeIds().isEmpty()) {
            finalAttendeeIds.addAll(request.getAttendeeIds());
        }
        // 如果会议属于群聊，自动添加所有群成员
        if (request.getGroupId() != null) {
            List<GroupMember> members = groupMemberMapper.findMembersByGroupId(request.getGroupId());
            for (GroupMember m : members) {
                if (!finalAttendeeIds.contains(m.getUserId())) {
                    finalAttendeeIds.add(m.getUserId());
                }
            }
        }

        if (!finalAttendeeIds.isEmpty()) {
            for (Long userId : finalAttendeeIds) {
                MeetingAttendee attendee = new MeetingAttendee();
                attendee.setMeetingId(meeting.getId());
                attendee.setUserId(userId);
                attendee.setStatus(0); // 未签到
                attendeeMapper.insert(attendee);
            }
        }

        return meeting;
    }

    @Override
    @Transactional
    public Meeting updateMeeting(Long id, MeetingCreateRequest request) {
        Meeting meeting = meetingMapper.selectById(id);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }

        if (meeting.getStatus() >= 2) {
            throw new BusinessException("会议已开始或结束，无法修改");
        }

        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setLocation(request.getLocation());
        meeting.setStartTime(request.getStartTime());
        meeting.setEndTime(request.getEndTime());

        // 签到时间处理：如果未设置，自动根据会议时间生成
        LocalDateTime checkinStartTime = request.getCheckinStartTime();
        LocalDateTime checkinEndTime = request.getCheckinEndTime();
        if (checkinStartTime == null) {
            checkinStartTime = request.getStartTime().minusMinutes(30);
        }
        if (checkinEndTime == null) {
            checkinEndTime = request.getStartTime().plusMinutes(15);
        }
        if (!checkinStartTime.isBefore(checkinEndTime)) {
            checkinEndTime = checkinStartTime.plusMinutes(30);
        }
        meeting.setCheckinStartTime(checkinStartTime);
        meeting.setCheckinEndTime(checkinEndTime);
        if (request.getLateTime() != null) {
            meeting.setLateTime(request.getLateTime());
        }
        if (request.getSignMethods() != null) {
            meeting.setSignMethods(request.getSignMethods());
            // 同步更新手势密码
            if (request.getSignMethods().contains("gesture")) {
                if (request.getGesturePassword() != null && !request.getGesturePassword().trim().isEmpty()) {
                    meeting.setGesturePassword(request.getGesturePassword().trim());
                }
            } else {
                // 不再支持手势签到时清空密码
                meeting.setGesturePassword(null);
            }
        }

        meetingMapper.updateById(meeting);
        log.info("更新会议成功: id={}", id);

        // 更新参会人员
        if (request.getAttendeeIds() != null) {
            // 先删除旧的参会人员
            attendeeMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MeetingAttendee>()
                            .eq("meeting_id", id)
            );
            // 添加新的参会人员
            for (Long userId : request.getAttendeeIds()) {
                MeetingAttendee attendee = new MeetingAttendee();
                attendee.setMeetingId(id);
                attendee.setUserId(userId);
                attendee.setStatus(0);
                attendeeMapper.insert(attendee);
            }
        }

        return meeting;
    }

    @Override
    public void deleteMeeting(Long id) {
        Meeting meeting = meetingMapper.selectById(id);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        meetingMapper.deleteById(id);
        log.info("删除会议成功: id={}", id);
    }

    @Override
    public Meeting getMeetingById(Long id) {
        Meeting meeting = meetingMapper.selectById(id);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        return meeting;
    }

    @Override
    public Page<Meeting> getMeetingList(int current, int size, Map<String, Object> params) {
        Page<Meeting> page = new Page<>(current, size);
        return meetingMapper.selectPageList(page, params);
    }

    @Override
    public Map<String, Object> generateQrcode(Long meetingId, Long userId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }

        // 检查权限：只有创建人、管理员、会议领导可以生成二维码
        if (!meeting.getCreatorId().equals(userId)) {
            MeetingAttendee attendee = attendeeMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MeetingAttendee>()
                            .eq("meeting_id", meetingId)
                            .eq("user_id", userId)
            );
            if (attendee == null) {
                throw new BusinessException(403, "无权限生成二维码");
            }
        }

        // 设置二维码过期时间
        int expireMinutes = defaultExpire;
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(expireMinutes);
        meeting.setQrcodeExpire(expireTime);
        meetingMapper.updateById(meeting);

        // 生成二维码
        String qrcodeContent = QrcodeUtil.generateMeetingQrcodeContent(meeting.getQrcodeToken());
        Map<String, Object> result = new HashMap<>();
        result.put("token", meeting.getQrcodeToken());
        result.put("qrcodeContent", qrcodeContent);
        result.put("expireTime", expireTime);
        result.put("expireMinutes", expireMinutes);

        try {
            // 生成Base64二维码图片
            String base64Image = QrcodeUtil.generateQrcodeBase64(qrcodeContent, defaultWidth, defaultHeight);
            result.put("qrcodeImage", "data:image/png;base64," + base64Image);
            result.put("qrcodeWidth", defaultWidth);
            result.put("qrcodeHeight", defaultHeight);
        } catch (Exception e) {
            log.error("生成二维码失败", e);
            throw new BusinessException("生成二维码失败");
        }

        return result;
    }

    @Override
    public Map<String, Object> getMeetingByQrcode(String token) {
        Meeting meeting = meetingMapper.findByQrcodeToken(token);
        if (meeting == null) {
            throw new BusinessException("二维码无效");
        }

        // 检查二维码是否过期
        if (meeting.getQrcodeExpire() != null && meeting.getQrcodeExpire().isBefore(LocalDateTime.now())) {
            throw new BusinessException("二维码已过期");
        }

        // 检查会议状态
        if (meeting.getStatus() == 0) {
            throw new BusinessException("会议未发布");
        }

        Map<String, Object> result = new HashMap<>();
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

    @Override
    public Meeting publishMeeting(Long id) {
        Meeting meeting = meetingMapper.selectById(id);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        meeting.setStatus(1);
        meetingMapper.updateById(meeting);
        log.info("发布会议成功: id={}", id);
        return meeting;
    }

    @Override
    public Meeting startMeeting(Long id) {
        Meeting meeting = meetingMapper.selectById(id);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        meeting.setStatus(2);
        meetingMapper.updateById(meeting);
        log.info("开始会议: id={}", id);
        return meeting;
    }

    @Override
    public Meeting endMeeting(Long id) {
        Meeting meeting = meetingMapper.selectById(id);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        meeting.setStatus(3);
        meetingMapper.updateById(meeting);
        log.info("结束会议: id={}", id);
        return meeting;
    }

    @Override
    @Transactional
    public int clearMeetingsByGroupId(Long groupId) {
        // 查询该群聊下的所有会议
        List<Meeting> meetings = meetingMapper.selectList(
                new QueryWrapper<Meeting>().eq("group_id", groupId)
        );
        if (meetings.isEmpty()) {
            return 0;
        }
        List<Long> meetingIds = meetings.stream().map(Meeting::getId).toList();
        int deleted = 0;
        for (Long meetingId : meetingIds) {
            // 删除签到记录
            checkInRecordMapper.delete(new QueryWrapper<CheckInRecord>().eq("meeting_id", meetingId));
            // 删除参会人员
            attendeeMapper.delete(new QueryWrapper<MeetingAttendee>().eq("meeting_id", meetingId));
            // 删除会议
            meetingMapper.deleteById(meetingId);
            deleted++;
        }
        log.info("清空群聊会议记录: groupId={}, 删除{}个会议", groupId, deleted);
        return deleted;
    }
}

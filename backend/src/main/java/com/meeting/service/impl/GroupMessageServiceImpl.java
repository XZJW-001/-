package com.meeting.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeting.common.exception.BusinessException;
import com.meeting.entity.GroupMessage;
import com.meeting.entity.Meeting;
import com.meeting.entity.SysUser;
import com.meeting.mapper.GroupMessageMapper;
import com.meeting.mapper.MeetingMapper;
import com.meeting.mapper.SysUserMapper;
import com.meeting.service.GroupMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMessageServiceImpl implements GroupMessageService {

    private final GroupMessageMapper messageMapper;
    private final MeetingMapper meetingMapper;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public GroupMessage sendMessage(Long groupId, Long userId, String type, String content, String extra) {
        GroupMessage msg = new GroupMessage();
        msg.setGroupId(groupId);
        msg.setUserId(userId);
        msg.setType(type);
        msg.setContent(content);
        msg.setExtra(extra);
        messageMapper.insert(msg);

        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            msg.setUser(user);
        }
        log.info("发送群消息: groupId={}, userId={}, type={}", groupId, userId, type);
        return msg;
    }

    @Override
    public List<Map<String, Object>> getMessages(Long groupId, Integer limit) {
        if (limit == null || limit <= 0) limit = 100;
        List<GroupMessage> messages = messageMapper.findMessagesWithUser(groupId, limit);
        // 反转按时间升序排列
        Collections.reverse(messages);
        return convertToMapList(messages);
    }

    @Override
    public List<Map<String, Object>> getLatestMessages(Long groupId, Long lastId) {
        List<GroupMessage> messages = messageMapper.findLatestMessages(groupId, lastId);
        return convertToMapList(messages);
    }

    @Override
    public GroupMessage sendMeetingCard(Long groupId, Long userId, Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        Map<String, Object> extraMap = new HashMap<>();
        extraMap.put("meetingId", meeting.getId());
        extraMap.put("title", meeting.getTitle());
        extraMap.put("startTime", formatLdt(meeting.getStartTime()));
        extraMap.put("location", meeting.getLocation());
        extraMap.put("status", meeting.getStatus());
        extraMap.put("qrcodeToken", meeting.getQrcodeToken());
        String extra;
        try {
            extra = objectMapper.writeValueAsString(extraMap);
        } catch (JsonProcessingException e) {
            extra = "{}";
        }
        return sendMessage(groupId, userId, "meeting",
                String.format("发起了会议：%s", meeting.getTitle()), extra);
    }

    @Override
    public GroupMessage sendCheckinCard(Long groupId, Long userId, Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        Map<String, Object> extraMap = new HashMap<>();
        extraMap.put("meetingId", meeting.getId());
        extraMap.put("qrcodeToken", meeting.getQrcodeToken());
        extraMap.put("title", meeting.getTitle());
        extraMap.put("startTime", formatLdt(meeting.getStartTime()));
        extraMap.put("endTime", formatLdt(meeting.getEndTime()));
        extraMap.put("checkinStartTime", formatLdt(meeting.getCheckinStartTime()));
        extraMap.put("checkinEndTime", formatLdt(meeting.getCheckinEndTime()));
        extraMap.put("status", meeting.getStatus());
        extraMap.put("location", meeting.getLocation());
        extraMap.put("signMethods", meeting.getSignMethods());
        String extra;
        try {
            extra = objectMapper.writeValueAsString(extraMap);
        } catch (JsonProcessingException e) {
            extra = "{}";
        }
        return sendMessage(groupId, userId, "checkin",
                String.format("发布签到：%s", meeting.getTitle()), extra);
    }

    private String formatLdt(java.time.LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public void sendSystemMessage(Long groupId, String content) {
        sendMessage(groupId, 0L, "system", content, null);
    }

    @Override
    public int clearMessages(Long groupId) {
        int deleted = messageMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<GroupMessage>()
                        .eq("group_id", groupId)
        );
        log.info("清空群聊消息: groupId={}, 删除{}条", groupId, deleted);
        return deleted;
    }

    private List<Map<String, Object>> convertToMapList(List<GroupMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupMessage msg : messages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", msg.getId());
            item.put("groupId", msg.getGroupId());
            item.put("userId", msg.getUserId());
            item.put("type", msg.getType());
            item.put("content", msg.getContent());
            item.put("extra", msg.getExtra());
            item.put("createTime", msg.getCreateTime());

            if (msg.getUser() != null) {
                item.put("userName", msg.getUser().getRealName());
                item.put("userAvatar", msg.getUser().getAvatar());
                item.put("userType", msg.getUser().getUserType());
            }
            result.add(item);
        }
        return result;
    }
}

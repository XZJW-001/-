package com.meeting.service;

import com.meeting.entity.GroupMessage;

import java.util.List;
import java.util.Map;

/**
 * 群聊消息服务接口
 */
public interface GroupMessageService {

    /**
     * 发送消息
     */
    GroupMessage sendMessage(Long groupId, Long userId, String type, String content, String extra);

    /**
     * 获取群消息列表
     */
    List<Map<String, Object>> getMessages(Long groupId, Integer limit);

    /**
     * 获取最新消息
     */
    List<Map<String, Object>> getLatestMessages(Long groupId, Long lastId);

    /**
     * 发送会议卡片
     */
    GroupMessage sendMeetingCard(Long groupId, Long userId, Long meetingId);

    /**
     * 发送签到卡片
     */
    GroupMessage sendCheckinCard(Long groupId, Long userId, Long meetingId);

    /**
     * 发送系统消息
     */
    void sendSystemMessage(Long groupId, String content);

    /**
     * 清空群聊消息
     */
    int clearMessages(Long groupId);
}

package com.meeting.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meeting.dto.MeetingCreateRequest;
import com.meeting.entity.Meeting;

import java.util.Map;

/**
 * 会议服务接口
 */
public interface MeetingService {

    /**
     * 创建会议
     */
    Meeting createMeeting(Long creatorId, MeetingCreateRequest request);

    /**
     * 更新会议
     */
    Meeting updateMeeting(Long id, MeetingCreateRequest request);

    /**
     * 删除会议
     */
    void deleteMeeting(Long id);

    /**
     * 获取会议详情
     */
    Meeting getMeetingById(Long id);

    /**
     * 分页查询会议列表
     */
    Page<Meeting> getMeetingList(int current, int size, Map<String, Object> params);

    /**
     * 生成会议二维码
     */
    Map<String, Object> generateQrcode(Long meetingId, Long userId);

    /**
     * 根据二维码Token获取会议信息
     */
    Map<String, Object> getMeetingByQrcode(String token);

    /**
     * 发布会议
     */
    Meeting publishMeeting(Long id);

    /**
     * 开始会议
     */
    Meeting startMeeting(Long id);

    /**
     * 结束会议
     */
    Meeting endMeeting(Long id);

    /**
     * 清空群聊下的所有会议（包括参会人员和签到记录）
     */
    int clearMeetingsByGroupId(Long groupId);
}

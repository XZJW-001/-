package com.meeting.service;

import com.meeting.dto.CheckInRequest;
import com.meeting.dto.MakeUpApplyRequest;
import com.meeting.dto.ProxySignApplyRequest;
import com.meeting.dto.ProxySignRequest;

import java.util.List;
import java.util.Map;

/**
 * 签到服务接口
 */
public interface CheckInService {

    /**
     * 用户签到
     */
    Map<String, Object> checkIn(Long meetingId, Long userId, CheckInRequest request);

    /**
     * 管理员直接签到（不检查迟到）
     */
    Map<String, Object> adminCheckIn(Long meetingId, Long userId);

    /**
     * 获取签到记录（按会议）
     */
    List<Map<String, Object>> getCheckInRecordsByMeeting(Long meetingId);

    /**
     * 获取签到记录（按用户）
     */
    List<Map<String, Object>> getCheckInRecordsByUser(Long userId);

    /**
     * 申请补签
     */
    Map<String, Object> applyMakeUp(Long meetingId, Long userId, MakeUpApplyRequest request);

    /**
     * 审批补签
     */
    Map<String, Object> approveMakeUp(Long applyId, Long approverId, Integer status, String remark);

    /**
     * 获取补签申请列表
     */
    List<Map<String, Object>> getMakeUpList(Long meetingId, Integer status);

    /**
     * 获取所有补签申请列表（跨会议）
     */
    List<Map<String, Object>> getAllMakeUpList(Integer status);

    Map<String, Object> applyProxySign(Long meetingId, Long applicantId, ProxySignApplyRequest request);

    Map<String, Object> approveProxySignApply(Long applyId, Long approverId, Integer status, String remark);

    void cancelProxySignApply(Long applyId, Long applicantId);

    List<Map<String, Object>> getMyProxySignApplications(Long applicantId);

    List<Map<String, Object>> getProxySignApplications(Long meetingId, Integer status);

    List<Map<String, Object>> getAllProxySignApplications(Integer status);

    List<Map<String, Object>> getProxyCandidates(Long meetingId, Long applicantId);

    List<Map<String, Object>> getMyProxyEligibleMeetings(Long applicantId);

    /**
     * 代签
     */
    Map<String, Object> proxySign(Long meetingId, Long proxyUserId, ProxySignRequest request);

    /**
     * 获取代签记录
     */
    List<Map<String, Object>> getProxySignList(Long meetingId);

    /**
     * 获取所有代签记录（跨会议）
     */
    List<Map<String, Object>> getAllProxySignList();

    /**
     * 扫码查看签到情况
     */
    Map<String, Object> getMeetingCheckInStatus(Long meetingId, Long userId);
}

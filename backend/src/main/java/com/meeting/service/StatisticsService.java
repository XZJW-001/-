package com.meeting.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取概览统计（用于首页仪表盘）
     */
    Map<String, Object> getOverview();

    /**
     * 获取会议签到统计
     */
    Map<String, Object> getMeetingStatistics(Long meetingId);

    /**
     * 获取会议统计（用于数据统计页面）
     */
    Map<String, Object> getMeetingStats(Long meetingId);

    /**
     * 获取人员状态分布
     */
    Map<String, Object> getStatusDistribution(Long meetingId);

    /**
     * 获取补签统计
     */
    Map<String, Object> getMakeUpStatistics(Long meetingId);

    /**
     * 获取代签统计
     */
    Map<String, Object> getProxyStatistics(Long meetingId);

    /**
     * 获取用户签到统计
     */
    Map<String, Object> getUserStatistics(Long userId);

    /**
     * 获取时间段签到统计
     */
    Map<String, Object> getTimeRangeStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取部门签到统计
     */
    List<Map<String, Object>> getDeptStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 导出会议签到报表
     */
    byte[] exportMeetingReport(Long meetingId, String format);

    /**
     * 获取会议定位签到位置列表（用于地图展示）
     */
    List<Map<String, Object>> getMeetingCheckInLocations(Long meetingId);

    /**
     * 获取会议拍照签到照片列表（用于照片展示）
     */
    List<Map<String, Object>> getMeetingCheckInPhotos(Long meetingId);
}

package com.meeting.controller;

import com.meeting.common.Result;
import com.meeting.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据统计控制器
 */
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Tag(name = "数据统计", description = "会议签到统计、人员状态统计等接口")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 获取概览统计
     */
    @GetMapping("/overview")
    @Operation(summary = "获取概览统计", description = "获取系统整体统计数据，用于首页仪表盘")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> result = statisticsService.getOverview();
        return Result.success(result);
    }

    /**
     * 获取会议签到统计
     */
    @GetMapping("/meeting/{meetingId}")
    @Operation(summary = "获取会议签到统计", description = "获取指定会议的签到统计数据")
    public Result<Map<String, Object>> getMeetingStatistics(@PathVariable Long meetingId) {
        Map<String, Object> result = statisticsService.getMeetingStatistics(meetingId);
        return Result.success(result);
    }

    /**
     * 获取会议统计（用于数据统计页面）
     */
    @GetMapping("/meeting/{meetingId}/stats")
    @Operation(summary = "获取会议统计", description = "获取会议的详细统计数据")
    public Result<Map<String, Object>> getMeetingStats(@PathVariable Long meetingId) {
        Map<String, Object> result = statisticsService.getMeetingStats(meetingId);
        return Result.success(result);
    }

    /**
     * 获取人员状态分布
     */
    @GetMapping("/meeting/{meetingId}/status-distribution")
    @Operation(summary = "获取人员状态分布", description = "获取会议参会人员的状态分布")
    public Result<Map<String, Object>> getStatusDistribution(@PathVariable Long meetingId) {
        Map<String, Object> result = statisticsService.getStatusDistribution(meetingId);
        return Result.success(result);
    }

    /**
     * 获取补签统计
     */
    @GetMapping("/meeting/{meetingId}/makeup")
    @Operation(summary = "获取补签统计", description = "获取会议补签情况统计")
    public Result<Map<String, Object>> getMakeUpStatistics(@PathVariable Long meetingId) {
        Map<String, Object> result = statisticsService.getMakeUpStatistics(meetingId);
        return Result.success(result);
    }

    /**
     * 获取代签统计
     */
    @GetMapping("/meeting/{meetingId}/proxy")
    @Operation(summary = "获取代签统计", description = "获取会议代签情况统计")
    public Result<Map<String, Object>> getProxyStatistics(@PathVariable Long meetingId) {
        Map<String, Object> result = statisticsService.getProxyStatistics(meetingId);
        return Result.success(result);
    }

    /**
     * 获取用户签到统计
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户签到统计", description = "获取指定用户的签到统计数据")
    public Result<Map<String, Object>> getUserStatistics(@PathVariable Long userId) {
        Map<String, Object> result = statisticsService.getUserStatistics(userId);
        return Result.success(result);
    }

    /**
     * 获取时间段签到统计
     */
    @GetMapping("/time-range")
    @Operation(summary = "获取时间段签到统计", description = "获取指定时间范围内的签到统计")
    public Result<Map<String, Object>> getTimeRangeStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Map<String, Object> result = statisticsService.getTimeRangeStatistics(startTime, endTime);
        return Result.success(result);
    }

    /**
     * 获取部门签到统计
     */
    @GetMapping("/dept")
    @Operation(summary = "获取部门签到统计", description = "按部门统计签到情况")
    public Result<List<Map<String, Object>>> getDeptStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<Map<String, Object>> result = statisticsService.getDeptStatistics(startTime, endTime);
        return Result.success(result);
    }

    /**
     * 导出会议签到报表
     */
    @GetMapping("/meeting/{meetingId}/export")
    @Operation(summary = "导出会议签到报表", description = "导出指定会议的签到报表")
    public byte[] exportMeetingReport(
            @PathVariable Long meetingId,
            @RequestParam(defaultValue = "csv") String format) {
        return statisticsService.exportMeetingReport(meetingId, format);
    }

    /**
     * 获取会议定位签到位置列表（地图展示用）
     */
    @GetMapping("/meeting/{meetingId}/locations")
    @Operation(summary = "获取会议定位签到位置", description = "获取会议定位签到的人员位置列表，用于地图展示")
    public Result<List<Map<String, Object>>> getMeetingCheckInLocations(@PathVariable Long meetingId) {
        List<Map<String, Object>> result = statisticsService.getMeetingCheckInLocations(meetingId);
        return Result.success(result);
    }

    /**
     * 获取会议拍照签到照片列表（照片展示用）
     */
    @GetMapping("/meeting/{meetingId}/photos")
    @Operation(summary = "获取会议拍照签到照片", description = "获取会议拍照签到的人员照片列表，用于照片展示")
    public Result<List<Map<String, Object>>> getMeetingCheckInPhotos(@PathVariable Long meetingId) {
        List<Map<String, Object>> result = statisticsService.getMeetingCheckInPhotos(meetingId);
        return Result.success(result);
    }
}

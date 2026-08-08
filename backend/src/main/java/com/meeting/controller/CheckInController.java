package com.meeting.controller;

import com.meeting.annotation.RequireRole;
import com.meeting.common.Result;
import com.meeting.dto.CheckInRequest;
import com.meeting.dto.MakeUpApplyRequest;
import com.meeting.dto.ProxySignApplyRequest;
import com.meeting.dto.ProxySignRequest;
import com.meeting.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 签到管理控制器
 */
@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
@Tag(name = "签到管理", description = "签到、补签、代签、统计等接口")
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * 用户签到
     */
    @PostMapping("/meeting/{meetingId}")
    @Operation(summary = "用户签到", description = "通过多种方式进行会议签到")
    public Result<Map<String, Object>> checkIn(
            HttpServletRequest request,
            @PathVariable Long meetingId,
            @Valid @RequestBody CheckInRequest checkInRequest) {
        Long userId = (Long) request.getAttribute("userId");
        checkInRequest.setIpAddress(getClientIp(request));
        Map<String, Object> result = checkInService.checkIn(meetingId, userId, checkInRequest);
        return Result.success("签到成功", result);
    }

    /**
     * 获取会议签到记录
     */
    @GetMapping("/meeting/{meetingId}/records")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "获取会议签到记录", description = "获取指定会议的签到记录")
    public Result<List<Map<String, Object>>> getCheckInRecords(@PathVariable Long meetingId) {
        List<Map<String, Object>> records = checkInService.getCheckInRecordsByMeeting(meetingId);
        return Result.success(records);
    }

    /**
     * 获取我的签到记录
     */
    @GetMapping("/my/records")
    @Operation(summary = "获取我的签到记录", description = "获取当前用户的签到记录")
    public Result<List<Map<String, Object>>> getMyCheckInRecords(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> records = checkInService.getCheckInRecordsByUser(userId);
        return Result.success(records);
    }

    /**
     * 获取会议签到状态（扫码查看）
     */
    @GetMapping("/meeting/{meetingId}/status")
    @Operation(summary = "获取会议签到状态", description = "扫码查看会议实时签到情况")
    public Result<Map<String, Object>> getMeetingCheckInStatus(
            HttpServletRequest request,
            @PathVariable Long meetingId) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = checkInService.getMeetingCheckInStatus(meetingId, userId);
        return Result.success(result);
    }

    /**
     * 申请补签
     */
    @PostMapping("/meeting/{meetingId}/makeup")
    @Operation(summary = "申请补签", description = "为未签到的会议申请补签")
    public Result<Map<String, Object>> applyMakeUp(
            HttpServletRequest request,
            @PathVariable Long meetingId,
            @Valid @RequestBody MakeUpApplyRequest applyRequest) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = checkInService.applyMakeUp(meetingId, userId, applyRequest);
        return Result.success("补签申请已提交", result);
    }

    /**
     * 审批补签
     */
    @PutMapping("/makeup/{applyId}/approve")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "审批补签", description = "审批用户的补签申请")
    public Result<Map<String, Object>> approveMakeUp(
            HttpServletRequest request,
            @PathVariable Long applyId,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {
        Long approverId = (Long) request.getAttribute("userId");
        Map<String, Object> result = checkInService.approveMakeUp(applyId, approverId, status, remark);
        return Result.success(result);
    }

    /**
     * 获取补签申请列表
     */
    @GetMapping("/meeting/{meetingId}/makeup/list")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "获取补签申请列表", description = "获取指定会议的补签申请列表")
    public Result<List<Map<String, Object>>> getMakeUpList(
            @PathVariable Long meetingId,
            @RequestParam(required = false) Integer status) {
        List<Map<String, Object>> list = checkInService.getMakeUpList(meetingId, status);
        return Result.success(list);
    }

    /**
     * 获取所有补签申请列表（跨会议，管理员用）
     */
    @GetMapping("/makeup/list")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "获取所有补签申请列表", description = "获取所有会议的补签申请列表，管理员审批中心使用")
    public Result<List<Map<String, Object>>> getAllMakeUpList(
            @RequestParam(required = false) Integer status) {
        List<Map<String, Object>> list = checkInService.getAllMakeUpList(status);
        return Result.success(list);
    }

    @PostMapping("/meeting/{meetingId}/proxy/apply")
    @Operation(summary = "提交代签申请", description = "参会人申请由同一会议的其他参会人代为签到")
    public Result<Map<String, Object>> applyProxySign(
            HttpServletRequest request,
            @PathVariable Long meetingId,
            @Valid @RequestBody ProxySignApplyRequest applyRequest) {
        Long applicantId = (Long) request.getAttribute("userId");
        return Result.success("代签申请已提交", checkInService.applyProxySign(meetingId, applicantId, applyRequest));
    }

    @GetMapping("/meeting/{meetingId}/proxy/candidates")
    @Operation(summary = "获取代签候选人", description = "获取当前参会人可选择的同会议代签候选人")
    public Result<List<Map<String, Object>>> getProxyCandidates(
            HttpServletRequest request,
            @PathVariable Long meetingId) {
        Long applicantId = (Long) request.getAttribute("userId");
        return Result.success(checkInService.getProxyCandidates(meetingId, applicantId));
    }

    @GetMapping("/proxy/eligible-meetings")
    @Operation(summary = "获取可申请代签的会议", description = "获取当前用户尚未签到且可提交代签申请的会议")
    public Result<List<Map<String, Object>>> getMyProxyEligibleMeetings(HttpServletRequest request) {
        Long applicantId = (Long) request.getAttribute("userId");
        return Result.success(checkInService.getMyProxyEligibleMeetings(applicantId));
    }

    @GetMapping("/proxy/applications/my")
    @Operation(summary = "获取我的代签申请", description = "获取当前用户提交的代签申请及审批结果")
    public Result<List<Map<String, Object>>> getMyProxyApplications(HttpServletRequest request) {
        Long applicantId = (Long) request.getAttribute("userId");
        return Result.success(checkInService.getMyProxySignApplications(applicantId));
    }

    @DeleteMapping("/proxy/applications/{applyId}")
    @Operation(summary = "撤销代签申请", description = "申请人可撤销尚未审批的代签申请")
    public Result<Void> cancelProxyApplication(HttpServletRequest request, @PathVariable Long applyId) {
        Long applicantId = (Long) request.getAttribute("userId");
        checkInService.cancelProxySignApply(applyId, applicantId);
        return Result.success("代签申请已撤销", null);
    }

    @PutMapping("/proxy/applications/{applyId}/approve")
    @RequireRole({"2", "3"})
    @Operation(summary = "审批代签申请", description = "审批通过后自动生成代签记录和签到记录")
    public Result<Map<String, Object>> approveProxyApplication(
            HttpServletRequest request,
            @PathVariable Long applyId,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {
        Long approverId = (Long) request.getAttribute("userId");
        return Result.success(checkInService.approveProxySignApply(applyId, approverId, status, remark));
    }

    @GetMapping("/meeting/{meetingId}/proxy/applications")
    @RequireRole({"2", "3"})
    @Operation(summary = "获取会议代签申请", description = "获取指定会议的代签申请列表")
    public Result<List<Map<String, Object>>> getProxyApplications(
            @PathVariable Long meetingId,
            @RequestParam(required = false) Integer status) {
        return Result.success(checkInService.getProxySignApplications(meetingId, status));
    }

    @GetMapping("/proxy/applications")
    @RequireRole({"2", "3"})
    @Operation(summary = "获取全部代签申请", description = "管理员审批中心获取全部代签申请")
    public Result<List<Map<String, Object>>> getAllProxyApplications(
            @RequestParam(required = false) Integer status) {
        return Result.success(checkInService.getAllProxySignApplications(status));
    }

    /**
     * 管理员应急代签
     */
    @PostMapping("/meeting/{meetingId}/proxy")
    @RequireRole({"2", "3"})
    @Operation(summary = "代签", description = "管理员或会议领导为用户代签")
    public Result<Map<String, Object>> proxySign(
            HttpServletRequest request,
            @PathVariable Long meetingId,
            @Valid @RequestBody ProxySignRequest proxyRequest) {
        Long proxyUserId = (Long) request.getAttribute("userId");
        Map<String, Object> result = checkInService.proxySign(meetingId, proxyUserId, proxyRequest);
        return Result.success(result);
    }

    /**
     * 获取代签记录
     */
    @GetMapping("/meeting/{meetingId}/proxy/list")
    @RequireRole({"2", "3"})
    @Operation(summary = "获取代签记录", description = "获取指定会议的代签记录")
    public Result<List<Map<String, Object>>> getProxySignList(@PathVariable Long meetingId) {
        List<Map<String, Object>> list = checkInService.getProxySignList(meetingId);
        return Result.success(list);
    }

    /**
     * 获取所有代签记录（跨会议，管理员用）
     */
    @GetMapping("/proxy/list")
    @RequireRole({"2", "3"})
    @Operation(summary = "获取所有代签记录", description = "获取所有会议的代签记录，管理员审批中心使用")
    public Result<List<Map<String, Object>>> getAllProxySignList() {
        List<Map<String, Object>> list = checkInService.getAllProxySignList();
        return Result.success(list);
    }

    /**
     * 模拟签到（PC端快速测试）
     */
    @PostMapping("/meeting/{meetingId}/mock")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "模拟签到", description = "管理员在PC端快速模拟签到")
    public Result<Map<String, Object>> mockCheckIn(
            HttpServletRequest request,
            @PathVariable Long meetingId,
            @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        
        // 直接设置状态为"已签到"（不检查迟到）
        Map<String, Object> result = checkInService.adminCheckIn(meetingId, userId);
        return Result.success("签到成功", result);
    }

    /**
     * 快速签到（群内一键签到，不检查时间/token）
     */
    @PostMapping("/meeting/{meetingId}/quick")
    @Operation(summary = "快速签到", description = "群内点击立即签到，不检查时间和二维码")
    public Result<Map<String, Object>> quickCheckIn(
            HttpServletRequest request,
            @PathVariable Long meetingId) {
        Long userId = (Long) request.getAttribute("userId");
        CheckInRequest quickRequest = new CheckInRequest();
        quickRequest.setSignMethod("qrcode");
        quickRequest.setIpAddress(getClientIp(request));
        Map<String, Object> result = checkInService.checkIn(meetingId, userId, quickRequest);
        return Result.success("签到成功", result);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

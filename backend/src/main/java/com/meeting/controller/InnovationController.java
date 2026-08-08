package com.meeting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeting.annotation.RequireRole;
import com.meeting.common.Result;
import com.meeting.common.exception.BusinessException;
import com.meeting.dto.CheckInRequest;
import com.meeting.service.CheckInService;
import com.meeting.service.InnovationExperienceService;
import com.meeting.service.InnovationSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/innovation")
@RequiredArgsConstructor
@Tag(name = "创新功能", description = "动态签到、风控、大屏、提醒、访客、画像、预警和智能纪要")
public class InnovationController {

    private final InnovationSecurityService securityService;
    private final InnovationExperienceService experienceService;
    private final CheckInService checkInService;
    private final ObjectMapper objectMapper;

    @GetMapping("/meeting/{meetingId}/config")
    @Operation(summary = "获取智能签到规则")
    public Result<Map<String, Object>> getConfig(@PathVariable Long meetingId) {
        return Result.success(securityService.getConfig(meetingId));
    }

    @PutMapping("/meeting/{meetingId}/config")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "保存智能签到规则")
    public Result<Map<String, Object>> saveConfig(HttpServletRequest request,
                                                   @PathVariable Long meetingId,
                                                   @RequestBody Map<String, Object> body) {
        return Result.success("规则保存成功",
                securityService.saveConfig(meetingId, userId(request), body));
    }

    @GetMapping("/meeting/{meetingId}/dynamic-qrcode")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "生成当前时段的动态二维码")
    public Result<Map<String, Object>> dynamicQrcode(@PathVariable Long meetingId) {
        return Result.success(securityService.issueDynamicQrcode(meetingId));
    }

    @GetMapping("/public/dynamic-ticket/{ticket}")
    @Operation(summary = "解析动态签到票据")
    public Result<Map<String, Object>> previewDynamicTicket(@PathVariable String ticket) {
        return Result.success(securityService.previewDynamicTicket(ticket));
    }

    @GetMapping("/meeting/{meetingId}/offline-permit")
    @Operation(summary = "获取弱网签到许可")
    public Result<Map<String, Object>> offlinePermit(HttpServletRequest request,
                                                      @PathVariable Long meetingId) {
        return Result.success(securityService.issueOfflinePermit(meetingId, userId(request)));
    }

    @PostMapping("/offline/sync")
    @Operation(summary = "批量同步离线签到")
    public Result<Map<String, Object>> syncOffline(HttpServletRequest request,
                                                   @RequestBody Map<String, Object> body) {
        Object rawItems = body.get("items");
        if (!(rawItems instanceof List<?> items)) {
            throw new BusinessException("离线签到数据格式不正确");
        }
        Long userId = userId(request);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Object item : items) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> row = (Map<String, Object>) item;
                Long meetingId = Long.valueOf(String.valueOf(row.get("meetingId")));
                CheckInRequest checkInRequest = objectMapper.convertValue(row.get("data"), CheckInRequest.class);
                checkInRequest.setIpAddress(clientIp(request));
                Map<String, Object> existing = securityService.findOfflineReceipt(
                        checkInRequest.getClientRequestId(), userId);
                Map<String, Object> result = existing != null
                        ? existing : checkInService.checkIn(meetingId, userId, checkInRequest);
                results.add(Map.of(
                        "clientRequestId", checkInRequest.getClientRequestId(),
                        "success", true,
                        "result", result
                ));
            } catch (Exception e) {
                results.add(Map.of(
                        "clientRequestId", extractClientRequestId(item),
                        "success", false,
                        "message", e.getMessage() == null ? "同步失败" : e.getMessage()
                ));
            }
        }
        long successCount = results.stream().filter(row -> Boolean.TRUE.equals(row.get("success"))).count();
        return Result.success(Map.of(
                "total", results.size(),
                "successCount", successCount,
                "failedCount", results.size() - successCount,
                "records", results
        ));
    }

    @GetMapping("/meeting/{meetingId}/risks")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "获取签到风险记录")
    public Result<List<Map<String, Object>>> risks(@PathVariable Long meetingId,
                                                   @RequestParam(required = false) String level) {
        return Result.success(securityService.getRisks(meetingId, level));
    }

    @PutMapping("/risk/{riskId}/review")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "复核签到风险")
    public Result<Void> reviewRisk(HttpServletRequest request,
                                   @PathVariable Long riskId,
                                   @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status")));
        securityService.reviewRisk(riskId, userId(request), status, string(body.get("remark")));
        return Result.success("风险记录已复核", null);
    }

    @GetMapping("/meeting/{meetingId}/live")
    @Operation(summary = "获取实时签到大屏快照")
    public Result<Map<String, Object>> live(@PathVariable Long meetingId) {
        return Result.success(experienceService.getLiveSnapshot(meetingId));
    }

    @GetMapping(value = "/meeting/{meetingId}/stream", produces = "text/event-stream")
    @Operation(summary = "订阅实时签到事件")
    public SseEmitter stream(@PathVariable Long meetingId) {
        return experienceService.subscribe(meetingId);
    }

    @PostMapping("/meeting/{meetingId}/reminders/send")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "一键发送会议提醒")
    public Result<Map<String, Object>> sendReminders(@PathVariable Long meetingId,
                                                     @RequestBody(required = false) Map<String, Object> body) {
        String type = body == null ? null : string(body.get("type"));
        boolean onlyUnsigned = body == null || !Boolean.FALSE.equals(body.get("onlyUnsigned"));
        return Result.success("提醒已生成", experienceService.sendReminders(meetingId, type, onlyUnsigned));
    }

    @GetMapping("/reminders/my")
    @Operation(summary = "获取我的智能提醒")
    public Result<List<Map<String, Object>>> myReminders(HttpServletRequest request,
                                                         @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return Result.success(experienceService.getMyReminders(userId(request), unreadOnly));
    }

    @PutMapping("/reminders/{reminderId}/read")
    @Operation(summary = "标记提醒已读")
    public Result<Void> readReminder(HttpServletRequest request, @PathVariable Long reminderId) {
        experienceService.markReminderRead(reminderId, userId(request));
        return Result.success(null);
    }

    @PostMapping("/meeting/{meetingId}/guest-invite")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "生成访客邀请二维码")
    public Result<Map<String, Object>> createGuestInvite(HttpServletRequest request,
                                                         @PathVariable Long meetingId,
                                                         @RequestBody(required = false) Map<String, Object> body) {
        Integer validHours = body == null || body.get("validHours") == null
                ? 24 : Integer.valueOf(String.valueOf(body.get("validHours")));
        return Result.success(experienceService.createGuestInvite(meetingId, userId(request), validHours));
    }

    @GetMapping("/public/guest/{token}")
    @Operation(summary = "获取访客邀请信息")
    public Result<Map<String, Object>> guestInvite(@PathVariable String token) {
        return Result.success(experienceService.getGuestInvite(token));
    }

    @PostMapping("/public/guest/{token}/checkin")
    @Operation(summary = "访客免注册签到")
    public Result<Map<String, Object>> guestCheckIn(HttpServletRequest request,
                                                    @PathVariable String token,
                                                    @RequestBody Map<String, Object> body) {
        return Result.success("访客签到成功",
                experienceService.guestCheckIn(token, body, clientIp(request)));
    }

    @GetMapping("/meeting/{meetingId}/guests")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "获取会议访客名单")
    public Result<List<Map<String, Object>>> guests(@PathVariable Long meetingId) {
        return Result.success(experienceService.getGuests(meetingId));
    }

    @GetMapping("/profile/me")
    @Operation(summary = "获取个人出勤画像")
    public Result<Map<String, Object>> profile(HttpServletRequest request) {
        return Result.success(experienceService.getAttendanceProfile(userId(request)));
    }

    @GetMapping("/alerts")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "获取数据异常预警")
    public Result<Map<String, Object>> alerts() {
        return Result.success(experienceService.getAlerts());
    }

    @PostMapping("/meeting/{meetingId}/minutes/generate")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "生成本地智能会议纪要")
    public Result<Map<String, Object>> generateMinutes(HttpServletRequest request,
                                                       @PathVariable Long meetingId,
                                                       @RequestBody Map<String, Object> body) {
        return Result.success("智能纪要生成成功", experienceService.generateMinutes(
                meetingId, userId(request), string(body.get("sourceText"))));
    }

    @GetMapping("/meeting/{meetingId}/minutes")
    @Operation(summary = "获取会议纪要")
    public Result<List<Map<String, Object>>> minutes(@PathVariable Long meetingId) {
        return Result.success(experienceService.getMinutes(meetingId));
    }

    private Long userId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "请先登录");
        return (Long) userId;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    @SuppressWarnings("unchecked")
    private String extractClientRequestId(Object item) {
        try {
            Map<String, Object> row = (Map<String, Object>) item;
            Map<String, Object> data = (Map<String, Object>) row.get("data");
            return string(data.get("clientRequestId"));
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

package com.meeting.controller;

import com.meeting.annotation.RequireRole;
import com.meeting.common.Result;
import com.meeting.entity.GroupMessage;
import com.meeting.service.GroupMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 群聊消息控制器
 */
@RestController
@RequestMapping("/group/message")
@RequiredArgsConstructor
@Tag(name = "群聊消息", description = "群聊消息发送和查询")
public class GroupMessageController {

    private final GroupMessageService messageService;

    @GetMapping("/{groupId}")
    @Operation(summary = "获取群消息列表")
    public Result<List<Map<String, Object>>> getMessages(
            @PathVariable Long groupId,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        List<Map<String, Object>> list = messageService.getMessages(groupId, limit);
        return Result.success(list);
    }

    @GetMapping("/{groupId}/latest")
    @Operation(summary = "获取最新消息（轮询）")
    public Result<List<Map<String, Object>>> getLatestMessages(
            @PathVariable Long groupId,
            @RequestParam Long lastId) {
        List<Map<String, Object>> list = messageService.getLatestMessages(groupId, lastId);
        return Result.success(list);
    }

    @PostMapping("/{groupId}/send")
    @Operation(summary = "发送文本消息")
    public Result<Map<String, Object>> sendText(
            HttpServletRequest request,
            @PathVariable Long groupId,
            @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String content = body.get("content");
        GroupMessage msg = messageService.sendMessage(groupId, userId, "text", content, null);
        Map<String, Object> data = Map.of(
            "id", msg.getId(),
            "createTime", msg.getCreateTime()
        );
        return Result.success("发送成功", data);
    }

    @PostMapping("/{groupId}/meeting-card")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "发送会议卡片到群")
    public Result<Map<String, Object>> sendMeetingCard(
            HttpServletRequest request,
            @PathVariable Long groupId,
            @RequestBody Map<String, Long> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long meetingId = body.get("meetingId");
        GroupMessage msg = messageService.sendMeetingCard(groupId, userId, meetingId);
        Map<String, Object> data = Map.of(
            "id", msg.getId(),
            "createTime", msg.getCreateTime()
        );
        return Result.success("会议卡片已发送", data);
    }

    @PostMapping("/{groupId}/checkin-card")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "发送签到卡片到群")
    public Result<Map<String, Object>> sendCheckinCard(
            HttpServletRequest request,
            @PathVariable Long groupId,
            @RequestBody Map<String, Long> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long meetingId = body.get("meetingId");
        GroupMessage msg = messageService.sendCheckinCard(groupId, userId, meetingId);
        Map<String, Object> data = Map.of(
            "id", msg.getId(),
            "createTime", msg.getCreateTime()
        );
        return Result.success("签到卡片已发送", data);
    }

    @DeleteMapping("/{groupId}/clear")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "清空群聊消息", description = "清空指定群聊的所有消息记录")
    public Result<Map<String, Object>> clearMessages(@PathVariable Long groupId) {
        int deleted = messageService.clearMessages(groupId);
        messageService.sendSystemMessage(groupId, "聊天记录已清空");
        Map<String, Object> data = Map.of("deletedCount", deleted);
        return Result.success("聊天记录已清空", data);
    }
}

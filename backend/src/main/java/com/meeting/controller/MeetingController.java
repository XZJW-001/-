package com.meeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meeting.annotation.RequireRole;
import com.meeting.common.Result;
import com.meeting.dto.MeetingCreateRequest;
import com.meeting.entity.Meeting;
import com.meeting.entity.MeetingAttendee;
import com.meeting.mapper.MeetingAttendeeMapper;
import com.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/meeting")
@RequiredArgsConstructor
@Tag(name = "会议管理", description = "会议的增删改查、二维码生成等接口")
public class MeetingController {

    private final MeetingService meetingService;
    private final MeetingAttendeeMapper attendeeMapper;

    @PostMapping
    @RequireRole({"2", "3"})
    @Operation(summary = "创建会议", description = "创建新的会议并设置参会人员")
    public Result<Meeting> createMeeting(
            HttpServletRequest request,
            @Valid @RequestBody MeetingCreateRequest createRequest) {
        Long userId = (Long) request.getAttribute("userId");
        Meeting meeting = meetingService.createMeeting(userId, createRequest);
        return Result.success("会议创建成功", meeting);
    }

    @PutMapping("/{id}")
    @RequireRole({"2", "3"})
    @Operation(summary = "更新会议", description = "更新会议信息")
    public Result<Meeting> updateMeeting(
            @PathVariable Long id,
            @Valid @RequestBody MeetingCreateRequest updateRequest) {
        Meeting meeting = meetingService.updateMeeting(id, updateRequest);
        return Result.success("会议更新成功", meeting);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"1", "2"})
    @Operation(summary = "删除会议", description = "删除指定会议")
    public Result<Void> deleteMeeting(@PathVariable Long id) {
        meetingService.deleteMeeting(id);
        return Result.success("会议删除成功", null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取会议详情", description = "获取会议详细信息")
    public Result<Meeting> getMeetingDetail(@PathVariable Long id) {
        Meeting meeting = meetingService.getMeetingById(id);
        return Result.success(meeting);
    }

    @GetMapping("/page")
    @Operation(summary = "查询会议列表", description = "分页查询会议列表")
    public Result<Page<Meeting>> getMeetingList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long groupId) {
        Map<String, Object> params = new HashMap<>();
        if (title != null) {
            params.put("title", title);
        }
        if (status != null) {
            params.put("status", status);
        }
        if (groupId != null) {
            params.put("groupId", groupId);
        }
        Page<Meeting> page = meetingService.getMeetingList(current, size, params);
        return Result.success(page);
    }

    @PostMapping("/{id}/qrcode")
    @RequireRole({"2", "3"})
    @Operation(summary = "生成会议二维码", description = "生成会议签到二维码")
    public Result<Map<String, Object>> generateQrcode(
            HttpServletRequest request,
            @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = meetingService.generateQrcode(id, userId);
        return Result.success("二维码生成成功", result);
    }

    @GetMapping("/public/qrcode/{token}")
    @Operation(summary = "根据二维码获取会议信息", description = "扫码后获取会议信息")
    public Result<Map<String, Object>> getMeetingByQrcode(@PathVariable String token) {
        Map<String, Object> result = meetingService.getMeetingByQrcode(token);
        return Result.success(result);
    }

    @PutMapping("/{id}/publish")
    @RequireRole({"2", "3"})
    @Operation(summary = "发布会议", description = "将草稿状态的会议发布")
    public Result<Meeting> publishMeeting(@PathVariable Long id) {
        Meeting meeting = meetingService.publishMeeting(id);
        return Result.success("会议发布成功", meeting);
    }

    @PutMapping("/{id}/start")
    @RequireRole({"2", "3"})
    @Operation(summary = "开始会议", description = "将会议状态改为进行中")
    public Result<Meeting> startMeeting(@PathVariable Long id) {
        Meeting meeting = meetingService.startMeeting(id);
        return Result.success("会议已开始", meeting);
    }

    @PutMapping("/{id}/end")
    @RequireRole({"2", "3"})
    @Operation(summary = "结束会议", description = "将会议状态改为已结束")
    public Result<Meeting> endMeeting(@PathVariable Long id) {
        Meeting meeting = meetingService.endMeeting(id);
        return Result.success("会议已结束", meeting);
    }

    @GetMapping("/{id}/attendees")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "获取会议参会人员", description = "获取会议的所有参会人员列表")
    public Result<List<Map<String, Object>>> getMeetingAttendees(@PathVariable Long id) {
        List<MeetingAttendee> attendees = attendeeMapper.findByMeetingIdWithUser(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (MeetingAttendee attendee : attendees) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", attendee.getId());
            item.put("meetingId", attendee.getMeetingId());
            item.put("userId", attendee.getUserId());
            item.put("status", attendee.getStatus());
            item.put("signTime", attendee.getSignTime());
            item.put("signMethod", attendee.getSignMethod());
            item.put("signStatus", attendee.getStatus());
            
            if (attendee.getUser() != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", attendee.getUser().getId());
                userMap.put("realName", attendee.getUser().getRealName());
                userMap.put("username", attendee.getUser().getUsername());
                userMap.put("position", attendee.getUser().getPosition());
                userMap.put("avatar", attendee.getUser().getAvatar());
                item.put("user", userMap);
            }
            result.add(item);
        }
        return Result.success(result);
    }

    @DeleteMapping("/group/{groupId}/clear")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "清空群聊会议记录", description = "清空指定群聊下的所有会议及其签到记录")
    public Result<Map<String, Object>> clearGroupMeetings(@PathVariable Long groupId) {
        int deleted = meetingService.clearMeetingsByGroupId(groupId);
        Map<String, Object> data = new HashMap<>();
        data.put("deletedCount", deleted);
        return Result.success("会议记录已清空", data);
    }
}

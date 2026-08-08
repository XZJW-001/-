package com.meeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meeting.annotation.RequireRole;
import com.meeting.common.Result;
import com.meeting.entity.GroupInvite;
import com.meeting.entity.GroupMember;
import com.meeting.entity.MeetingGroup;
import com.meeting.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
@Tag(name = "群组管理", description = "群组/组织的增删改查、成员管理、邀请等接口")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @RequireRole({"1", "2", "3", "4"})
    @Operation(summary = "创建群组")
    public Result<MeetingGroup> createGroup(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        MeetingGroup group = groupService.createGroup(userId, params);
        return Result.success("群组创建成功", group);
    }

    @PutMapping("/{id}")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "更新群组信息")
    public Result<MeetingGroup> updateGroup(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        MeetingGroup group = groupService.updateGroup(id, userId, params);
        return Result.success("更新成功", group);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "解散群组")
    public Result<Void> deleteGroup(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.deleteGroup(id, userId);
        return Result.success("群组已解散", null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取群组详情")
    public Result<MeetingGroup> getGroup(@PathVariable Long id) {
        MeetingGroup group = groupService.getGroupById(id);
        return Result.success(group);
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "通过编号查询群组")
    public Result<MeetingGroup> getGroupByCode(@PathVariable String code) {
        MeetingGroup group = groupService.getGroupByCode(code);
        if (group == null) {
            return Result.fail("群组不存在");
        }
        return Result.success(group);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询群组列表")
    public Result<Page<MeetingGroup>> getGroupList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> params = new HashMap<>();
        if (keyword != null) {
            params.put("keyword", keyword);
        }
        Page<MeetingGroup> page = groupService.getGroupList(userId, current, size, params);
        return Result.success(page);
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的群组列表")
    public Result<List<MeetingGroup>> getMyGroups(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<MeetingGroup> groups = groupService.getMyGroups(userId);
        return Result.success(groups);
    }

    @PostMapping("/{id}/join")
    @RequireRole({"1", "2", "3", "4"})
    @Operation(summary = "加入群组")
    public Result<GroupMember> joinGroup(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        GroupMember member = groupService.joinGroup(id, userId);
        return Result.success("加入成功", member);
    }

    @PostMapping("/{id}/leave")
    @Operation(summary = "退出群组")
    public Result<Void> leaveGroup(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.leaveGroup(id, userId);
        return Result.success("退出成功", null);
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "获取群组成员列表")
    public Result<List<GroupMember>> getGroupMembers(@PathVariable Long id) {
        List<GroupMember> members = groupService.getGroupMembers(id);
        return Result.success(members);
    }

    @PutMapping("/{id}/members/{userId}/role")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "修改成员角色")
    public Result<Void> updateMemberRole(
            HttpServletRequest request,
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam int role) {
        Long operatorId = (Long) request.getAttribute("userId");
        groupService.updateMemberRole(id, userId, role, operatorId);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "移除群成员", description = "群主或管理员移除群内成员")
    public Result<Void> removeMember(
            HttpServletRequest request,
            @PathVariable Long id,
            @PathVariable Long userId) {
        Long operatorId = (Long) request.getAttribute("userId");
        groupService.removeMember(id, userId, operatorId);
        return Result.success("已移除", null);
    }

    @PostMapping("/{id}/apply")
    @Operation(summary = "申请加入群组")
    public Result<GroupInvite> applyJoinGroup(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> params) {
        Long userId = (Long) request.getAttribute("userId");
        String remark = params != null ? params.get("remark") : null;
        GroupInvite invite = groupService.applyJoinGroup(id, userId, remark);
        return Result.success("申请已提交", invite);
    }

    @PostMapping("/{id}/invite")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "邀请用户加入群组")
    public Result<GroupInvite> inviteToGroup(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        Long targetUserId = Long.valueOf(params.get("targetUserId").toString());
        String remark = (String) params.get("remark");
        GroupInvite invite = groupService.inviteToGroup(id, userId, targetUserId, remark);
        return Result.success("邀请已发送", invite);
    }

    @PutMapping("/invite/{id}/handle")
    @RequireRole({"1", "2", "3"})
    @Operation(summary = "处理邀请/申请")
    public Result<Void> handleInvite(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam boolean accept) {
        Long userId = (Long) request.getAttribute("userId");
        groupService.handleInvite(id, userId, accept);
        return Result.success(accept ? "已通过" : "已拒绝", null);
    }

    @GetMapping("/invite/my")
    @Operation(summary = "获取我的邀请/申请列表")
    public Result<List<GroupInvite>> getMyInvites(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<GroupInvite> invites = groupService.getMyInvites(userId);
        return Result.success(invites);
    }
}

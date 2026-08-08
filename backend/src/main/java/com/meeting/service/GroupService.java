package com.meeting.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meeting.entity.GroupInvite;
import com.meeting.entity.GroupMember;
import com.meeting.entity.MeetingGroup;

import java.util.List;
import java.util.Map;

public interface GroupService {

    MeetingGroup createGroup(Long userId, Map<String, Object> params);

    MeetingGroup updateGroup(Long groupId, Long userId, Map<String, Object> params);

    void deleteGroup(Long groupId, Long userId);

    MeetingGroup getGroupById(Long groupId);

    MeetingGroup getGroupByCode(String groupCode);

    Page<MeetingGroup> getGroupList(Long userId, int current, int size, Map<String, Object> params);

    List<MeetingGroup> getMyGroups(Long userId);

    GroupMember joinGroup(Long groupId, Long userId);

    void leaveGroup(Long groupId, Long userId);

    List<GroupMember> getGroupMembers(Long groupId);

    void updateMemberRole(Long groupId, Long userId, int role, Long operatorId);

    /**
     * 移除群成员（仅群主或管理员可操作）
     */
    void removeMember(Long groupId, Long userId, Long operatorId);

    GroupInvite applyJoinGroup(Long groupId, Long userId, String remark);

    GroupInvite inviteToGroup(Long groupId, Long userId, Long targetUserId, String remark);

    void handleInvite(Long inviteId, Long userId, boolean accept);

    List<GroupInvite> getMyInvites(Long userId);
}

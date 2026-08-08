package com.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meeting.common.exception.BusinessException;
import com.meeting.entity.GroupInvite;
import com.meeting.entity.GroupMember;
import com.meeting.entity.MeetingGroup;
import com.meeting.entity.SysUser;
import com.meeting.mapper.GroupInviteMapper;
import com.meeting.mapper.GroupMemberMapper;
import com.meeting.mapper.GroupMapper;
import com.meeting.mapper.SysUserMapper;
import com.meeting.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupMapper groupMapper;
    private final GroupMemberMapper memberMapper;
    private final GroupInviteMapper inviteMapper;
    private final SysUserMapper userMapper;

    @Override
    @Transactional
    public MeetingGroup createGroup(Long userId, Map<String, Object> params) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        MeetingGroup group = new MeetingGroup();
        group.setGroupName((String) params.get("groupName"));
        group.setDescription((String) params.get("description"));
        group.setOwnerId(userId);
        group.setMaxMembers(params.get("maxMembers") != null ?
            ((Number) params.get("maxMembers")).intValue() : 500);
        group.setStatus(1);
        group.setMemberCount(1);

        String groupCode = generateGroupCode();
        while (groupMapper.findByGroupCode(groupCode) != null) {
            groupCode = generateGroupCode();
        }
        group.setGroupCode(groupCode);

        groupMapper.insert(group);
        log.info("创建群组成功: id={}, name={}", group.getId(), group.getGroupName());

        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(userId);
        ownerMember.setRole(3);
        ownerMember.setNickname(user.getRealName());
        ownerMember.setStatus(1);
        memberMapper.insert(ownerMember);

        return groupMapper.findByIdWithOwner(group.getId());
    }

    private String generateGroupCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public MeetingGroup updateGroup(Long groupId, Long userId, Map<String, Object> params) {
        MeetingGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("群组不存在");
        }
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException("只有群主可以修改群组信息");
        }

        if (params.containsKey("groupName")) {
            group.setGroupName((String) params.get("groupName"));
        }
        if (params.containsKey("description")) {
            group.setDescription((String) params.get("description"));
        }
        if (params.containsKey("avatar")) {
            group.setAvatar((String) params.get("avatar"));
        }
        if (params.containsKey("maxMembers")) {
            group.setMaxMembers(((Number) params.get("maxMembers")).intValue());
        }

        groupMapper.updateById(group);
        return groupMapper.findByIdWithOwner(groupId);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        MeetingGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("群组不存在");
        }
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException("只有群主可以解散群组");
        }
        group.setStatus(0);
        groupMapper.updateById(group);
        log.info("解散群组: id={}", groupId);
    }

    @Override
    public MeetingGroup getGroupById(Long groupId) {
        MeetingGroup group = groupMapper.findByIdWithOwner(groupId);
        if (group == null) {
            throw new BusinessException("群组不存在");
        }
        return group;
    }

    @Override
    public MeetingGroup getGroupByCode(String groupCode) {
        QueryWrapper<MeetingGroup> wrapper = new QueryWrapper<>();
        wrapper.eq("group_code", groupCode);
        return groupMapper.selectOne(wrapper);
    }

    @Override
    public Page<MeetingGroup> getGroupList(Long userId, int current, int size, Map<String, Object> params) {
        Page<MeetingGroup> page = new Page<>(current, size);
        return groupMapper.selectPageList(page, params);
    }

    @Override
    public List<MeetingGroup> getMyGroups(Long userId) {
        QueryWrapper<GroupMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("user_id", userId).eq("status", 1);
        List<GroupMember> members = memberMapper.selectList(memberWrapper);

        if (members.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> groupIds = members.stream().map(GroupMember::getGroupId).collect(Collectors.toList());
        QueryWrapper<MeetingGroup> groupWrapper = new QueryWrapper<>();
        groupWrapper.in("id", groupIds).eq("status", 1).orderByDesc("create_time");
        List<MeetingGroup> groups = groupMapper.selectList(groupWrapper);

        for (MeetingGroup group : groups) {
            group.setIsMember(true);
        }

        return groups;
    }

    @Override
    @Transactional
    public GroupMember joinGroup(Long groupId, Long userId) {
        MeetingGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() == 0) {
            throw new BusinessException("群组不存在或已解散");
        }

        QueryWrapper<GroupMember> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId).eq("user_id", userId).eq("status", 1);
        GroupMember existing = memberMapper.selectOne(wrapper);
        if (existing != null) {
            throw new BusinessException("您已是该群成员");
        }

        if (group.getMemberCount() >= group.getMaxMembers()) {
            throw new BusinessException("群组已满");
        }

        SysUser user = userMapper.selectById(userId);
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(1);
        member.setNickname(user.getRealName());
        member.setStatus(1);
        memberMapper.insert(member);

        group.setMemberCount(group.getMemberCount() + 1);
        groupMapper.updateById(group);

        return member;
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        MeetingGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("群组不存在");
        }
        if (group.getOwnerId().equals(userId)) {
            throw new BusinessException("群主不能退出群组，请先解散或转让群主");
        }

        QueryWrapper<GroupMember> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId).eq("user_id", userId);
        GroupMember member = memberMapper.selectOne(wrapper);
        if (member == null) {
            throw new BusinessException("您不是该群成员");
        }

        member.setStatus(0);
        memberMapper.updateById(member);

        group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
        groupMapper.updateById(group);
    }

    @Override
    public List<GroupMember> getGroupMembers(Long groupId) {
        return memberMapper.findMembersByGroupId(groupId);
    }

    @Override
    @Transactional
    public void updateMemberRole(Long groupId, Long userId, int role, Long operatorId) {
        MeetingGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("群组不存在");
        }
        if (!group.getOwnerId().equals(operatorId)) {
            throw new BusinessException("只有群主可以修改成员角色");
        }

        QueryWrapper<GroupMember> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId).eq("user_id", userId);
        GroupMember member = memberMapper.selectOne(wrapper);
        if (member == null) {
            throw new BusinessException("成员不存在");
        }

        member.setRole(role);
        memberMapper.updateById(member);
    }

    @Override
    @Transactional
    public void removeMember(Long groupId, Long userId, Long operatorId) {
        MeetingGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("群组不存在");
        }

        // 校验操作者权限：群主或管理员
        QueryWrapper<GroupMember> operatorWrapper = new QueryWrapper<>();
        operatorWrapper.eq("group_id", groupId).eq("user_id", operatorId).eq("status", 1);
        GroupMember operator = memberMapper.selectOne(operatorWrapper);
        if (operator == null || operator.getRole() < 2) {
            throw new BusinessException("无权限：仅群主或管理员可移除成员");
        }

        // 不能移除群主
        if (group.getOwnerId().equals(userId)) {
            throw new BusinessException("不能移除群主");
        }

        // 操作者若是管理员，不能移除其他管理员
        if (operator.getRole() == 2) {
            QueryWrapper<GroupMember> targetWrapper = new QueryWrapper<>();
            targetWrapper.eq("group_id", groupId).eq("user_id", userId);
            GroupMember target = memberMapper.selectOne(targetWrapper);
            if (target != null && target.getRole() >= 2) {
                throw new BusinessException("管理员不能移除其他管理员或群主");
            }
        }

        // 软删除：将状态置为0
        QueryWrapper<GroupMember> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId).eq("user_id", userId);
        GroupMember member = memberMapper.selectOne(wrapper);
        if (member == null) {
            throw new BusinessException("成员不存在");
        }
        member.setStatus(0);
        memberMapper.updateById(member);

        group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
        groupMapper.updateById(group);
        log.info("移除群成员: groupId={}, userId={}, operatorId={}", groupId, userId, operatorId);
    }

    @Override
    @Transactional
    public GroupInvite applyJoinGroup(Long groupId, Long userId, String remark) {
        MeetingGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() == 0) {
            throw new BusinessException("群组不存在或已解散");
        }

        QueryWrapper<GroupMember> memberWrapper = new QueryWrapper<>();
        memberWrapper.eq("group_id", groupId).eq("user_id", userId).eq("status", 1);
        if (memberMapper.selectOne(memberWrapper) != null) {
            throw new BusinessException("您已是该群成员");
        }

        QueryWrapper<GroupInvite> inviteWrapper = new QueryWrapper<>();
        inviteWrapper.eq("group_id", groupId).eq("user_id", userId).eq("type", 1).eq("status", 0);
        if (inviteMapper.selectOne(inviteWrapper) != null) {
            throw new BusinessException("您已提交过申请，请等待审核");
        }

        GroupInvite invite = new GroupInvite();
        invite.setGroupId(groupId);
        invite.setUserId(userId);
        invite.setType(1);
        invite.setStatus(0);
        invite.setRemark(remark);
        inviteMapper.insert(invite);

        return invite;
    }

    @Override
    @Transactional
    public GroupInvite inviteToGroup(Long groupId, Long userId, Long targetUserId, String remark) {
        MeetingGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("群组不存在");
        }

        QueryWrapper<GroupMember> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", groupId).eq("user_id", userId).eq("status", 1);
        GroupMember operatorMember = memberMapper.selectOne(wrapper);
        if (operatorMember == null || operatorMember.getRole() < 2) {
            throw new BusinessException("无权限邀请成员");
        }

        GroupInvite invite = new GroupInvite();
        invite.setGroupId(groupId);
        invite.setUserId(targetUserId);
        invite.setType(2);
        invite.setStatus(0);
        invite.setRemark(remark);
        inviteMapper.insert(invite);

        return invite;
    }

    @Override
    @Transactional
    public void handleInvite(Long inviteId, Long userId, boolean accept) {
        GroupInvite invite = inviteMapper.selectById(inviteId);
        if (invite == null) {
            throw new BusinessException("邀请不存在");
        }
        if (invite.getStatus() != 0) {
            throw new BusinessException("该邀请已处理");
        }

        if (invite.getType() == 1) {
            QueryWrapper<GroupMember> wrapper = new QueryWrapper<>();
            wrapper.eq("group_id", invite.getGroupId()).eq("user_id", userId).eq("status", 1);
            GroupMember operatorMember = memberMapper.selectOne(wrapper);
            if (operatorMember == null || operatorMember.getRole() < 2) {
                throw new BusinessException("无权限处理申请");
            }
        } else {
            if (!invite.getUserId().equals(userId)) {
                throw new BusinessException("这不是您的邀请");
            }
        }

        if (accept) {
            joinGroup(invite.getGroupId(), invite.getUserId());
            invite.setStatus(1);
        } else {
            invite.setStatus(2);
        }
        inviteMapper.updateById(invite);
    }

    @Override
    public List<GroupInvite> getMyInvites(Long userId) {
        QueryWrapper<GroupInvite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("status", 0).orderByDesc("create_time");
        return inviteMapper.selectList(wrapper);
    }
}

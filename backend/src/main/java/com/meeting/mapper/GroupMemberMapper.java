package com.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meeting.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    @Select("SELECT gm.*, u.real_name as userName, u.avatar as userAvatar FROM group_member gm LEFT JOIN sys_user u ON gm.user_id = u.id WHERE gm.group_id = #{groupId} AND gm.status = 1 ORDER BY gm.role DESC, gm.join_time ASC")
    List<GroupMember> findMembersByGroupId(@Param("groupId") Long groupId);

    @Select("SELECT COUNT(*) FROM group_member WHERE group_id = #{groupId} AND status = 1")
    int countMembers(@Param("groupId") Long groupId);
}

package com.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meeting.entity.MeetingGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface GroupMapper extends BaseMapper<MeetingGroup> {

    Page<MeetingGroup> selectPageList(Page<MeetingGroup> page, @Param("params") Map<String, Object> params);

    @Select("SELECT g.*, u.real_name as ownerName FROM meeting_group g LEFT JOIN sys_user u ON g.owner_id = u.id WHERE g.group_code = #{code}")
    MeetingGroup findByGroupCode(@Param("code") String code);

    @Select("SELECT g.*, u.real_name as ownerName FROM meeting_group g LEFT JOIN sys_user u ON g.owner_id = u.id WHERE g.id = #{id}")
    MeetingGroup findByIdWithOwner(@Param("id") Long id);
}

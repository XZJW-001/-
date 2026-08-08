package com.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meeting.entity.GroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 群聊消息Mapper接口
 */
@Mapper
public interface GroupMessageMapper extends BaseMapper<GroupMessage> {

    /**
     * 查询群消息列表（带用户信息）
     */
    List<GroupMessage> findMessagesWithUser(@Param("groupId") Long groupId, @Param("limit") Integer limit);

    /**
     * 查询最新消息（用于轮询）
     */
    List<GroupMessage> findLatestMessages(@Param("groupId") Long groupId, @Param("lastId") Long lastId);
}

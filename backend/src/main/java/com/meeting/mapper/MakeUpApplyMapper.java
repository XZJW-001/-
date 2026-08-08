package com.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meeting.entity.MakeUpApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 补签申请Mapper接口
 */
@Mapper
public interface MakeUpApplyMapper extends BaseMapper<MakeUpApply> {

    /**
     * 查询会议补签申请列表（带用户信息）
     */
    List<MakeUpApply> findByMeetingIdWithUser(@Param("meetingId") Long meetingId, @Param("status") Integer status);

    /**
     * 查询用户补签申请列表
     */
    List<MakeUpApply> findByUserId(@Param("userId") Long userId);

    /**
     * 查询待审批的补签申请
     */
    List<MakeUpApply> findPendingList();
}

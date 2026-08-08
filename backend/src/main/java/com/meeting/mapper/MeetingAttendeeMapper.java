package com.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meeting.entity.MeetingAttendee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会议参会人员Mapper接口
 */
@Mapper
public interface MeetingAttendeeMapper extends BaseMapper<MeetingAttendee> {

    /**
     * 查询会议参会人员列表（带用户信息）
     */
    List<MeetingAttendee> findByMeetingIdWithUser(@Param("meetingId") Long meetingId);

    /**
     * 查询用户参加的会议列表
     */
    List<MeetingAttendee> findByUserId(@Param("userId") Long userId);

    /**
     * 统计会议签到情况
     */
    java.util.Map<String, Object> countByMeetingId(@Param("meetingId") Long meetingId);
}

package com.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meeting.entity.CheckInRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 签到记录Mapper接口
 */
@Mapper
public interface CheckInRecordMapper extends BaseMapper<CheckInRecord> {

    /**
     * 查询会议签到记录
     */
    List<CheckInRecord> findByMeetingId(@Param("meetingId") Long meetingId);

    /**
     * 查询用户签到记录
     */
    List<CheckInRecord> findByUserId(@Param("userId") Long userId);

    /**
     * 查询会议拍照签到记录（含 verify_data，仅 photo 方式）
     */
    List<CheckInRecord> findPhotoRecordsByMeetingId(@Param("meetingId") Long meetingId);

    /**
     * 统计签到数据（按会议ID）
     */
    Map<String, Object> statisticsByMeetingId(@Param("meetingId") Long meetingId);

    /**
     * 统计签到数据（按用户ID）
     */
    Map<String, Object> statisticsByUserId(@Param("userId") Long userId);

    /**
     * 统计指定时间范围内的签到数据
     */
    Map<String, Object> statisticsByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}

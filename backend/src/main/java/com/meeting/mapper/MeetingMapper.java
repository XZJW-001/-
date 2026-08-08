package com.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meeting.entity.Meeting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 会议Mapper接口
 */
@Mapper
public interface MeetingMapper extends BaseMapper<Meeting> {

    /**
     * 分页查询会议列表
     */
    Page<Meeting> selectPageList(Page<Meeting> page, @Param("params") Map<String, Object> params);

    /**
     * 根据二维码Token查询会议
     */
    Meeting findByQrcodeToken(@Param("token") String token);

    /**
     * 根据创建人查询会议列表
     */
    List<Meeting> findByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * 查询需要自动发布的会议（草稿状态且签到开始时间已到）
     */
    List<Meeting> findMeetingsToPublish(@Param("now") LocalDateTime now);

    /**
     * 查询需要自动开始的会议（已发布状态且会议开始时间已到）
     */
    List<Meeting> findMeetingsToStart(@Param("now") LocalDateTime now);

    /**
     * 查询需要自动结束的会议（进行中状态且会议结束时间已到）
     */
    List<Meeting> findMeetingsToEnd(@Param("now") LocalDateTime now);
}

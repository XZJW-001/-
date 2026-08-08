package com.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meeting.entity.ProxySignRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代签记录Mapper接口
 */
@Mapper
public interface ProxySignRecordMapper extends BaseMapper<ProxySignRecord> {

    /**
     * 查询会议代签记录（带用户信息）
     */
    List<ProxySignRecord> findByMeetingIdWithUser(@Param("meetingId") Long meetingId);

    /**
     * 查询代签人相关的记录
     */
    List<ProxySignRecord> findByProxyUserId(@Param("proxyUserId") Long proxyUserId);

    /**
     * 查询被代签人相关的记录
     */
    List<ProxySignRecord> findByTargetUserId(@Param("targetUserId") Long targetUserId);
}

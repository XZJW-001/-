package com.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 签到记录实体类
 */
@Data
@TableName(value = "check_in_record", autoResultMap = true)
public class CheckInRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long meetingId;

    private Long userId;

    private String signMethod;

    private LocalDateTime signTime;

    private Integer signStatus;

    private String location;

    /**
     * 纬度（定位签到时使用）
     */
    private Double latitude;

    /**
     * 经度（定位签到时使用）
     */
    private Double longitude;

    private String deviceInfo;

    private String ipAddress;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> verifyData;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

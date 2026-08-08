package com.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议实体类
 */
@Data
@TableName(value = "meeting", autoResultMap = true)
public class Meeting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime checkinStartTime;

    private LocalDateTime checkinEndTime;

    private Integer lateTime;

    private String qrcodeToken;

    private LocalDateTime qrcodeExpire;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> signMethods;

    /**
     * 手势签到密码（九宫格点位序列，如 "0-1-2-5-8"）
     */
    private String gesturePassword;

    private Integer status;

    private Long creatorId;

    private Long groupId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

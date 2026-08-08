package com.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会议参会人员实体类
 */
@Data
@TableName("meeting_attendee")
public class MeetingAttendee {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long meetingId;

    private Long userId;

    private Integer status;

    private LocalDateTime signTime;

    private String signMethod;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 关联用户信息（非数据库字段）
     */
    @TableField(exist = false)
    private SysUser user;
}

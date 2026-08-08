package com.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 补签申请实体类
 */
@Data
@TableName("make_up_apply")
public class MakeUpApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long meetingId;

    private Long userId;

    private String reason;

    private String proofUrl;

    private Integer status;

    private Long approverId;

    private LocalDateTime approveTime;

    private String approveRemark;

    private LocalDateTime originalSignTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private SysUser user;

    @TableField(exist = false)
    private Meeting meeting;
}

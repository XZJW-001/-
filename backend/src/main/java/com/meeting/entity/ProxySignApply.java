package com.meeting.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** Persistent proxy check-in approval request. */
@Data
@TableName("proxy_sign_apply")
public class ProxySignApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long meetingId;

    private Long applicantId;

    private Long proxyUserId;

    private String reason;

    /** 0 pending, 1 approved, 2 rejected, 3 cancelled. */
    private Integer status;

    private Long approverId;

    private LocalDateTime approveTime;

    private String approveRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

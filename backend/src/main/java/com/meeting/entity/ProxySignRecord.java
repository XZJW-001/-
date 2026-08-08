package com.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 代签记录实体类
 */
@Data
@TableName("proxy_sign_record")
public class ProxySignRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long meetingId;

    private Long proxyUserId;

    private Long targetUserId;

    private String reason;

    private Integer status;

    private LocalDateTime signTime;

    private Long approverId;

    private LocalDateTime approveTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private SysUser proxyUser;

    @TableField(exist = false)
    private SysUser targetUser;
}

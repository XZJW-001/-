package com.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群聊消息实体类
 */
@Data
@TableName("group_message")
public class GroupMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long userId;

    /**
     * 消息类型：text-文本 meeting-会议卡片 checkin-签到卡片 system-系统消息
     */
    private String type;

    private String content;

    /**
     * 附加数据（JSON格式，如会议ID、签到token等）
     */
    private String extra;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private SysUser user;
}

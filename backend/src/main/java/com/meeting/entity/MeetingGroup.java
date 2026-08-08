package com.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("meeting_group")
public class MeetingGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String groupName;
    private String groupCode;
    private String avatar;
    private String description;
    private Long ownerId;
    private Integer maxMembers;
    private Integer memberCount;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private Boolean isMember;
}
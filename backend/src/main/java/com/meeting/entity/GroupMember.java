package com.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_member")
public class GroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    private Integer role;
    private String nickname;
    private LocalDateTime joinTime;
    private Integer status;
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String userAvatar;
}
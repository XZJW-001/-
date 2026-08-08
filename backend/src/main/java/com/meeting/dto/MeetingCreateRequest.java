package com.meeting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建会议请求DTO
 */
@Data
public class MeetingCreateRequest {

    @NotBlank(message = "会议主题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "会议地点不能为空")
    private String location;

    @NotNull(message = "会议开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "会议结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkinStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkinEndTime;

    private Integer lateTime = 15;

    /**
     * 支持的签到方式列表：qrcode/photo/gesture/location
     */
    private List<String> signMethods;

    /**
     * 手势签到密码（九宫格点位序列，如 "0-1-2-5-8"），仅当 signMethods 包含 gesture 时需要
     */
    private String gesturePassword;

    /**
     * 参会人员ID列表
     */
    private List<Long> attendeeIds;

    /**
     * 二维码有效时长（分钟）
     */
    private Integer qrcodeExpireMinutes;

    /**
     * 所属群聊ID（群聊内创建会议时传入，自动将群成员添加为参会人）
     */
    private Long groupId;
}

package com.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 补签申请请求DTO
 */
@Data
public class MakeUpApplyRequest {

    @NotBlank(message = "补签原因不能为空")
    private String reason;

    /**
     * 证明材料URL
     */
    private String proofUrl;

    /**
     * 原签到时间
     */
    private LocalDateTime originalSignTime;
}

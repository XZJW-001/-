package com.meeting.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 代签请求DTO
 */
@Data
public class ProxySignRequest {

    /**
     * 被代签的用户ID列表
     */
    @NotEmpty(message = "被代签用户列表不能为空")
    private List<Long> targetUserIds;

    /**
     * 代签原因
     */
    private String reason;
}

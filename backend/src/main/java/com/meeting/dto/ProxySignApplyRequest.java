package com.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request for a participant to submit a proxy check-in application. */
@Data
public class ProxySignApplyRequest {

    @NotNull(message = "请选择代签人")
    private Long proxyUserId;

    @NotBlank(message = "代签原因不能为空")
    @Size(max = 500, message = "代签原因不能超过500个字符")
    private String reason;
}

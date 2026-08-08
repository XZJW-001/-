package com.meeting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 签到请求DTO
 */
@Data
public class CheckInRequest {

    /**
     * 签到方式：qrcode/photo/gesture/location
     */
    @NotBlank(message = "签到方式不能为空")
    private String signMethod;

    /**
     * 二维码Token（扫码签到时使用）
     */
    private String qrcodeToken;

    /**
     * Server-signed rotating QR ticket.
     */
    private String dynamicTicket;

    /**
     * 位置信息（定位签到时使用）
     */
    private String location;

    /**
     * 位置坐标
     */
    private Double latitude;

    private Double longitude;

    /**
     * 设备信息
     */
    private String deviceInfo;

    /**
     * Stable device identifier generated and persisted by the client.
     */
    private String deviceId;

    /**
     * Client-side idempotency key used when an offline request is retried.
     */
    private String clientRequestId;

    /**
     * Server-signed permission acquired while the device is online.
     */
    private String offlinePermit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime offlineSignedAt;

    @JsonIgnore
    private String ipAddress;

    /**
     * 验证数据（照片、手势密码等）
     */
    private Map<String, Object> verifyData;
}

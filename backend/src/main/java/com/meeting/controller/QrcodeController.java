package com.meeting.controller;

import com.meeting.common.Result;
import com.meeting.utils.QrcodeUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/qrcode")
@Tag(name = "二维码管理", description = "二维码生成、验证等接口")
public class QrcodeController {

    @Value("${app.qrcode.default-width:300}")
    private int defaultWidth;

    @Value("${app.qrcode.default-height:300}")
    private int defaultHeight;

    /**
     * 生成二维码
     */
    @PostMapping("/generate")
    @Operation(summary = "生成二维码", description = "根据内容生成二维码图片")
    public Result<Map<String, Object>> generateQrcode(@RequestBody Map<String, String> params) {
        String content = params.get("content");
        if (content == null || content.isEmpty()) {
            return Result.fail("内容不能为空");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("timestamp", System.currentTimeMillis());

        try {
            String base64Image = QrcodeUtil.generateQrcodeBase64(content, defaultWidth, defaultHeight);
            result.put("qrcodeImage", "data:image/png;base64," + base64Image);
            result.put("width", defaultWidth);
            result.put("height", defaultHeight);
        } catch (Exception e) {
            return Result.fail("生成二维码失败: " + e.getMessage());
        }

        return Result.success(result);
    }

    /**
     * 生成会议签到二维码（带自动更新功能）
     */
    @GetMapping("/meeting/{meetingId}")
    @Operation(summary = "获取会议签到二维码", description = "获取会议签到二维码，支持定时刷新")
    public Result<Map<String, Object>> getMeetingQrcode(
            @PathVariable Long meetingId,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "300") int height) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 生成带时间戳的二维码内容，用于定时刷新
            String token = meetingId + "_" + System.currentTimeMillis();
            String content = "MEETING_CHECKIN:" + token;
            
            Map<String, String> params = new HashMap<>();
            params.put("content", content);
            
            String base64Image = QrcodeUtil.generateQrcodeBase64(content, width, height);
            result.put("token", token);
            result.put("content", content);
            result.put("qrcodeImage", "data:image/png;base64," + base64Image);
            result.put("width", width);
            result.put("height", height);
            result.put("timestamp", System.currentTimeMillis());
            result.put("refreshInterval", 30); // 建议刷新间隔（秒）
        } catch (Exception e) {
            return Result.fail("生成二维码失败: " + e.getMessage());
        }

        return Result.success(result);
    }

    /**
     * 生成群组邀请二维码
     */
    @GetMapping("/group/{groupId}")
    @Operation(summary = "获取群组二维码", description = "获取群组邀请二维码")
    public Result<Map<String, Object>> getGroupQrcode(
            @PathVariable Long groupId,
            @RequestParam(required = false) String groupCode) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String content = String.format("GROUP_JOIN:{\"groupId\":%d,\"code\":\"%s\",\"t\":%d}", 
                groupId, 
                groupCode != null ? groupCode : "", 
                System.currentTimeMillis());
            
            String base64Image = QrcodeUtil.generateQrcodeBase64(content, defaultWidth, defaultHeight);
            result.put("content", content);
            result.put("qrcodeImage", "data:image/png;base64," + base64Image);
            result.put("width", defaultWidth);
            result.put("height", defaultHeight);
            result.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            return Result.fail("生成二维码失败: " + e.getMessage());
        }

        return Result.success(result);
    }
}
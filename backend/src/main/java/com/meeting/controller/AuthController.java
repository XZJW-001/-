package com.meeting.controller;

import com.meeting.common.Result;
import com.meeting.common.exception.BusinessException;
import com.meeting.dto.LoginRequest;
import com.meeting.entity.SysUser;
import com.meeting.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、登出、密码修改等接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户名密码登录，返回Token和用户信息")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        Map<String, Object> result = authService.login(loginRequest);
        return Result.success("登录成功", result);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "退出登录")
    public Result<Void> logout(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        authService.logout(userId);
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/userInfo")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public Result<SysUser> getUserInfo(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        SysUser user = authService.getCurrentUser(userId);
        // 移除密码字段
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前用户的登录密码")
    public Result<Void> changePassword(
            HttpServletRequest request,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        Long userId = getCurrentUserId(request);
        authService.changePassword(userId, oldPassword, newPassword);
        return Result.success("密码修改成功", null);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Long id) {
            return id;
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        throw new BusinessException(401, "Unauthorized");
    }
}

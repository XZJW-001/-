package com.meeting.service;

import com.meeting.dto.LoginRequest;
import com.meeting.entity.SysUser;

import java.util.Map;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 登录结果（包含token和用户信息）
     */
    Map<String, Object> login(LoginRequest loginRequest);

    /**
     * 用户登出
     * @param userId 用户ID
     */
    void logout(Long userId);

    /**
     * 获取当前登录用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    SysUser getCurrentUser(Long userId);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}

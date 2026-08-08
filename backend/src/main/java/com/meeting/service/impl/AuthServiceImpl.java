package com.meeting.service.impl;

import com.meeting.common.exception.BusinessException;
import com.meeting.config.JwtConfig;
import com.meeting.dto.LoginRequest;
import com.meeting.entity.SysUser;
import com.meeting.mapper.SysUserMapper;
import com.meeting.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    @Override
    public Map<String, Object> login(LoginRequest loginRequest) {
        SysUser user = userMapper.findByUsername(loginRequest.getUsername());
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }

        String roleCode = getUserRoleCode(user.getUserType());

        String token = jwtConfig.generateToken(user.getId(), user.getUsername(), roleCode);

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功: {}", user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("phone", user.getPhone());
        userInfo.put("email", user.getEmail());
        userInfo.put("deptId", user.getDeptId());
        userInfo.put("position", user.getPosition());
        userInfo.put("userType", user.getUserType());
        userInfo.put("roleCode", roleCode);
        result.put("user", userInfo);

        return result;
    }

    @Override
    public void logout(Long userId) {
        log.info("用户登出: userId={}", userId);
    }

    @Override
    public SysUser getCurrentUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(401, "旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("用户修改密码成功: userId={}", userId);
    }

    private String getUserRoleCode(Integer userType) {
        return switch (userType) {
            case 1 -> "SUPER_ADMIN";
            case 2 -> "ADMIN";
            case 3 -> "LEADER";
            default -> "USER";
        };
    }
}

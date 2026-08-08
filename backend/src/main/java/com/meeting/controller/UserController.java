package com.meeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meeting.annotation.RequireRole;
import com.meeting.common.Result;
import com.meeting.entity.SysUser;
import com.meeting.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;

    @GetMapping("/list")
    @RequireRole({"1", "2"})
    public Result<List<SysUser>> getAllUsers() {
        List<SysUser> users = userMapper.findAll();
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @GetMapping("/page")
    @RequireRole({"1", "2"})
    public Result<Page<SysUser>> getUserList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long deptId) {

        Page<SysUser> page = new Page<>(current, size);
        Map<String, Object> params = new HashMap<>();
        if (username != null && !username.isEmpty()) {
            params.put("username", username);
        }
        if (deptId != null) {
            params.put("deptId", deptId);
        }

        Page<SysUser> result = userMapper.selectPageList(page, params);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<SysUser> getUserDetail(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PostMapping
    @RequireRole({"1", "2"})
    public Result<SysUser> createUser(@RequestBody SysUser user) {
        user.setPassword(null);
        userMapper.insert(user);
        return Result.success("用户创建成功", user);
    }

    @PutMapping("/{id}")
    @RequireRole({"1", "2"})
    public Result<SysUser> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        user.setPassword(null);
        userMapper.updateById(user);
        return Result.success("用户更新成功", user);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"1"})
    public Result<Void> deleteUser(@PathVariable Long id) {
        userMapper.deleteById(id);
        return Result.success("用户删除成功", null);
    }
}

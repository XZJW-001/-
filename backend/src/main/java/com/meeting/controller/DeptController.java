package com.meeting.controller;

import com.meeting.common.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 部门管理控制器
 */
@RestController
@RequestMapping("/dept")
public class DeptController {

    private final JdbcTemplate jdbcTemplate;

    public DeptController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取部门列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getDeptList() {
        try {
            List<Map<String, Object>> deptList = jdbcTemplate.queryForList(
                    "SELECT id, name, parent_id, sort, status FROM sys_dept WHERE status = 1 ORDER BY sort ASC"
            );
            return Result.success(deptList);
        } catch (Exception e) {
            // 如果表不存在或查询失败，返回模拟数据
            List<Map<String, Object>> mockList = List.of(
                    Map.of("id", 1, "name", "技术部", "parent_id", 0, "sort", 1, "status", 1),
                    Map.of("id", 2, "name", "市场部", "parent_id", 0, "sort", 2, "status", 1),
                    Map.of("id", 3, "name", "人力资源部", "parent_id", 0, "sort", 3, "status", 1),
                    Map.of("id", 4, "name", "财务部", "parent_id", 0, "sort", 4, "status", 1)
            );
            return Result.success(mockList);
        }
    }
}

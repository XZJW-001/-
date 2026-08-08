package com.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meeting.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser findByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     */
    SysUser findByPhone(@Param("phone") String phone);

    /**
     * 根据部门ID查询用户列表
     */
    List<SysUser> findByDeptId(@Param("deptId") Long deptId);

    /**
     * 查询所有用户
     */
    List<SysUser> findAll();

    /**
     * 分页查询用户列表
     */
    Page<SysUser> selectPageList(Page<SysUser> page, @Param("params") Map<String, Object> params);
}

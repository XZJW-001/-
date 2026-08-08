-- =============================================
-- 会议签到与数据统计系统 数据库脚本
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- =============================================

CREATE DATABASE IF NOT EXISTS `meeting_checkin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `meeting_checkin`;

-- =============================================
-- 1. 部门表
-- =============================================
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父部门ID',
  `dept_name` VARCHAR(50) NOT NULL COMMENT '部门名称',
  `order_num` INT DEFAULT 0 COMMENT '显示顺序',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常 0-停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- =============================================
-- 2. 角色表
-- =============================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常 0-停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- =============================================
-- 3. 用户表
-- =============================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
  `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门ID',
  `position` VARCHAR(50) DEFAULT NULL COMMENT '职位',
  `user_type` TINYINT DEFAULT 1 COMMENT '用户类型：1-普通用户 2-管理员 3-会议领导',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常 0-停用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================
-- 4. 用户角色关联表
-- =============================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- =============================================
-- 5. 会议表
-- =============================================
DROP TABLE IF EXISTS `meeting`;
CREATE TABLE `meeting` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会议ID',
  `title` VARCHAR(200) NOT NULL COMMENT '会议主题',
  `description` TEXT DEFAULT NULL COMMENT '会议描述',
  `location` VARCHAR(200) NOT NULL COMMENT '会议地点',
  `start_time` DATETIME NOT NULL COMMENT '会议开始时间',
  `end_time` DATETIME NOT NULL COMMENT '会议结束时间',
  `checkin_start_time` DATETIME NOT NULL COMMENT '签到开始时间',
  `checkin_end_time` DATETIME NOT NULL COMMENT '签到结束时间',
  `late_time` INT DEFAULT 15 COMMENT '迟到时间阈值（分钟）',
  `qrcode_token` VARCHAR(100) NOT NULL COMMENT '二维码唯一标识Token',
  `qrcode_expire` DATETIME DEFAULT NULL COMMENT '二维码过期时间',
  `sign_methods` JSON DEFAULT NULL COMMENT '支持的签到方式列表',
  `gesture_password` VARCHAR(50) DEFAULT NULL COMMENT '手势签到密码（九宫格点位序列，如 0-1-2-5-8）',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-进行中 3-已结束',
  `creator_id` BIGINT DEFAULT NULL COMMENT '创建人ID',
  `group_id` BIGINT DEFAULT NULL COMMENT '所属群组ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qrcode_token` (`qrcode_token`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_status` (`status`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议表';

-- =============================================
-- 6. 会议参会人员表
-- =============================================
DROP TABLE IF EXISTS `meeting_attendee`;
CREATE TABLE `meeting_attendee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `meeting_id` BIGINT NOT NULL COMMENT '会议ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `status` TINYINT DEFAULT 0 COMMENT '参会状态：0-未签到 1-已签到 2-迟到 3-缺勤',
  `sign_time` DATETIME DEFAULT NULL COMMENT '签到时间',
  `sign_method` VARCHAR(30) DEFAULT NULL COMMENT '签到方式：qrcode/photo/gesture/location',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_meeting_user` (`meeting_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议参会人员表';

-- =============================================
-- 7. 签到记录表
-- =============================================
DROP TABLE IF EXISTS `check_in_record`;
CREATE TABLE `check_in_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `meeting_id` BIGINT NOT NULL COMMENT '会议ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `sign_method` VARCHAR(30) NOT NULL COMMENT '签到方式：qrcode/photo/gesture/location',
  `sign_time` DATETIME NOT NULL COMMENT '签到时间',
  `sign_status` TINYINT DEFAULT 1 COMMENT '签到状态：1-正常 2-迟到 3-无效',
  `location` VARCHAR(500) DEFAULT NULL COMMENT '签到位置信息',
  `device_info` VARCHAR(500) DEFAULT NULL COMMENT '设备信息',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `verify_data` JSON DEFAULT NULL COMMENT '验证数据（照片、手势密码等）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_meeting_id` (`meeting_id`),
  KEY `idx_meeting_method_time` (`meeting_id`, `sign_method`, `sign_time`, `id`),
  KEY `idx_meeting_time` (`meeting_id`, `sign_time`, `id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sign_time` (`sign_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录表';

-- =============================================
-- 8. 补签申请表
-- =============================================
DROP TABLE IF EXISTS `make_up_apply`;
CREATE TABLE `make_up_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `meeting_id` BIGINT NOT NULL COMMENT '会议ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `reason` VARCHAR(500) NOT NULL COMMENT '补签原因',
  `proof_url` VARCHAR(500) DEFAULT NULL COMMENT '证明材料URL',
  `status` TINYINT DEFAULT 0 COMMENT '审批状态：0-待审批 1-已通过 2-已拒绝',
  `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `approve_remark` VARCHAR(500) DEFAULT NULL COMMENT '审批备注',
  `original_sign_time` DATETIME DEFAULT NULL COMMENT '原签到时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_meeting_id` (`meeting_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补签申请表';

-- =============================================
-- 9. 代签记录表
-- =============================================
DROP TABLE IF EXISTS `proxy_sign_record`;
CREATE TABLE `proxy_sign_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `meeting_id` BIGINT NOT NULL COMMENT '会议ID',
  `proxy_user_id` BIGINT NOT NULL COMMENT '代签人ID',
  `target_user_id` BIGINT NOT NULL COMMENT '被代签人ID',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '代签原因',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-待确认 1-已确认 2-已拒绝',
  `sign_time` DATETIME NOT NULL COMMENT '代签时间',
  `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_meeting_id` (`meeting_id`),
  KEY `idx_proxy_user_id` (`proxy_user_id`),
  KEY `idx_target_user_id` (`target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代签记录表';

-- =============================================
-- 10. 代签申请表
-- =============================================
DROP TABLE IF EXISTS `proxy_sign_apply`;
CREATE TABLE `proxy_sign_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `meeting_id` BIGINT NOT NULL COMMENT '会议ID',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
  `proxy_user_id` BIGINT NOT NULL COMMENT '拟代签人ID',
  `reason` VARCHAR(500) NOT NULL COMMENT '申请原因',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-待审批 1-已通过 2-已驳回 3-已撤销',
  `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `approve_remark` VARCHAR(500) DEFAULT NULL COMMENT '审批说明',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_proxy_apply_meeting_status` (`meeting_id`, `status`, `create_time`),
  KEY `idx_proxy_apply_applicant_status` (`applicant_id`, `status`, `create_time`),
  KEY `idx_proxy_apply_proxy_meeting` (`proxy_user_id`, `meeting_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代签申请表';

-- =============================================
-- 11. 系统通知表
-- =============================================
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` TEXT COMMENT '通知内容',
  `type` TINYINT DEFAULT 1 COMMENT '类型：1-签到提醒 2-会议通知 3-系统通知',
  `target_type` TINYINT DEFAULT 1 COMMENT '目标类型：1-全体 2-指定部门 3-指定用户',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-已发送 0-草稿',
  `creator_id` BIGINT DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';

-- =============================================
-- 11. 用户通知记录表
-- =============================================
DROP TABLE IF EXISTS `user_notice`;
CREATE TABLE `user_notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `notice_id` BIGINT NOT NULL COMMENT '通知ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_notice_id` (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知记录表';

-- =============================================
-- 12. 反馈表
-- =============================================
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `user_id` BIGINT NOT NULL COMMENT '提交人ID',
  `title` VARCHAR(200) NOT NULL COMMENT '反馈标题',
  `content` TEXT COMMENT '反馈内容',
  `category` VARCHAR(30) DEFAULT NULL COMMENT '分类：功能建议/问题反馈/其他',
  `status` TINYINT DEFAULT 0 COMMENT '处理状态：0-待处理 1-处理中 2-已处理',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `handle_result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈表';

-- =============================================
-- 初始化数据
-- =============================================

-- 初始化部门数据
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `order_num`) VALUES
(1, 0, '总公司', 0),
(2, 1, '技术部', 1),
(3, 1, '市场部', 2),
(4, 1, '人力资源部', 3),
(5, 2, '前端组', 1),
(6, 2, '后端组', 2);

-- 初始化角色数据
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有所有权限'),
(2, '管理员', 'ADMIN', '会议管理、签到监控、数据统计'),
(3, '会议领导', 'LEADER', '查看签到情况、数据统计'),
(4, '普通用户', 'USER', '签到、查看个人记录');

-- 初始化用户数据（密码均为 123456 的加密版本）
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `dept_id`, `position`, `user_type`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', '13800138000', 'admin@example.com', 1, '系统管理员', 2, 1),
(2, 'leader', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张总', '13800138001', 'leader@example.com', 2, '技术总监', 3, 1),
(3, 'user01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', '13800138002', 'zhangsan@example.com', 5, '前端工程师', 1, 1),
(4, 'user02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', '13800138003', 'lisi@example.com', 6, '后端工程师', 1, 1),
(5, 'user03', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', '13800138004', 'wangwu@example.com', 3, '市场经理', 1, 1),
(6, 'user04', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵六', '13800138005', 'zhaoliu@example.com', 4, 'HR经理', 1, 1);

-- =============================================
-- 13. 群组/组织表
-- =============================================
DROP TABLE IF EXISTS `meeting_group`;
CREATE TABLE `meeting_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群组ID',
  `group_name` VARCHAR(100) NOT NULL COMMENT '群组名称',
  `group_code` VARCHAR(20) NOT NULL COMMENT '群组编号（用于搜索添加）',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '群组头像',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '群组描述',
  `owner_id` BIGINT NOT NULL COMMENT '群主ID',
  `max_members` INT DEFAULT 500 COMMENT '最大成员数',
  `member_count` INT DEFAULT 0 COMMENT '当前成员数',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常 0-解散',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_code` (`group_code`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组/组织表';

-- =============================================
-- 14. 群组成员表
-- =============================================
DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `group_id` BIGINT NOT NULL COMMENT '群组ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` TINYINT DEFAULT 1 COMMENT '群内角色：1-普通成员 2-管理员 3-群主',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '群内昵称',
  `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常 0-已退出',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组成员表';

-- =============================================
-- 15. 群组邀请/申请表
-- =============================================
DROP TABLE IF EXISTS `group_invite`;
CREATE TABLE `group_invite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `group_id` BIGINT NOT NULL COMMENT '群组ID',
  `user_id` BIGINT NOT NULL COMMENT '申请/被邀请用户ID',
  `invite_code` VARCHAR(50) DEFAULT NULL COMMENT '邀请码',
  `type` TINYINT DEFAULT 1 COMMENT '类型：1-申请加入 2-邀请加入',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-待处理 1-已通过 2-已拒绝',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组邀请/申请表';

-- 初始化用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 3),
(3, 4),
(4, 4),
(5, 4),
(6, 4);

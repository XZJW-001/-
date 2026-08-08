-- =============================================
-- 11. 群聊消息表
-- =============================================
DROP TABLE IF EXISTS `group_message`;
CREATE TABLE `group_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `group_id` BIGINT NOT NULL COMMENT '群组ID',
  `user_id` BIGINT NOT NULL COMMENT '发送用户ID（0为系统消息）',
  `type` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '消息类型：text文本 meeting会议卡片 checkin签到卡片 system系统',
  `content` TEXT COMMENT '消息内容文本',
  `extra` TEXT COMMENT '附加数据JSON',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊消息表';

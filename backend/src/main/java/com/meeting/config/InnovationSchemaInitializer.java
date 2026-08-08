package com.meeting.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates the additive tables used by the innovation module without touching
 * existing meeting or attendance data.
 */
@Slf4j
@Order(10)
@Component
@RequiredArgsConstructor
public class InnovationSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> statements = List.of(
                """
                CREATE TABLE IF NOT EXISTS meeting_feature_config (
                  meeting_id BIGINT NOT NULL,
                  dynamic_qr_enabled TINYINT DEFAULT 0,
                  qr_refresh_seconds INT DEFAULT 20,
                  require_location TINYINT DEFAULT 0,
                  require_photo TINYINT DEFAULT 0,
                  venue_latitude DECIMAL(10,7) DEFAULT NULL,
                  venue_longitude DECIMAL(10,7) DEFAULT NULL,
                  radius_meters INT DEFAULT 300,
                  offline_allowed TINYINT DEFAULT 1,
                  offline_max_minutes INT DEFAULT 120,
                  reminder_enabled TINYINT DEFAULT 1,
                  reminder_minutes INT DEFAULT 30,
                  updated_by BIGINT DEFAULT NULL,
                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (meeting_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议智能签到配置'
                """,
                """
                CREATE TABLE IF NOT EXISTS checkin_risk (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  record_id BIGINT NOT NULL,
                  meeting_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  risk_score INT DEFAULT 0,
                  risk_level VARCHAR(20) DEFAULT 'LOW',
                  reasons JSON DEFAULT NULL,
                  review_status TINYINT DEFAULT 0,
                  reviewer_id BIGINT DEFAULT NULL,
                  review_remark VARCHAR(500) DEFAULT NULL,
                  review_time DATETIME DEFAULT NULL,
                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_risk_record (record_id),
                  KEY idx_risk_meeting (meeting_id),
                  KEY idx_risk_level (risk_level),
                  KEY idx_risk_meeting_score (meeting_id, risk_score, create_time, id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到风险记录'
                """,
                """
                CREATE TABLE IF NOT EXISTS offline_checkin_receipt (
                  client_request_id VARCHAR(80) NOT NULL,
                  meeting_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  record_id BIGINT DEFAULT NULL,
                  result_json JSON DEFAULT NULL,
                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (client_request_id),
                  KEY idx_offline_user (user_id),
                  KEY idx_offline_meeting (meeting_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='离线签到幂等回执'
                """,
                """
                CREATE TABLE IF NOT EXISTS meeting_guest_invite (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  meeting_id BIGINT NOT NULL,
                  invite_token VARCHAR(80) NOT NULL,
                  expire_time DATETIME NOT NULL,
                  status TINYINT DEFAULT 1,
                  creator_id BIGINT DEFAULT NULL,
                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_guest_invite_token (invite_token),
                  KEY idx_guest_invite_meeting (meeting_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客邀请'
                """,
                """
                CREATE TABLE IF NOT EXISTS meeting_guest (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  meeting_id BIGINT NOT NULL,
                  invite_id BIGINT NOT NULL,
                  guest_name VARCHAR(60) NOT NULL,
                  organization VARCHAR(120) DEFAULT NULL,
                  phone VARCHAR(30) DEFAULT NULL,
                  sign_time DATETIME NOT NULL,
                  device_info VARCHAR(500) DEFAULT NULL,
                  ip_address VARCHAR(50) DEFAULT NULL,
                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_guest_phone (invite_id, phone),
                  KEY idx_guest_meeting (meeting_id),
                  KEY idx_guest_meeting_time (meeting_id, sign_time, id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议访客签到'
                """,
                """
                CREATE TABLE IF NOT EXISTS smart_reminder (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  meeting_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  reminder_type VARCHAR(30) NOT NULL,
                  content VARCHAR(500) NOT NULL,
                  trigger_key VARCHAR(100) NOT NULL,
                  is_read TINYINT DEFAULT 0,
                  read_time DATETIME DEFAULT NULL,
                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_reminder_trigger (user_id, trigger_key),
                  KEY idx_reminder_user (user_id, is_read),
                  KEY idx_reminder_meeting (meeting_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能会议提醒'
                """,
                """
                CREATE TABLE IF NOT EXISTS meeting_minutes (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  meeting_id BIGINT NOT NULL,
                  source_text MEDIUMTEXT NOT NULL,
                  summary TEXT NOT NULL,
                  action_items JSON DEFAULT NULL,
                  created_by BIGINT DEFAULT NULL,
                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  KEY idx_minutes_meeting (meeting_id),
                  KEY idx_minutes_meeting_time (meeting_id, create_time, id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能会议纪要'
                """
        );

        for (String statement : statements) {
            jdbcTemplate.execute(statement);
        }
        ensureIndex("check_in_record", "idx_meeting_method_time",
                "meeting_id, sign_method, sign_time, id");
        ensureIndex("check_in_record", "idx_meeting_time", "meeting_id, sign_time, id");
        ensureIndex("checkin_risk", "idx_risk_meeting_score",
                "meeting_id, risk_score, create_time, id");
        ensureIndex("meeting_guest", "idx_guest_meeting_time", "meeting_id, sign_time, id");
        ensureIndex("meeting_minutes", "idx_minutes_meeting_time", "meeting_id, create_time, id");
        log.info("Innovation feature schema is ready");
    }

    private void ensureIndex(String tableName, String indexName, String columns) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """, Integer.class, tableName, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD INDEX `" + indexName + "` (" + columns + ")");
        }
    }
}

package com.meeting.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adds the proxy approval workflow table without modifying existing records. */
@Component
@Order(11)
@RequiredArgsConstructor
public class ProxySignSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS proxy_sign_apply (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  meeting_id BIGINT NOT NULL,
                  applicant_id BIGINT NOT NULL,
                  proxy_user_id BIGINT NOT NULL,
                  reason VARCHAR(500) NOT NULL,
                  status TINYINT DEFAULT 0,
                  approver_id BIGINT DEFAULT NULL,
                  approve_time DATETIME DEFAULT NULL,
                  approve_remark VARCHAR(500) DEFAULT NULL,
                  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  KEY idx_proxy_apply_meeting_status (meeting_id, status, create_time),
                  KEY idx_proxy_apply_applicant_status (applicant_id, status, create_time),
                  KEY idx_proxy_apply_proxy_meeting (proxy_user_id, meeting_id, status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代签申请表'
                """);
    }
}

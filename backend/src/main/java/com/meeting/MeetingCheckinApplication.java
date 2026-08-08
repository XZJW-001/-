package com.meeting;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 会议签到与数据统计系统 - 主启动类
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.meeting.mapper")
public class MeetingCheckinApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingCheckinApplication.class, args);
        System.out.println("===========================================");
        System.out.println("会议签到系统启动成功！");
        System.out.println("API文档地址: http://localhost:8080/api/doc.html");
        System.out.println("===========================================");
    }
}

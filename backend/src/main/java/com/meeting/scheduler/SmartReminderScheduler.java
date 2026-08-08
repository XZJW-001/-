package com.meeting.scheduler;

import com.meeting.service.InnovationExperienceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartReminderScheduler {

    private final InnovationExperienceService experienceService;

    @Scheduled(cron = "20 * * * * ?")
    public void createMeetingReminders() {
        try {
            experienceService.processAutomaticReminders();
        } catch (Exception e) {
            log.error("Automatic meeting reminder scan failed", e);
        }
    }
}

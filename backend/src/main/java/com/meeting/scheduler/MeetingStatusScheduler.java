package com.meeting.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.meeting.entity.Meeting;
import com.meeting.entity.MeetingAttendee;
import com.meeting.mapper.MeetingAttendeeMapper;
import com.meeting.mapper.MeetingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议状态自动流转定时任务
 *
 * 会议状态流转：
 *   0(草稿) --签到开始时间到达--> 1(已发布)
 *   1(已发布) --会议开始时间到达--> 2(进行中)
 *   2(进行中) --会议结束时间到达--> 3(已结束)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingStatusScheduler {

    private final MeetingMapper meetingMapper;
    private final MeetingAttendeeMapper attendeeMapper;

    /**
     * 每分钟检查一次，自动更新会议状态
     */
    @Scheduled(cron = "0 * * * * ?")
    public void autoUpdateMeetingStatus() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("开始检查会议状态自动流转, 当前时间: {}", now);

        try {
            // 1. 草稿 -> 已发布：签到开始时间已到
            autoPublishMeetings(now);

            // 2. 已发布 -> 进行中：会议开始时间已到
            autoStartMeetings(now);

            // 3. 进行中 -> 已结束：会议结束时间已到
            autoEndMeetings(now);

        } catch (Exception e) {
            log.error("会议状态自动流转异常", e);
        }
    }

    /**
     * 自动发布会议：草稿状态且签到开始时间已到
     */
    private void autoPublishMeetings(LocalDateTime now) {
        List<Meeting> meetings = meetingMapper.findMeetingsToPublish(now);
        if (meetings.isEmpty()) {
            return;
        }
        for (Meeting meeting : meetings) {
            meeting.setStatus(1);
            meetingMapper.updateById(meeting);
            log.info("【自动发布】会议 [{}](id={}) 签到开始时间已到, 状态由草稿变为已发布",
                    meeting.getTitle(), meeting.getId());
        }
    }

    /**
     * 自动开始会议：已发布状态且会议开始时间已到
     */
    private void autoStartMeetings(LocalDateTime now) {
        List<Meeting> meetings = meetingMapper.findMeetingsToStart(now);
        if (meetings.isEmpty()) {
            return;
        }
        for (Meeting meeting : meetings) {
            meeting.setStatus(2);
            meetingMapper.updateById(meeting);
            log.info("【自动开始】会议 [{}](id={}) 开始时间已到, 状态由已发布变为进行中",
                    meeting.getTitle(), meeting.getId());
        }
    }

    /**
     * 自动结束会议：进行中状态且会议结束时间已到
     */
    private void autoEndMeetings(LocalDateTime now) {
        List<Meeting> meetings = meetingMapper.findMeetingsToEnd(now);
        if (meetings.isEmpty()) {
            return;
        }
        for (Meeting meeting : meetings) {
            meeting.setStatus(3);
            meetingMapper.updateById(meeting);
            log.info("【自动结束】会议 [{}](id={}) 结束时间已到, 状态由进行中变为已结束",
                    meeting.getTitle(), meeting.getId());

            // 会议结束后，将未签到的参会人员标记为缺席
            markAbsentAttendees(meeting.getId());
        }
    }

    /**
     * 会议结束后，将未签到的参会人员（status=0）标记为缺席（status=3）
     */
    private void markAbsentAttendees(Long meetingId) {
        try {
            int updated = attendeeMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<MeetingAttendee>()
                            .eq("meeting_id", meetingId)
                            .eq("status", 0)
                            .set("status", 3)
                            .set("update_time", LocalDateTime.now())
            );
            if (updated > 0) {
                log.info("【自动标记缺席】会议 id={}, 已将 {} 名未签到人员标记为缺席", meetingId, updated);
            }
        } catch (Exception e) {
            log.error("标记缺席人员失败, meetingId={}", meetingId, e);
        }
    }
}

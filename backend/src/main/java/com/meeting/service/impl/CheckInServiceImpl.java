package com.meeting.service.impl;

import com.meeting.common.exception.BusinessException;
import com.meeting.dto.CheckInRequest;
import com.meeting.dto.MakeUpApplyRequest;
import com.meeting.dto.ProxySignApplyRequest;
import com.meeting.dto.ProxySignRequest;
import com.meeting.entity.*;
import com.meeting.mapper.*;
import com.meeting.service.CheckInService;
import com.meeting.service.InnovationExperienceService;
import com.meeting.service.InnovationSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 签到服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final MeetingMapper meetingMapper;
    private final MeetingAttendeeMapper attendeeMapper;
    private final CheckInRecordMapper recordMapper;
    private final MakeUpApplyMapper makeUpMapper;
    private final ProxySignApplyMapper proxySignApplyMapper;
    private final ProxySignRecordMapper proxySignMapper;
    private final SysUserMapper userMapper;
    private final InnovationSecurityService innovationSecurityService;
    private final InnovationExperienceService innovationExperienceService;

    @Value("${app.checkin.allow-proxy:true}")
    private boolean proxyAllowed;

    @Value("${app.checkin.proxy-max-count:3}")
    private int proxyMaxCount;

    @Override
    @Transactional
    public Map<String, Object> checkIn(Long meetingId, Long userId, CheckInRequest request) {
        Map<String, Object> existingReceipt = innovationSecurityService.findOfflineReceipt(
                request.getClientRequestId(), userId);
        if (existingReceipt != null) {
            return existingReceipt;
        }

        // 获取会议信息
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }

        // 验证签到方式是否支持
        List<String> signMethods = meeting.getSignMethods();
        if (signMethods != null && !signMethods.isEmpty() && !signMethods.contains(request.getSignMethod())) {
            throw new BusinessException("该会议不支持此签到方式");
        }

        // 统一验证动态二维码、组合验证和弱网凭证
        innovationSecurityService.validateRules(meeting, userId, request);

        // 合法离线签到使用现场记录时间，否则使用服务器当前时间
        LocalDateTime now = request.getOfflineSignedAt() != null
                ? request.getOfflineSignedAt() : LocalDateTime.now();
        if (now.isBefore(meeting.getCheckinStartTime())) {
            throw new BusinessException("签到还未开始");
        }
        if (now.isAfter(meeting.getCheckinEndTime())) {
            throw new BusinessException("签到已结束");
        }

        // 检查是否为参会人员
        MeetingAttendee attendee = attendeeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MeetingAttendee>()
                        .eq("meeting_id", meetingId)
                        .eq("user_id", userId)
        );
        if (attendee == null) {
            throw new BusinessException("您不是本次会议的参会人员");
        }

        // 检查是否已签到
        if (attendee.getStatus() == 1 || attendee.getStatus() == 2) {
            throw new BusinessException("您已签到，请勿重复签到");
        }

        // 验证签到方式
        verifySignMethod(request, meeting);

        // 判断是否迟到
        int signStatus = 1;
        Integer lateTime = meeting.getLateTime() != null ? meeting.getLateTime() : 15;
        LocalDateTime lateThreshold = meeting.getStartTime().plusMinutes(lateTime);
        if (now.isAfter(lateThreshold)) {
            signStatus = 2;
        }

        // 创建签到记录
        CheckInRecord record = new CheckInRecord();
        record.setMeetingId(meetingId);
        record.setUserId(userId);
        record.setSignMethod(request.getSignMethod());
        record.setSignTime(now);
        record.setSignStatus(signStatus);
        record.setLocation(request.getLocation());
        record.setLatitude(request.getLatitude());
        record.setLongitude(request.getLongitude());
        record.setDeviceInfo(request.getDeviceId() != null && !request.getDeviceId().isBlank()
                ? request.getDeviceId() : request.getDeviceInfo());
        record.setIpAddress(request.getIpAddress());
        record.setVerifyData(request.getVerifyData());
        recordMapper.insert(record);

        // 更新参会人员状态
        attendee.setStatus(signStatus);
        attendee.setSignTime(now);
        attendee.setSignMethod(request.getSignMethod());
        attendeeMapper.updateById(attendee);

        log.info("签到成功: meetingId={}, userId={}, method={}, status={}", 
                meetingId, userId, request.getSignMethod(), signStatus);

        // 返回签到结果
        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("signTime", now);
        result.put("signStatus", signStatus);
        result.put("signStatusText", signStatus == 1 ? "签到成功" : "签到成功（迟到）");
        result.put("signMethod", request.getSignMethod());

        Map<String, Object> risk = innovationSecurityService.assessAndSaveRisk(record, request);
        result.putAll(risk);
        innovationSecurityService.saveOfflineReceipt(
                request, meetingId, userId, record.getId(), result);
        innovationExperienceService.publishLiveUpdate(meetingId);

        return result;
    }

    @Override
    public Map<String, Object> adminCheckIn(Long meetingId, Long userId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }

        // 验证签到时间（与普通签到保持一致）
        LocalDateTime now = LocalDateTime.now();
        if (meeting.getCheckinStartTime() != null && now.isBefore(meeting.getCheckinStartTime())) {
            throw new BusinessException("签到还未开始");
        }
        if (meeting.getCheckinEndTime() != null && now.isAfter(meeting.getCheckinEndTime())) {
            throw new BusinessException("签到已结束");
        }

        MeetingAttendee attendee = attendeeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MeetingAttendee>()
                        .eq("meeting_id", meetingId)
                        .eq("user_id", userId)
        );
        if (attendee == null) {
            throw new BusinessException("不是本次会议的参会人员");
        }

        // 如果已签到（status=1），拒绝重复签到
        if (attendee.getStatus() == 1 || attendee.getStatus() == 2) {
            throw new BusinessException("已签到，请勿重复签到");
        }

        int signStatus = 1;
        Integer lateTime = meeting.getLateTime() != null ? meeting.getLateTime() : 15;
        LocalDateTime lateThreshold = meeting.getStartTime().plusMinutes(lateTime);
        if (now.isAfter(lateThreshold)) {
            signStatus = 2;
        }

        attendee.setStatus(signStatus);
        attendee.setSignTime(now);
        attendee.setSignMethod("qrcode");
        attendeeMapper.updateById(attendee);

        CheckInRecord record = new CheckInRecord();
        record.setMeetingId(meetingId);
        record.setUserId(userId);
        record.setSignMethod("qrcode");
        record.setSignTime(now);
        record.setSignStatus(signStatus);
        recordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("signTime", now);
        result.put("signStatus", signStatus);
        result.put("signStatusText", signStatus == 1 ? "签到成功" : "签到成功（迟到）");
        result.put("signMethod", "qrcode");
        return result;
    }

    /**
     * 验证签到方式
     */
    private void verifySignMethod(CheckInRequest request, Meeting meeting) {
        switch (request.getSignMethod()) {
            case "qrcode":
                // 二维码签到：验证Token
                String token = request.getQrcodeToken();
                // 如果未传 token，使用会议自身的 token（群内直接签到场景）
                if (token == null || token.isEmpty()) {
                    token = meeting.getQrcodeToken();
                }
                if (token == null || token.isEmpty()) {
                    throw new BusinessException("请扫描会议二维码");
                }
                Meeting qrMeeting = meetingMapper.findByQrcodeToken(token);
                if (qrMeeting == null || !qrMeeting.getId().equals(meeting.getId())) {
                    throw new BusinessException("二维码无效");
                }
                break;
            case "photo":
                // 拍照签到：验证照片
                if (request.getVerifyData() == null) {
                    throw new BusinessException("请上传签到照片");
                }
                break;
            case "location":
                // 定位签到：验证位置
                if (request.getLatitude() == null || request.getLongitude() == null) {
                    throw new BusinessException("请开启定位服务");
                }
                break;
            case "gesture":
                // 手势签到：验证手势密码是否匹配
                if (request.getVerifyData() == null || request.getVerifyData().get("gesturePattern") == null) {
                    throw new BusinessException("请绘制手势密码");
                }
                String inputPattern = String.valueOf(request.getVerifyData().get("gesturePattern")).trim();
                String expectedPattern = meeting.getGesturePassword();
                if (expectedPattern == null || expectedPattern.isEmpty()) {
                    throw new BusinessException("该会议未设置手势密码，请联系管理员");
                }
                if (!expectedPattern.equals(inputPattern)) {
                    throw new BusinessException("手势密码错误，请重新绘制");
                }
                break;
            default:
                throw new BusinessException("不支持的签到方式");
        }
    }

    @Override
    public List<Map<String, Object>> getCheckInRecordsByMeeting(Long meetingId) {
        List<CheckInRecord> records = recordMapper.findByMeetingId(meetingId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CheckInRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("meetingId", record.getMeetingId());
            item.put("userId", record.getUserId());
            item.put("signMethod", record.getSignMethod());
            item.put("signTime", record.getSignTime());
            item.put("signStatus", record.getSignStatus());
            item.put("location", record.getLocation());
            item.put("latitude", record.getLatitude());
            item.put("longitude", record.getLongitude());
            item.put("deviceInfo", record.getDeviceInfo());
            
            // 获取用户信息（嵌套结构，供前端表格使用）
            SysUser user = userMapper.selectById(record.getUserId());
            if (user != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("realName", user.getRealName());
                userMap.put("username", user.getUsername());
                userMap.put("avatar", user.getAvatar());
                userMap.put("position", user.getPosition());
                userMap.put("deptId", user.getDeptId());
                item.put("user", userMap);
            }
            
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getCheckInRecordsByUser(Long userId) {
        List<CheckInRecord> records = recordMapper.findByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (CheckInRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("meetingId", record.getMeetingId());
            item.put("signMethod", record.getSignMethod());
            item.put("signTime", record.getSignTime() != null ? record.getSignTime().format(formatter) : null);
            item.put("signStatus", record.getSignStatus());
            item.put("signStatusText", record.getSignStatus() == 1 ? "正常" : 
                    record.getSignStatus() == 2 ? "迟到" : "无效");
            
            // 获取会议信息
            Meeting meeting = meetingMapper.selectById(record.getMeetingId());
            if (meeting != null) {
                item.put("meetingTitle", meeting.getTitle());
                item.put("meetingLocation", meeting.getLocation());
            }
            
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> applyMakeUp(Long meetingId, Long userId, MakeUpApplyRequest request) {
        // 检查会议是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }

        // 检查是否为参会人员
        MeetingAttendee attendee = attendeeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MeetingAttendee>()
                        .eq("meeting_id", meetingId)
                        .eq("user_id", userId)
        );
        if (attendee == null) {
            throw new BusinessException("您不是本次会议的参会人员");
        }

        // 检查是否已签到
        if (attendee.getStatus() == 1 || attendee.getStatus() == 2) {
            throw new BusinessException("您已签到，无需补签");
        }

        // 创建补签申请
        MakeUpApply apply = new MakeUpApply();
        apply.setMeetingId(meetingId);
        apply.setUserId(userId);
        apply.setReason(request.getReason());
        apply.setProofUrl(request.getProofUrl());
        apply.setStatus(0); // 待审批
        apply.setOriginalSignTime(request.getOriginalSignTime());
        makeUpMapper.insert(apply);

        log.info("创建补签申请: meetingId={}, userId={}, reason={}", meetingId, userId, request.getReason());

        Map<String, Object> result = new HashMap<>();
        result.put("applyId", apply.getId());
        result.put("status", 0);
        result.put("statusText", "待审批");
        result.put("message", "补签申请已提交，请等待审批");

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> approveMakeUp(Long applyId, Long approverId, Integer status, String remark) {
        MakeUpApply apply = makeUpMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException("补签申请不存在");
        }

        // 更新申请状态
        apply.setStatus(status);
        apply.setApproverId(approverId);
        apply.setApproveTime(LocalDateTime.now());
        apply.setApproveRemark(remark);
        makeUpMapper.updateById(apply);

        // 如果审批通过，更新参会人员状态
        if (status == 1) {
            MeetingAttendee attendee = attendeeMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MeetingAttendee>()
                            .eq("meeting_id", apply.getMeetingId())
                            .eq("user_id", apply.getUserId())
            );
            if (attendee != null) {
                attendee.setStatus(1); // 已签到
                attendee.setSignTime(LocalDateTime.now());
                attendee.setSignMethod("makeup");
                attendeeMapper.updateById(attendee);
            }

            // 添加签到记录
            CheckInRecord record = new CheckInRecord();
            record.setMeetingId(apply.getMeetingId());
            record.setUserId(apply.getUserId());
            record.setSignMethod("makeup");
            record.setSignTime(LocalDateTime.now());
            record.setSignStatus(1);
            record.setRemark("补签：" + apply.getReason());
            recordMapper.insert(record);
        }

        log.info("审批补签: applyId={}, status={}", applyId, status);

        Map<String, Object> result = new HashMap<>();
        result.put("applyId", applyId);
        result.put("status", status);
        result.put("statusText", status == 1 ? "已通过" : "已拒绝");

        return result;
    }

    @Override
    public List<Map<String, Object>> getMakeUpList(Long meetingId, Integer status) {
        List<MakeUpApply> applies;
        if (status != null) {
            applies = makeUpMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MakeUpApply>()
                            .eq("meeting_id", meetingId)
                            .eq("status", status)
                            .orderByDesc("create_time")
            );
        } else {
            applies = makeUpMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MakeUpApply>()
                            .eq("meeting_id", meetingId)
                            .orderByDesc("create_time")
            );
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (MakeUpApply apply : applies) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", apply.getId());
            item.put("meetingId", apply.getMeetingId());
            item.put("userId", apply.getUserId());
            item.put("reason", apply.getReason());
            item.put("proofUrl", apply.getProofUrl());
            item.put("status", apply.getStatus());
            item.put("statusText", apply.getStatus() == 0 ? "待审批" : 
                    apply.getStatus() == 1 ? "已通过" : "已拒绝");
            item.put("applyTime", apply.getCreateTime());
            item.put("approveRemark", apply.getApproveRemark());
            
            // 获取用户信息
            SysUser user = userMapper.selectById(apply.getUserId());
            if (user != null) {
                item.put("userName", user.getRealName());
            }
            
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getAllMakeUpList(Integer status) {
        List<MakeUpApply> applies;
        if (status != null) {
            applies = makeUpMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MakeUpApply>()
                            .eq("status", status)
                            .orderByDesc("create_time")
            );
        } else {
            applies = makeUpMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MakeUpApply>()
                            .orderByDesc("create_time")
            );
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (MakeUpApply apply : applies) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", apply.getId());
            item.put("meetingId", apply.getMeetingId());
            item.put("userId", apply.getUserId());
            item.put("reason", apply.getReason());
            item.put("status", apply.getStatus());
            item.put("statusText", apply.getStatus() == 0 ? "待审批" :
                    apply.getStatus() == 1 ? "已通过" : "已拒绝");
            item.put("createTime", apply.getCreateTime());
            item.put("approveRemark", apply.getApproveRemark());

            SysUser user = userMapper.selectById(apply.getUserId());
            if (user != null) {
                item.put("userName", user.getRealName());
            }
            Meeting meeting = meetingMapper.selectById(apply.getMeetingId());
            if (meeting != null) {
                item.put("meetingTitle", meeting.getTitle());
            }

            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> applyProxySign(Long meetingId, Long applicantId, ProxySignApplyRequest request) {
        requireProxyEnabled();
        if (applicantId.equals(request.getProxyUserId())) {
            throw new BusinessException("代签人不能选择本人");
        }

        Meeting meeting = requireProxyApplicationMeeting(meetingId);
        MeetingAttendee applicant = requireUnsignedAttendee(meetingId, applicantId, true);
        if (applicant.getStatus() != 0) {
            throw new BusinessException("当前签到状态不支持提交代签申请");
        }
        requireProxyCandidate(meetingId, request.getProxyUserId());
        assertProxyCapacity(meetingId, request.getProxyUserId());

        Long pendingCount = proxySignApplyMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignApply>()
                        .eq("meeting_id", meetingId)
                        .eq("applicant_id", applicantId)
                        .eq("status", 0));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("该会议已有待审批的代签申请");
        }

        ProxySignApply apply = new ProxySignApply();
        apply.setMeetingId(meeting.getId());
        apply.setApplicantId(applicantId);
        apply.setProxyUserId(request.getProxyUserId());
        apply.setReason(request.getReason().trim());
        apply.setStatus(0);
        proxySignApplyMapper.insert(apply);

        return Map.of(
                "applyId", apply.getId(),
                "status", 0,
                "statusText", "待审批",
                "message", "代签申请已提交，请等待管理员审批"
        );
    }

    @Override
    @Transactional
    public Map<String, Object> approveProxySignApply(Long applyId, Long approverId, Integer status, String remark) {
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException("审批状态仅支持通过或驳回");
        }
        String approveRemark = trimToNull(remark);
        if (status == 2 && approveRemark == null) {
            throw new BusinessException("驳回代签申请时必须填写原因");
        }
        if (approveRemark != null && approveRemark.length() > 500) {
            throw new BusinessException("审批说明不能超过500个字符");
        }
        ProxySignApply apply = proxySignApplyMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignApply>()
                        .eq("id", applyId)
                        .last("FOR UPDATE"));
        if (apply == null) {
            throw new BusinessException("代签申请不存在");
        }
        if (apply.getStatus() == null || apply.getStatus() != 0) {
            throw new BusinessException("该代签申请已处理，不能重复审批");
        }

        LocalDateTime now = LocalDateTime.now();
        if (status == 1) {
            completeProxySign(apply.getMeetingId(), apply.getProxyUserId(), apply.getApplicantId(),
                    apply.getReason(), approverId, now, true);
        }

        apply.setStatus(status);
        apply.setApproverId(approverId);
        apply.setApproveTime(now);
        apply.setApproveRemark(approveRemark);
        proxySignApplyMapper.updateById(apply);

        return Map.of(
                "applyId", applyId,
                "status", status,
                "statusText", status == 1 ? "已通过" : "已驳回"
        );
    }

    @Override
    @Transactional
    public void cancelProxySignApply(Long applyId, Long applicantId) {
        ProxySignApply apply = proxySignApplyMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignApply>()
                        .eq("id", applyId)
                        .last("FOR UPDATE"));
        if (apply == null || !applicantId.equals(apply.getApplicantId())) {
            throw new BusinessException("代签申请不存在或无权撤销");
        }
        if (apply.getStatus() == null || apply.getStatus() != 0) {
            throw new BusinessException("仅待审批的代签申请可以撤销");
        }
        apply.setStatus(3);
        proxySignApplyMapper.updateById(apply);
    }

    @Override
    public List<Map<String, Object>> getMyProxySignApplications(Long applicantId) {
        List<ProxySignApply> applies = proxySignApplyMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignApply>()
                        .eq("applicant_id", applicantId)
                        .orderByDesc("create_time"));
        return toProxyApplicationMaps(applies);
    }

    @Override
    public List<Map<String, Object>> getProxySignApplications(Long meetingId, Integer status) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignApply> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignApply>()
                        .eq("meeting_id", meetingId);
        if (status != null) {
            wrapper.eq("status", status);
        }
        return toProxyApplicationMaps(proxySignApplyMapper.selectList(wrapper.orderByDesc("create_time")));
    }

    @Override
    public List<Map<String, Object>> getAllProxySignApplications(Integer status) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignApply> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        return toProxyApplicationMaps(proxySignApplyMapper.selectList(wrapper.orderByDesc("create_time")));
    }

    @Override
    public List<Map<String, Object>> getProxyCandidates(Long meetingId, Long applicantId) {
        requireProxyApplicationMeeting(meetingId);
        requireUnsignedAttendee(meetingId, applicantId);
        List<MeetingAttendee> attendees = attendeeMapper.findByMeetingIdWithUser(meetingId);
        Map<Long, Long> usageByProxyUser = proxySignMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignRecord>()
                                .eq("meeting_id", meetingId)
                                .eq("status", 1))
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ProxySignRecord::getProxyUserId,
                        java.util.stream.Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (MeetingAttendee attendee : attendees) {
            if (attendee.getUserId().equals(applicantId)
                    || attendee.getUser() == null
                    || attendee.getUser().getStatus() == null
                    || attendee.getUser().getStatus() != 1) {
                continue;
            }
            long used = usageByProxyUser.getOrDefault(attendee.getUserId(), 0L);
            if (used >= proxyMaxCount) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", attendee.getUserId());
            item.put("realName", attendee.getUser().getRealName());
            item.put("position", attendee.getUser().getPosition());
            item.put("avatar", attendee.getUser().getAvatar());
            item.put("remainingQuota", proxyMaxCount - used);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMyProxyEligibleMeetings(Long applicantId) {
        List<MeetingAttendee> attendees = attendeeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MeetingAttendee>()
                        .eq("user_id", applicantId)
                        .eq("status", 0)
                        .orderByDesc("create_time"));
        if (attendees.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> pendingMeetingIds = proxySignApplyMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignApply>()
                                .select("meeting_id")
                                .eq("applicant_id", applicantId)
                                .eq("status", 0))
                .stream()
                .map(ProxySignApply::getMeetingId)
                .collect(java.util.stream.Collectors.toSet());
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Meeting> meetingsById = meetingMapper.selectBatchIds(
                        attendees.stream().map(MeetingAttendee::getMeetingId).distinct().toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Meeting::getId, meeting -> meeting));
        for (MeetingAttendee attendee : attendees) {
            Meeting meeting = meetingsById.get(attendee.getMeetingId());
            if (pendingMeetingIds.contains(attendee.getMeetingId())) {
                continue;
            }
            if (meeting == null || meeting.getStatus() == null || meeting.getStatus() == 0 || meeting.getStatus() == 3) {
                continue;
            }
            if (meeting.getCheckinEndTime() != null && now.isAfter(meeting.getCheckinEndTime())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", meeting.getId());
            item.put("title", meeting.getTitle());
            item.put("location", meeting.getLocation());
            item.put("startTime", meeting.getStartTime());
            item.put("checkinEndTime", meeting.getCheckinEndTime());
            item.put("status", meeting.getStatus());
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> proxySign(Long meetingId, Long proxyUserId, ProxySignRequest request) {
        requireProxyEnabled();
        List<Long> successList = new ArrayList<>();
        List<Long> failList = new ArrayList<>();

        for (Long targetUserId : request.getTargetUserIds().stream().filter(Objects::nonNull).distinct().toList()) {
            try {
                completeProxySign(meetingId, proxyUserId, targetUserId, request.getReason(),
                        null, LocalDateTime.now(), false);
                successList.add(targetUserId);
            } catch (Exception e) {
                log.warn("代签失败: meetingId={}, targetUserId={}, reason={}", meetingId, targetUserId, e.getMessage());
                failList.add(targetUserId);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successList.size());
        result.put("failCount", failList.size());
        result.put("successUserIds", successList);
        result.put("failUserIds", failList);
        result.put("message", String.format("代签完成：成功%d人，失败%d人", successList.size(), failList.size()));

        return result;
    }

    private Meeting requireProxyApplicationMeeting(Long meetingId) {
        requireProxyEnabled();
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        if (meeting.getStatus() == null || meeting.getStatus() == 0 || meeting.getStatus() == 3) {
            throw new BusinessException("当前会议不支持代签申请");
        }
        if (meeting.getCheckinEndTime() != null && LocalDateTime.now().isAfter(meeting.getCheckinEndTime())) {
            throw new BusinessException("签到已结束，不能提交代签申请");
        }
        return meeting;
    }

    private MeetingAttendee requireUnsignedAttendee(Long meetingId, Long userId) {
        return requireUnsignedAttendee(meetingId, userId, false);
    }

    private MeetingAttendee requireUnsignedAttendee(Long meetingId, Long userId, boolean lockForUpdate) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MeetingAttendee> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MeetingAttendee>()
                        .eq("meeting_id", meetingId)
                        .eq("user_id", userId);
        if (lockForUpdate) {
            wrapper.last("FOR UPDATE");
        }
        MeetingAttendee attendee = attendeeMapper.selectOne(wrapper);
        if (attendee == null) {
            throw new BusinessException("您不是本次会议的参会人员");
        }
        if (attendee.getStatus() == 1 || attendee.getStatus() == 2) {
            throw new BusinessException("该用户已完成签到，不能代签");
        }
        return attendee;
    }

    private void requireProxyCandidate(Long meetingId, Long proxyUserId) {
        SysUser proxyUser = userMapper.selectById(proxyUserId);
        if (proxyUser == null || proxyUser.getStatus() == null || proxyUser.getStatus() != 1) {
            throw new BusinessException("代签人不存在或已被停用");
        }
        MeetingAttendee proxyAttendee = attendeeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MeetingAttendee>()
                        .eq("meeting_id", meetingId)
                        .eq("user_id", proxyUserId));
        if (proxyAttendee == null) {
            throw new BusinessException("代签人必须是本次会议的参会人员");
        }
    }

    private void assertProxyCapacity(Long meetingId, Long proxyUserId) {
        Long used = proxySignMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignRecord>()
                        .eq("meeting_id", meetingId)
                        .eq("proxy_user_id", proxyUserId)
                        .eq("status", 1));
        if (used != null && used >= proxyMaxCount) {
            throw new BusinessException("该代签人已达到本场会议的代签上限");
        }
    }

    private void completeProxySign(Long meetingId, Long proxyUserId, Long targetUserId, String reason,
                                   Long approverId, LocalDateTime signTime, boolean requireProxyAttendee) {
        if (proxyUserId.equals(targetUserId)) {
            throw new BusinessException("不能为本人代签");
        }
        if (meetingMapper.selectById(meetingId) == null) {
            throw new BusinessException("会议不存在");
        }
        if (requireProxyAttendee) {
            requireProxyCandidate(meetingId, proxyUserId);
        }
        MeetingAttendee attendee = requireUnsignedAttendee(meetingId, targetUserId, true);
        assertProxyCapacity(meetingId, proxyUserId);

        attendee.setStatus(1);
        attendee.setSignTime(signTime);
        attendee.setSignMethod("proxy");
        attendeeMapper.updateById(attendee);

        CheckInRecord record = new CheckInRecord();
        record.setMeetingId(meetingId);
        record.setUserId(targetUserId);
        record.setSignMethod("proxy");
        record.setSignTime(signTime);
        record.setSignStatus(1);
        record.setRemark("代签：" + (hasText(reason) ? reason : "无"));
        recordMapper.insert(record);

        ProxySignRecord proxyRecord = new ProxySignRecord();
        proxyRecord.setMeetingId(meetingId);
        proxyRecord.setProxyUserId(proxyUserId);
        proxyRecord.setTargetUserId(targetUserId);
        proxyRecord.setReason(trimToNull(reason));
        proxyRecord.setStatus(1);
        proxyRecord.setSignTime(signTime);
        proxyRecord.setApproverId(approverId);
        proxyRecord.setApproveTime(approverId == null ? null : signTime);
        proxySignMapper.insert(proxyRecord);
    }

    private List<Map<String, Object>> toProxyApplicationMaps(List<ProxySignApply> applies) {
        if (applies.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Meeting> meetingsById = meetingMapper.selectBatchIds(
                        applies.stream().map(ProxySignApply::getMeetingId).distinct().toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Meeting::getId, meeting -> meeting));

        Set<Long> userIds = new HashSet<>();
        for (ProxySignApply apply : applies) {
            userIds.add(apply.getApplicantId());
            userIds.add(apply.getProxyUserId());
            if (apply.getApproverId() != null) {
                userIds.add(apply.getApproverId());
            }
        }
        Map<Long, SysUser> usersById = userMapper.selectBatchIds(userIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(SysUser::getId, user -> user));

        return applies.stream()
                .map(apply -> toProxyApplicationMap(apply, meetingsById, usersById))
                .toList();
    }

    private Map<String, Object> toProxyApplicationMap(
            ProxySignApply apply,
            Map<Long, Meeting> meetingsById,
            Map<Long, SysUser> usersById) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", apply.getId());
        item.put("meetingId", apply.getMeetingId());
        item.put("applicantId", apply.getApplicantId());
        item.put("proxyUserId", apply.getProxyUserId());
        item.put("reason", apply.getReason());
        item.put("status", apply.getStatus());
        item.put("statusText", proxyApplyStatusText(apply.getStatus()));
        item.put("approverId", apply.getApproverId());
        item.put("approveTime", apply.getApproveTime());
        item.put("approveRemark", apply.getApproveRemark());
        item.put("createTime", apply.getCreateTime());
        item.put("canCancel", apply.getStatus() != null && apply.getStatus() == 0);

        Meeting meeting = meetingsById.get(apply.getMeetingId());
        item.put("meetingTitle", meeting == null ? "会议#" + apply.getMeetingId() : meeting.getTitle());
        SysUser applicant = usersById.get(apply.getApplicantId());
        item.put("applicantName", applicant == null ? "用户#" + apply.getApplicantId() : applicant.getRealName());
        SysUser proxyUser = usersById.get(apply.getProxyUserId());
        item.put("proxyUserName", proxyUser == null ? "用户#" + apply.getProxyUserId() : proxyUser.getRealName());
        if (apply.getApproverId() != null) {
            SysUser approver = usersById.get(apply.getApproverId());
            item.put("approverName", approver == null ? "用户#" + apply.getApproverId() : approver.getRealName());
        }
        return item;
    }

    private String proxyApplyStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审批";
            case 1 -> "已通过";
            case 2 -> "已驳回";
            case 3 -> "已撤销";
            default -> "未知";
        };
    }

    private void requireProxyEnabled() {
        if (!proxyAllowed) {
            throw new BusinessException("系统当前未开放代签功能");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    @Override
    public List<Map<String, Object>> getProxySignList(Long meetingId) {
        List<ProxySignRecord> records = proxySignMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignRecord>()
                        .eq("meeting_id", meetingId)
                        .orderByDesc("create_time")
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (ProxySignRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("meetingId", record.getMeetingId());
            item.put("proxyUserId", record.getProxyUserId());
            item.put("targetUserId", record.getTargetUserId());
            item.put("reason", record.getReason());
            item.put("status", record.getStatus());
            item.put("signTime", record.getSignTime());

            // 获取用户信息
            SysUser proxyUser = userMapper.selectById(record.getProxyUserId());
            if (proxyUser != null) {
                item.put("proxyUserName", proxyUser.getRealName());
            }
            SysUser targetUser = userMapper.selectById(record.getTargetUserId());
            if (targetUser != null) {
                item.put("targetUserName", targetUser.getRealName());
            }

            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getAllProxySignList() {
        List<ProxySignRecord> records = proxySignMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProxySignRecord>()
                        .orderByDesc("create_time")
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (ProxySignRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("meetingId", record.getMeetingId());
            item.put("operatorName", "");
            item.put("targetName", "");
            item.put("reason", record.getReason());
            item.put("status", record.getStatus());
            item.put("createTime", record.getSignTime());

            SysUser proxyUser = userMapper.selectById(record.getProxyUserId());
            if (proxyUser != null) {
                item.put("operatorName", proxyUser.getRealName());
            }
            SysUser targetUser = userMapper.selectById(record.getTargetUserId());
            if (targetUser != null) {
                item.put("targetName", targetUser.getRealName());
            }
            Meeting meeting = meetingMapper.selectById(record.getMeetingId());
            if (meeting != null) {
                item.put("meetingTitle", meeting.getTitle());
            }

            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getMeetingCheckInStatus(Long meetingId, Long userId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }

        // 获取参会人员列表（带用户信息）
        List<MeetingAttendee> attendees = attendeeMapper.findByMeetingIdWithUser(meetingId);
        
        // 统计签到情况
        int totalCount = attendees.size();
        int signedCount = 0;
        int notSignedCount = 0;
        int lateCount = 0;
        int absentCount = 0;

        List<Map<String, Object>> attendeeList = new ArrayList<>();
        for (MeetingAttendee attendee : attendees) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", attendee.getId());
            item.put("userId", attendee.getUserId());
            item.put("status", attendee.getStatus());
            item.put("signTime", attendee.getSignTime());
            item.put("signMethod", attendee.getSignMethod());

            if (attendee.getUser() != null) {
                item.put("userName", attendee.getUser().getRealName());
                item.put("userAvatar", attendee.getUser().getAvatar());
                item.put("userDeptId", attendee.getUser().getDeptId());
                item.put("userPosition", attendee.getUser().getPosition());
            }

            attendeeList.add(item);

            // 统计
            switch (attendee.getStatus()) {
                case 1:
                    signedCount++;
                    break;
                case 2:
                    lateCount++;
                    break;
                case 3:
                    absentCount++;
                    break;
                default:
                    notSignedCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("meetingId", meetingId);
        result.put("meetingTitle", meeting.getTitle());
        result.put("meetingStatus", meeting.getStatus());
        result.put("totalCount", totalCount);
        result.put("signedCount", signedCount);
        result.put("notSignedCount", notSignedCount);
        result.put("lateCount", lateCount);
        result.put("absentCount", absentCount);
        result.put("attendanceRate", totalCount > 0 ? 
                Math.round((signedCount + lateCount) * 100.0 / totalCount * 100.0) / 100.0 : 0);
        result.put("attendees", attendeeList);

        return result;
    }
}

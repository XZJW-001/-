<template>
  <div class="chat-app" v-loading="loading">
    <!-- 头部：群信息 -->
    <div class="chat-header" v-if="group">
      <div class="header-left">
        <div class="group-avatar" :style="{background: avatarGradient}">{{ group.groupName?.charAt(0) }}</div>
        <div class="group-info">
          <div class="group-name">
            {{ group.groupName }}
            <el-tag size="small" class="code-tag">编号 {{ group.groupCode }}</el-tag>
          </div>
          <div class="group-sub">
            <span>{{ group.memberCount }} 人</span>
            <span class="dot">·</span>
            <span>{{ group.description || '暂无群简介' }}</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-button type="primary" plain size="small" @click="showQrcode">
          <el-icon><Picture /></el-icon>群二维码
        </el-button>
        <el-button type="primary" size="small" v-if="isManager" @click="openMeetingDialog">
          <el-icon><Plus /></el-icon>发起会议
        </el-button>
        <el-dropdown trigger="click">
          <el-button plain size="small">
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="showSettings">群设置</el-dropdown-item>
              <el-dropdown-item @click="showInvite">邀请成员</el-dropdown-item>
              <el-dropdown-item divided v-if="isManager" @click="confirmClearMessages">清空聊天记录</el-dropdown-item>
              <el-dropdown-item v-if="isManager" @click="confirmClearMeetings">清空会议记录</el-dropdown-item>
              <el-dropdown-item divided @click="confirmLeave">退出群聊</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 主体：聊天区 + 右侧面板 -->
    <div class="chat-body">
      <!-- 聊天主区域 -->
      <div class="chat-main">
        <!-- 消息列表 -->
        <div class="message-list" ref="msgListRef">
          <div v-for="msg in messages" :key="msg.id" :class="['msg-row', { 'self': msg.userId === userId, 'system': msg.type === 'system' }]">
            <!-- 系统消息 -->
            <div v-if="msg.type === 'system'" class="system-msg">{{ msg.content }}</div>

            <!-- 普通消息 -->
            <template v-else>
              <div class="msg-avatar" :style="{background: getAvatarColor(msg.userId)}">
                {{ getAvatarChar(msg) }}
              </div>
              <div class="msg-body">
                <div class="msg-meta-row">
                  <span class="msg-name">{{ msg.userName || '未知用户' }}</span>
                  <span class="msg-time">{{ formatMsgTime(msg.createTime) }}</span>
                </div>
                <!-- 文本 -->
                <div v-if="msg.type === 'text'" class="msg-bubble">{{ msg.content }}</div>
                <!-- 会议卡片 -->
                <div v-else-if="msg.type === 'meeting'" class="card-meeting" @click="openMeetingFromCard(msg)">
                  <div class="card-icon"><el-icon :size="26"><Calendar /></el-icon></div>
                  <div class="card-body">
                    <div class="card-title">{{ parseExtra(msg.extra).title || msg.content }}</div>
                    <div class="card-meta">
                      <span>📅 {{ formatShortTime(parseExtra(msg.extra).startTime) }}</span>
                      <span>📍 {{ parseExtra(msg.extra).location || '待定' }}</span>
                    </div>
                    <el-tag size="small" :type="getMeetingStatusTag(parseExtra(msg.extra).status)">
                      {{ getMeetingStatusText(parseExtra(msg.extra).status) }}
                    </el-tag>
                  </div>
                  <div class="card-arrow"><el-icon><ArrowRight /></el-icon></div>
                </div>
                <!-- 签到卡片 -->
                <div v-else-if="msg.type === 'checkin'" class="card-checkin" @click="openCheckinFromCard(msg)">
                  <div class="card-icon checkin-color"><el-icon :size="26"><Tickets /></el-icon></div>
                  <div class="card-body">
                    <div class="card-title">📋 {{ parseExtra(msg.extra).title || msg.content }}</div>
                    <div class="card-meta">
                      <span>📅 {{ formatShortTime(parseExtra(msg.extra).startTime) }}</span>
                      <span>⏰ 截止：{{ formatShortTime(parseExtra(msg.extra).checkinEndTime) }}</span>
                    </div>
                    <div class="card-meta" v-if="parseExtra(msg.extra).location">
                      <span>📍 {{ parseExtra(msg.extra).location }}</span>
                    </div>
                    <div class="card-meta">
                      <span>📝 签到方式：{{ formatSignMethods(parseExtra(msg.extra).signMethods) }}</span>
                    </div>
                    <div class="card-action-row">
                      <el-tag size="small" :type="getCheckinStatusTag(parseExtra(msg.extra).status)">会议状态：{{ getMeetingStatusText(parseExtra(msg.extra).status) }}</el-tag>
                      <el-button size="small" type="danger" plain>立即签到</el-button>
                    </div>
                  </div>
                  <div class="card-arrow"><el-icon><ArrowRight /></el-icon></div>
                </div>
              </div>
            </template>
          </div>
          <div v-if="polling && messages.length > 0" class="polling-tip">消息同步中…</div>
        </div>

        <!-- 工具栏 + 输入框 -->
        <div class="chat-input-area">
          <div class="toolbar">
            <el-tooltip content="发布会议签到">
              <el-button class="tool-btn" @click="openPickMeeting('checkin')" :disabled="!isManager">
                <el-icon :size="18"><Tickets /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="发送会议卡片">
              <el-button class="tool-btn" @click="openPickMeeting('card')" :disabled="!isManager">
                <el-icon :size="18"><Calendar /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="发起补签审批" v-if="isManager">
              <el-button class="tool-btn" @click="openApprovalInGroup">
                <el-icon :size="18"><DocumentChecked /></el-icon>
              </el-button>
            </el-tooltip>
            <span class="divider"></span>
            <el-tooltip content="群二维码">
              <el-button class="tool-btn" @click="showQrcode">
                <el-icon :size="18"><Picture /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="成员列表">
              <el-button class="tool-btn" @click="toggleRightPanel('members')">
                <el-icon :size="18"><UserFilled /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
          <div class="input-row">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="2"
              placeholder="输入消息，Enter 发送 / Shift+Enter 换行"
              resize="none"
              @keydown="handleKeydown"
            />
            <el-button type="primary" @click="sendText" :disabled="!inputText.trim()" :loading="sending">发送</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧面板 -->
      <div class="chat-side" v-show="rightPanel !== 'none'">
        <div class="side-tabs">
          <div :class="['tab', {active: rightPanel==='members'}]" @click="rightPanel='members'">成员</div>
          <div :class="['tab', {active: rightPanel==='meetings'}]" @click="rightPanel='meetings'">会议</div>
          <div :class="['tab', {active: rightPanel==='records'}]" @click="rightPanel='records'">签到记录</div>
          <div class="close" @click="rightPanel='none'"><el-icon><Close /></el-icon></div>
        </div>
        <div class="side-body">
          <!-- 成员 -->
          <div v-if="rightPanel==='members'" class="side-members">
            <div class="member-row" v-for="m in members" :key="m.userId">
              <div class="m-avatar" :style="{background: getAvatarColor(m.userId)}">{{ getAvatarChar(m) }}</div>
              <div class="m-info">
                <div class="m-name">{{ m.userName }}
                  <el-tag size="small" v-if="m.role===3" type="danger" style="margin-left:6px;">群主</el-tag>
                  <el-tag size="small" v-else-if="m.role===2" type="warning" style="margin-left:6px;">管理员</el-tag>
                </div>
                <div class="m-sub">{{ getDeptName(m) }}</div>
              </div>
              <div class="m-actions" v-if="isOwner && m.userId !== ownerId">
                <el-dropdown trigger="click">
                  <el-button size="small" link>设置</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="changeMemberRole(m, m.role===2?1:2)">
                        {{ m.role===2 ? '取消管理员' : '设为管理员' }}
                      </el-dropdown-item>
                      <el-dropdown-item divided @click="removeMember(m)">移出群组</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>

          <!-- 会议 -->
          <div v-if="rightPanel==='meetings'" class="side-meetings">
            <div class="meeting-card" v-for="m in meetings" :key="m.id">
              <div class="mc-main" @click="viewMeeting(m.id)">
                <div class="mc-title">{{ m.title }}</div>
                <div class="mc-meta">📅 {{ formatShortTime(m.startTime) }} · 📍 {{ m.location || '待定' }}</div>
              </div>
              <div class="mc-foot">
                <el-tag size="small" :type="getMeetingStatusTag(m.status)">{{ getMeetingStatusText(m.status) }}</el-tag>
                <div class="mc-btns">
                  <el-button size="small" type="warning" link @click.stop="openEditMeeting(m)" v-if="isManager && m.status < 2">
                    编辑
                  </el-button>
                  <el-button size="small" type="primary" link @click.stop="openCheckin(m.id)" v-if="isManager && m.status>=1">
                    发布签到
                  </el-button>
                  <el-button size="small" link @click.stop="viewMeeting(m.id)">详情</el-button>
                  <el-button size="small" type="danger" link @click.stop="confirmDeleteMeeting(m)" v-if="isManager">
                    删除
                  </el-button>
                </div>
              </div>
            </div>
            <el-empty v-if="meetings.length===0" description="暂无会议，点击右上角发起会议" />
          </div>

          <!-- 签到记录 -->
          <div v-if="rightPanel==='records'" class="side-records">
            <div class="records-header">
              <el-select v-model="recordFilterMeetingId" placeholder="选择会议" size="small" clearable @change="loadCheckinRecords" style="width:100%;">
                <el-option v-for="m in meetings" :key="m.id" :label="m.title" :value="m.id" />
              </el-select>
            </div>
            <div class="records-summary" v-if="recordStats">
              <div class="rs-item"><span class="rs-num">{{ recordStats.total }}</span><span class="rs-label">应到</span></div>
              <div class="rs-item"><span class="rs-num green">{{ recordStats.signed }}</span><span class="rs-label">已签到</span></div>
              <div class="rs-item"><span class="rs-num red">{{ recordStats.unsigned }}</span><span class="rs-label">未签到</span></div>
            </div>
            <el-table :data="checkinRecords" size="small" stripe max-height="400">
              <el-table-column label="用户" min-width="80">
                <template #default="{row}">{{ row.userName || row.user?.realName || '-' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="80">
                <template #default="{row}">
                  <el-tag size="small" :type="getCheckinStatusType(row.signStatus)">
                    {{ getCheckinStatusText(row.signStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="签到时间" width="130">
                <template #default="{row}">{{ row.signTime ? formatShortTime(row.signTime) : '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="120" v-if="isManager && recordFilterMeetingId">
                <template #default="{row}">
                  <el-button size="small" type="success" link @click="handleProxySign(row)" v-if="row.signStatus === 0">
                    代签
                  </el-button>
                  <el-button size="small" type="warning" link @click="openMakeupDialog(row)" v-if="row.signStatus === 0">
                    补签
                  </el-button>
                  <span v-else style="color:#909399;font-size:12px;">已处理</span>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="checkinRecords.length===0" description="暂无签到记录" />
          </div>
        </div>
      </div>
    </div>

    <!-- 选择会议弹窗 -->
    <el-dialog v-model="pickMeetingDialogVisible" :title="pickMeetingMode==='checkin'?'发布会议签到':'发送会议卡片'" width="500px">
      <el-select v-model="pickedMeetingId" placeholder="选择要操作的会议" style="width:100%;" size="large">
        <el-option v-for="m in meetings" :key="m.id" :label="m.title" :value="m.id" />
      </el-select>
      <template #footer>
        <el-button @click="pickMeetingDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmPickMeeting" :disabled="!pickedMeetingId">确认</el-button>
      </template>
    </el-dialog>

    <!-- 创建会议弹窗 -->
    <el-dialog v-model="meetingDialogVisible" :title="editingMeetingId ? '编辑会议' : '发起会议'" width="640px" top="5vh">
      <el-form :model="meetingForm" label-width="100px">
        <el-form-item label="会议标题" required>
          <el-input v-model="meetingForm.title" placeholder="请输入会议标题" />
        </el-form-item>
        <el-form-item label="会议地点">
          <el-input v-model="meetingForm.location" placeholder="选填" />
        </el-form-item>
        <el-form-item label="会议描述">
          <el-input v-model="meetingForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" required>
              <el-date-picker v-model="meetingForm.startTime" type="datetime" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" required>
              <el-date-picker v-model="meetingForm.endTime" type="datetime" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 签到时间设置 -->
        <el-form-item label="签到时间">
          <div class="checkin-time-setup">
            <el-radio-group v-model="meetingForm.checkinMode" style="margin-bottom: 12px;">
              <el-radio-button label="auto">快捷设置</el-radio-button>
              <el-radio-button label="custom">自定义时间</el-radio-button>
            </el-radio-group>
            <!-- 快捷设置：偏移量 -->
            <div v-if="meetingForm.checkinMode === 'auto'" class="offset-row">
              <span>会议开始前</span>
              <el-input-number v-model="meetingForm.checkinStartOffset" :min="0" :max="180" :step="5" size="small" controls-position="right" />
              <span>分钟开始签到，会议开始后</span>
              <el-input-number v-model="meetingForm.checkinEndOffset" :min="0" :max="180" :step="5" size="small" controls-position="right" />
              <span>分钟结束签到</span>
            </div>
            <!-- 自定义时间 -->
            <el-row :gutter="16" v-else>
              <el-col :span="12">
                <el-date-picker v-model="meetingForm.checkinStartTime" type="datetime" placeholder="签到开始时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
              </el-col>
              <el-col :span="12">
                <el-date-picker v-model="meetingForm.checkinEndTime" type="datetime" placeholder="签到结束时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
              </el-col>
            </el-row>
          </div>
        </el-form-item>
        <el-form-item label="迟到阈值">
          <el-input-number v-model="meetingForm.lateTime" :min="0" :max="120" /> 分钟
        </el-form-item>
        <el-form-item label="签到方式" required>
          <el-checkbox-group v-model="meetingForm.signMethods">
            <el-checkbox label="qrcode">二维码</el-checkbox>
            <el-checkbox label="location">定位</el-checkbox>
            <el-checkbox label="photo">拍照</el-checkbox>
            <el-checkbox label="gesture">手势</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <!-- 手势密码设置（仅当选择手势签到时显示） -->
        <el-form-item label="手势密码" v-if="meetingForm.signMethods.includes('gesture')">
          <div class="gesture-setter">
            <div class="gesture-setter-tip">
              请绘制手势密码（至少连接4个点）：当前 {{ meetingForm.gesturePassword ? meetingForm.gesturePassword.split('-').length : 0 }} 个点
            </div>
            <div class="gesture-grid" @mouseleave="onGestureMouseLeave">
              <div
                v-for="i in 9"
                :key="i"
                class="gesture-dot"
                :class="{
                  active: gestureSelected.includes(i - 1),
                  current: gestureCurrentDot === i - 1
                }"
                @mousedown="onGestureStart(i - 1)"
                @mouseenter="onGestureEnter(i - 1)"
              ></div>
            </div>
            <div class="gesture-setter-actions">
              <span class="gesture-preview">{{ meetingForm.gesturePassword ? '已设置：' + meetingForm.gesturePassword : '未设置' }}</span>
              <el-button size="small" @click="meetingForm.gesturePassword = ''">清除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="meetingDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitMeeting">{{ editingMeetingId ? '保存修改' : '创建并发送' }}</el-button>
      </template>
    </el-dialog>

    <!-- 代签对话框 -->
    <el-dialog v-model="proxyDialogVisible" title="管理员代签" width="440px">
      <div v-if="proxyTarget" class="dialog-info">
        <p>将为 <b>{{ proxyTarget.userName }}</b> 代签会议：</p>
        <p class="di-meeting">📋 {{ proxyTarget.meetingTitle }}</p>
        <el-input v-model="proxyReason" placeholder="代签理由（可选）" type="textarea" :rows="2" style="margin-top:12px;" />
      </div>
      <template #footer>
        <el-button @click="proxyDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmProxySign">确认代签</el-button>
      </template>
    </el-dialog>

    <!-- 补签对话框（管理员为用户提交补签） -->
    <el-dialog v-model="makeupDialogVisible" title="提交补签申请" width="440px">
      <div v-if="makeupTarget" class="dialog-info">
        <p>为 <b>{{ makeupTarget.userName }}</b> 提交补签申请：</p>
        <p class="di-meeting">📋 {{ makeupTarget.meetingTitle }}</p>
        <el-input v-model="makeupReason" placeholder="补签理由（例如：迟到、网络问题等）" type="textarea" :rows="3" style="margin-top:12px;" />
      </div>
      <template #footer>
        <el-button @click="makeupDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitMakeupByAdmin">提交补签</el-button>
      </template>
    </el-dialog>

    <!-- 二维码弹窗 -->
    <el-dialog v-model="qrcodeVisible" title="群二维码 · 扫码加入" width="420px">
      <div class="qrcode-box">
        <img :src="groupQrcodeImg" alt="群二维码" v-if="groupQrcodeImg" />
        <div class="qrcode-info">
          <div><b>群名称：</b>{{ group?.groupName }}</div>
          <div><b>群编号：</b>{{ group?.groupCode }}</div>
        </div>
      </div>
    </el-dialog>

    <!-- 签到弹窗（按签到方式） -->
    <el-dialog v-model="checkinDialogVisible" title="会议签到" width="480px">
      <div v-if="checkinTargetMeeting" class="checkin-dialog-body">
        <div class="cd-meeting-info">
          <div class="cd-meeting-title">📋 {{ checkinTargetMeeting.title }}</div>
        </div>
        <!-- 签到方式选择 -->
        <div class="cd-section">
          <div class="cd-section-title">选择签到方式</div>
          <div class="cd-method-list">
            <div
              v-for="m in checkinTargetMeeting.signMethods"
              :key="m"
              :class="['cd-method-item', { active: checkinSelectedMethod === m }]"
              @click="checkinSelectedMethod = m; resetCheckinVerifyData()"
            >
              <span class="cd-method-icon">{{ METHOD_ICONS[m] }}</span>
              <span>{{ METHOD_LABELS[m] }}</span>
              <span class="cd-method-check" v-if="checkinSelectedMethod === m">✓</span>
            </div>
          </div>
        </div>
        <!-- 验证区域 -->
        <div class="cd-section">
          <div class="cd-section-title">签到验证</div>
          <!-- 二维码 -->
          <div v-if="checkinSelectedMethod === 'qrcode'" class="cd-verify-box">
            <el-icon :size="36"><Tickets /></el-icon>
            <p>请使用手机微信扫描会议二维码进行签到</p>
            <el-button type="primary" @click="confirmQrcode" :disabled="checkinVerifyData.qrcodeVerified">
              {{ checkinVerifyData.qrcodeVerified ? '✓ 已确认扫码' : '我已完成扫码' }}
            </el-button>
          </div>
          <!-- 拍照 -->
          <div v-else-if="checkinSelectedMethod === 'photo'" class="cd-verify-box">
            <el-upload
              :before-upload="handlePhotoUpload"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button type="primary" :disabled="checkinVerifyData.photoVerified">
                {{ checkinVerifyData.photoVerified ? '✓ 照片已上传' : '上传签到照片' }}
              </el-button>
            </el-upload>
          </div>
          <!-- 手势 -->
          <div v-else-if="checkinSelectedMethod === 'gesture'" class="cd-verify-box">
            <el-button type="primary" @click="startGestureVerify" :disabled="checkinVerifyData.gestureVerified">
              {{ checkinVerifyData.gestureVerified ? '✓ 手势已验证' : '进行手势签到' }}
            </el-button>
          </div>
          <!-- 定位 -->
          <div v-else-if="checkinSelectedMethod === 'location'" class="cd-verify-box">
            <el-button type="primary" @click="startLocationVerify" :disabled="checkinVerifyData.locationVerified">
              {{ checkinVerifyData.locationVerified ? '✓ 已定位' : '获取当前位置' }}
            </el-button>
            <p v-if="checkinVerifyData.locationVerified" class="cd-loc-text">
              📍 {{ checkinVerifyData.location || `${checkinVerifyData.longitude?.toFixed(6)}, ${checkinVerifyData.latitude?.toFixed(6)}` }}
            </p>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="checkinDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCheckinFromDialog" :loading="checkinSubmitting" :disabled="!isMethodVerified">
          确认签到
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as Icons from '@element-plus/icons-vue'
import {
  Calendar, Picture, Plus, MoreFilled, Tickets, ArrowRight, UserFilled,
  Close, DocumentChecked
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'

import {
  getGroup, getGroupMembers, updateGroup, removeMember as removeMemberApi,
  updateMemberRole, leaveGroup as leaveGroupApi,
  getMessages, getLatestMessages, sendTextMessage, sendMeetingCard, sendCheckinCard
} from '@/api/group'
import { getMeetingList, createMeeting, updateMeeting, generateGroupQrcode, getMeetingAttendees, deleteMeeting, clearGroupMeetings, clearGroupMessages } from '@/api/meeting'
import { getAllMakeUpList, getAllProxySignList, approveMakeUp, quickCheckIn, checkIn, getCheckInRecords, proxySign, applyMakeUp } from '@/api/checkin'
import { getAllUsers } from '@/api/user'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const userId = computed(() => Number(userStore.userId))
const userName = computed(() => userStore.userName)

const groupId = computed(() => Number(route.params.id))
const loading = ref(false)
const group = ref(null)
const members = ref([])
const meetings = ref([])
const messages = ref([])
const checkinRecords = ref([])
const recordFilterMeetingId = ref(null)
const recordStats = ref(null)
const makeupList = ref([])
const proxyList = ref([])
const userList = ref([])

const msgListRef = ref(null)
const polling = ref(false)
let pollTimer = null
const sending = ref(false)
const inputText = ref('')

const rightPanel = ref('meetings') // none | members | meetings | records
const groupQrcodeImg = ref('')
const qrcodeVisible = ref(false)

// 会议弹窗
const meetingDialogVisible = ref(false)
const editingMeetingId = ref(null)
const meetingForm = ref({
  title: '', location: '', description: '',
  startTime: null, endTime: null,
  checkinMode: 'auto',
  checkinStartOffset: 30,
  checkinEndOffset: 15,
  checkinStartTime: null, checkinEndTime: null,
  lateTime: 15,
  signMethods: ['qrcode'],
  gesturePassword: ''
})

// 手势密码绘制状态（PC端设置器）
const gestureSelected = ref([])
const gestureCurrentDot = ref(-1)
let gestureDrawing = false

const onGestureStart = (idx) => {
  gestureDrawing = true
  gestureSelected.value = [idx]
  gestureCurrentDot.value = idx
}
const onGestureEnter = (idx) => {
  if (!gestureDrawing) return
  if (!gestureSelected.value.includes(idx)) {
    gestureSelected.value.push(idx)
  }
  gestureCurrentDot.value = idx
}
const onGestureMouseLeave = () => {
  if (!gestureDrawing) return
  gestureDrawing = false
  // 至少4个点才算有效
  if (gestureSelected.value.length >= 4) {
    meetingForm.value.gesturePassword = gestureSelected.value.join('-')
  } else if (gestureSelected.value.length > 0) {
    ElMessage.warning('手势密码至少需要连接4个点')
  }
  gestureSelected.value = []
  gestureCurrentDot.value = -1
}

// 选择会议弹窗
const pickMeetingDialogVisible = ref(false)
const pickMeetingMode = ref('card')
const pickedMeetingId = ref(null)

// 签到弹窗（按签到方式）
const checkinDialogVisible = ref(false)
const checkinTargetMeeting = ref(null)
const checkinSelectedMethod = ref('qrcode')
const checkinSubmitting = ref(false)
const checkinVerifyData = ref({
  qrcodeVerified: false,
  photoVerified: false,
  photoFile: null,
  gestureVerified: false,
  locationVerified: false,
  latitude: null,
  longitude: null,
  location: null
})
const METHOD_LABELS = {
  qrcode: '二维码签到',
  photo: '拍照签到',
  gesture: '手势签到',
  location: '定位签到'
}
const METHOD_ICONS = {
  qrcode: '📱', photo: '📷', gesture: '✋', location: '📍'
}

const ownerId = computed(() => group.value?.ownerId)
const myRole = computed(() => {
  const me = members.value.find(m => Number(m.userId) === userId.value)
  return me?.role || 0
})
const isOwner = computed(() => Number(ownerId.value) === userId.value)
const isManager = computed(() => isOwner.value || myRole.value >= 2 || userStore.isAdmin || userStore.isLeader)

const avatarGradient = computed(() => {
  const palettes = [
    '#2563EB',
    '#0F766E',
    '#7C3AED',
    '#475569',
    '#D97706'
  ]
  const idx = (groupId.value || 0) % palettes.length
  return palettes[idx]
})

const loadGroup = async () => {
  try {
    const res = await getGroup(groupId.value)
    group.value = res.data
  } catch (e) {
    ElMessage.error('群组不存在或您无权限访问')
    router.push('/groups')
    throw e
  }
}

const loadMembers = async () => {
  const res = await getGroupMembers(groupId.value)
  members.value = res.data || []
}

const loadMeetings = async () => {
  const res = await getMeetingList({ groupId: groupId.value, current: 1, size: 100 })
  meetings.value = res.data?.records || []
}

const loadCheckinRecords = async () => {
  const targetMeetings = recordFilterMeetingId.value
    ? meetings.value.filter(m => m.id === recordFilterMeetingId.value)
    : meetings.value.slice(0, 20)
  
  const list = []
  let totalSigned = 0, totalUnsigned = 0
  
  for (const m of targetMeetings) {
    try {
      const [recordsRes, attendeesRes] = await Promise.all([
        getCheckInRecords(m.id).catch(() => ({ data: [] })),
        getMeetingAttendees(m.id).catch(() => ({ data: [] }))
      ])
      const records = recordsRes.data || []
      const attendees = attendeesRes.data || []
      const signedMap = {}
      records.forEach(r => { signedMap[r.userId] = r })
      
      // 已签到人员
      records.forEach(r => {
        list.push({
          ...r,
          meetingTitle: m.title,
          signStatus: r.signStatus ?? (r.status === 1 ? 1 : 2)
        })
      })
      // 未签到人员
      attendees.forEach(a => {
        if (!signedMap[a.userId]) {
          list.push({
            userId: a.userId,
            userName: a.user?.realName || '未知用户',
            meetingTitle: m.title,
            signStatus: 0,
            signTime: null,
            signMethod: null
          })
        }
      })
      totalSigned += records.length
      totalUnsigned += attendees.filter(a => !signedMap[a.userId]).length
    } catch {}
  }
  
  checkinRecords.value = list
  recordStats.value = {
    total: list.length,
    signed: totalSigned,
    unsigned: totalUnsigned
  }
}

// ============ 代签 & 补签 ============
const proxyDialogVisible = ref(false)
const proxyTarget = ref(null)
const proxyReason = ref('')

const makeupDialogVisible = ref(false)
const makeupTarget = ref(null)
const makeupReason = ref('')

const handleProxySign = (row) => {
  proxyTarget.value = row
  proxyReason.value = ''
  proxyDialogVisible.value = true
}

const confirmProxySign = async () => {
  if (!proxyTarget.value || !recordFilterMeetingId.value) return
  try {
    await ElMessageBox.confirm(
      `确定为「${proxyTarget.value.userName}」代签「${proxyTarget.value.meetingTitle}」吗？`,
      '代签确认',
      { type: 'warning' }
    )
    await proxySign(recordFilterMeetingId.value, {
      targetUserIds: [proxyTarget.value.userId],
      reason: proxyReason.value || '管理员代签'
    })
    ElMessage.success('代签成功')
    proxyDialogVisible.value = false
    proxyTarget.value = null
    await loadCheckinRecords()
  } catch (e) {
    if (e !== 'cancel') console.error('代签失败:', e)
  }
}

const openMakeupDialog = (row) => {
  makeupTarget.value = row
  makeupReason.value = ''
  makeupDialogVisible.value = true
}

const submitMakeupByAdmin = async () => {
  if (!makeupTarget.value || !recordFilterMeetingId.value) return
  if (!makeupReason.value.trim()) {
    ElMessage.warning('请填写补签理由')
    return
  }
  try {
    await applyMakeUp(recordFilterMeetingId.value, {
      reason: makeupReason.value,
      proofUrl: ''
    })
    // 同时设置申请人ID（作为管理员代申请场景，使用该用户的ID）
    // 注意：补签申请是当前登录用户申请，管理员应直接通过后端代签
    // 这里改为直接使用代签接口完成
    await proxySign(recordFilterMeetingId.value, {
      targetUserIds: [makeupTarget.value.userId],
      reason: makeupReason.value
    })
    ElMessage.success(`已为「${makeupTarget.value.userName}」提交补签`)
    makeupDialogVisible.value = false
    makeupTarget.value = null
    await loadCheckinRecords()
  } catch (e) {
    if (e !== 'cancel') console.error('补签失败:', e)
  }
}

const loadAll = async () => {
  loading.value = true
  try {
    await Promise.all([loadGroup(), loadMembers(), loadMeetings()])
    await loadMessages()
    loadCheckinRecords()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// ============ 消息 ============
const loadMessages = async () => {
  try {
    const res = await getMessages(groupId.value, 150)
    messages.value = res.data || []
    nextTick(scrollToBottom)
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

const startPolling = () => {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!messages.value.length) return
    const lastId = Math.max(...messages.value.map(m => m.id))
    try {
      polling.value = true
      const res = await getLatestMessages(groupId.value, lastId)
      if (res.data && res.data.length) {
        messages.value = [...messages.value, ...res.data]
        nextTick(scrollToBottom)
      }
    } finally {
      polling.value = false
    }
  }, 3000)
}
const stopPolling = () => pollTimer && (clearInterval(pollTimer), pollTimer = null)

const scrollToBottom = () => {
  if (msgListRef.value) {
    msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  }
}

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendText()
  }
}

const sendText = async () => {
  const text = inputText.value.trim()
  if (!text || sending.value) return
  sending.value = true
  try {
    // 立即在本地显示
    const tempId = -Date.now()
    messages.value.push({
      id: tempId, userId: userId.value, type: 'text',
      content: text, userName: userName.value,
      createTime: new Date().toISOString()
    })
    nextTick(scrollToBottom)
    await sendTextMessage(groupId.value, text)
    inputText.value = ''
    // 重新拉取最新（替换本地临时）
    const lastId = Math.max(0, ...messages.value.filter(m => m.id > 0).map(m => m.id))
    const res = await getLatestMessages(groupId.value, lastId)
    messages.value = messages.value.filter(m => m.id > 0).concat(res.data || [])
  } catch (e) {
    ElMessage.error('发送失败')
    // 回滚：删除最后一条临时消息
    messages.value = messages.value.filter(m => m.id > 0)
  } finally {
    sending.value = false
    nextTick(scrollToBottom)
  }
}

// ============ 工具栏动作 ============
const openMeetingDialog = () => {
  editingMeetingId.value = null
  meetingForm.value = {
    title: '', location: '', description: '',
    startTime: dayjs().add(1, 'hour').format('YYYY-MM-DD HH:mm:ss'),
    endTime: dayjs().add(2, 'hour').format('YYYY-MM-DD HH:mm:ss'),
    checkinMode: 'auto',
    checkinStartOffset: 30,
    checkinEndOffset: 15,
    checkinStartTime: null,
    checkinEndTime: null,
    lateTime: 15, signMethods: ['qrcode'],
    gesturePassword: ''
  }
  gestureSelected.value = []
  gestureCurrentDot.value = -1
  gestureDrawing = false
  meetingDialogVisible.value = true
}

const openEditMeeting = (m) => {
  editingMeetingId.value = m.id
  // 判断编辑模式：如果有签到时间，尝试反推偏移量
  let checkinMode = 'auto'
  let startOffset = 30
  let endOffset = 15
  if (m.checkinStartTime && m.startTime) {
    const cs = dayjs(m.checkinStartTime)
    const ms = dayjs(m.startTime)
    const ce = dayjs(m.checkinEndTime)
    const diffStart = ms.diff(cs, 'minute')
    const diffEnd = ce.diff(ms, 'minute')
    // 如果偏移量在合理范围内，使用快捷模式
    if (diffStart >= 0 && diffStart <= 180 && diffEnd >= 0 && diffEnd <= 180) {
      startOffset = diffStart
      endOffset = diffEnd
    } else {
      checkinMode = 'custom'
    }
  }
  meetingForm.value = {
    title: m.title || '',
    location: m.location || '',
    description: m.description || '',
    startTime: m.startTime ? dayjs(m.startTime).format('YYYY-MM-DD HH:mm:ss') : null,
    endTime: m.endTime ? dayjs(m.endTime).format('YYYY-MM-DD HH:mm:ss') : null,
    checkinMode,
    checkinStartOffset: startOffset,
    checkinEndOffset: endOffset,
    checkinStartTime: m.checkinStartTime ? dayjs(m.checkinStartTime).format('YYYY-MM-DD HH:mm:ss') : null,
    checkinEndTime: m.checkinEndTime ? dayjs(m.checkinEndTime).format('YYYY-MM-DD HH:mm:ss') : null,
    lateTime: m.lateTime || 15,
    signMethods: m.signMethods || ['qrcode'],
    gesturePassword: m.gesturePassword || ''
  }
  gestureSelected.value = []
  gestureCurrentDot.value = -1
  gestureDrawing = false
  meetingDialogVisible.value = true
}

const submitMeeting = async () => {
  const f = meetingForm.value
  if (!f.title) return ElMessage.warning('请输入标题')
  if (!f.signMethods?.length) return ElMessage.warning('请选择签到方式')
  // 手势签到必须设置手势密码
  if (f.signMethods.includes('gesture')) {
    if (!f.gesturePassword || f.gesturePassword.split('-').length < 4) {
      return ElMessage.warning('请绘制手势密码（至少连接4个点）')
    }
  }
  try {
    // 根据签到模式计算实际签到时间
    let checkinStartTime = null
    let checkinEndTime = null
    if (f.checkinMode === 'auto' && f.startTime) {
      const meetingStart = dayjs(f.startTime)
      checkinStartTime = meetingStart.subtract(f.checkinStartOffset, 'minute').format('YYYY-MM-DD HH:mm:ss')
      checkinEndTime = meetingStart.add(f.checkinEndOffset, 'minute').format('YYYY-MM-DD HH:mm:ss')
    } else {
      checkinStartTime = f.checkinStartTime ? dayjs(f.checkinStartTime).format('YYYY-MM-DD HH:mm:ss') : null
      checkinEndTime = f.checkinEndTime ? dayjs(f.checkinEndTime).format('YYYY-MM-DD HH:mm:ss') : null
    }

    const payload = {
      title: f.title,
      location: f.location,
      description: f.description,
      startTime: f.startTime ? dayjs(f.startTime).format('YYYY-MM-DD HH:mm:ss') : null,
      endTime: f.endTime ? dayjs(f.endTime).format('YYYY-MM-DD HH:mm:ss') : null,
      checkinStartTime,
      checkinEndTime,
      lateTime: f.lateTime,
      signMethods: f.signMethods,
      gesturePassword: f.signMethods.includes('gesture') ? f.gesturePassword : null,
      groupId: groupId.value
    }
    if (editingMeetingId.value) {
      // 编辑模式
      await updateMeeting(editingMeetingId.value, payload)
      ElMessage.success('会议修改成功')
      meetingDialogVisible.value = false
      loadMeetings()
    } else {
      // 创建模式
      const res = await createMeeting(payload)
      const meetingId = res.data?.id || res.data?.meetingId || res.data
      // 发送会议卡片
      await sendMeetingCard(groupId.value, Number(meetingId))
      ElMessage.success('会议创建成功，已发送至群聊')
      meetingDialogVisible.value = false
      loadMeetings()
      loadMessages()
    }
  } catch (e) {
    console.error(e)
  }
}

const openPickMeeting = (mode) => {
  if (meetings.value.length === 0) return ElMessage.warning('请先创建会议')
  pickMeetingMode.value = mode
  pickedMeetingId.value = meetings.value[0].id
  pickMeetingDialogVisible.value = true
}

const confirmPickMeeting = async () => {
  if (!pickedMeetingId.value) return
  try {
    if (pickMeetingMode.value === 'checkin') {
      await sendCheckinCard(groupId.value, pickedMeetingId.value)
      ElMessage.success('签到卡片已发送')
    } else {
      await sendMeetingCard(groupId.value, pickedMeetingId.value)
      ElMessage.success('会议卡片已发送')
    }
    pickMeetingDialogVisible.value = false
    loadMessages()
  } catch (e) {
    console.error(e)
  }
}

const openMeetingFromCard = (msg) => {
  const extra = parseExtra(msg.extra)
  const mid = extra.meetingId
  if (mid) router.push(`/groups/${groupId.value}/meetings/${mid}`)
}

const openCheckinFromCard = (msg) => {
  const extra = parseExtra(msg.extra)
  const mid = extra.meetingId
  if (!mid) {
    ElMessage.warning('无法获取会议信息')
    return
  }
  // 获取会议配置的签到方式
  const methods = Array.isArray(extra.signMethods) && extra.signMethods.length
    ? extra.signMethods
    : ['qrcode']
  // 打开签到弹窗，由用户选择/确认签到方式
  checkinTargetMeeting.value = {
    id: mid,
    title: extra.title || msg.content,
    qrcodeToken: extra.qrcodeToken || '',
    signMethods: methods
  }
  checkinSelectedMethod.value = methods[0]
  resetCheckinVerifyData()
  checkinDialogVisible.value = true
}

const resetCheckinVerifyData = () => {
  checkinVerifyData.value = {
    qrcodeVerified: false,
    photoVerified: false,
    photoFile: null,
    gestureVerified: false,
    locationVerified: false,
    latitude: null,
    longitude: null,
    location: null
  }
}

// 拍照签到：上传照片
const handlePhotoUpload = (file) => {
  checkinVerifyData.value.photoFile = file
  checkinVerifyData.value.photoVerified = true
  ElMessage.success('照片已上传')
  return false // 阻止默认上传行为
}

// 手势签到
const startGestureVerify = () => {
  ElMessageBox.confirm('请在屏幕上绘制签到手势（向上滑动），完成后点击确定', '手势签到', {
    confirmButtonText: '我已完成',
    type: 'info'
  }).then(() => {
    checkinVerifyData.value.gestureVerified = true
    ElMessage.success('手势验证通过')
  }).catch(() => {})
}

// 定位签到
const startLocationVerify = () => {
  if (!navigator.geolocation) {
    ElMessage.error('您的浏览器不支持定位功能')
    return
  }
  ElMessage.info('正在获取定位…')
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const lat = pos.coords.latitude
      const lng = pos.coords.longitude
      checkinVerifyData.value.latitude = lat
      checkinVerifyData.value.longitude = lng
      // 使用高德地图逆地理编码获取地址
      if (typeof AMap !== 'undefined' && AMap.Geocoder) {
        const geocoder = new AMap.Geocoder({ key: '1618efc3f5ca2a1f1717f5d33512bded' })
        geocoder.getAddress([lng, lat], (status, result) => {
          if (status === 'complete' && result.info === 'OK') {
            checkinVerifyData.value.location = result.regeocode.formattedAddress
          } else {
            checkinVerifyData.value.location = `${lng},${lat}`
          }
          checkinVerifyData.value.locationVerified = true
          ElMessage.success('定位成功')
        })
      } else {
        // 高德SDK未加载，使用坐标作为位置
        checkinVerifyData.value.location = `${lng.toFixed(6)},${lat.toFixed(6)}`
        checkinVerifyData.value.locationVerified = true
        ElMessage.success('定位成功')
      }
    },
    () => {
      ElMessage.error('定位失败，请检查浏览器定位权限')
    }
  )
}

// 二维码签到确认（用户确认已看到二维码）
const confirmQrcode = () => {
  checkinVerifyData.value.qrcodeVerified = true
  ElMessage.success('二维码已确认')
}

// 签到方式是否已完成验证
const isMethodVerified = computed(() => {
  const m = checkinSelectedMethod.value
  const v = checkinVerifyData.value
  switch (m) {
    case 'qrcode': return v.qrcodeVerified
    case 'photo': return v.photoVerified
    case 'gesture': return v.gestureVerified
    case 'location': return v.locationVerified
    default: return false
  }
})

// 提交签到
const submitCheckinFromDialog = async () => {
  const meeting = checkinTargetMeeting.value
  if (!meeting) return
  if (!isMethodVerified.value) {
    ElMessage.warning('请先完成签到验证')
    return
  }
  checkinSubmitting.value = true
  try {
    const method = checkinSelectedMethod.value
    const v = checkinVerifyData.value
    const payload = { signMethod: method, qrcodeToken: meeting.qrcodeToken }
    if (method === 'location') {
      payload.latitude = v.latitude
      payload.longitude = v.longitude
      payload.location = v.location
    } else if (method === 'photo' || method === 'gesture') {
      payload.verifyData = { verified: true, timestamp: Date.now() }
    }
    await checkIn(meeting.id, payload)
    ElMessage.success('签到成功！')
    checkinDialogVisible.value = false
    loadCheckinRecords()
    loadMessages()
  } catch (e) {
    const errMsg = e?.response?.data?.message || e?.message || '签到失败'
    ElMessage.error(errMsg)
  } finally {
    checkinSubmitting.value = false
  }
}

const openCheckin = (meetingId) => {
  sendCheckinCard(groupId.value, meetingId)
    .then(() => { ElMessage.success('签到已发布'); loadMessages() })
    .catch(e => console.error(e))
}

const viewMeeting = (mid) => router.push(`/groups/${groupId.value}/meetings/${mid}`)

const openApprovalInGroup = () => {
  router.push('/approvals')
}

// ============ 群设置 ============
const showQrcode = async () => {
  try {
    const res = await generateGroupQrcode(groupId.value)
    const data = res.data || res
    groupQrcodeImg.value = typeof data === 'string' ? data : (data?.image || data?.url)
    qrcodeVisible.value = true
  } catch (e) {
    ElMessage.error('生成二维码失败')
  }
}

const showSettings = () => router.push(`/groups/${groupId.value}/settings`)
const showInvite = () => showQrcode()

const confirmLeave = async () => {
  try {
    await ElMessageBox.confirm('确定要退出该群聊吗？', '确认', { type: 'warning' })
    await leaveGroupApi(groupId.value)
    ElMessage.success('已退出')
    router.push('/groups')
  } catch {}
}

// ============ 成员 ============
const changeMemberRole = async (m, role) => {
  try {
    await updateMemberRole(groupId.value, m.userId, role)
    ElMessage.success('已更新')
    loadMembers()
  } catch (e) {
    console.error(e)
  }
}

const removeMember = async (m) => {
  try {
    await ElMessageBox.confirm(`确定要移除成员 ${m.userName}？`, '确认', { type: 'warning' })
    await removeMemberApi(groupId.value, m.userId)
    ElMessage.success('已移除')
    loadMembers()
  } catch {}
}

const toggleRightPanel = (panel) => {
  rightPanel.value = rightPanel.value === panel ? 'none' : panel
}

// ============ 工具函数 ============
const parseExtra = (extra) => {
  if (!extra) return {}
  try { return JSON.parse(extra) } catch { return {} }
}

const formatSignMethods = (methods) => {
  if (!Array.isArray(methods) || !methods.length) return '二维码'
  const labels = { qrcode: '二维码', photo: '拍照', gesture: '手势', location: '定位' }
  return methods.map(m => labels[m] || m).join('、')
}

const formatShortTime = (t) => {
  if (!t) return '-'
  return dayjs(t).format('MM-DD HH:mm')
}

const formatMsgTime = (t) => {
  if (!t) return ''
  const d = dayjs(t)
  const now = dayjs()
  if (d.isSame(now, 'day')) {
    return d.format('HH:mm')
  }
  return d.format('MM-DD HH:mm')
}

const getMeetingStatusTag = (s) => {
  const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'info' }
  return map[s] || 'info'
}
const getMeetingStatusText = (s) => {
  const map = { 0: '草稿', 1: '已发布', 2: '进行中', 3: '已结束' }
  return map[s] || '未知'
}

const getMethodText = (m) => {
  const map = { qrcode: '二维码', photo: '拍照', gesture: '手势', location: '定位', makeup: '补签', proxy: '代签' }
  return map[m] || m || '-'
}

const getCheckinStatusTag = (s) => {
  const map = { 0: 'danger', 1: 'success', 2: 'warning' }
  return map[s] || 'info'
}

const getCheckinStatusText = (s) => {
  const map = { 0: '未签到', 1: '已签到', 2: '迟到' }
  return map[s] || '未知'
}

const getCheckinStatusType = (s) => {
  const map = { 0: 'danger', 1: 'success', 2: 'warning' }
  return map[s] || 'info'
}

const confirmDeleteMeeting = async (m) => {
  try {
    await ElMessageBox.confirm(
      `确定删除会议「${m.title}」吗？相关的签到记录也将被删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteMeeting(m.id)
    ElMessage.success('会议已删除')
    await loadMeetings()
    loadCheckinRecords()
    loadMessages()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '删除失败')
    }
  }
}

const confirmClearMessages = async () => {
  try {
    await ElMessageBox.confirm(
      '确定清空本群所有聊天记录吗？此操作不可恢复！',
      '清空聊天记录',
      { type: 'warning', confirmButtonText: '确定清空', cancelButtonText: '取消' }
    )
    await clearGroupMessages(groupId.value)
    ElMessage.success('聊天记录已清空')
    await loadMessages()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '清空失败')
    }
  }
}

const confirmClearMeetings = async () => {
  try {
    await ElMessageBox.confirm(
      '确定清空本群所有会议记录吗？相关的签到记录也将一并删除，此操作不可恢复！',
      '清空会议记录',
      { type: 'warning', confirmButtonText: '确定清空', cancelButtonText: '取消' }
    )
    await clearGroupMeetings(groupId.value)
    ElMessage.success('会议记录已清空')
    await loadMeetings()
    loadCheckinRecords()
    loadMessages()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '清空失败')
    }
  }
}

const getDeptName = (m) => m.position || m.deptId || '普通成员'

const getAvatarChar = (obj) => {
  const name = obj.userName || obj.realName || '?'
  return name.charAt(0)
}

const getAvatarColor = (uid) => {
  const colors = ['#2563EB', '#0F766E', '#7C3AED', '#475569', '#DC2626', '#D97706', '#059669', '#9333EA', '#0891B2', '#64748B']
  return colors[Number(uid || 0) % colors.length]
}

watch(groupId, () => {
  if (groupId.value) {
    messages.value = []
    loadAll()
  }
})

onMounted(() => {
  loadAll()
  nextTick(startPolling)
})

onUnmounted(stopPolling)
</script>

<style lang="scss" scoped>
.chat-app {
  height: calc(100vh - 120px);
  min-height: 600px;
  background: #fff;
  border: 1px solid $border-light;
  border-radius: $radius-base;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: $shadow-base;
}

.chat-header {
  flex: none;
  height: 72px;
  padding: 0 20px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $bg-white;

  .header-left {
    display: flex;
    align-items: center;
    gap: 14px;
  }
  .group-avatar {
    width: 48px; height: 48px;
    border-radius: 12px;
    display: flex; align-items: center; justify-content: center;
    color: $color-primary; font-size: 22px; font-weight: 700;
    background: $color-primary-bg !important;
  }
  .group-name {
    font-size: 17px; font-weight: 600; color: $text-primary;
    display: flex; align-items: center; gap: 8px;
  }
  .code-tag { background: $color-primary-bg; color: $color-primary; border: none; }
  .group-sub {
    margin-top: 4px;
    font-size: 12px; color: $text-secondary;
    .dot { margin: 0 6px; }
  }
  .header-right { display: flex; gap: 10px; }
}

.chat-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-right: 1px solid #ebeef5;
}

.message-list {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  background: $bg-base;

  .msg-row {
    display: flex;
    margin-bottom: 20px;
    align-items: flex-start;
    gap: 12px;

    &.self {
      flex-direction: row-reverse;
      .msg-avatar { order: 0; }
      .msg-name { text-align: right; }
    }
    &.system {
      justify-content: center;
    }
    .system-msg {
      background: rgba(0,0,0,0.05);
      border-radius: 14px;
      padding: 4px 14px;
      font-size: 12px;
      color: $text-secondary;
    }
  }
  .msg-avatar {
    width: 40px; height: 40px;
    border-radius: 10px;
    color: $color-primary;
    background: $color-primary-bg !important;
    display: flex; align-items: center; justify-content: center;
    font-weight: 700; flex-shrink: 0;
  }
  .msg-body {
    max-width: 60%;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .msg-meta-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .msg-name {
    font-size: 12px; color: $text-secondary;
  }
  .msg-time {
    font-size: 11px;
    color: $text-placeholder;
  }
  .self .msg-meta-row {
    flex-direction: row-reverse;
  }
  .self .msg-time {
    order: 0;
  }
  .msg-bubble {
    background: #fff;
    padding: 10px 14px;
    border-radius: 14px;
    border-top-left-radius: 4px;
    font-size: 14px;
    color: $text-primary;
    line-height: 1.55;
    word-break: break-all;
    box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  }
  .self .msg-bubble {
    background: $color-primary;
    color: #fff;
    border-top-left-radius: 14px;
    border-top-right-radius: 4px;
  }

  /* 卡片 */
  .card-meeting, .card-checkin {
    background: #fff;
    border-radius: 14px;
    padding: 12px 14px;
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    min-width: 280px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.06);
    transition: all 0.2s;
    &:hover { transform: translateY(-1px); box-shadow: $shadow-hover; }
  }
  .card-icon {
    width: 48px; height: 48px;
    border-radius: 12px;
    background: $color-primary-bg;
    color: $color-primary;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0;
  }
  .card-icon.checkin-color { background: $color-danger-bg; color: $color-danger; }
  .card-body { flex: 1; min-width: 0; }
  .card-title {
    font-size: 14px; font-weight: 600; color: $text-primary;
    margin-bottom: 6px;
    white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  }
  .card-meta {
    font-size: 12px; color: $text-secondary; margin-bottom: 8px;
    display: flex; flex-wrap: wrap; gap: 10px;
  }
  .card-arrow { color: #c0c4cc; }
  .self .card-meeting, .self .card-checkin {
    border-color: $color-primary;
  }
  .polling-tip { text-align: center; font-size: 12px; color: #c0c4cc; }
}

.chat-input-area {
  flex: none;
  border-top: 1px solid #ebeef5;
  background: #fff;
  padding: 10px 16px 14px;

  .toolbar {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-bottom: 8px;
    padding: 2px 4px;
  }
  .tool-btn {
    background: transparent;
    border: none;
    color: $text-regular;
    padding: 6px 10px;
    border-radius: 6px;
    &:hover { background: $color-primary-bg; color: $color-primary; }
  }
  .divider {
    width: 1px; height: 18px; background: #ebeef5;
    margin: 0 8px;
  }
  .input-row {
    display: flex;
    gap: 10px;
    align-items: flex-end;
    :deep(.el-textarea__inner) {
      font-size: 14px;
    }
  }
  .input-row :deep(.el-textarea) { flex: 1; }
}

/* 右侧 */
.chat-side {
  width: 340px;
  flex: none;
  display: flex;
  flex-direction: column;
  background: #fafbff;
}
.side-tabs {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  .tab {
    flex: 1;
    text-align: center;
    padding: 12px 0;
    cursor: pointer;
    color: $text-regular;
    font-size: 14px;
    border-bottom: 2px solid transparent;
    &.active {
      color: $color-primary;
      border-color: $color-primary;
      font-weight: 600;
    }
  }
  .close {
    width: 40px;
    color: #909399;
    cursor: pointer;
    display: flex; align-items: center; justify-content: center;
    &:hover { color: $color-primary; }
  }
}
.side-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
}

/* 成员列表 */
.side-members .member-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px; border-radius: 8px;
  margin-bottom: 6px;
  &:hover { background: #f0f3ff; }
}
.side-members .m-avatar {
  width: 36px; height: 36px; border-radius: 8px;
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-weight: 600; flex-shrink: 0;
}
.side-members .m-info { flex: 1; min-width: 0; }
.side-members .m-name { font-size: 13px; color: #303133; }
.side-members .m-sub { font-size: 11px; color: #909399; margin-top: 2px; }

/* 会议列表 */
.side-meetings .meeting-card {
  background: #fff;
  padding: 12px 14px;
  border-radius: 10px;
  margin-bottom: 10px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: all 0.2s;
  border: 1px solid transparent;
  &:hover {
    border-color: $color-primary;
    box-shadow: 0 4px 14px rgba(37, 99, 235, 0.10);
  }
  .mc-main { cursor: pointer; }
  .mc-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 6px; }
  .mc-meta { font-size: 12px; color: #909399; margin-bottom: 8px; }
  .mc-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
  .mc-btns { display: flex; gap: 8px; }
}

/* 签到时间设置 */
.checkin-time-setup {
  width: 100%;
}
.offset-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 14px;
  color: #606266;
  padding: 8px 0;
}

/* 签到记录 */
.side-records {
  .records-header { margin-bottom: 12px; }
  .records-summary {
    display: flex;
    justify-content: space-around;
    background: #f5f7fa;
    border-radius: 8px;
    padding: 12px 8px;
    margin-bottom: 12px;
    .rs-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      .rs-num { font-size: 20px; font-weight: 700; color: #303133; }
      .rs-num.green { color: #67C23A; }
      .rs-num.red { color: #F56C6C; }
      .rs-label { font-size: 11px; color: #909399; margin-top: 2px; }
    }
  }
}

/* 签到卡片行内按钮 */
.card-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

/* 二维码 */
.dialog-info {
  font-size: 14px;
  line-height: 1.8;
}
.dialog-info .di-meeting {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 6px;
  color: #303133;
  margin: 4px 0;
}

.qrcode-box {
  text-align: center;
  img {
    width: 260px; height: 260px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 8px;
    background: #fff;
  }
  .qrcode-info {
    margin-top: 14px;
    font-size: 13px;
    color: #606266;
    line-height: 1.8;
  }
}

/* 签到弹窗 */
.checkin-dialog-body {
  .cd-meeting-info {
    margin-bottom: 16px;
    .cd-meeting-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }
  .cd-section {
    margin-bottom: 18px;
    .cd-section-title {
      font-size: 13px;
      font-weight: 600;
      color: #909399;
      margin-bottom: 10px;
    }
  }
  .cd-method-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
  .cd-method-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    cursor: pointer;
    font-size: 13px;
    color: #606266;
    transition: all .2s;
    &:hover {
      border-color: #409eff;
      color: #409eff;
    }
    &.active {
      border-color: #409eff;
      background: #ecf5ff;
      color: #409eff;
    }
    .cd-method-icon {
      font-size: 16px;
    }
    .cd-method-check {
      margin-left: 4px;
      color: #67c23a;
      font-weight: bold;
    }
  }
  .cd-verify-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 8px;
    p {
      margin: 0;
      font-size: 13px;
      color: #909399;
      text-align: center;
    }
    .cd-loc-text {
      color: #67c23a;
    }
  }
}

/* 手势密码设置器 */
.gesture-setter {
  width: 100%;
}

.gesture-setter-tip {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.gesture-grid {
  width: 240px;
  height: 240px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
  gap: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  user-select: none;
  margin-bottom: 12px;
}

.gesture-dot {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #fff;
  border: 2px solid #c0c4cc;
  cursor: pointer;
  transition: all 0.15s;

  &.active {
    background: $color-primary;
    border-color: $color-primary;
    transform: scale(1.15);
    box-shadow: 0 0 8px rgba(37, 99, 235, 0.25);
  }

  &.current {
    border-color: #67C23A;
  }
}

.gesture-setter-actions {
  display: flex;
  align-items: center;
  gap: 12px;

  .gesture-preview {
    font-size: 13px;
    color: #606266;
    flex: 1;
  }
}
</style>

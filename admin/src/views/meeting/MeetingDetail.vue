<template>
  <div class="meeting-detail">
    <!-- 页面头部 -->
    <div class="page-header detail-hero">
      <div class="hero-left">
        <el-button class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <h2>{{ meeting?.title || '加载中...' }}</h2>
        <p>{{ meeting?.description || '会议详情与签到管理' }}</p>
      </div>
      <div class="hero-status" v-if="meeting">
        <el-tag :type="getStatusType(meeting.status)" effect="dark" size="large">
          {{ getStatusText(meeting.status) }}
        </el-tag>
      </div>
      <el-icon class="deco-icon dec-1"><Calendar /></el-icon>
      <el-icon class="deco-icon dec-2"><Tickets /></el-icon>
    </div>
    
    <el-row :gutter="20">
      <!-- 会议基本信息 -->
      <el-col :xs="24" :lg="16">
        <div class="card-container">
          <div class="card-header">
            <div class="ch-left">
              <el-icon class="ch-icon"><Calendar /></el-icon>
              <h3>会议信息</h3>
            </div>
          </div>
          <el-descriptions v-if="meeting" :column="2" border>
            <el-descriptions-item label="会议主题">{{ meeting.title }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(meeting.status)">{{ getStatusText(meeting.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="会议地点">{{ meeting.location }}</el-descriptions-item>
            <el-descriptions-item label="会议时间">
              {{ formatTime(meeting.startTime) }} ~ {{ formatTime(meeting.endTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="签到时间">
              {{ formatTime(meeting.checkinStartTime) }} ~ {{ formatTime(meeting.checkinEndTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="迟到阈值">{{ meeting.lateTime }} 分钟</el-descriptions-item>
            <el-descriptions-item label="签到方式" :span="2">
              <el-tag v-for="method in meeting.signMethods" :key="method" class="method-tag">
                {{ getMethodText(method) }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
          
          <div class="action-buttons" v-if="meeting">
            <el-button type="primary" v-if="meeting.status === 0" @click="handlePublish">发布会议</el-button>
            <el-button type="warning" v-if="meeting.status === 1" @click="handleStart">开始会议</el-button>
            <el-button type="danger" v-if="meeting.status === 2" @click="handleEnd">结束会议</el-button>
            <el-button @click="handleGenerateQrcode">生成签到二维码</el-button>
          </div>
        </div>
        
        <!-- 签到统计 -->
        <div class="card-container">
          <div class="card-header">
            <div class="ch-left">
              <el-icon class="ch-icon"><DataLine /></el-icon>
              <h3>签到统计</h3>
            </div>
          </div>
          <el-row :gutter="16" v-if="statistics" class="stat-grid-row">
            <el-col :xs="12" :sm="6">
              <div class="stat-card-item primary">
                <div class="stat-icon-box"><el-icon><User /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ statistics.totalCount || 0 }}</div>
                  <div class="stat-name">应到人数</div>
                </div>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="stat-card-item success">
                <div class="stat-icon-box"><el-icon><CircleCheck /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ statistics.signedCount || 0 }}</div>
                  <div class="stat-name">已签到</div>
                </div>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="stat-card-item warning">
                <div class="stat-icon-box"><el-icon><Clock /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ statistics.lateCount || 0 }}</div>
                  <div class="stat-name">迟到</div>
                </div>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="stat-card-item danger">
                <div class="stat-icon-box"><el-icon><CircleClose /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-num">{{ statistics.notSignedCount || 0 }}</div>
                  <div class="stat-name">未签到</div>
                </div>
              </div>
            </el-col>
          </el-row>
          
          <!-- 签到进度条 -->
          <div class="progress-section" v-if="statistics">
            <div class="progress-label">
              <span>出勤率</span>
              <strong>{{ statistics.attendanceRate || 0 }}%</strong>
            </div>
            <el-progress 
              :percentage="statistics.attendanceRate || 0" 
              :color="progressColor"
              :stroke-width="10"
              :show-text="false"
            />
          </div>
        </div>
      </el-col>
      
      <!-- 二维码和签到列表 -->
      <el-col :xs="24" :lg="8">
        <div class="card-container" v-if="qrcodeData">
          <div class="card-header">
            <div class="ch-left">
              <el-icon class="ch-icon"><Tickets /></el-icon>
              <h3>签到二维码</h3>
            </div>
          </div>
          <div class="qrcode-display">
            <img :src="qrcodeData.qrcodeImage" alt="二维码" />
            <p class="expire-time">有效期至：{{ formatTime(qrcodeData.expireTime) }}</p>
          </div>
        </div>
        
        <div class="card-container">
          <div class="card-header">
            <div class="ch-left">
              <el-icon class="ch-icon"><List /></el-icon>
              <h3>参会人员签到情况</h3>
            </div>
          </div>
          <div class="table-toolbar">
            <span class="toolbar-count">共 {{ attendees.length }} 人</span>
            <el-button 
              type="primary" 
              size="small" 
              :disabled="!attendees.some(a => a.status === 0)"
              @click="batchCheckIn"
            >
              一键签到所有未签到
            </el-button>
          </div>
          <el-table :data="attendees" v-loading="attendeesLoading" size="small" max-height="400">
            <el-table-column prop="user.realName" label="姓名" width="80">
              <template #default="{ row }">
                {{ row.user?.realName || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="user.position" label="职位" width="100">
              <template #default="{ row }">
                {{ row.user?.position || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="getAttendeeStatusType(row.status)" size="small">
                  {{ getAttendeeStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="签到时间" width="140">
              <template #default="{ row }">
                {{ row.signTime ? formatTime(row.signTime) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button 
                  v-if="row.status === 0" 
                  type="primary" 
                  size="small" 
                  @click="handleMockCheckIn(row.userId, row.user?.realName || '用户')"
                >
                  签到
                </el-button>
                <el-text v-else type="success" size="small">已签到</el-text>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <!-- 二维码对话框 -->
    <el-dialog 
      v-model="showQrcodeDialog" 
      title="会议签到二维码" 
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="qrcode-dialog-content">
        <div v-if="qrcodeData" class="qrcode-display">
          <img :src="qrcodeData.qrcodeImage" alt="二维码" />
          <p class="qrcode-tip">请使用手机扫描二维码进行签到</p>
          <p class="qrcode-expire">有效期至：{{ formatTime(qrcodeData.expireTime) }}</p>
          <p class="qrcode-refresh">二维码将每30秒自动刷新</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="showQrcodeDialog = false">关闭</el-button>
        <el-button type="primary" @click="handleGenerateQrcode">刷新二维码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getMeetingDetail, publishMeeting, startMeeting, endMeeting, generateQrcode,
  getMeetingAttendees, mockCheckIn
} from '@/api/meeting'
import { getMeetingStatistics } from '@/api/statistics'
import { getCheckInRecords } from '@/api/checkin'
import dayjs from 'dayjs'
import { ArrowLeft, Calendar, Tickets, User, CircleCheck, CircleClose, Clock, DataLine, List } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const meeting = ref(null)
const statistics = ref(null)
const attendees = ref([])
const attendeesLoading = ref(false)
const qrcodeData = ref(null)
const showQrcodeDialog = ref(false)
const currentUserId = ref(null)

// 二维码定时刷新
let qrcodeTimer = null

const startQrcodeAutoRefresh = () => {
  stopQrcodeAutoRefresh()
  qrcodeTimer = setInterval(() => {
    if (showQrcodeDialog.value) {
      handleGenerateQrcode()
    }
  }, 30000) // 每30秒刷新一次
}

const stopQrcodeAutoRefresh = () => {
  if (qrcodeTimer) {
    clearInterval(qrcodeTimer)
    qrcodeTimer = null
  }
}

// 在 showQrcodeDialog 变为 true 时启动定时刷新
watch(showQrcodeDialog, (newVal) => {
  if (newVal) {
    startQrcodeAutoRefresh()
  } else {
    stopQrcodeAutoRefresh()
  }
})

// 组件卸载时停止
onBeforeUnmount(() => {
  stopQrcodeAutoRefresh()
})

const progressColor = computed(() => {
  const rate = statistics.value?.attendanceRate || 0
  if (rate >= 80) return '#67C23A'
  if (rate >= 60) return '#E6A23C'
  return '#F56C6C'
})

const loadData = async () => {
  const id = route.params.mid
  try {
    const [meetingRes, statsRes, recordsRes, attendeesRes] = await Promise.all([
      getMeetingDetail(id),
      getMeetingStatistics(id),
      getCheckInRecords(id),
      getMeetingAttendees(id).catch(() => ({ data: [] }))
    ])
    meeting.value = meetingRes.data
    statistics.value = statsRes.data
    // 合并参会人员和签到记录
    const checkinMap = {}
    ;(recordsRes.data || []).forEach(r => { checkinMap[r.userId] = r })
    const allAttendees = (attendeesRes.data || []).map(a => ({
      ...a,
      user: a.user || { realName: '未知用户' },
      signTime: checkinMap[a.userId]?.signTime || a.signTime,
      signMethod: checkinMap[a.userId]?.signMethod || a.signMethod
    }))
    // 如果没有参会人员接口数据，使用签到记录
    attendees.value = allAttendees.length > 0 ? allAttendees : (recordsRes.data || [])
  } catch (error) {
    console.error('加载会议详情失败:', error)
  }
}

const handlePublish = async () => {
  try {
    await ElMessageBox.confirm('确定发布该会议吗？', '提示')
    await publishMeeting(route.params.mid)
    ElMessage.success('会议发布成功')
    loadData()
  } catch {}
}

const handleStart = async () => {
  try {
    await ElMessageBox.confirm('确定开始该会议吗？', '提示')
    await startMeeting(route.params.mid)
    ElMessage.success('会议已开始')
    loadData()
  } catch {}
}

const handleEnd = async () => {
  try {
    await ElMessageBox.confirm('确定结束该会议吗？', '提示')
    await endMeeting(route.params.mid)
    ElMessage.success('会议已结束')
    loadData()
  } catch {}
}

const handleGenerateQrcode = async () => {
  try {
    const res = await generateQrcode(route.params.mid)
    qrcodeData.value = res.data
    showQrcodeDialog.value = true
  } catch (error) {
    console.error('生成二维码失败:', error)
    ElMessage.error('生成二维码失败')
  }
}

const goBack = () => router.push(`/groups/${route.params.gid}`)

const handleMockCheckIn = async (userId, userName) => {
  try {
    await ElMessageBox.confirm(`确定为用户「${userName}」签到吗？`, '签到确认')
    await mockCheckIn(route.params.mid, { userId })
    ElMessage.success(`${userName} 签到成功`)
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '签到失败')
    }
  }
}

const batchCheckIn = async () => {
  const unsigned = attendees.value.filter(a => a.status === 0)
  if (unsigned.length === 0) {
    ElMessage.info('所有人员均已签到')
    return
  }
  try {
    await ElMessageBox.confirm(`确定为 ${unsigned.length} 位未签到人员快速签到吗？`, '批量签到')
    for (const a of unsigned) {
      try {
        await mockCheckIn(route.params.mid, { userId: a.userId })
      } catch {}
    }
    ElMessage.success(`已为 ${unsigned.length} 人签到`)
    loadData()
  } catch {}
}

const formatTime = (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
const getStatusType = (status) => ['info', 'warning', 'success', 'danger'][status] || 'info'
const getStatusText = (status) => ['草稿', '已发布', '进行中', '已结束'][status] || '未知'
const getMethodText = (method) => ({
  qrcode: '二维码', photo: '拍照',
  gesture: '手势', location: '定位'
}[method] || method)
const getAttendeeStatusType = (status) => ['info', 'success', 'warning', 'danger'][status] || 'info'
const getAttendeeStatusText = (status) => ['未签到', '已签到', '迟到', '缺勤'][status] || '未知'

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.meeting-detail {
  padding: $spacing-xl;
  max-width: 1400px;
  margin: 0 auto;
}

// 页面头部
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
  position: relative;
  overflow: hidden;

  &.detail-hero {
    background: $bg-white;
    border: 1px solid $border-light;
    border-radius: $radius-base;
    padding: $spacing-xl;
    color: $text-primary;
    box-shadow: $shadow-base;

    &::before {
      content: none;
    }

    &::after {
      content: none;
    }

    .hero-left {
      z-index: 2;
      flex: 1;

      .back-btn {
        margin-bottom: $spacing-md;
        background: $bg-base;
        border: 1px solid $border-light;
        color: $text-regular;

        &:hover {
          background: $color-primary-bg;
          color: $color-primary;
          border-color: rgba(37, 99, 235, 0.18);
        }
      }

      h2 {
        margin: 0 0 6px 0;
        font-size: 24px;
        font-weight: 600;
        color: $text-primary;
      }

      p {
        margin: 0;
        font-size: 13px;
        color: $text-secondary;
      }
    }

    .hero-status {
      z-index: 2;
    }

    .deco-icon {
      display: none;
    }

    .dec-1 { animation: float 3s ease-in-out infinite; }
    .dec-2 { animation: float 3s ease-in-out infinite 0.5s; }
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

// 卡片容器
.card-container {
  background: $bg-white;
  border-radius: $radius-base;
  border: 1px solid $border-light;
  padding: $spacing-lg;
  margin-bottom: $spacing-lg;
  box-shadow: $shadow-base;
  transition: $transition-base;

  &:hover {
    border-color: $border-base;
    box-shadow: $shadow-hover;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
  padding-bottom: $spacing-md;
  border-bottom: 1px solid $border-extra-light;

  .ch-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .ch-icon {
      font-size: 18px;
      color: $color-primary;
    }

    h3 {
      margin: 0;
      font-size: 16px;
      color: $text-primary;
    }
  }
}

.method-tag {
  margin-right: 8px;
}

.action-buttons {
  margin-top: $spacing-md;
  display: flex;
  gap: $spacing-sm;
  flex-wrap: wrap;
}

// 统计卡片
.stat-grid-row {
  margin-bottom: $spacing-md;
}

.stat-card-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: $bg-base;
  border-radius: $radius-base;
  transition: $transition-base;

  &:hover {
    transform: translateY(-1px);
    box-shadow: $shadow-light;
  }

  .stat-icon-box {
    width: 44px;
    height: 44px;
    border-radius: $radius-base;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    color: $color-primary;
    flex-shrink: 0;
  }

  &.primary .stat-icon-box { background: $color-primary-bg; color: $color-primary; }
  &.success .stat-icon-box { background: $color-success-bg; color: $color-success; }
  &.warning .stat-icon-box { background: $color-warning-bg; color: $color-warning; }
  &.danger .stat-icon-box { background: $color-danger-bg; color: $color-danger; }

  .stat-info {
    flex: 1;
    min-width: 0;
  }

  .stat-num {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    line-height: 1.2;
  }

  .stat-name {
    font-size: 12px;
    color: $text-secondary;
    margin-top: 2px;
  }
}

.progress-section {
  .progress-label {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    font-size: 13px;
    color: $text-secondary;

    strong {
      font-size: 16px;
      color: $color-primary;
    }
  }
}

// 二维码展示
.qrcode-display {
  text-align: center;
  padding: $spacing-md;

  img {
    width: 200px;
    height: 200px;
    border-radius: $radius-base;
  }

  .expire-time {
    margin-top: $spacing-sm;
    font-size: 13px;
    color: $text-secondary;
  }
}

.qrcode-dialog-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 250px;
}

.qrcode-tip {
  margin-top: $spacing-md;
  font-size: 14px;
  color: $text-regular;
}

.qrcode-expire {
  margin-top: $spacing-sm;
  font-size: 13px;
  color: $text-secondary;
}

.qrcode-refresh {
  margin-top: $spacing-sm;
  font-size: 12px;
  color: $text-placeholder;
  font-style: italic;
}

// 表格工具栏
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-sm;

  .toolbar-count {
    color: $text-secondary;
    font-size: 13px;
  }
}

// 响应式
@media (max-width: 768px) {
  .page-header.detail-hero {
    flex-direction: column;
    align-items: flex-start;
    gap: $spacing-md;
  }

  .stat-card-item {
    padding: 12px;
  }
}
</style>

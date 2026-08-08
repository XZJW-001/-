<template>
  <div class="dashboard-container">
    <!-- 页面头部 -->
    <div class="page-header dashboard-hero">
      <div class="header-content">
        <h2>工作台</h2>
        <p>欢迎回来，{{ userStore.userName }}。今天有 {{ stats.todayCheckIns }} 场会议安排，{{ stats.ongoingMeetings }} 个会议进行中</p>
      </div>
      <div class="header-right">
        <div class="header-time" v-if="currentTime">
          <el-icon><Clock /></el-icon>
          <span>{{ currentTime }}</span>
        </div>
        <div class="header-date">
          {{ currentDate }}
        </div>
      </div>
      <el-icon class="deco-icon dec-1"><DataLine /></el-icon>
      <el-icon class="deco-icon dec-2"><TrendCharts /></el-icon>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card primary">
        <div class="stat-icon"><el-icon><Calendar /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.totalMeetings }}</div>
          <div class="stat-label">总会议数</div>
        </div>
      </div>
      <div class="stat-card success">
        <div class="stat-icon"><el-icon><VideoCamera /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.ongoingMeetings }}</div>
          <div class="stat-label">进行中会议</div>
        </div>
      </div>
      <div class="stat-card warning">
        <div class="stat-icon"><el-icon><Tickets /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.todayCheckIns }}</div>
          <div class="stat-label">今日会议</div>
        </div>
      </div>
      <div class="stat-card info">
        <div class="stat-icon"><el-icon><TrendCharts /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.attendanceRate }}%</div>
          <div class="stat-label">平均出勤率</div>
        </div>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：最近会议 + 今日安排 -->
      <el-col :xs="24" :lg="16">
        <div class="card-container">
          <div class="card-header">
            <div class="ch-left">
              <el-icon class="ch-icon"><Calendar /></el-icon>
              <h3>最近会议</h3>
            </div>
            <el-button type="primary" link @click="goToGroups">查看全部</el-button>
          </div>
          <el-table :data="recentMeetings" style="width: 100%" stripe>
            <el-table-column prop="title" label="会议主题" min-width="150" />
            <el-table-column prop="location" label="地点" width="120" />
            <el-table-column label="时间" width="160">
              <template #default="{ row }">
                {{ formatTime(row.startTime) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 近期活动 -->
        <div class="card-container activity-card">
          <div class="card-header">
            <div class="ch-left">
              <el-icon class="ch-icon"><Clock /></el-icon>
              <h3>近期活动</h3>
            </div>
            <el-button link type="primary" @click="goToGroups">查看日历</el-button>
          </div>
          <div class="timeline" v-if="allUpcomingMeetings.length > 0">
            <div class="timeline-item" v-for="m in allUpcomingMeetings" :key="m.id">
              <div class="timeline-dot" :class="'dot-' + getStatusClass(m.status)"></div>
              <div class="timeline-content">
                <div class="tl-header">
                  <span class="tl-title">{{ m.title }}</span>
                  <el-tag size="small" :type="getStatusType(m.status)">{{ getStatusText(m.status) }}</el-tag>
                </div>
                <div class="tl-meta">
                  <span><el-icon><Location /></el-icon> {{ m.location || '待定' }}</span>
                  <span><el-icon><Clock /></el-icon> {{ formatTime(m.startTime) }}</span>
                </div>
              </div>
              <div class="tl-action">
                <el-button size="small" type="primary" link @click="viewDetail(m)">进入</el-button>
              </div>
            </div>
          </div>
          <div class="activity-empty" v-else>
            <el-icon class="empty-icon"><Calendar /></el-icon>
            <p>近期暂无活动</p>
          </div>
        </div>
      </el-col>

      <!-- 右侧：快捷操作 + 待办 + 系统 -->
      <el-col :xs="24" :lg="8">
        <!-- 快捷入口网格 -->
        <div class="card-container">
          <div class="card-header">
            <div class="ch-left">
              <el-icon class="ch-icon"><Grid /></el-icon>
              <h3>快捷入口</h3>
            </div>
          </div>
          <div class="action-grid">
            <div class="ag-item" @click="createGroup">
              <div class="ag-icon ag-primary"><el-icon><Plus /></el-icon></div>
              <span>创建群组</span>
            </div>
            <div class="ag-item" @click="goToGroups">
              <div class="ag-icon ag-success"><el-icon><ChatLineRound /></el-icon></div>
              <span>群聊列表</span>
            </div>
            <div class="ag-item" @click="goToStatistics">
              <div class="ag-icon ag-warning"><el-icon><TrendCharts /></el-icon></div>
              <span>数据统计</span>
            </div>
            <div class="ag-item" @click="goToApproval">
              <div class="ag-icon ag-danger"><el-icon><DocumentChecked /></el-icon></div>
              <span>审批中心</span>
            </div>
            <div class="ag-item" @click="goToCheckInList">
              <div class="ag-icon ag-info"><el-icon><Tickets /></el-icon></div>
              <span>签到记录</span>
            </div>
            <div class="ag-item" @click="goToUser">
              <div class="ag-icon ag-primary"><el-icon><UserFilled /></el-icon></div>
              <span>用户管理</span>
            </div>
          </div>
        </div>

        <!-- 我的待办 -->
        <div class="card-container">
          <div class="card-header">
            <div class="ch-left">
              <el-icon class="ch-icon"><Bell /></el-icon>
              <h3>我的待办</h3>
            </div>
            <el-badge :value="todoCount" class="todo-badge" v-if="todoCount > 0" />
          </div>
          <div class="todo-list" v-if="todoCount > 0">
            <div class="todo-item" v-for="todo in todos" :key="todo.id">
              <div class="todo-icon" :class="todo.type">
                <el-icon v-if="todo.type === 'checkin'"><Tickets /></el-icon>
                <el-icon v-else-if="todo.type === 'approval'"><DocumentChecked /></el-icon>
                <el-icon v-else><Calendar /></el-icon>
              </div>
              <div class="todo-body">
                <div class="todo-title">{{ todo.title }}</div>
                <div class="todo-desc">{{ todo.desc }}</div>
              </div>
              <el-button size="small" type="primary" link @click="handleTodo(todo)">处理</el-button>
            </div>
          </div>
          <div class="todo-empty" v-else>
            <el-icon class="empty-icon"><CircleCheck /></el-icon>
            <p>暂无待办事项</p>
          </div>
        </div>

        <!-- 系统信息 -->
        <div class="card-container">
          <div class="card-header">
            <div class="ch-left">
              <el-icon class="ch-icon"><InfoFilled /></el-icon>
              <h3>系统信息</h3>
            </div>
          </div>
          <div class="system-info">
            <div class="info-row">
              <span class="info-label">系统版本</span>
              <span class="info-value">V1.0.0</span>
            </div>
            <div class="info-row">
              <span class="info-label">数据同步</span>
              <span class="info-value">实时</span>
            </div>
            <div class="info-row">
              <span class="info-label">运行状态</span>
              <el-tag size="small" type="success">● 运行中</el-tag>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getMeetingList } from '@/api/meeting'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { Clock, Calendar, VideoCamera, Tickets, TrendCharts, Grid, Plus, ChatLineRound, DocumentChecked, Bell, CircleCheck, InfoFilled, Location, DataLine, Document, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

dayjs.locale('zh-cn')

const stats = reactive({
  totalMeetings: 0,
  ongoingMeetings: 0,
  todayCheckIns: 0,
  attendanceRate: 0
})

const recentMeetings = ref([])
const todayMeetings = ref([])
const currentTime = ref('')
const currentDate = ref('')
let timeTimer = null

// 合并近期活动（所有会议，不限今天）
const allUpcomingMeetings = computed(() => {
  return [...recentMeetings.value].sort((a, b) => {
    return new Date(b.startTime) - new Date(a.startTime)
  }).slice(0, 5)
})

// 待办事项 - 根据会议数据动态生成
const todos = computed(() => {
  const items = []
  const today = dayjs().format('YYYY-MM-DD')
  
  todayMeetings.value.forEach(m => {
    if (m.status === 1 || m.status === 2) {
      items.push({
        id: 'checkin-' + m.id,
        type: 'checkin',
        title: `待签到：${m.title}`,
        desc: `${dayjs(m.startTime).format('HH:mm')} 开始`,
        url: '/checkin'
      })
    }
  })
  
  if (items.length === 0) {
    items.push({
      id: 'no-todo',
      type: 'meeting',
      title: '暂无待办事项',
      desc: '所有签到任务已完成',
      url: ''
    })
  }
  
  return items.slice(0, 3)
})
const todoCount = computed(() => todos.value.filter(t => t.url).length)

const updateTime = () => {
  currentTime.value = dayjs().format('HH:mm:ss')
  currentDate.value = dayjs().format('YYYY年MM月DD日 dddd')
}

const loadData = async () => {
  try {
    const res = await getMeetingList({ current: 1, size: 5 })
    recentMeetings.value = res.data.records || []
    stats.totalMeetings = res.data.total || 0
    stats.ongoingMeetings = recentMeetings.value.filter(m => m.status === 2).length
    
    // 计算今日安排
    const today = dayjs().format('YYYY-MM-DD')
    todayMeetings.value = recentMeetings.value.filter(m => {
      const d = dayjs(m.startTime).format('YYYY-MM-DD')
      return d === today
    })
    
    // 动态计算签到数据
    stats.todayCheckIns = todayMeetings.value.length
    const totalSessions = recentMeetings.value.length || 1
    const signedMeetings = recentMeetings.value.filter(m => m.status >= 2).length
    stats.attendanceRate = Math.round((signedMeetings / totalSessions) * 100)
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return dayjs(time).format('MM-DD HH:mm')
}

const getStatusType = (status) => {
  const types = ['info', 'warning', 'success', 'danger']
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = ['草稿', '已发布', '进行中', '已结束']
  return texts[status] || '未知'
}

const getStatusClass = (status) => {
  const classes = ['draft', 'published', 'ongoing', 'ended']
  return classes[status] || 'draft'
}

const viewDetail = (row) => {
  if (row.groupId) {
    router.push(`/groups/${row.groupId}/meetings/${row.id}`)
  } else {
    router.push('/statistics')
  }
}

const goToGroups = () => router.push('/groups')
const goToStatistics = () => router.push('/statistics')
const goToApproval = () => router.push('/approvals')
const goToCheckInList = () => router.push('/checkins')
const goToUser = () => router.push('/users')
const createGroup = () => router.push('/groups')

const handleTodo = (todo) => {
  if (todo.url) {
    router.push(todo.url)
  }
}

onMounted(() => {
  loadData()
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
})
</script>

<style lang="scss" scoped>
.dashboard-container {
  width: min(100%, 1560px);
  min-height: 100%;
  margin: 0 auto;
  padding: clamp(16px, 2vw, 28px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
  position: relative;
  overflow: hidden;

  &.dashboard-hero {
    background: transparent;
    border: 0;
    border-radius: 0;
    padding: 4px 0 8px;
    color: $text-primary;
    box-shadow: none;

    &::before {
      content: none;
    }

    &::after {
      content: none;
    }

    .header-content {
      z-index: 2;
      flex: 1;

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

    .header-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 8px;
      z-index: 2;

      .header-time {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 8px 14px;
        background: $color-primary-bg;
        border-radius: $radius-round;
        font-size: 14px;
        color: $color-primary;

        .el-icon { color: $color-primary; }
      }

      .header-date {
        font-size: 12px;
        color: $text-secondary;
      }
    }

    .deco-icon {
      display: none;
    }

    .dec-1 { animation: float 3s ease-in-out infinite; }
    .dec-2 { animation: float 3s ease-in-out infinite 0.5s; }
  }

  .header-content {
    h2 {
      margin: 0 0 6px 0;
      font-size: 22px;
      font-weight: 600;
      color: $text-primary;
    }
    p {
      margin: 0;
      font-size: 13px;
      color: $text-secondary;
    }
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

// 统计卡片网格
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-lg;
}

.stat-card {
  background: $bg-white;
  border-radius: $radius-base;
  padding: $spacing-lg;
  border: 1px solid $border-light;
  display: flex;
  align-items: center;
  gap: $spacing-md;
  box-shadow: $shadow-base;
  transition: $transition-base;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
  }

  &::after {
    content: none;
  }

  &.primary::before { background: $color-primary; }
  &.primary::after { background: $color-primary; }
  &.success::before { background: $color-success; }
  &.success::after { background: $color-success; }
  &.warning::before { background: $color-warning; }
  &.warning::after { background: $color-warning; }
  &.info::before { background: $color-info; }
  &.info::after { background: $color-info; }

  &:hover {
    border-color: $border-base;
    box-shadow: $shadow-light;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: $radius-base;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    color: #fff;
    flex-shrink: 0;
    z-index: 1;
  }

  &.primary .stat-icon { background: $color-primary-bg; color: $color-primary; }
  &.success .stat-icon { background: $color-success-bg; color: $color-success; }
  &.warning .stat-icon { background: $color-warning-bg; color: $color-warning; }
  &.info .stat-icon { background: $color-info-bg; color: $color-info; }

  .stat-body {
    z-index: 1;

    .stat-value {
      font-size: 28px;
      font-weight: 600;
      color: $text-primary;
      line-height: 1.2;
    }

    .stat-label {
      font-size: 13px;
      color: $text-secondary;
      margin-top: 2px;
    }
  }
}

// 卡片容器
.card-container {
  background: $bg-white;
  border-radius: $radius-base;
  padding: $spacing-lg;
  margin-bottom: $spacing-lg;
  border: 1px solid $border-light;
  box-shadow: $shadow-base;

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
}

// 快捷入口网格（2列3行）
.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-sm;
}

.ag-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: $spacing-md $spacing-sm;
  border-radius: $radius-base;
  cursor: pointer;
  transition: $transition-fast;
  border: 1px solid $border-extra-light;
  font-size: 13px;
  color: $text-regular;

  &:hover {
    background: $bg-base;
    border-color: $color-primary;
    transform: translateY(-1px);
    box-shadow: $shadow-light;
  }

  .ag-icon {
    width: 44px;
    height: 44px;
    border-radius: $radius-base;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #fff;

    &.ag-primary { background: $color-primary-bg; color: $color-primary; }
    &.ag-success { background: $color-success-bg; color: $color-success; }
    &.ag-warning { background: $color-warning-bg; color: $color-warning; }
    &.ag-danger { background: $color-danger-bg; color: $color-danger; }
    &.ag-info { background: $color-info-bg; color: $color-info; }
  }
}

// 时间线（今日安排）
.timeline {
  position: relative;
  padding-left: 24px;
}

.timeline-item {
  position: relative;
  padding: 12px 0 12px 16px;
  border-left: 2px solid $border-lighter;
  display: flex;
  align-items: center;
  gap: $spacing-md;

  &:last-child {
    border-left-color: transparent;
  }

  .timeline-dot {
    position: absolute;
    left: -25px;
    top: 18px;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: $border-base;
    border: 2px solid #fff;

    &.dot-draft { background: $color-info; }
    &.dot-published { background: $color-warning; }
    &.dot-ongoing { background: $color-success; box-shadow: 0 0 0 4px rgba(103, 194, 58, 0.2); }
    &.dot-ended { background: $color-danger; }
  }

  .timeline-content {
    flex: 1;
    min-width: 0;

    .tl-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 6px;

      .tl-title {
        font-size: 14px;
        font-weight: 600;
        color: $text-primary;
      }
    }

    .tl-meta {
      display: flex;
      gap: $spacing-md;
      font-size: 12px;
      color: $text-secondary;

      span {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

// 待办列表
.todo-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm;
  border-radius: $radius-base;
  transition: $transition-fast;

  &:hover {
    background: $bg-base;
  }

  .todo-icon {
    width: 36px;
    height: 36px;
    border-radius: $radius-base;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    color: #fff;
    flex-shrink: 0;

    &.checkin { background: $color-primary-bg; color: $color-primary; }
    &.approval { background: $color-warning-bg; color: $color-warning; }
    &.meeting { background: $color-success-bg; color: $color-success; }
  }

  .todo-body {
    flex: 1;
    min-width: 0;

    .todo-title {
      font-size: 13px;
      font-weight: 500;
      color: $text-primary;
    }

    .todo-desc {
      font-size: 12px;
      color: $text-secondary;
      margin-top: 2px;
    }
  }
}

.todo-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-md;
  color: $text-secondary;

  .empty-icon {
    font-size: 48px;
    color: $color-success;
    margin-bottom: $spacing-sm;
  }

  p {
    font-size: 13px;
    margin: 0;
  }
}

// 近期活动空状态 - 紧凑版
.activity-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.activity-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-lg;
  color: $text-secondary;
  min-height: 160px;

  .empty-icon {
    font-size: 40px;
    color: $color-info;
    margin-bottom: $spacing-sm;
    opacity: 0.5;
  }

  p {
    font-size: 13px;
    margin: 0;
  }
}

.todo-badge {
  :deep(.el-badge__content) {
    background: $color-danger;
  }
}

// 系统信息
.system-info {
  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px dashed $border-lighter;

    &:last-child { border-bottom: none; }

    .info-label {
      font-size: 13px;
      color: $text-secondary;
    }
    .info-value {
      font-size: 13px;
      color: $text-primary;
      font-weight: 500;
    }
  }
}

// 响应式
@media (max-width: 992px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .action-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 480px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>

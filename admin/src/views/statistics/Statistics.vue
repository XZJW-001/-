<template>
  <div class="statistics-page">
    <!-- 页面头部 -->
    <div class="page-header stat-header">
      <div class="header-content">
        <h2>数据统计</h2>
        <p>{{ selectedMeeting ? selectedMeeting.title : '请选择会议' }} · 签到数据统计与分析</p>
      </div>
      <div class="header-decorate">
        <div class="stat-hero">
          <div class="hero-ring">
            <svg viewBox="0 0 100 100" class="ring-svg">
              <circle class="ring-bg" cx="50" cy="50" r="42" />
              <circle class="ring-fg" cx="50" cy="50" r="42"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="ringOffset" />
            </svg>
            <div class="ring-text">
              <span class="ring-value">{{ currentStats.attendanceRate || 0 }}%</span>
              <span class="ring-label">出勤率</span>
            </div>
          </div>
        </div>
        <el-icon class="dec-icon dec-1"><DataLine /></el-icon>
        <el-icon class="dec-icon dec-2"><TrendCharts /></el-icon>
        <el-icon class="dec-icon dec-3"><PieChart /></el-icon>
      </div>
    </div>

    <!-- 会议选择 -->
    <div class="card-container filter-card">
      <div class="filter-header">
        <div class="ch-left">
          <el-icon class="ch-icon"><Calendar /></el-icon>
          <h3>选择会议查看统计</h3>
        </div>
        <el-select v-model="selectedMeetingId" placeholder="请选择会议" style="width: 400px;" @change="loadStatistics" filterable>
          <el-option
            v-for="meeting in meetingOptions"
            :key="meeting.id"
            :label="`${meeting.title} (${formatMeetingTime(meeting.startTime)})`"
            :value="meeting.id"
          />
        </el-select>
      </div>
      <!-- 会议快速信息 -->
      <div class="meeting-quick-info" v-if="selectedMeeting">
        <span class="info-chip">
          <el-icon><Location /></el-icon>
          {{ selectedMeeting.location || '地点待定' }}
        </span>
        <span class="info-chip">
          <el-icon><Clock /></el-icon>
          {{ formatMeetingTime(selectedMeeting.startTime) }} ~ {{ formatMeetingTime(selectedMeeting.endTime) }}
        </span>
        <span class="info-chip" v-if="selectedMeeting.groupId">
          <el-icon><ChatLineRound /></el-icon>
          群聊签到
        </span>
        <span class="info-chip method-chip" v-for="method in selectedMeeting.signMethods" :key="method">
          {{ getMethodText(method) }}
        </span>
      </div>
    </div>

    <template v-if="selectedMeetingId">
      <!-- 核心指标卡片网格 -->
      <div class="stat-grid">
        <div class="stat-card primary">
          <div class="stat-card-bg"></div>
          <div class="stat-icon-wrap">
            <div class="stat-icon"><el-icon><User /></el-icon></div>
          </div>
          <div class="stat-body">
            <div class="stat-value-row">
              <span class="stat-value">{{ currentStats.totalCount || 0 }}</span>
              <span class="stat-unit">人</span>
            </div>
            <div class="stat-label">应到人数</div>
          </div>
          <div class="stat-foot">
            <span class="foot-item">总参会</span>
          </div>
        </div>

        <div class="stat-card success">
          <div class="stat-card-bg"></div>
          <div class="stat-icon-wrap">
            <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
            <div class="stat-badge">{{ getRate(currentStats.signedCount, currentStats.totalCount) }}%</div>
          </div>
          <div class="stat-body">
            <div class="stat-value-row">
              <span class="stat-value">{{ currentStats.signedCount || 0 }}</span>
              <span class="stat-unit">人</span>
            </div>
            <div class="stat-label">已签到</div>
          </div>
          <div class="stat-foot">
            <span class="foot-item">占比 {{ getRate(currentStats.signedCount, currentStats.totalCount) }}%</span>
          </div>
        </div>

        <div class="stat-card warning">
          <div class="stat-card-bg"></div>
          <div class="stat-icon-wrap">
            <div class="stat-icon"><el-icon><Clock /></el-icon></div>
            <div class="stat-badge">{{ getRate(currentStats.lateCount, currentStats.totalCount) }}%</div>
          </div>
          <div class="stat-body">
            <div class="stat-value-row">
              <span class="stat-value">{{ currentStats.lateCount || 0 }}</span>
              <span class="stat-unit">人</span>
            </div>
            <div class="stat-label">迟到</div>
          </div>
          <div class="stat-foot">
            <span class="foot-item">占比 {{ getRate(currentStats.lateCount, currentStats.totalCount) }}%</span>
          </div>
        </div>

        <div class="stat-card info">
          <div class="stat-card-bg"></div>
          <div class="stat-icon-wrap">
            <div class="stat-icon"><el-icon><DocumentAdd /></el-icon></div>
            <div class="stat-badge">{{ getRate(currentStats.makeupCount, currentStats.totalCount) }}%</div>
          </div>
          <div class="stat-body">
            <div class="stat-value-row">
              <span class="stat-value">{{ currentStats.makeupCount || 0 }}</span>
              <span class="stat-unit">人</span>
            </div>
            <div class="stat-label">补签</div>
          </div>
          <div class="stat-foot">
            <span class="foot-item">占比 {{ getRate(currentStats.makeupCount, currentStats.totalCount) }}%</span>
          </div>
        </div>

        <div class="stat-card proxy">
          <div class="stat-card-bg"></div>
          <div class="stat-icon-wrap">
            <div class="stat-icon"><el-icon><Switch /></el-icon></div>
          </div>
          <div class="stat-body">
            <div class="stat-value-row">
              <span class="stat-value">{{ currentStats.proxyCount || 0 }}</span>
              <span class="stat-unit">人</span>
            </div>
            <div class="stat-label">代签</div>
          </div>
          <div class="stat-foot">
            <span class="foot-item">占比 {{ getRate(currentStats.proxyCount, currentStats.totalCount) }}%</span>
          </div>
        </div>

        <div class="stat-card danger">
          <div class="stat-card-bg"></div>
          <div class="stat-icon-wrap">
            <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
            <div class="stat-badge danger-badge">{{ getRate(currentStats.notSignedCount, currentStats.totalCount) }}%</div>
          </div>
          <div class="stat-body">
            <div class="stat-value-row">
              <span class="stat-value">{{ currentStats.notSignedCount || 0 }}</span>
              <span class="stat-unit">人</span>
            </div>
            <div class="stat-label">未签到</div>
          </div>
          <div class="stat-foot">
            <span class="foot-item">占比 {{ getRate(currentStats.notSignedCount, currentStats.totalCount) }}%</span>
          </div>
        </div>
      </div>

      <!-- 第一行：出勤率环形图 + 右侧面板 -->
      <el-row :gutter="20">
        <el-col :xs="24" :lg="16">
          <!-- 出勤率环形图 -->
          <div class="card-container rate-card">
            <div class="rate-main">
              <div class="rate-ring-wrap">
                <svg viewBox="0 0 160 160" class="rate-ring-svg">
                  <defs>
                    <linearGradient id="ringGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stop-color="#2563EB" />
                      <stop offset="100%" stop-color="#60A5FA" />
                    </linearGradient>
                  </defs>
                  <circle class="rate-ring-bg" cx="80" cy="80" r="68" />
                  <circle class="rate-ring-fg" cx="80" cy="80" r="68"
                    stroke="url(#ringGrad)"
                    :stroke-dasharray="ringCircumference"
                    :stroke-dashoffset="rateRingOffset"
                    stroke-linecap="round" />
                </svg>
                <div class="rate-ring-center">
                  <div class="rate-ring-value">{{ currentStats.attendanceRate || 0 }}%</div>
                  <div class="rate-ring-label">出勤率</div>
                </div>
              </div>
              <div class="rate-details">
                <div class="detail-item">
                  <div class="detail-bar">
                    <div class="bar-fill bar-success" :style="{ width: getRate(currentStats.signedCount, currentStats.totalCount) + '%' }"></div>
                  </div>
                  <div class="detail-meta">
                    <span class="dot-success"></span>
                    <span>已签到</span>
                    <strong>{{ currentStats.signedCount || 0 }} 人</strong>
                  </div>
                </div>
                <div class="detail-item">
                  <div class="detail-bar">
                    <div class="bar-fill bar-warning" :style="{ width: getRate(currentStats.lateCount, currentStats.totalCount) + '%' }"></div>
                  </div>
                  <div class="detail-meta">
                    <span class="dot-warning"></span>
                    <span>迟到</span>
                    <strong>{{ currentStats.lateCount || 0 }} 人</strong>
                  </div>
                </div>
                <div class="detail-item">
                  <div class="detail-bar">
                    <div class="bar-fill bar-danger" :style="{ width: getRate(currentStats.notSignedCount, currentStats.totalCount) + '%' }"></div>
                  </div>
                  <div class="detail-meta">
                    <span class="dot-danger"></span>
                    <span>未签到</span>
                    <strong>{{ currentStats.notSignedCount || 0 }} 人</strong>
                  </div>
                </div>
                <div class="detail-item">
                  <div class="detail-bar">
                    <div class="bar-fill bar-info" :style="{ width: getRate((currentStats.makeupCount || 0) + (currentStats.proxyCount || 0), currentStats.totalCount) + '%' }"></div>
                  </div>
                  <div class="detail-meta">
                    <span class="dot-info"></span>
                    <span>补/代签</span>
                    <strong>{{ (currentStats.makeupCount || 0) + (currentStats.proxyCount || 0) }} 人</strong>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 右列 -->
        <el-col :xs="24" :lg="8">
          <!-- 签到方式统计 -->
          <div class="card-container method-card">
            <div class="card-header">
              <div class="ch-left">
                <el-icon class="ch-icon"><List /></el-icon>
                <h3>签到方式统计</h3>
              </div>
            </div>
            <div class="method-list">
              <div class="method-item" v-for="item in methodStats" :key="item.key">
                <div class="method-icon" :style="{ background: item.color }">
                  <el-icon><component :is="methodIconMap[item.key]" /></el-icon>
                </div>
                <div class="method-info">
                  <div class="method-name">{{ item.name }}</div>
                  <div class="method-bar">
                    <div class="method-bar-fill" :style="{ width: item.percent + '%', background: item.color }"></div>
                  </div>
                </div>
                <div class="method-count">{{ item.count }}</div>
              </div>
              <div class="method-empty" v-if="methodStats.length === 0">
                <p>暂无签到数据</p>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 第二行：图表区域（全宽） -->
      <el-row :gutter="20">
        <el-col :xs="24" :md="12">
          <div class="card-container chart-card">
            <div class="card-header">
              <div class="ch-left">
                <el-icon class="ch-icon"><PieChart /></el-icon>
                <h3>会议出勤率</h3>
              </div>
            </div>
            <div ref="pieChartRef" class="chart-box"></div>
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="card-container chart-card">
            <div class="card-header">
              <div class="ch-left">
                <el-icon class="ch-icon"><Histogram /></el-icon>
                <h3>签到方式分布</h3>
              </div>
            </div>
            <div ref="barChartRef" class="chart-box"></div>
          </div>
        </el-col>
      </el-row>

      <!-- 第三行：导出 + 说明 -->
      <el-row :gutter="20">
        <el-col :xs="24" :md="12">
          <div class="card-container export-card">
            <div class="export-header">
              <el-icon class="export-icon-lg"><Download /></el-icon>
              <div class="export-title">报表导出</div>
              <div class="export-desc">导出当前会议签到统计</div>
            </div>
            <div class="export-buttons">
              <el-button type="primary" size="large" @click="exportCSV" :disabled="!selectedMeetingId" :loading="exporting">
                <el-icon><Download /></el-icon>
                导出 CSV 报表
              </el-button>
              <el-button size="large" @click="exportPDF" :disabled="!selectedMeetingId">
                <el-icon><DocumentCopy /></el-icon>
                导出 PDF 报表
              </el-button>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="card-container guide-card">
            <div class="card-header">
              <div class="ch-left">
                <el-icon class="ch-icon"><InfoFilled /></el-icon>
                <h3>统计说明</h3>
              </div>
            </div>
            <ul class="stat-desc">
              <li><el-icon class="desc-icon success"><CircleCheck /></el-icon><div><strong>出勤率：</strong>已签到人数 / 应到人数</div></li>
              <li><el-icon class="desc-icon warning"><Clock /></el-icon><div><strong>迟到：</strong>签到时间超过阈值</div></li>
              <li><el-icon class="desc-icon danger"><CircleClose /></el-icon><div><strong>未签到：</strong>会议结束仍未签到</div></li>
              <li><el-icon class="desc-icon info"><Warning /></el-icon><div><strong>补签/代签：</strong>管理员手动签到</div></li>
            </ul>
          </div>
        </el-col>
      </el-row>

      <!-- 人员签到明细 -->
      <div class="card-container detail-card">
        <div class="card-header">
          <div class="ch-left">
            <el-icon class="ch-icon"><List /></el-icon>
            <h3>人员签到状态明细</h3>
          </div>
          <div class="detail-summary">
            <el-tag type="success" size="small">已签到 {{ currentStats.signedCount || 0 }}</el-tag>
            <el-tag type="warning" size="small">迟到 {{ currentStats.lateCount || 0 }}</el-tag>
            <el-tag type="danger" size="small">未签到 {{ currentStats.notSignedCount || 0 }}</el-tag>
          </div>
        </div>
        <el-table :data="attendeeList" v-loading="loading" stripe style="width: 100%" :header-cell-style="{background: '#FAFBFC', color: '#606266'}">
          <el-table-column label="姓名" width="100">
            <template #default="{ row }">{{ row.user?.realName || '-' }}</template>
          </el-table-column>
          <el-table-column label="职位" width="120">
            <template #default="{ row }">{{ row.user?.position || '-' }}</template>
          </el-table-column>
          <el-table-column label="签到状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getAttendeeStatusType(row.status)" effect="light" size="small">{{ getAttendeeStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="签到时间" width="160">
            <template #default="{ row }">{{ row.signTime ? formatTime(row.signTime) : '-' }}</template>
          </el-table-column>
          <el-table-column label="签到方式" width="100">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ getMethodText(row.signMethod) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="签到位置" min-width="180">
            <template #default="{ row }">{{ row.location || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewDetail(row)">
                <el-icon><View /></el-icon>详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 定位签到地图展示 -->
      <div class="card-container map-card" v-if="hasLocationSign">
        <div class="map-header">
          <div class="ch-left">
            <el-icon class="ch-icon"><Location /></el-icon>
            <h3>定位签到位置分布</h3>
          </div>
          <el-tag v-if="locationList.length" type="success" size="small" effect="light">共 {{ locationList.length }} 个定位签到</el-tag>
          <el-tag v-else type="info" size="small" effect="light">暂无定位签到数据</el-tag>
        </div>
        <div v-if="!mapAvailable" class="map-unavailable">
          <el-icon class="unavail-icon"><Warning /></el-icon>
          <p>地图服务未加载，无法显示位置分布</p>
        </div>
        <template v-else>
          <div ref="mapContainerRef" class="map-container"></div>
          <div class="location-list" v-if="locationList.length">
            <div class="location-item" v-for="(loc, idx) in locationList" :key="idx" @click="focusLocation(loc)">
              <div class="loc-avatar">{{ loc.user?.realName?.charAt(0) || '?' }}</div>
              <div class="loc-info">
                <div class="loc-name">{{ loc.user?.realName || '未知' }}</div>
                <div class="loc-address">{{ loc.location || `${loc.longitude}, ${loc.latitude}` }}</div>
                <div class="loc-time">{{ formatTime(loc.signTime) }}</div>
              </div>
              <el-tag size="small" :type="loc.signStatus === 1 ? 'success' : 'warning'" effect="light">
                {{ loc.signStatus === 1 ? '正常' : '迟到' }}
              </el-tag>
            </div>
          </div>
        </template>
      </div>

      <!-- 拍照签到照片展示 -->
      <div class="card-container photo-card" v-if="hasPhotoSign">
        <div class="map-header">
          <div class="ch-left">
            <el-icon class="ch-icon"><Camera /></el-icon>
            <h3>拍照签到记录</h3>
          </div>
          <el-tag v-if="photoList.length" type="success" size="small" effect="light">共 {{ photoList.length }} 张签到照片</el-tag>
          <el-tag v-else type="info" size="small" effect="light">暂无拍照签到数据</el-tag>
        </div>
        <div class="photo-grid" v-if="photoList.length">
          <div class="photo-card-item" v-for="(photo, idx) in photoList" :key="idx" @click="previewPhoto(photo)">
            <img :src="photo.photoData" :alt="photo.user?.realName || '签到照片'" class="photo-thumb" />
            <div class="photo-overlay">
              <div class="photo-name">{{ photo.user?.realName || '未知用户' }}</div>
              <div class="photo-time">{{ formatTime(photo.signTime) }}</div>
            </div>
            <el-tag class="photo-status" size="small" :type="photo.signStatus === 1 ? 'success' : 'warning'" effect="dark">
              {{ photo.signStatus === 1 ? '正常' : '迟到' }}
            </el-tag>
          </div>
        </div>
        <el-empty v-else description="暂无拍照签到数据" :image-size="120" />
      </div>
    </template>

    <!-- 未选择会议 -->
    <el-empty v-if="!selectedMeetingId" description="请选择会议查看统计数据" :image-size="200" />

    <!-- 照片预览弹窗 -->
    <el-dialog v-model="photoPreviewVisible" title="签到照片预览" width="600px" @close="photoPreviewVisible = false">
      <div class="photo-preview-wrap" v-if="currentPhoto">
        <img :src="currentPhoto.photoData" class="photo-full" />
        <div class="photo-preview-meta">
          <p><strong>签到人：</strong>{{ currentPhoto.user?.realName || '未知' }}</p>
          <p><strong>签到时间：</strong>{{ formatTime(currentPhoto.signTime) }}</p>
          <p><strong>签到状态：</strong>{{ currentPhoto.signStatus === 1 ? '正常' : '迟到' }}</p>
        </div>
      </div>
    </el-dialog>

    <!-- 签到详情弹窗 -->
    <el-dialog v-model="detailVisible" title="签到详情" width="520px" @close="detailVisible = false">
      <div class="detail-dialog" v-if="currentDetail">
        <div class="detail-section">
          <div class="detail-avatar">
            {{ currentDetail.user?.realName?.charAt(0) || '?' }}
          </div>
          <div class="detail-info">
            <div class="detail-name">{{ currentDetail.user?.realName || '未知用户' }}</div>
            <div class="detail-position">{{ currentDetail.user?.position || '-' }}</div>
          </div>
          <el-tag :type="getAttendeeStatusType(currentDetail.status)" effect="dark" size="large">
            {{ getAttendeeStatusText(currentDetail.status) }}
          </el-tag>
        </div>
        <div class="detail-grid">
          <div class="detail-field">
            <span class="field-label">签到方式</span>
            <el-tag size="small" effect="plain">{{ getMethodText(currentDetail.signMethod) }}</el-tag>
          </div>
          <div class="detail-field">
            <span class="field-label">签到时间</span>
            <span class="field-value">{{ formatTime(currentDetail.signTime) }}</span>
          </div>
          <div class="detail-field" v-if="currentDetail.location">
            <span class="field-label">签到位置</span>
            <span class="field-value">{{ currentDetail.location }}</span>
          </div>
          <div class="detail-field" v-if="currentDetail.user?.email">
            <span class="field-label">邮箱</span>
            <span class="field-value">{{ currentDetail.user.email }}</span>
          </div>
          <div class="detail-field" v-if="currentDetail.user?.phone">
            <span class="field-label">手机号</span>
            <span class="field-value">{{ currentDetail.user.phone }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  Calendar, User, CircleCheck, CircleClose, Clock, DocumentAdd, Switch,
  DataLine, TrendCharts, PieChart, Histogram, List,
  Download, DocumentCopy, InfoFilled, Warning, Location, Camera,
  ChatLineRound, Tickets, Aim, View
} from '@element-plus/icons-vue'
import { getMeetingList, getMeetingAttendees } from '@/api/meeting'
import { getMeetingStatistics, getStatusDistribution, getMeetingStats, exportMeetingReport, getMeetingCheckInLocations, getMeetingCheckInPhotos } from '@/api/statistics'
import dayjs from 'dayjs'

const meetingOptions = ref([])
const selectedMeetingId = ref(null)
const loading = ref(false)
const exporting = ref(false)
const attendeeList = ref([])
const methodDistribution = ref({})
const locationList = ref([])
const photoList = ref([])
const photoPreviewVisible = ref(false)
const currentPhoto = ref(null)
const mapAvailable = ref(true)
let mapInstance = null
let markers = []

const circumference = 2 * Math.PI * 42
const ringCircumference = 2 * Math.PI * 68

const selectedMeeting = computed(() =>
  meetingOptions.value.find(m => m.id === selectedMeetingId.value) || null
)
const hasLocationSign = computed(() =>
  selectedMeeting.value?.signMethods?.includes('location') || false
)
const hasPhotoSign = computed(() =>
  selectedMeeting.value?.signMethods?.includes('photo') || false
)

const ringOffset = computed(() => {
  const rate = currentStats.attendanceRate || 0
  return circumference - (rate / 100) * circumference
})

const rateRingOffset = computed(() => {
  const rate = currentStats.attendanceRate || 0
  return ringCircumference - (rate / 100) * ringCircumference
})

const currentStats = reactive({
  totalCount: 0,
  signedCount: 0,
  lateCount: 0,
  makeupCount: 0,
  proxyCount: 0,
  notSignedCount: 0,
  attendanceRate: 0
})

const getRate = (count, total) => {
  if (!total || total === 0) return 0
  return Math.round((count / total) * 100)
}

const methodIconMap = {
  qrcode: Tickets,
  makeup: DocumentAdd,
  proxy: Switch,
  photo: Camera,
  gesture: Aim,
  location: Location
}

const methodStats = computed(() => {
  const md = methodDistribution.value
  const total = Object.values(md).reduce((s, v) => s + Number(v || 0), 0)
  const allMethods = [
    { key: 'qrcode', name: '二维码签到', color: '#2563EB', count: md.qrcode || 0 },
    { key: 'makeup', name: '补签', color: '#909399', count: md.makeup || 0 },
    { key: 'proxy', name: '代签', color: '#8B5CF6', count: md.proxy || 0 },
    { key: 'photo', name: '拍照签到', color: '#E6A23C', count: md.photo || 0 },
    { key: 'gesture', name: '手势签到', color: '#10B981', count: md.gesture || 0 },
    { key: 'location', name: '定位签到', color: '#F56C6C', count: md.location || 0 }
  ]
  return allMethods.filter(m => m.count > 0).map(m => ({
    ...m,
    percent: total > 0 ? Math.round((m.count / total) * 100) : 0
  }))
})

let pieChart = null
let barChart = null
const pieChartRef = ref(null)
const barChartRef = ref(null)
const mapContainerRef = ref(null)

const loadMeetings = async () => {
  try {
    const res = await getMeetingList({ current: 1, size: 100 })
    meetingOptions.value = res.data.records || []
    if (meetingOptions.value.length > 0) {
      selectedMeetingId.value = meetingOptions.value[0].id
      loadStatistics()
    }
  } catch (error) {
    console.error('加载会议列表失败:', error)
  }
}

const loadStatistics = async () => {
  if (!selectedMeetingId.value) return

  loading.value = true
  try {
    const promises = [
      getMeetingStatistics(selectedMeetingId.value),
      getStatusDistribution(selectedMeetingId.value),
      getMeetingStats(selectedMeetingId.value),
      getMeetingAttendees(selectedMeetingId.value)
    ]
    if (hasLocationSign.value) {
      promises.push(getMeetingCheckInLocations(selectedMeetingId.value))
    }
    if (hasPhotoSign.value) {
      promises.push(getMeetingCheckInPhotos(selectedMeetingId.value))
    }
    const results = await Promise.all(promises)
    const [statsRes, distRes, statsDetailRes, attendeesRes] = results
    let locIdx = 4
    let photoIdx = 4 + (hasLocationSign.value ? 1 : 0)

    Object.assign(currentStats, statsRes.data)
    if (statsDetailRes.data) {
      currentStats.makeupCount = statsDetailRes.data.makeupCount || 0
      currentStats.proxyCount = statsDetailRes.data.proxyCount || 0
    }
    attendeeList.value = attendeesRes.data || []
    methodDistribution.value = statsDetailRes.data?.methodDistribution || {}

    if (hasLocationSign.value) {
      locationList.value = results[locIdx]?.data || []
    } else {
      locationList.value = []
    }
    if (hasPhotoSign.value) {
      photoList.value = results[photoIdx]?.data || []
    } else {
      photoList.value = []
    }

    nextTick(() => {
      initCharts(distRes.data)
      if (hasLocationSign.value) {
        nextTick(() => initMap())
      }
    })
  } catch (error) {
    console.error('加载统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

const previewPhoto = (photo) => {
  currentPhoto.value = photo
  photoPreviewVisible.value = true
}

const initMap = () => {
  const container = mapContainerRef.value
  if (!container) return

  if (typeof AMap === 'undefined') {
    mapAvailable.value = false
    return
  }
  mapAvailable.value = true

  if (markers.length) {
    markers.forEach(m => mapInstance?.remove(m))
    markers = []
  }

  if (!locationList.value.length) {
    if (!mapInstance) {
      mapInstance = new AMap.Map(container, {
        zoom: 13,
        center: [116.397428, 39.90923],
        viewMode: '2D'
      })
      mapInstance.addControl(new AMap.Scale())
      mapInstance.addControl(new AMap.ToolBar())
    }
    return
  }

  const points = locationList.value.map(l => [Number(l.longitude), Number(l.latitude)])
  const centerLng = points.reduce((s, p) => s + p[0], 0) / points.length
  const centerLat = points.reduce((s, p) => s + p[1], 0) / points.length

  if (!mapInstance) {
    mapInstance = new AMap.Map(container, {
      zoom: 14,
      center: [centerLng, centerLat],
      viewMode: '2D'
    })
    mapInstance.addControl(new AMap.Scale())
    mapInstance.addControl(new AMap.ToolBar())
  } else {
    mapInstance.setCenter([centerLng, centerLat])
  }

  locationList.value.forEach((loc) => {
    const marker = new AMap.Marker({
      position: [Number(loc.longitude), Number(loc.latitude)],
      title: loc.user?.realName || '未知',
      label: {
        content: `<div style="padding:2px 8px;background:#2563EB;color:#fff;border-radius:12px;font-size:12px;box-shadow:0 2px 8px rgba(37,99,235,.18);">${loc.user?.realName || '?'}</div>`,
        direction: 'top'
      }
    })
    const info = new AMap.InfoWindow({
      content: `<div style="padding:8px;min-width:180px;">
        <div style="font-weight:bold;margin-bottom:4px;color:#303133;">${loc.user?.realName || '未知'}</div>
        <div style="color:#606266;font-size:12px;">📍 ${loc.location || '未提供地址'}</div>
        <div style="color:#909399;font-size:12px;margin-top:4px;">🕐 ${formatTime(loc.signTime)}</div>
        <div style="margin-top:4px;font-size:12px;">状态：${loc.signStatus === 1 ? '✅ 正常' : '⚠️ 迟到'}</div>
      </div>`,
      offset: new AMap.Pixel(0, -30)
    })
    marker.on('click', () => {
      info.open(mapInstance, marker.getPosition())
    })
    markers.push(marker)
    mapInstance.add(marker)
  })

  if (points.length > 1) {
    mapInstance.setFitView(markers, false, [60, 60, 60, 60])
  }
}

const focusLocation = (loc) => {
  if (!mapInstance || !loc.longitude || !loc.latitude) return
  mapInstance.setZoomAndCenter(16, [Number(loc.longitude), Number(loc.latitude)])
}

const initCharts = (distributionData) => {
  if (pieChartRef.value) {
    if (pieChart) pieChart.dispose()
    pieChart = echarts.init(pieChartRef.value)
    const total = (distributionData?.distribution?.signed || 0) +
      (distributionData?.distribution?.late || 0) +
      (distributionData?.distribution?.notSigned || 0) +
      (distributionData?.distribution?.absent || 0)
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
      legend: {
        orient: 'horizontal',
        bottom: 0,
        itemWidth: 10,
        itemHeight: 10,
        textStyle: { fontSize: 12, color: '#606266' }
      },
      series: [{
        type: 'pie',
        radius: ['45%', '72%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
        label: {
          show: true,
          position: 'center',
          formatter: () => `{total|${total}}\n{label|总人数}`,
          rich: {
            total: { fontSize: 28, fontWeight: 'bold', color: '#303133', lineHeight: 36 },
            label: { fontSize: 12, color: '#909399', lineHeight: 20 }
          }
        },
        emphasis: {
          label: { show: false },
          scale: true,
          scaleSize: 8
        },
        labelLine: { show: false },
        data: [
          { value: distributionData?.distribution?.signed || 0, name: '已签到', itemStyle: { color: '#67C23A' } },
          { value: distributionData?.distribution?.late || 0, name: '迟到', itemStyle: { color: '#E6A23C' } },
          { value: distributionData?.distribution?.notSigned || 0, name: '未签到', itemStyle: { color: '#F56C6C' } },
          { value: distributionData?.distribution?.absent || 0, name: '缺勤', itemStyle: { color: '#909399' } }
        ]
      }]
    })
  }

  if (barChartRef.value) {
    if (barChart) barChart.dispose()
    barChart = echarts.init(barChartRef.value)
    const md = methodDistribution.value
    const barData = [
      { value: md.qrcode || 0, name: '二维码' },
      { value: md.makeup || 0, name: '补签' },
      { value: md.proxy || 0, name: '代签' },
      { value: md.photo || 0, name: '拍照' },
      { value: md.gesture || 0, name: '手势' },
      { value: md.location || 0, name: '定位' }
    ]
    barChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.95)',
        borderColor: '#EBEEF5',
        borderWidth: 1,
        textStyle: { color: '#303133' },
        formatter: (params) => {
          const p = params[0]
          return `<div style="font-weight:600;margin-bottom:4px;">${p.name}</div>
                  <div style="color:#606266;">签到人数：<strong style="color:${p.color}">${p.value}</strong> 人</div>`
        }
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: barData.map(d => d.name),
        axisLine: { lineStyle: { color: '#EBEEF5' } },
        axisTick: { show: false },
        axisLabel: { color: '#606266', fontSize: 12 }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#F2F6FC', type: 'dashed' } },
        axisLabel: { color: '#909399', fontSize: 12 }
      },
      series: [{
        type: 'bar',
        data: barData.map(d => d.value),
        barWidth: '36%',
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: (params) => {
            const colors = [
              ['#2563EB', '#60A5FA'],
              ['#909399', '#B1B3B8'],
              ['#8B5CF6', '#A78BFA'],
              ['#E6A23C', '#F0B860'],
              ['#10B981', '#34D399'],
              ['#F56C6C', '#F87171']
            ]
            const c = colors[params.dataIndex] || colors[0]
            return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: c[0] },
              { offset: 1, color: c[1] }
            ])
          }
        }
      }]
    })
  }
}

const exportCSV = async () => {
  if (!selectedMeetingId.value) return
  exporting.value = true
  try {
    const res = await exportMeetingReport(selectedMeetingId.value, 'csv')
    const blob = new Blob(['\ufeff' + res], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    const meeting = meetingOptions.value.find(m => m.id === selectedMeetingId.value)
    link.download = `签到统计_${meeting?.title || selectedMeetingId.value}.csv`
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('CSV报表导出成功')
  } catch (error) {
    console.error('导出CSV失败:', error)
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const exportPDF = () => {
  ElMessage.info('PDF导出功能开发中，请使用CSV导出')
}

const formatTime = (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
const formatMeetingTime = (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'

const detailVisible = ref(false)
const currentDetail = ref(null)

const viewDetail = (row) => {
  currentDetail.value = row
  detailVisible.value = true
}

const getMethodText = (method) => {
  const map = { qrcode: '二维码', makeup: '补签', proxy: '代签', photo: '拍照', gesture: '手势', location: '定位' }
  return map[method] || '-'
}

const getAttendeeStatusType = (status) => {
  const types = ['info', 'success', 'warning', 'danger']
  return types[status] || 'info'
}

const getAttendeeStatusText = (status) => {
  const texts = ['未签到', '已签到', '迟到', '缺勤']
  return texts[status] || '未知'
}

const handleResize = () => {
  pieChart?.resize()
  barChart?.resize()
  mapInstance?.setSize()
}

onMounted(() => {
  loadMeetings()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  barChart?.dispose()
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<style lang="scss" scoped>
.statistics-page {
  padding: $spacing-xl;
  max-width: 1400px;
  margin: 0 auto;
}

// 页面头部
.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
  background: $bg-white;
  border: 1px solid $border-light;
  border-radius: $radius-base;
  padding: $spacing-xl;
  color: $text-primary;
  position: relative;
  overflow: hidden;
  box-shadow: $shadow-base;

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

  .header-decorate {
    display: flex;
    align-items: center;
    gap: 16px;
    z-index: 2;

    .stat-hero {
      margin-right: 16px;
    }

    .hero-ring {
      position: relative;
      width: 72px;
      height: 72px;

      .ring-svg {
        width: 100%;
        height: 100%;
        transform: rotate(-90deg);
      }

      .ring-bg {
        fill: none;
        stroke: $border-light;
        stroke-width: 6;
      }

      .ring-fg {
        fill: none;
        stroke: $color-primary;
        stroke-width: 6;
        stroke-linecap: round;
        transition: stroke-dashoffset 0.6s ease;
      }

      .ring-text {
        position: absolute;
        inset: 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: $text-primary;

        .ring-value {
          font-size: 16px;
          font-weight: 700;
          line-height: 1;
        }

        .ring-label {
          font-size: 10px;
          opacity: 0.8;
          margin-top: 2px;
        }
      }
    }

    .dec-icon {
      display: none;
    }

    .dec-1 { animation: float 3s ease-in-out infinite; }
    .dec-2 { animation: float 3s ease-in-out infinite 0.5s; }
    .dec-3 { animation: float 3s ease-in-out infinite 1s; }
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

// 通用卡片
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

// 过滤器卡片
.filter-card {
  .filter-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: $spacing-md;

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

  .meeting-quick-info {
    display: flex;
    gap: 8px;
    margin-top: $spacing-md;
    flex-wrap: wrap;

    .info-chip {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 4px 10px;
      background: $bg-base;
      border-radius: $radius-round;
      font-size: 12px;
      color: $text-regular;

      .el-icon { font-size: 13px; color: $color-primary; }

      &.method-chip {
        background: $color-primary-bg;
        color: $color-primary;
        font-weight: 500;
      }
    }
  }
}

// 统计卡片
.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-lg;
}

.stat-card {
  background: $bg-white;
  border-radius: $radius-base;
  border: 1px solid $border-light;
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  box-shadow: $shadow-base;
  transition: $transition-base;
  position: relative;
  overflow: hidden;

  .stat-card-bg {
    position: absolute;
    top: -50%;
    right: -20%;
    width: 120px;
    height: 120px;
    display: none;
  }

  &.primary .stat-card-bg { background: $color-primary; }
  &.success .stat-card-bg { background: $color-success; }
  &.warning .stat-card-bg { background: $color-warning; }
  &.info .stat-card-bg { background: $color-info; }
  &.proxy .stat-card-bg { background: #8B5CF6; }
  &.danger .stat-card-bg { background: $color-danger; }

  &:hover {
    box-shadow: $shadow-hover;
    transform: translateY(-1px);
  }

  .stat-icon-wrap {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    position: relative;
    z-index: 1;
  }

  .stat-icon {
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

  &.primary .stat-icon { background: $color-primary-bg; color: $color-primary; }
  &.success .stat-icon { background: $color-success-bg; color: $color-success; }
  &.warning .stat-icon { background: $color-warning-bg; color: $color-warning; }
  &.info .stat-icon { background: $color-info-bg; color: $color-info; }
  &.proxy .stat-icon { background: #F5F3FF; color: #7C3AED; }
  &.danger .stat-icon { background: $color-danger-bg; color: $color-danger; }

  .stat-badge {
    font-size: 11px;
    font-weight: 600;
    padding: 2px 8px;
    border-radius: $radius-round;
    background: rgba(103, 194, 58, 0.12);
    color: $color-success;

    &.danger-badge {
      background: rgba(245, 108, 108, 0.12);
      color: $color-danger;
    }
  }

  .stat-body {
    position: relative;
    z-index: 1;

    .stat-value-row {
      display: flex;
      align-items: baseline;
      gap: 4px;
    }

    .stat-value {
      font-size: 32px;
      font-weight: 700;
      color: $text-primary;
      line-height: 1.1;
    }

    .stat-unit {
      font-size: 13px;
      color: $text-secondary;
    }

    .stat-label {
      font-size: 13px;
      color: $text-secondary;
      margin-top: 2px;
    }
  }

  .stat-foot {
    position: relative;
    z-index: 1;
    padding-top: $spacing-sm;
    border-top: 1px dashed $border-lighter;

    .foot-item {
      font-size: 12px;
      color: $text-placeholder;
    }
  }
}

// 出勤率卡片
.rate-card {
  .rate-main {
    display: flex;
    gap: $spacing-xl;
    align-items: center;
  }

  .rate-ring-wrap {
    position: relative;
    width: 160px;
    height: 160px;
    flex-shrink: 0;
  }

  .rate-ring-svg {
    width: 100%;
    height: 100%;
    transform: rotate(-90deg);
  }

  .rate-ring-bg {
    fill: none;
    stroke: $bg-base;
    stroke-width: 10;
  }

  .rate-ring-fg {
    fill: none;
    stroke-width: 10;
    transition: stroke-dashoffset 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .rate-ring-center {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    .rate-ring-value {
      font-size: 32px;
      font-weight: 700;
      color: $text-primary;
      line-height: 1;
    }

    .rate-ring-label {
      font-size: 13px;
      color: $text-secondary;
      margin-top: 4px;
    }
  }

  .rate-details {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
  }

  .detail-item {
    .detail-bar {
      height: 8px;
      background: $bg-base;
      border-radius: $radius-round;
      overflow: hidden;
      margin-bottom: 6px;

      .bar-fill {
        height: 100%;
        border-radius: $radius-round;
        transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);

        &.bar-success { background: $color-success; }
        &.bar-warning { background: $color-warning; }
        &.bar-danger { background: $color-danger; }
        &.bar-info { background: $color-info; }
      }
    }

    .detail-meta {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: $text-regular;

      strong {
        margin-left: auto;
        font-weight: 600;
        color: $text-primary;
      }

      .dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;

        &.dot-success { background: $color-success; }
        &.dot-warning { background: $color-warning; }
        &.dot-danger { background: $color-danger; }
        &.dot-info { background: $color-info; }
      }
    }
  }
}

// 图表卡片
.chart-card {
  margin-bottom: 0;

  .chart-box {
    height: 280px;
  }
}

// 签到方式卡片
.method-card {
  .method-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .method-item {
    display: flex;
    align-items: center;
    gap: 10px;

    .method-icon {
      width: 36px;
      height: 36px;
      border-radius: $radius-base;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 16px;
      flex-shrink: 0;
    }

    .method-info {
      flex: 1;
      min-width: 0;

      .method-name {
        font-size: 13px;
        font-weight: 500;
        color: $text-primary;
        margin-bottom: 4px;
      }

      .method-bar {
        height: 6px;
        background: $bg-base;
        border-radius: $radius-round;
        overflow: hidden;

        .method-bar-fill {
          height: 100%;
          border-radius: $radius-round;
          transition: width 0.6s ease;
        }
      }
    }

    .method-count {
      font-size: 18px;
      font-weight: 700;
      color: $text-primary;
      min-width: 32px;
      text-align: right;
    }
  }

  .method-empty {
    padding: $spacing-lg;
    text-align: center;
    color: $text-secondary;
    font-size: 13px;
  }
}

// 导出卡片
.export-card {
  background: #F8FAFC;
  border: 1px solid $border-light;

  .export-header {
    text-align: center;
    margin-bottom: $spacing-md;

    .export-icon-lg {
      font-size: 36px;
      color: $color-primary;
      margin-bottom: $spacing-sm;
    }

    .export-title {
      font-size: 16px;
      font-weight: 600;
      color: $text-primary;
      margin-bottom: 4px;
    }

    .export-desc {
      font-size: 13px;
      color: $text-secondary;
    }
  }

  .export-buttons {
    display: flex;
    flex-direction: column;
    gap: $spacing-sm;

    .el-button {
      width: 100%;
    }
  }
}

// 统计说明
.guide-card {
  .stat-desc {
    margin: 0;
    padding: 0;
    list-style: none;

    li {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      margin: 10px 0;
      font-size: 13px;
      color: $text-regular;

      .desc-icon {
        font-size: 16px;
        margin-top: 2px;
        flex-shrink: 0;

        &.success { color: $color-success; }
        &.warning { color: $color-warning; }
        &.danger { color: $color-danger; }
        &.info { color: $color-info; }
      }

      div { flex: 1; }
    }
  }
}

// 明细卡片
.detail-card {
  .detail-summary {
    display: flex;
    gap: 8px;
  }

  :deep(.el-table) {
    --el-table-header-bg-color: #FAFBFC;
    --el-table-row-hover-bg-color: #F5F8FF;
    border-radius: $radius-base;
    overflow: hidden;

    th.el-table__cell {
      font-weight: 600;
      font-size: 13px;
    }

    td.el-table__cell {
      font-size: 13px;
    }
  }
}

// 地图卡片
.map-card {
  .map-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;

    .ch-left {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 1;

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

  .map-unavailable {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: $spacing-xl;
    color: $text-secondary;

    .unavail-icon {
      font-size: 48px;
      color: $color-warning;
      margin-bottom: $spacing-sm;
    }

    p { margin: 0; font-size: 13px; }
  }

  .map-container {
    width: 100%;
    height: 400px;
    border-radius: $radius-base;
    overflow: hidden;
    border: 1px solid $border-light;
    margin-bottom: $spacing-md;
  }

  .location-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 12px;
    max-height: 280px;
    overflow-y: auto;
    padding: 4px;
  }

  .location-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    background: $bg-base;
    border-radius: $radius-base;
    cursor: pointer;
    transition: $transition-fast;
    border: 1px solid transparent;

    &:hover {
      background: $color-primary-bg;
      border-color: $color-primary;
      transform: translateX(4px);
    }

    .loc-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: $color-primary;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: bold;
      flex-shrink: 0;
    }

    .loc-info {
      flex: 1;
      min-width: 0;
    }

    .loc-name {
      font-size: 14px;
      font-weight: 600;
      color: $text-primary;
      margin-bottom: 2px;
    }

    .loc-address {
      font-size: 12px;
      color: $text-regular;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .loc-time {
      font-size: 11px;
      color: $text-secondary;
      margin-top: 2px;
    }
  }
}

// 拍照卡片
.photo-card {
  .photo-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: $spacing-md;
    padding: 4px;
  }

  .photo-card-item {
    background: $bg-white;
    border: 1px solid $border-light;
    border-radius: $radius-base;
    overflow: hidden;
    cursor: pointer;
    transition: $transition-base;
    position: relative;

    &:hover {
      transform: translateY(-4px);
      box-shadow: $shadow-hover;
      border-color: $color-primary;

      .photo-overlay {
        transform: translateY(0);
      }
    }

    .photo-thumb {
      width: 100%;
      height: 180px;
      object-fit: cover;
      display: block;
      background: $bg-base;
    }

    .photo-overlay {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      padding: 10px 12px;
      background: linear-gradient(transparent, rgba(0,0,0,0.7));
      color: #fff;
      transform: translateY(100%);
      transition: transform 0.3s ease;

      .photo-name {
        font-size: 14px;
        font-weight: 600;
        margin-bottom: 2px;
      }

      .photo-time {
        font-size: 12px;
        opacity: 0.85;
      }
    }

    .photo-status {
      position: absolute;
      top: 8px;
      right: 8px;
    }
  }
}

.photo-preview-wrap {
  text-align: center;

  .photo-full {
    max-width: 100%;
    max-height: 400px;
    border-radius: $radius-base;
    margin-bottom: $spacing-md;
  }

  .photo-preview-meta {
    text-align: left;
    padding: $spacing-md;
    background: $bg-base;
    border-radius: $radius-base;

    p {
      margin: 6px 0;
      font-size: 14px;
      color: $text-regular;
    }
  }
}

// 详情对话框
.detail-dialog {
  .detail-section {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px;
    background: #F8FAFC;
    border: 1px solid $border-light;
    border-radius: $radius-base;
    margin-bottom: $spacing-md;

    .detail-avatar {
      width: 52px;
      height: 52px;
      border-radius: 50%;
      background: $color-primary;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      font-weight: 600;
      flex-shrink: 0;
    }

    .detail-info {
      flex: 1;
    }

    .detail-name {
      font-size: 16px;
      font-weight: 600;
      color: $text-primary;
    }

    .detail-position {
      font-size: 13px;
      color: $text-secondary;
      margin-top: 2px;
    }
  }

  .detail-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    padding: 4px;

    .detail-field {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .field-label {
        font-size: 12px;
        color: $text-secondary;
      }

      .field-value {
        font-size: 14px;
        color: $text-primary;
        font-weight: 500;
      }
    }
  }
}

// 响应式
@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .rate-card .rate-main {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }

  .stat-header {
    flex-direction: column;
    gap: $spacing-md;
    align-items: flex-start;

    .header-decorate {
      align-self: flex-end;
    }
  }
}
</style>

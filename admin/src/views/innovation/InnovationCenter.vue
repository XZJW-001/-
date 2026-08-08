<template>
  <div class="page-container innovation-page">
    <header class="page-header operations-header">
      <div>
        <h2>智慧运营</h2>
        <p>{{ currentMeeting?.title || '选择会议后查看实时运营状态' }}</p>
      </div>
      <div class="header-actions">
        <el-select v-model="meetingId" filterable placeholder="选择会议" class="meeting-select" @change="changeMeeting">
          <el-option
            v-for="meeting in meetings"
            :key="meeting.id"
            :label="meeting.title"
            :value="meeting.id"
          />
        </el-select>
        <el-tooltip content="刷新数据" placement="bottom">
          <el-button :icon="Refresh" circle :loading="loading" @click="refreshAll" />
        </el-tooltip>
      </div>
    </header>

    <div v-if="!meetingId" class="empty-state">
      <el-icon :size="42"><DataAnalysis /></el-icon>
      <strong>请选择一场会议</strong>
      <span>会议的实时数据、规则和智能服务将在这里统一管理</span>
    </div>

    <template v-else>
      <section class="signal-strip">
        <div class="signal-item primary">
          <span>实时签到率</span>
          <strong>{{ live.attendanceRate || 0 }}%</strong>
          <small>{{ live.signed || 0 }} / {{ live.total || 0 }} 人</small>
        </div>
        <div class="signal-item success">
          <span>正常到场</span>
          <strong>{{ live.signed || 0 }}</strong>
          <small>含迟到 {{ live.late || 0 }} 人</small>
        </div>
        <div class="signal-item warning">
          <span>待签到</span>
          <strong>{{ live.pending || 0 }}</strong>
          <small>缺席 {{ live.absent || 0 }} 人</small>
        </div>
        <div class="signal-item danger">
          <span>高风险记录</span>
          <strong>{{ live.highRiskCount || 0 }}</strong>
          <small>访客 {{ live.guestCount || 0 }} 人</small>
        </div>
      </section>

      <el-tabs v-model="activeTab" class="operations-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="实时大屏" name="live">
          <section ref="livePanel" class="live-workspace">
            <div class="section-bar">
              <div>
                <h3>{{ currentMeeting?.title }}</h3>
                <p>{{ formatTime(currentMeeting?.startTime) }} · {{ currentMeeting?.location }}</p>
              </div>
              <div class="section-actions">
                <span class="live-indicator"><i></i>实时连接</span>
                <el-tooltip content="全屏显示" placement="bottom">
                  <el-button :icon="FullScreen" circle @click="openFullscreen" />
                </el-tooltip>
              </div>
            </div>

            <div class="live-grid">
              <div class="attendance-gauge">
                <el-progress
                  type="dashboard"
                  :percentage="Number(live.attendanceRate || 0)"
                  :width="220"
                  :stroke-width="16"
                  color="#1d4ed8"
                >
                  <template #default="{ percentage }">
                    <strong>{{ percentage }}%</strong>
                    <span>当前签到率</span>
                  </template>
                </el-progress>
                <div class="gauge-counts">
                  <span><i class="dot signed"></i>已签到 {{ live.signed || 0 }}</span>
                  <span><i class="dot pending"></i>待签到 {{ live.pending || 0 }}</span>
                  <span><i class="dot guest"></i>访客 {{ live.guestCount || 0 }}</span>
                </div>
              </div>

              <div class="latest-board">
                <div class="board-title">最新签到</div>
                <div v-if="live.latest?.length" class="arrival-list">
                  <div v-for="item in live.latest" :key="item.id" class="arrival-row">
                    <el-avatar :size="36" :src="item.avatar">{{ item.realName?.[0] || 'U' }}</el-avatar>
                    <div class="arrival-copy">
                      <strong>{{ item.realName || '未知用户' }}</strong>
                      <span>{{ methodName(item.signMethod) }} · {{ timeOnly(item.signTime) }}</span>
                    </div>
                    <el-tag v-if="item.riskLevel !== 'LOW'" :type="item.riskLevel === 'HIGH' ? 'danger' : 'warning'" size="small">
                      {{ riskName(item.riskLevel) }}
                    </el-tag>
                    <el-tag v-else type="success" size="small">已到场</el-tag>
                  </div>
                </div>
                <el-empty v-else description="暂无签到记录" :image-size="76" />
              </div>

              <div class="dynamic-board">
                <div class="board-title">动态签到码</div>
                <template v-if="config.dynamicQrEnabled && dynamicQr.qrcodeImage">
                  <img :src="dynamicQr.qrcodeImage" class="qr-image" alt="动态签到二维码" />
                  <div class="qr-status">
                    <span>每 {{ config.qrRefreshSeconds }} 秒自动刷新</span>
                    <el-progress :percentage="qrProgress" :show-text="false" :stroke-width="4" />
                  </div>
                </template>
                <div v-else class="qr-disabled">
                  <el-icon :size="38"><Lock /></el-icon>
                  <strong>动态签到码未开启</strong>
                  <el-button link type="primary" @click="activeTab = 'rules'">前往规则配置</el-button>
                </div>
              </div>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="安全规则" name="rules">
          <section class="tool-surface rules-surface">
            <div class="section-bar">
              <div><h3>智能签到规则</h3><p>配置当前会议的验证条件与弱网策略</p></div>
              <el-button type="primary" :icon="Check" :loading="savingConfig" @click="saveConfig">保存规则</el-button>
            </div>
            <el-form label-position="top" class="rules-form">
              <div class="rule-row">
                <div class="rule-copy"><strong>动态二维码</strong><span>二维码按时间片轮换并校验服务器签名</span></div>
                <el-switch v-model="config.dynamicQrEnabled" />
              </div>
              <el-form-item v-if="config.dynamicQrEnabled" label="刷新周期">
                <el-slider v-model="config.qrRefreshSeconds" :min="10" :max="120" :step="5" show-input />
              </el-form-item>

              <div class="rule-row">
                <div class="rule-copy"><strong>现场定位</strong><span>签到时同时校验与会场坐标的距离</span></div>
                <el-switch v-model="config.requireLocation" />
              </div>
              <div v-if="config.requireLocation" class="coordinate-grid">
                <el-form-item label="会场纬度"><el-input-number v-model="config.venueLatitude" :precision="7" :step="0.0001" controls-position="right" /></el-form-item>
                <el-form-item label="会场经度"><el-input-number v-model="config.venueLongitude" :precision="7" :step="0.0001" controls-position="right" /></el-form-item>
                <el-form-item label="允许半径（米）"><el-input-number v-model="config.radiusMeters" :min="50" :max="5000" :step="50" controls-position="right" /></el-form-item>
              </div>

              <div class="rule-row">
                <div class="rule-copy"><strong>现场照片</strong><span>无论主签到方式为何，都要求同时提交现场照片</span></div>
                <el-switch v-model="config.requirePhoto" />
              </div>
              <div class="rule-row">
                <div class="rule-copy"><strong>弱网签到</strong><span>使用在线预签许可记录现场签到，联网后幂等同步</span></div>
                <el-switch v-model="config.offlineAllowed" />
              </div>
              <el-form-item v-if="config.offlineAllowed" label="最长同步时限（分钟）">
                <el-input-number v-model="config.offlineMaxMinutes" :min="5" :max="1440" :step="5" controls-position="right" />
              </el-form-item>
              <div class="rule-row">
                <div class="rule-copy"><strong>智能提醒</strong><span>会前及签到开放后自动提醒未签到人员</span></div>
                <el-switch v-model="config.reminderEnabled" />
              </div>
              <el-form-item v-if="config.reminderEnabled" label="会前提醒时间（分钟）">
                <el-input-number v-model="config.reminderMinutes" :min="5" :max="1440" :step="5" controls-position="right" />
              </el-form-item>
            </el-form>
          </section>
        </el-tab-pane>

        <el-tab-pane name="risks">
          <template #label>风险中心 <el-badge v-if="unreviewedRisks" :value="unreviewedRisks" class="tab-badge" /></template>
          <section class="tool-surface">
            <div class="section-bar">
              <div><h3>签到风险</h3><p>按设备、网络、位置和离线延迟综合评分</p></div>
              <el-radio-group v-model="riskFilter" size="small" @change="loadRisks">
                <el-radio-button label="">全部</el-radio-button>
                <el-radio-button label="HIGH">高风险</el-radio-button>
                <el-radio-button label="MEDIUM">中风险</el-radio-button>
              </el-radio-group>
            </div>
            <el-table :data="risks" stripe>
              <el-table-column label="人员" min-width="140"><template #default="{ row }"><strong>{{ row.realName || row.username }}</strong></template></el-table-column>
              <el-table-column label="分值" width="90"><template #default="{ row }"><span class="risk-score">{{ row.riskScore }}</span></template></el-table-column>
              <el-table-column label="等级" width="100"><template #default="{ row }"><el-tag :type="riskTag(row.riskLevel)">{{ riskName(row.riskLevel) }}</el-tag></template></el-table-column>
              <el-table-column label="风险依据" min-width="260"><template #default="{ row }">{{ row.reasons?.join('；') || '未发现明显异常' }}</template></el-table-column>
              <el-table-column label="签到时间" width="170"><template #default="{ row }">{{ formatTime(row.signTime) }}</template></el-table-column>
              <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.reviewStatus ? 'success' : 'info'">{{ row.reviewStatus ? '已复核' : '待复核' }}</el-tag></template></el-table-column>
              <el-table-column label="操作" width="90" fixed="right"><template #default="{ row }"><el-button v-if="!row.reviewStatus" link type="primary" @click="review(row)">复核</el-button><span v-else>完成</span></template></el-table-column>
            </el-table>
          </section>
        </el-tab-pane>

        <el-tab-pane label="访客与提醒" name="guests">
          <div class="split-layout">
            <section class="tool-surface guest-tool">
              <div class="section-bar">
                <div><h3>访客免注册签到</h3><p>生成临时邀请并查看现场访客</p></div>
                <el-button type="primary" :icon="Tickets" @click="createInvite">生成邀请</el-button>
              </div>
              <div v-if="guestInvite.qrcodeImage" class="guest-invite">
                <img :src="guestInvite.qrcodeImage" alt="访客邀请二维码" />
                <div><strong>访客邀请已生效</strong><span>有效期至 {{ formatTime(guestInvite.expireTime) }}</span></div>
              </div>
              <el-table :data="guests" stripe>
                <el-table-column prop="guestName" label="访客" min-width="100" />
                <el-table-column prop="organization" label="单位" min-width="130" />
                <el-table-column prop="phone" label="联系电话" min-width="130" />
                <el-table-column label="签到时间" min-width="165"><template #default="{ row }">{{ formatTime(row.signTime) }}</template></el-table-column>
              </el-table>
            </section>

            <section class="tool-surface reminder-tool">
              <div class="section-bar"><div><h3>一键催签</h3><p>提醒当前仍未完成签到的参会者</p></div></div>
              <div class="reminder-summary">
                <el-icon :size="34"><Bell /></el-icon>
                <strong>{{ live.pending || 0 }} 人待签到</strong>
                <span>提醒将进入参会者的小程序消息中心</span>
              </div>
              <el-button type="primary" plain :icon="Promotion" :loading="sendingReminder" @click="sendReminder">发送签到提醒</el-button>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="预警分析" name="alerts">
          <section class="tool-surface">
            <div class="section-bar"><div><h3>数据异常预警</h3><p>基于历史出勤和未复核风险生成运营建议</p></div><el-tag type="danger">高风险待复核 {{ alerts.unreviewedHighRiskCount || 0 }}</el-tag></div>
            <div class="alert-columns">
              <div>
                <h4>会议预警</h4>
                <div v-if="alerts.meetingWarnings?.length" class="warning-list">
                  <div v-for="item in alerts.meetingWarnings" :key="item.meetingId" class="warning-row">
                    <el-tag :type="item.level === 'HIGH' ? 'danger' : 'warning'">{{ item.predictedAttendanceRate }}%</el-tag>
                    <div><strong>{{ item.title }}</strong><span>{{ item.suggestion }}</span></div>
                  </div>
                </div>
                <el-empty v-else description="近期会议状态稳定" :image-size="74" />
              </div>
              <div>
                <h4>人员预警</h4>
                <div v-if="alerts.userWarnings?.length" class="warning-list">
                  <div v-for="item in alerts.userWarnings" :key="item.userId" class="warning-row">
                    <el-tag :type="item.level === 'HIGH' ? 'danger' : 'warning'">缺席 {{ item.absenceCount }}</el-tag>
                    <div><strong>{{ item.realName }}</strong><span>{{ item.suggestion }}</span></div>
                  </div>
                </div>
                <el-empty v-else description="暂无高频缺席人员" :image-size="74" />
              </div>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="智能纪要" name="minutes">
          <div class="minutes-layout">
            <section class="tool-surface minutes-editor">
              <div class="section-bar"><div><h3>会议记录</h3><p>本地提取模式，不向外部服务发送会议内容</p></div><el-tag type="success">本地处理</el-tag></div>
              <el-input v-model="minutesSource" type="textarea" :rows="14" maxlength="10000" show-word-limit placeholder="粘贴会议记录、语音转写文本或决议内容" />
              <el-button type="primary" :icon="MagicStick" :loading="generatingMinutes" @click="createMinutes">生成摘要与待办</el-button>
            </section>
            <section class="tool-surface minutes-result">
              <div class="section-bar"><div><h3>最新纪要</h3><p>{{ latestMinutes ? formatTime(latestMinutes.createTime) : '尚未生成' }}</p></div></div>
              <template v-if="latestMinutes">
                <h4>会议摘要</h4>
                <p class="summary-text">{{ latestMinutes.summary }}</p>
                <h4>行动事项</h4>
                <div class="action-list">
                  <div v-for="(item, index) in latestMinutes.actionItems" :key="index" class="action-row">
                    <el-checkbox :model-value="item.status === 'DONE'" />
                    <div><strong>{{ item.task }}</strong><span>{{ item.owner }} · {{ item.dueDate }}</span></div>
                  </div>
                </div>
              </template>
              <el-empty v-else description="生成后将在此显示摘要与待办" :image-size="90" />
            </section>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, DataAnalysis, FullScreen, Lock, Refresh, Tickets, Bell, Promotion, MagicStick } from '@element-plus/icons-vue'
import { getMeetingList } from '@/api/meeting'
import {
  createGuestInvite, generateMinutes, getAlerts, getDynamicQrcode, getFeatureConfig,
  getGuests, getLiveSnapshot, getMinutes, getRisks, reviewRisk, saveFeatureConfig,
  sendMeetingReminders
} from '@/api/innovation'

const meetings = ref([])
const meetingId = ref(null)
const activeTab = ref('live')
const loading = ref(false)
const savingConfig = ref(false)
const sendingReminder = ref(false)
const generatingMinutes = ref(false)
const risks = ref([])
const riskFilter = ref('')
const guests = ref([])
const minutesList = ref([])
const minutesSource = ref('')
const livePanel = ref(null)
const qrProgress = ref(100)
const live = reactive({ latest: [] })
const dynamicQr = reactive({})
const guestInvite = reactive({})
const alerts = reactive({ meetingWarnings: [], userWarnings: [] })
const config = reactive({
  dynamicQrEnabled: false, qrRefreshSeconds: 20,
  requireLocation: false, requirePhoto: false,
  venueLatitude: null, venueLongitude: null, radiusMeters: 300,
  offlineAllowed: true, offlineMaxMinutes: 120,
  reminderEnabled: true, reminderMinutes: 30
})

let eventSource = null
let fallbackTimer = null
let qrTimer = null
let qrTick = null

const currentMeeting = computed(() => meetings.value.find(item => item.id === meetingId.value))
const latestMinutes = computed(() => minutesList.value[0] || null)
const unreviewedRisks = computed(() => risks.value.filter(item => !item.reviewStatus && item.riskLevel !== 'LOW').length)

const loadMeetings = async () => {
  const res = await getMeetingList({ current: 1, size: 200 })
  meetings.value = res.data?.records || []
  if (!meetingId.value && meetings.value.length) {
    meetingId.value = meetings.value[0].id
  }
}

const changeMeeting = async () => {
  stopRealtime()
  await refreshAll()
  startRealtime()
}

const refreshAll = async () => {
  if (!meetingId.value) return
  loading.value = true
  try {
    const [configRes, liveRes, riskRes, guestRes, alertRes, minutesRes] = await Promise.all([
      getFeatureConfig(meetingId.value), getLiveSnapshot(meetingId.value), getRisks(meetingId.value),
      getGuests(meetingId.value), getAlerts(), getMinutes(meetingId.value)
    ])
    Object.assign(config, configRes.data || {})
    Object.assign(live, liveRes.data || {})
    risks.value = riskRes.data || []
    guests.value = guestRes.data || []
    Object.assign(alerts, alertRes.data || {})
    minutesList.value = minutesRes.data || []
    setupDynamicQr()
  } finally {
    loading.value = false
  }
}

const startRealtime = () => {
  if (!meetingId.value) return
  const token = localStorage.getItem('token') || ''
  eventSource = new EventSource(`/api/innovation/meeting/${meetingId.value}/stream?token=${encodeURIComponent(token)}`)
  eventSource.addEventListener('snapshot', event => Object.assign(live, JSON.parse(event.data)))
  eventSource.onerror = () => {
    eventSource?.close()
    eventSource = null
    if (!fallbackTimer) fallbackTimer = setInterval(loadLive, 5000)
  }
}

const stopRealtime = () => {
  eventSource?.close()
  eventSource = null
  clearInterval(fallbackTimer)
  fallbackTimer = null
  clearInterval(qrTimer)
  clearInterval(qrTick)
  qrTimer = null
  qrTick = null
}

const loadLive = async () => {
  if (!meetingId.value) return
  const res = await getLiveSnapshot(meetingId.value)
  Object.assign(live, res.data || {})
}

const setupDynamicQr = async () => {
  clearInterval(qrTimer)
  clearInterval(qrTick)
  if (!config.dynamicQrEnabled) {
    Object.keys(dynamicQr).forEach(key => delete dynamicQr[key])
    return
  }
  await loadDynamicQr()
  let elapsed = 0
  qrProgress.value = 100
  qrTick = setInterval(() => {
    elapsed += 1
    qrProgress.value = Math.max(0, 100 - elapsed * 100 / config.qrRefreshSeconds)
    if (elapsed >= config.qrRefreshSeconds) elapsed = 0
  }, 1000)
  qrTimer = setInterval(loadDynamicQr, config.qrRefreshSeconds * 1000)
}

const loadDynamicQr = async () => {
  const res = await getDynamicQrcode(meetingId.value)
  Object.assign(dynamicQr, res.data || {})
  qrProgress.value = 100
}

const saveConfig = async () => {
  if (config.requireLocation && (config.venueLatitude == null || config.venueLongitude == null)) {
    ElMessage.warning('请填写会场经纬度')
    return
  }
  savingConfig.value = true
  try {
    const res = await saveFeatureConfig(meetingId.value, config)
    Object.assign(config, res.data || {})
    ElMessage.success('智能签到规则已保存')
    setupDynamicQr()
  } finally { savingConfig.value = false }
}

const loadRisks = async () => {
  const res = await getRisks(meetingId.value, riskFilter.value || undefined)
  risks.value = res.data || []
}

const review = async row => {
  try {
    const { value } = await ElMessageBox.prompt('填写复核结论', '风险复核', { inputPlaceholder: '例如：已核验为本人签到', confirmButtonText: '确认', cancelButtonText: '取消' })
    await reviewRisk(row.id, { status: 1, remark: value })
    ElMessage.success('复核完成')
    loadRisks()
    loadLive()
  } catch { /* cancelled */ }
}

const createInvite = async () => {
  const res = await createGuestInvite(meetingId.value, { validHours: 24 })
  Object.assign(guestInvite, res.data || {})
  ElMessage.success('访客邀请已生成')
}

const sendReminder = async () => {
  sendingReminder.value = true
  try {
    const res = await sendMeetingReminders(meetingId.value, { type: 'CHECKIN_OPEN', onlyUnsigned: true })
    ElMessage.success(`已提醒 ${res.data?.createdCount || 0} 人`)
  } finally { sendingReminder.value = false }
}

const createMinutes = async () => {
  if (!minutesSource.value.trim()) return ElMessage.warning('请先填写会议记录')
  generatingMinutes.value = true
  try {
    await generateMinutes(meetingId.value, minutesSource.value)
    const res = await getMinutes(meetingId.value)
    minutesList.value = res.data || []
    ElMessage.success('摘要与行动事项已生成')
  } finally { generatingMinutes.value = false }
}

const handleTabChange = name => {
  if (name === 'risks') loadRisks()
  if (name === 'alerts') getAlerts().then(res => Object.assign(alerts, res.data || {}))
}

const openFullscreen = async () => {
  await nextTick()
  if (livePanel.value?.requestFullscreen) await livePanel.value.requestFullscreen()
}

const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 16) : '-'
const timeOnly = value => value ? String(value).replace('T', ' ').slice(11, 16) : '-'
const methodName = value => ({ qrcode: '二维码', photo: '拍照', gesture: '手势', location: '定位', makeup: '补签', proxy: '代签' }[value] || value || '签到')
const riskName = value => ({ HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险' }[value] || '低风险')
const riskTag = value => value === 'HIGH' ? 'danger' : value === 'MEDIUM' ? 'warning' : 'success'

onMounted(async () => {
  await loadMeetings()
  await refreshAll()
  startRealtime()
})
onBeforeUnmount(stopRealtime)
</script>

<style lang="scss" scoped>
.innovation-page { min-height: 100%; }
.operations-header { align-items: center; }
.header-actions, .section-actions { display: flex; align-items: center; gap: 10px; }
.meeting-select { width: 290px; }
.empty-state { min-height: 420px; display: grid; place-content: center; justify-items: center; gap: 10px; color: $text-secondary; background: $bg-white; border: 1px solid $border-light; border-radius: $radius-base; }
.empty-state strong { color: $text-primary; font-size: 18px; }
.signal-strip { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); margin-bottom: 18px; background: $bg-white; border: 1px solid $border-light; border-radius: $radius-base; overflow: hidden; }
.signal-item { position: relative; display: grid; gap: 5px; min-height: 112px; padding: 20px 22px; border-right: 1px solid $border-light; }
.signal-item:last-child { border-right: 0; }
.signal-item::before { content: ''; position: absolute; inset: 0 auto 0 0; width: 3px; background: $color-primary; }
.signal-item.success::before { background: $color-success; }.signal-item.warning::before { background: $color-warning; }.signal-item.danger::before { background: $color-danger; }
.signal-item span { color: $text-secondary; font-size: 13px; }.signal-item strong { color: $text-primary; font-size: 28px; line-height: 1; }.signal-item small { color: $text-placeholder; font-size: 12px; }
.operations-tabs { :deep(.el-tabs__header) { margin-bottom: 16px; } :deep(.el-tabs__nav-wrap::after) { height: 1px; background: $border-light; } }
.tab-badge { margin-left: 5px; transform: translateY(-1px); }
.tool-surface, .live-workspace { padding: 22px; background: $bg-white; border: 1px solid $border-light; border-radius: $radius-base; }
.section-bar { display: flex; justify-content: space-between; align-items: flex-start; gap: 18px; margin-bottom: 20px; }
.section-bar h3 { margin: 0 0 5px; color: $text-primary; font-size: 17px; }.section-bar p { margin: 0; color: $text-secondary; font-size: 13px; }
.live-indicator { display: inline-flex; align-items: center; gap: 7px; color: $color-success; font-size: 12px; font-weight: 600; }.live-indicator i { width: 8px; height: 8px; background: #20a67a; border-radius: 50%; box-shadow: 0 0 0 4px rgba(32,166,122,.12); }
.live-grid { display: grid; grid-template-columns: minmax(240px, .75fr) minmax(360px, 1.3fr) minmax(260px, .8fr); gap: 20px; }
.attendance-gauge, .latest-board, .dynamic-board { min-height: 330px; padding: 22px; background: $bg-soft; border: 1px solid $border-lighter; border-radius: $radius-base; }
.attendance-gauge { display: flex; flex-direction: column; justify-content: center; align-items: center; }.attendance-gauge :deep(.el-progress__text) { display: flex; flex-direction: column; gap: 6px; }.attendance-gauge :deep(.el-progress__text strong) { font-size: 34px; }.attendance-gauge :deep(.el-progress__text span) { color: $text-secondary; font-size: 12px; }
.gauge-counts { display: flex; flex-wrap: wrap; justify-content: center; gap: 12px; margin-top: 8px; color: $text-secondary; font-size: 12px; }.dot { display: inline-block; width: 7px; height: 7px; margin-right: 5px; border-radius: 50%; }.dot.signed { background: $color-success; }.dot.pending { background: $color-warning; }.dot.guest { background: $color-primary; }
.board-title { margin-bottom: 15px; color: $text-primary; font-size: 14px; font-weight: 650; }.arrival-list { display: grid; gap: 5px; }.arrival-row { display: flex; align-items: center; gap: 10px; min-height: 48px; padding: 6px 4px; border-bottom: 1px solid $border-lighter; }.arrival-copy { display: flex; flex: 1; flex-direction: column; min-width: 0; }.arrival-copy strong { color: $text-primary; font-size: 13px; }.arrival-copy span { color: $text-secondary; font-size: 11px; }
.dynamic-board { display: flex; flex-direction: column; align-items: center; }.dynamic-board .board-title { align-self: flex-start; }.qr-image { width: min(220px, 100%); aspect-ratio: 1; object-fit: contain; background: #fff; }.qr-status { width: 100%; margin-top: 14px; color: $text-secondary; font-size: 11px; }.qr-status span { display: block; margin-bottom: 8px; text-align: center; }.qr-disabled { flex: 1; display: grid; place-content: center; justify-items: center; gap: 10px; color: $text-placeholder; }.qr-disabled strong { color: $text-secondary; font-size: 13px; }
.live-workspace:fullscreen { padding: 40px; overflow: auto; background: #f4f6f8; }.live-workspace:fullscreen .live-grid { min-height: calc(100vh - 150px); }.live-workspace:fullscreen .attendance-gauge, .live-workspace:fullscreen .latest-board, .live-workspace:fullscreen .dynamic-board { min-height: 70vh; }
.rules-surface { max-width: 920px; }.rules-form { max-width: 760px; }.rule-row { display: flex; justify-content: space-between; align-items: center; gap: 20px; min-height: 72px; border-top: 1px solid $border-lighter; }.rule-copy { display: flex; flex-direction: column; gap: 4px; }.rule-copy strong { color: $text-primary; font-size: 14px; }.rule-copy span { color: $text-secondary; font-size: 12px; }.rules-form > .el-form-item { width: 440px; margin: 0 0 18px 0; }.coordinate-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; padding: 14px 0; }.coordinate-grid :deep(.el-input-number) { width: 100%; }
.risk-score { color: $color-danger; font-size: 18px; font-weight: 700; }
.split-layout { display: grid; grid-template-columns: minmax(0, 1.65fr) minmax(280px, .65fr); gap: 18px; }.guest-invite { display: flex; align-items: center; gap: 16px; margin-bottom: 18px; padding: 14px; background: $color-primary-bg; border: 1px solid #d8e5ff; border-radius: $radius-base; }.guest-invite img { width: 118px; height: 118px; }.guest-invite div { display: flex; flex-direction: column; gap: 6px; }.guest-invite span { color: $text-secondary; font-size: 12px; }.reminder-tool { align-self: start; }.reminder-summary { display: flex; flex-direction: column; align-items: center; gap: 9px; padding: 30px 10px; color: $color-primary; text-align: center; }.reminder-summary strong { color: $text-primary; font-size: 22px; }.reminder-summary span { color: $text-secondary; font-size: 12px; }.reminder-tool > .el-button { width: 100%; }
.alert-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 28px; }.alert-columns h4, .minutes-result h4 { margin: 0 0 12px; color: $text-primary; font-size: 14px; }.warning-list { display: grid; gap: 8px; }.warning-row { display: flex; align-items: flex-start; gap: 12px; padding: 14px; background: $bg-soft; border: 1px solid $border-lighter; border-radius: $radius-small; }.warning-row div { display: flex; flex-direction: column; gap: 4px; }.warning-row strong { color: $text-primary; font-size: 13px; }.warning-row span { color: $text-secondary; font-size: 12px; }
.minutes-layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 18px; }.minutes-editor > .el-button { margin-top: 16px; }.summary-text { margin: 0 0 24px; color: $text-regular; font-size: 14px; line-height: 1.8; }.action-list { display: grid; gap: 8px; }.action-row { display: flex; align-items: flex-start; gap: 10px; padding: 12px 0; border-bottom: 1px solid $border-lighter; }.action-row div { display: flex; flex-direction: column; gap: 4px; }.action-row strong { color: $text-primary; font-size: 13px; }.action-row span { color: $text-secondary; font-size: 11px; }
@media (max-width: 1180px) { .signal-strip { grid-template-columns: repeat(2, 1fr); }.signal-item:nth-child(2) { border-right: 0; }.signal-item:nth-child(-n+2) { border-bottom: 1px solid $border-light; }.live-grid { grid-template-columns: 1fr 1fr; }.dynamic-board { grid-column: 1 / -1; }.split-layout, .minutes-layout { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .operations-header { align-items: stretch; }.header-actions { width: 100%; }.meeting-select { width: 100%; }.signal-strip, .live-grid, .alert-columns, .coordinate-grid { grid-template-columns: 1fr; }.signal-item { border-right: 0; border-bottom: 1px solid $border-light; }.signal-item:last-child { border-bottom: 0; }.dynamic-board { grid-column: auto; }.section-bar { align-items: stretch; flex-direction: column; }.tool-surface, .live-workspace { padding: 16px; }.rules-form > .el-form-item { width: 100%; } }
</style>

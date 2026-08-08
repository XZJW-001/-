<template>
  <div class="approval-center page-container">
    <div class="page-header">
      <div>
        <span class="page-kicker">审核工作流</span>
        <h2>审批中心</h2>
        <p>集中处理所有群组的补签与代签申请</p>
      </div>
      <div class="approval-status">
        <span class="status-dot"></span>
        <span>审批服务在线</span>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="approval-tabs">
      <el-tab-pane label="补签审批" name="makeup">
        <div class="card-container">
          <div class="card-heading">
            <div>
              <h3>补签申请</h3>
              <p>核对申请人、会议与申请理由后完成处理</p>
            </div>
            <el-tag type="warning" effect="plain">{{ filteredMakeupList.length }} 条记录</el-tag>
          </div>
          <div class="filter-bar">
            <el-select v-model="makeupFilter.status" placeholder="审批状态" clearable style="width: 140px" @change="loadMakeupList">
              <el-option label="全部" value="" />
              <el-option label="待审批" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已驳回" :value="2" />
            </el-select>
            <el-select v-model="makeupFilter.meetingId" placeholder="筛选会议" clearable style="width: 200px" @change="loadMakeupList">
              <el-option v-for="m in allMeetings" :key="m.id" :label="m.title" :value="m.id" />
            </el-select>
            <el-button type="primary" plain @click="refreshAll">刷新</el-button>
          </div>
          <el-table :data="filteredMakeupList" v-loading="makeupLoading" empty-text="暂无待处理的补签申请" stripe>
            <el-table-column label="申请人" width="120">
              <template #default="{ row }">
                <div class="user-cell">
                  <div class="avatar-sm" :style="{background: getAvatarColor(row.userId)}">{{ getAvatarChar(row.userName) }}</div>
                  <span>{{ row.userName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="meetingTitle" label="关联会议" min-width="180">
              <template #default="{ row }">
                <div class="meeting-cell">
                  <div class="mt-title">{{ row.meetingTitle || '会议#' + row.meetingId }}</div>
                  <div class="mt-id">ID: {{ row.meetingId }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="申请理由" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getMakeupStatusType(row.status)">{{ getMakeupStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申请时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <el-button type="success" link @click="handleApproveMakeup(row, 1)">通过</el-button>
                  <el-button type="danger" link @click="handleApproveMakeup(row, 2)">驳回</el-button>
                </template>
                <span v-else class="text-muted">已处理</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="代签申请" name="proxy">
        <div class="card-container">
          <div class="card-heading">
            <div>
              <h3>代签申请</h3>
              <p>审批通过后，系统自动生成代签与签到记录</p>
            </div>
            <el-tag type="warning" effect="plain">{{ filteredProxyList.length }} 条记录</el-tag>
          </div>
          <div class="filter-bar">
            <el-select v-model="proxyFilter.status" placeholder="审批状态" clearable style="width: 140px" @change="loadProxyList">
              <el-option label="全部" value="" />
              <el-option label="待审批" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已驳回" :value="2" />
              <el-option label="已撤销" :value="3" />
            </el-select>
            <el-select v-model="proxyFilter.meetingId" placeholder="筛选会议" clearable style="width: 200px">
              <el-option v-for="m in allMeetings" :key="m.id" :label="m.title" :value="m.id" />
            </el-select>
            <el-button type="primary" plain @click="loadProxyList">刷新</el-button>
          </div>
          <el-table :data="filteredProxyList" v-loading="proxyLoading" empty-text="暂无代签申请" stripe>
            <el-table-column prop="applicantName" label="申请人" width="110" />
            <el-table-column prop="proxyUserName" label="拟代签人" width="110" />
            <el-table-column prop="meetingTitle" label="关联会议" min-width="160" />
            <el-table-column prop="reason" label="申请理由" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getProxyStatusType(row.status)">{{ getProxyStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申请时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="审批说明" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.approveRemark || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <el-button type="success" link @click="handleApproveProxy(row, 1)">通过</el-button>
                  <el-button type="danger" link @click="handleApproveProxy(row, 2)">驳回</el-button>
                </template>
                <span v-else class="text-muted">{{ row.approverName || '已处理' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveMakeUp, getAllMakeUpList, approveProxySignApplication, getAllProxySignApplications } from '@/api/checkin'
import { getMeetingList } from '@/api/meeting'
import dayjs from 'dayjs'

const activeTab = ref('makeup')
const makeupList = ref([])
const proxyList = ref([])
const makeupLoading = ref(false)
const proxyLoading = ref(false)
const allMeetings = ref([])
const makeupFilter = ref({ status: 0, meetingId: null })
const proxyFilter = ref({ status: 0, meetingId: null })

const filteredMakeupList = computed(() => {
  let list = makeupList.value
  if (makeupFilter.value.meetingId) {
    list = list.filter(m => m.meetingId === makeupFilter.value.meetingId)
  }
  return list
})

const filteredProxyList = computed(() => {
  let list = proxyList.value
  if (proxyFilter.value.meetingId) {
    list = list.filter(item => item.meetingId === proxyFilter.value.meetingId)
  }
  return list
})

const loadAllMeetings = async () => {
  try {
    const res = await getMeetingList({ current: 1, size: 200 })
    allMeetings.value = res.data?.records || []
  } catch (e) {
    console.error('加载会议列表失败:', e)
  }
}

const refreshAll = () => {
  loadMakeupList()
  loadProxyList()
}

const loadMakeupList = async () => {
  makeupLoading.value = true
  try {
    const status = makeupFilter.value.status === '' ? undefined : makeupFilter.value.status
    const res = await getAllMakeUpList(status)
    makeupList.value = res.data || []
  } catch (e) {
    console.error('加载补签列表失败:', e)
  } finally {
    makeupLoading.value = false
  }
}

const loadProxyList = async () => {
  proxyLoading.value = true
  try {
    const status = proxyFilter.value.status === '' ? undefined : proxyFilter.value.status
    const res = await getAllProxySignApplications(status)
    proxyList.value = res.data || []
  } catch (e) {
    console.error('加载代签列表失败:', e)
  } finally {
    proxyLoading.value = false
  }
}

const handleApproveProxy = async (row, status) => {
  try {
    const action = status === 1 ? '通过' : '驳回'
    let remark = '审批通过'
    if (status === 2) {
      const result = await ElMessageBox.prompt(
        `申请人：${row.applicantName}；拟代签人：${row.proxyUserName}；会议：${row.meetingTitle}`,
        '填写驳回原因',
        {
          confirmButtonText: '确认驳回',
          cancelButtonText: '取消',
          type: 'warning',
          inputType: 'textarea',
          inputPlaceholder: '请输入具体驳回原因',
          inputValidator: value => {
            if (!value || !value.trim()) return '驳回原因不能为空'
            if (value.trim().length > 500) return '驳回原因不能超过500个字符'
            return true
          }
        }
      )
      remark = result.value.trim()
    } else {
      await ElMessageBox.confirm(
        `确定通过该代签申请吗？\n申请人：${row.applicantName}\n拟代签人：${row.proxyUserName}\n会议：${row.meetingTitle}`,
        '确认审批',
        { type: 'success' }
      )
    }
    await approveProxySignApplication(row.id, status, remark)
    ElMessage.success(`已${action}`)
    loadProxyList()
  } catch (e) {
    if (e !== 'cancel') console.error('代签审批失败:', e)
  }
}

const handleApproveMakeup = async (row, status) => {
  try {
    const remark = status === 1 ? '审批通过' : '申请驳回'
    await ElMessageBox.confirm(
      `确定${status === 1 ? '通过' : '驳回'}该补签申请吗？\n申请人：${row.userName}\n会议：${row.meetingTitle}`,
      '确认',
      { type: status === 1 ? 'success' : 'warning' }
    )
    await approveMakeUp(row.id, status, remark)
    ElMessage.success(status === 1 ? '已通过' : '已驳回')
    loadMakeupList()
  } catch (e) {
    if (e !== 'cancel') console.error('审批失败:', e)
  }
}

const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
const getMakeupStatusType = (s) => ['warning', 'success', 'danger'][s] || 'info'
const getMakeupStatusText = (s) => ['待审批', '已通过', '已驳回'][s] || '未知'
const getProxyStatusType = (s) => ['warning', 'success', 'danger', 'info'][s] || 'info'
const getProxyStatusText = (s) => ['待审批', '已通过', '已驳回', '已撤销'][s] || '未知'

const COLORS = ['#2563EB', '#059669', '#D97706', '#DC2626', '#64748B', '#7C3AED', '#0F766E']
const getAvatarColor = (uid) => {
  if (!uid) return COLORS[0]
  return COLORS[Number(uid) % COLORS.length]
}
const getAvatarChar = (name) => {
  if (!name) return '?'
  return name.charAt(0).toUpperCase()
}

onMounted(() => {
  loadMakeupList()
  loadProxyList()
  loadAllMeetings()
})
</script>

<style lang="scss" scoped>
.approval-center {
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: $spacing-lg;
  margin-bottom: $spacing-lg;

  .page-kicker {
    display: block;
    margin-bottom: 6px;
    color: $color-primary;
    font-size: 11px;
    font-weight: 650;
  }

  h2 {
    margin: 0 0 6px;
    color: $text-primary;
    font-size: 24px;
    font-weight: 650;
  }

  p {
    margin: 0;
    color: $text-secondary;
    font-size: 13px;
  }
}

.approval-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  color: $text-secondary;
  background: $bg-white;
  border: 1px solid $border-light;
  border-radius: $radius-small;
  font-size: 12px;
}

.status-dot {
  width: 7px;
  height: 7px;
  background: $color-success;
  border-radius: 50%;
  box-shadow: 0 0 0 4px $color-success-bg;
}

.approval-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: $spacing-lg;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background: $border-light;
  }
}

.card-container {
  padding: $spacing-lg;
  background: $bg-white;
  border: 1px solid $border-light;
  border-radius: $radius-base;
  box-shadow: $shadow-base;
}

.card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $spacing-md;
  margin-bottom: $spacing-lg;
  padding-bottom: $spacing-md;
  border-bottom: 1px solid $border-extra-light;

  h3 {
    margin: 0 0 4px;
    color: $text-primary;
    font-size: 16px;
    font-weight: 650;
  }

  p {
    margin: 0;
    color: $text-secondary;
    font-size: 12px;
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: $spacing-lg;
  padding: 12px;
  background: $bg-soft;
  border: 1px solid $border-extra-light;
  border-radius: $radius-small;
}

.text-muted {
  color: $text-placeholder;
  font-size: 13px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar-sm {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
}

.meeting-cell {
  .mt-title {
    color: $text-primary;
    font-weight: 550;
  }

  .mt-id {
    margin-top: 3px;
    color: $text-secondary;
    font-size: 12px;
  }
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .approval-status {
    align-self: flex-start;
  }

  .card-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .filter-bar {
    :deep(.el-select) {
      width: 100% !important;
    }
  }
}
</style>

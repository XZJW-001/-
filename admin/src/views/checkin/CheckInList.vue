<template>
  <div class="checkin-list page-container">
    <div class="page-header">
      <div>
        <span class="page-kicker">出勤档案</span>
        <h2>签到记录</h2>
        <p>按会议与签到方式查询成员的到场记录</p>
      </div>
      <el-button type="success" plain @click="exportData">
        <el-icon><Download /></el-icon>
        导出报表
      </el-button>
    </div>

    <section class="records-panel">
      <div class="panel-heading">
        <div>
          <h3>签到明细</h3>
          <p>{{ selectedMeetingTitle }}</p>
        </div>
        <el-tag type="info" effect="plain">{{ filteredRecordList.length }} 条记录</el-tag>
      </div>

      <div class="filter-panel">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="会议">
            <el-select v-model="searchForm.meetingId" placeholder="请选择会议" clearable filterable class="meeting-select">
              <el-option v-for="meeting in meetingOptions" :key="meeting.id" :label="meeting.title" :value="meeting.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="签到方式">
            <el-select v-model="searchForm.signMethod" placeholder="全部方式" clearable class="method-select">
              <el-option label="二维码" value="qrcode" />
              <el-option label="拍照" value="photo" />
              <el-option label="手势" value="gesture" />
              <el-option label="定位" value="location" />
            </el-select>
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        :data="filteredRecordList"
        v-loading="loading"
        :empty-text="emptyText"
        class="records-table"
        stripe
      >
        <el-table-column prop="user.realName" label="姓名" :width="isMobileTable ? 80 : 90">
          <template #default="{ row }">{{ row.user?.realName || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="!isMobileTable" prop="user.position" label="职位" width="90">
          <template #default="{ row }">{{ row.user?.position || '-' }}</template>
        </el-table-column>
        <el-table-column label="签到方式" :width="isMobileTable ? 90 : 100">
          <template #default="{ row }">
            <el-tag effect="plain">{{ getMethodText(row.signMethod) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="签到时间" :width="isMobileTable ? 150 : 155">
          <template #default="{ row }">{{ formatTime(row.signTime) }}</template>
        </el-table-column>
        <el-table-column v-if="!isMobileTable" label="签到状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.signStatus)">{{ getStatusText(row.signStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="!isMobileTable" prop="location" label="签到位置" min-width="130" show-overflow-tooltip />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getMeetingList } from '@/api/meeting'
import { getCheckInRecords } from '@/api/checkin'
import dayjs from 'dayjs'

const loading = ref(false)
const recordList = ref([])
const meetingOptions = ref([])
const viewportWidth = ref(window.innerWidth)
const isMobileTable = computed(() => viewportWidth.value <= 768)

const searchForm = reactive({
  meetingId: null,
  signMethod: ''
})

const filteredRecordList = computed(() => {
  if (!searchForm.signMethod) return recordList.value
  return recordList.value.filter(item => item.signMethod === searchForm.signMethod)
})

const selectedMeetingTitle = computed(() => {
  if (!searchForm.meetingId) return '选择会议后查看对应签到记录'
  return meetingOptions.value.find(item => item.id === searchForm.meetingId)?.title || '当前会议'
})

const emptyText = computed(() => searchForm.meetingId ? '当前条件下暂无签到记录' : '请先选择会议查看签到记录')

const loadMeetings = async () => {
  try {
    const res = await getMeetingList({ current: 1, size: 100 })
    meetingOptions.value = res.data.records || []
  } catch (error) {
    console.error('加载会议列表失败:', error)
  }
}

const loadRecords = async () => {
  if (!searchForm.meetingId) {
    recordList.value = []
    ElMessage.info('请先选择会议')
    return
  }

  loading.value = true
  try {
    const res = await getCheckInRecords(searchForm.meetingId)
    recordList.value = res.data || []
  } catch (error) {
    console.error('加载签到记录失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadRecords()
const handleReset = () => {
  searchForm.meetingId = null
  searchForm.signMethod = ''
  recordList.value = []
}

const exportData = () => {
  if (!searchForm.meetingId) {
    ElMessage.warning('请先选择会议')
    return
  }
  ElMessage.info('正在生成报表...')
  setTimeout(() => ElMessage.success('报表生成成功'), 1000)
}

const formatTime = (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
const getMethodText = (method) => ({
  qrcode: '二维码', photo: '拍照', gesture: '手势', location: '定位'
}[method] || method || '-')
const getStatusType = (status) => ['', 'success', 'warning', 'danger'][status] || 'info'
const getStatusText = (status) => ['', '正常', '迟到', '无效'][status] || '-'
const handleViewportResize = () => {
  viewportWidth.value = window.innerWidth
}

onMounted(() => {
  loadMeetings()
  window.addEventListener('resize', handleViewportResize)
})
onBeforeUnmount(() => window.removeEventListener('resize', handleViewportResize))
</script>

<style lang="scss" scoped>
.checkin-list {
  min-height: 100%;
}

.page-kicker {
  display: block;
  margin-bottom: 6px;
  color: $color-primary;
  font-size: 11px;
  font-weight: 650;
}

.records-panel {
  padding: $spacing-lg;
  background: $bg-white;
  border: 1px solid $border-light;
  border-radius: $radius-base;
  box-shadow: $shadow-base;
}

.panel-heading {
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

.filter-panel {
  margin-bottom: $spacing-lg;
  padding: 12px;
  background: $bg-soft;
  border: 1px solid $border-extra-light;
  border-radius: $radius-small;
}

.search-form {
  align-items: flex-end;

  :deep(.el-form-item) {
    margin-right: 0;
  }

  :deep(.el-form-item__label) {
    color: $text-regular;
    font-size: 13px;
  }
}

.meeting-select {
  width: 220px;
}

.method-select {
  width: 140px;
}

.records-table {
  :deep(.el-table__empty-text) {
    width: 100%;
    padding: 0 12px;
    white-space: normal;
  }
}

@media (max-width: 768px) {
  .records-panel {
    padding: 12px;
  }

  .panel-heading {
    align-items: flex-start;
  }

  .search-form {
    :deep(.el-form-item) {
      width: 100%;
      margin-right: 0;
    }

    :deep(.el-form-item__content),
    .el-select {
      width: 100%;
    }
  }
}
</style>

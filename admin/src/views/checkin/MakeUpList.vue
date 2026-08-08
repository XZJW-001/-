<template>
  <div class="makeup-list">
    <div class="page-header">
      <h2>补签审批</h2>
      <p>审批用户的补签申请</p>
    </div>
    
    <div class="card-container">
      <div class="table-toolbar">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="会议">
            <el-select v-model="searchForm.meetingId" placeholder="选择会议" clearable style="width: 200px;">
              <el-option v-for="meeting in meetingOptions" :key="meeting.id" :label="meeting.title" :value="meeting.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部" clearable>
              <el-option label="待审批" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已拒绝" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <el-table :data="applyList" v-loading="loading" stripe>
        <el-table-column prop="user.realName" label="申请人" width="100">
          <template #default="{ row }">
            {{ row.user?.realName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="user.position" label="职位" width="120">
          <template #default="{ row }">
            {{ row.user?.position || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="补签原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" v-if="hasPending">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="success" size="small" @click="handleApprove(row.id, 1)">通过</el-button>
              <el-button type="danger" size="small" @click="handleApprove(row.id, 2)">拒绝</el-button>
            </template>
            <template v-else>
              <span class="text-muted">已处理</span>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMeetingList } from '@/api/meeting'
import { getMakeUpList, approveMakeUp } from '@/api/checkin'
import dayjs from 'dayjs'

const loading = ref(false)
const applyList = ref([])
const meetingOptions = ref([])

const searchForm = reactive({
  meetingId: null,
  status: null
})

const hasPending = computed(() => searchForm.status === null || searchForm.status === 0)

const loadMeetings = async () => {
  try {
    const res = await getMeetingList({ current: 1, size: 100 })
    meetingOptions.value = res.data.records || []
  } catch (error) {
    console.error('加载会议列表失败:', error)
  }
}

const loadList = async () => {
  if (!searchForm.meetingId) {
    applyList.value = []
    return
  }
  
  loading.value = true
  try {
    const res = await getMakeUpList(searchForm.meetingId, searchForm.status)
    applyList.value = res.data || []
  } catch (error) {
    console.error('加载补签列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadList()
const handleReset = () => {
  searchForm.meetingId = null
  searchForm.status = null
  applyList.value = []
}

const handleApprove = async (id, status) => {
  const action = status === 1 ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定${action}该补签申请吗？`, '提示', { type: 'warning' })
    await approveMakeUp(id, status)
    ElMessage.success(`已${action}`)
    loadList()
  } catch {}
}

const formatTime = (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
const getStatusType = (status) => ['warning', 'success', 'danger'][status] || 'info'
const getStatusText = (status) => ['待审批', '已通过', '已拒绝'][status] || '未知'

onMounted(() => loadMeetings())
</script>

<style lang="scss" scoped>
.text-muted {
  color: #909399;
  font-size: 13px;
}
</style>

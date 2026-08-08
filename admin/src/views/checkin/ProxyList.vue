<template>
  <div class="proxy-list">
    <div class="page-header">
      <h2>代签管理</h2>
      <p>查看已完成的代签记录，并处理管理员应急代签</p>
    </div>
    
    <div class="card-container">
      <div class="table-toolbar">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="会议">
            <el-select v-model="searchForm.meetingId" placeholder="选择会议" clearable style="width: 200px;">
              <el-option v-for="meeting in meetingOptions" :key="meeting.id" :label="meeting.title" :value="meeting.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        
        <el-button type="primary" @click="showProxyDialog">
          <el-icon><Plus /></el-icon>应急代签
        </el-button>
      </div>
      
      <el-table :data="proxyList" v-loading="loading" stripe>
        <el-table-column label="代签人" width="120">
          <template #default="{ row }">
            {{ row.proxyUserName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="被代签人" width="120">
          <template #default="{ row }">
            {{ row.targetUserName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="代签原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="代签时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.signTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <!-- 管理员应急代签对话框 -->
    <el-dialog v-model="proxyDialogVisible" title="管理员应急代签" width="500px">
      <el-form :model="proxyForm" label-width="100px">
        <el-alert title="当前操作人将作为代签人，常规代签请在审批中心处理申请。" type="warning" :closable="false" show-icon style="margin-bottom: 16px;" />
        <el-form-item label="被代签人">
          <el-select v-model="proxyForm.targetUserIds" placeholder="选择被代签人" multiple collapse-tags collapse-tags-tooltip filterable style="width: 100%;">
            <el-option v-for="user in userOptions" :key="user.id" :label="user.realName" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="代签原因">
          <el-input v-model="proxyForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请填写应急代签原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="proxyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleProxySign">确认代签</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMeetingList } from '@/api/meeting'
import { getAllUsers } from '@/api/user'
import { getProxySignList, proxySign } from '@/api/checkin'
import dayjs from 'dayjs'

const loading = ref(false)
const proxyList = ref([])
const meetingOptions = ref([])
const userOptions = ref([])
const proxyDialogVisible = ref(false)

const searchForm = reactive({
  meetingId: null
})

const proxyForm = reactive({
  targetUserIds: [],
  reason: ''
})

const loadData = async () => {
  try {
    const [meetingRes, userRes] = await Promise.all([
      getMeetingList({ current: 1, size: 100 }),
      getAllUsers()
    ])
    meetingOptions.value = meetingRes.data.records || []
    userOptions.value = userRes.data || []
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

const loadList = async () => {
  if (!searchForm.meetingId) {
    proxyList.value = []
    return
  }
  
  loading.value = true
  try {
    const res = await getProxySignList(searchForm.meetingId)
    proxyList.value = res.data || []
  } catch (error) {
    console.error('加载代签列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadList()
const handleReset = () => {
  searchForm.meetingId = null
  proxyList.value = []
}

const showProxyDialog = () => {
  if (!searchForm.meetingId) {
    ElMessage.warning('请先选择会议')
    return
  }
  proxyForm.targetUserIds = []
  proxyForm.reason = ''
  proxyDialogVisible.value = true
}

const handleProxySign = async () => {
  if (!proxyForm.targetUserIds.length) {
    ElMessage.warning('请选择被代签人')
    return
  }
  if (!proxyForm.reason.trim()) {
    ElMessage.warning('请填写代签原因')
    return
  }
  try {
    await proxySign(searchForm.meetingId, {
      targetUserIds: proxyForm.targetUserIds,
      reason: proxyForm.reason
    })
    ElMessage.success('代签成功')
    proxyDialogVisible.value = false
    loadList()
  } catch (error) {
    console.error('代签失败:', error)
  }
}

const formatTime = (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
const getStatusType = (status) => ['warning', 'success', 'info', 'danger'][status] || 'info'
const getStatusText = (status) => ['待确认', '已确认', '已拒绝'][status] || '未知'

onMounted(() => loadData())
</script>

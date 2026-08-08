<template>
  <div class="meeting-list">
    <div class="page-header">
      <h2>会议管理</h2>
      <p>管理和查看所有会议</p>
    </div>
    
    <div class="card-container">
      <div class="table-toolbar">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="会议主题">
            <el-input v-model="searchForm.title" placeholder="请输入会议主题" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
              <el-option label="草稿" :value="0" />
              <el-option label="已发布" :value="1" />
              <el-option label="进行中" :value="2" />
              <el-option label="已结束" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        
        <el-button type="primary" @click="createMeeting">
          <el-icon><Plus /></el-icon>创建会议
        </el-button>
      </div>
      
      <el-table :data="meetingList" v-loading="loading" stripe>
        <el-table-column prop="title" label="会议主题" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="viewDetail(row.id)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="地点" width="120" />
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row.id)">详情</el-button>
            <el-button type="success" link v-if="row.status === 0" @click="handlePublishMeeting(row.id)">发布</el-button>
            <el-button type="warning" link v-if="row.status === 1" @click="handleStartMeeting(row.id)">开始</el-button>
            <el-button type="danger" link v-if="row.status === 2" @click="handleEndMeeting(row.id)">结束</el-button>
            <el-button link @click="handleGenerateQrcode(row)">二维码</el-button>
            <el-button type="danger" link @click="handleDeleteMeeting(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        style="margin-top: 16px; justify-content: flex-end;"
        @size-change="loadMeetings"
        @current-change="loadMeetings"
      />
    </div>
    
    <!-- 二维码弹窗 -->
    <el-dialog v-model="qrcodeVisible" title="会议签到二维码" width="400px" center>
      <div class="qrcode-container">
        <img :src="qrcodeData?.qrcodeImage" alt="二维码" class="qrcode-image" />
        <p class="qrcode-tip">请使用手机扫描二维码进行签到</p>
        <p class="qrcode-expire">有效期至：{{ qrcodeData?.expireTime ? formatTime(qrcodeData.expireTime) : '-' }}</p>
      </div>
      <template #footer>
        <el-button @click="qrcodeVisible = false">关闭</el-button>
        <el-button type="primary" @click="printQrcode">打印二维码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMeetingList, publishMeeting, startMeeting, endMeeting, deleteMeeting, generateQrcode } from '@/api/meeting'
import dayjs from 'dayjs'

const router = useRouter()

const loading = ref(false)
const meetingList = ref([])
const qrcodeVisible = ref(false)
const qrcodeData = ref(null)

const searchForm = reactive({
  title: '',
  status: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const loadMeetings = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    if (searchForm.title) params.title = searchForm.title
    if (searchForm.status !== null) params.status = searchForm.status
    
    const res = await getMeetingList(params)
    meetingList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('加载会议列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadMeetings()
}

const handleReset = () => {
  searchForm.title = ''
  searchForm.status = null
  handleSearch()
}

const viewDetail = (id) => router.push(`/meetings/detail/${id}`)
const createMeeting = () => router.push('/meetings/create')

const handlePublishMeeting = async (id) => {
  try {
    await ElMessageBox.confirm('确定发布该会议吗？', '提示', { type: 'warning' })
    await publishMeeting(id)
    ElMessage.success('会议发布成功')
    loadMeetings()
  } catch {}
}

const handleStartMeeting = async (id) => {
  try {
    await ElMessageBox.confirm('确定开始该会议吗？', '提示', { type: 'warning' })
    await startMeeting(id)
    ElMessage.success('会议已开始')
    loadMeetings()
  } catch {}
}

const handleEndMeeting = async (id) => {
  try {
    await ElMessageBox.confirm('确定结束该会议吗？', '提示', { type: 'warning' })
    await endMeeting(id)
    ElMessage.success('会议已结束')
    loadMeetings()
  } catch {}
}

const handleDeleteMeeting = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该会议吗？删除后不可恢复', '提示', { type: 'error' })
    await deleteMeeting(id)
    ElMessage.success('删除成功')
    loadMeetings()
  } catch {}
}

const handleGenerateQrcode = async (row) => {
  try {
    const res = await generateQrcode(row.id)
    qrcodeData.value = res.data
    qrcodeVisible.value = true
  } catch (error) {
    console.error('生成二维码失败:', error)
  }
}

const printQrcode = () => {
  window.print()
}

const formatTime = (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
const getStatusType = (status) => ['info', 'warning', 'success', 'danger'][status] || 'info'
const getStatusText = (status) => ['草稿', '已发布', '进行中', '已结束'][status] || '未知'

onMounted(() => loadMeetings())
</script>

<style lang="scss" scoped>
.qrcode-container {
  text-align: center;
  padding: 20px;
  
  .qrcode-image {
    width: 260px;
    height: 260px;
    margin: 0 auto;
    display: block;
  }
  
  .qrcode-tip {
    margin-top: 16px;
    color: #606266;
  }
  
  .qrcode-expire {
    margin-top: 8px;
    color: #909399;
    font-size: 13px;
  }
}
</style>

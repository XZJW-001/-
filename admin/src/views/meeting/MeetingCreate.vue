<template>
  <div class="meeting-create">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑会议' : '创建会议' }}</h2>
      <p>{{ isEdit ? '修改会议信息' : '创建一个新的会议' }}</p>
    </div>
    
    <div class="card-container">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="meeting-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会议主题" prop="title">
              <el-input v-model="form.title" placeholder="请输入会议主题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会议地点" prop="location">
              <el-input v-model="form.location" placeholder="请输入会议地点" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="会议描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入会议描述" />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="迟到阈值(分)">
              <el-input-number v-model="form.lateTime" :min="0" :max="120" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="签到时间">
          <div class="checkin-time-setup">
            <el-radio-group v-model="form.checkinMode" style="margin-bottom: 12px;">
              <el-radio-button label="auto">快捷设置</el-radio-button>
              <el-radio-button label="custom">自定义时间</el-radio-button>
            </el-radio-group>
            <div v-if="form.checkinMode === 'auto'" class="offset-row">
              <span>会议开始前</span>
              <el-input-number v-model="form.checkinStartOffset" :min="0" :max="180" :step="5" size="small" controls-position="right" />
              <span>分钟开始签到，会议开始后</span>
              <el-input-number v-model="form.checkinEndOffset" :min="0" :max="180" :step="5" size="small" controls-position="right" />
              <span>分钟结束签到</span>
            </div>
            <el-row :gutter="20" v-else>
              <el-col :span="12">
                <el-date-picker v-model="form.checkinStartTime" type="datetime" placeholder="签到开始时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-col>
              <el-col :span="12">
                <el-date-picker v-model="form.checkinEndTime" type="datetime" placeholder="签到结束时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-col>
            </el-row>
          </div>
        </el-form-item>
        
        <el-form-item label="签到方式" prop="signMethods">
          <el-checkbox-group v-model="form.signMethods">
            <el-checkbox label="qrcode">二维码</el-checkbox>
            <el-checkbox label="photo">拍照签到</el-checkbox>
            <el-checkbox label="gesture">手势签到</el-checkbox>
            <el-checkbox label="location">定位签到</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        
        <el-form-item label="参会人员">
          <el-select v-model="form.attendeeIds" multiple filterable placeholder="选择参会人员" style="width: 100%">
            <el-option v-for="user in userList" :key="user.id" :label="user.realName" :value="user.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '创建会议' }}
          </el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createMeeting, updateMeeting, getMeetingDetail } from '@/api/meeting'
import { getAllUsers } from '@/api/user'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const submitting = ref(false)
const userList = ref([])

const isEdit = computed(() => !!route.params.mid)
const groupId = computed(() => route.params.gid)

const form = reactive({
  title: '',
  description: '',
  location: '',
  startTime: null,
  endTime: null,
  checkinMode: 'auto',
  checkinStartOffset: 30,
  checkinEndOffset: 15,
  checkinStartTime: null,
  checkinEndTime: null,
  lateTime: 15,
  signMethods: ['qrcode'],
  attendeeIds: [],
  groupId: null
})

const rules = {
  title: [{ required: true, message: '请输入会议主题', trigger: 'blur' }],
  location: [{ required: true, message: '请输入会议地点', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  signMethods: [{ required: true, message: '请至少选择一种签到方式', trigger: 'change' }]
}

const loadUsers = async () => {
  try {
    const res = await getAllUsers()
    userList.value = res.data || []
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
}

const loadMeeting = async () => {
  if (!isEdit.value) return
  try {
    const res = await getMeetingDetail(route.params.mid)
    const data = res.data
    form.title = data.title
    form.description = data.description
    form.location = data.location
    form.startTime = data.startTime
    form.endTime = data.endTime
    form.checkinStartTime = data.checkinStartTime
    form.checkinEndTime = data.checkinEndTime
    form.lateTime = data.lateTime
    form.signMethods = data.signMethods || ['qrcode']
    form.groupId = data.groupId
    // 反推偏移量
    if (data.checkinStartTime && data.startTime) {
      const diffStart = dayjs(data.startTime).diff(dayjs(data.checkinStartTime), 'minute')
      const diffEnd = dayjs(data.checkinEndTime).diff(dayjs(data.startTime), 'minute')
      if (diffStart >= 0 && diffStart <= 180 && diffEnd >= 0 && diffEnd <= 180) {
        form.checkinMode = 'auto'
        form.checkinStartOffset = diffStart
        form.checkinEndOffset = diffEnd
      } else {
        form.checkinMode = 'custom'
      }
    }
  } catch (error) {
    console.error('加载会议详情失败:', error)
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    // 根据签到模式计算实际签到时间
    let checkinStartTime = null
    let checkinEndTime = null
    if (form.checkinMode === 'auto' && form.startTime) {
      const meetingStart = dayjs(form.startTime)
      checkinStartTime = meetingStart.subtract(form.checkinStartOffset, 'minute').format('YYYY-MM-DD HH:mm:ss')
      checkinEndTime = meetingStart.add(form.checkinEndOffset, 'minute').format('YYYY-MM-DD HH:mm:ss')
    } else {
      checkinStartTime = form.checkinStartTime ? dayjs(form.checkinStartTime).format('YYYY-MM-DD HH:mm:ss') : null
      checkinEndTime = form.checkinEndTime ? dayjs(form.checkinEndTime).format('YYYY-MM-DD HH:mm:ss') : null
    }

    const submitData = {
      title: form.title,
      description: form.description,
      location: form.location,
      startTime: form.startTime ? dayjs(form.startTime).format('YYYY-MM-DD HH:mm:ss') : null,
      endTime: form.endTime ? dayjs(form.endTime).format('YYYY-MM-DD HH:mm:ss') : null,
      checkinStartTime,
      checkinEndTime,
      lateTime: form.lateTime,
      signMethods: form.signMethods,
      attendeeIds: form.attendeeIds,
      groupId: groupId.value
    }
    
    if (isEdit.value) {
      await updateMeeting(route.params.mid, submitData)
      ElMessage.success('会议更新成功')
    } else {
      const res = await createMeeting(submitData)
      ElMessage.success('会议创建成功')
      router.push(`/groups/${groupId.value}/meetings/${res.data.id}`)
      return
    }
    
    goBack()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitting.value = false
  }
}

const goBack = () => router.push(`/groups/${groupId.value}`)

onMounted(() => {
  loadUsers()
  loadMeeting()
})
</script>

<style scoped>
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
</style>

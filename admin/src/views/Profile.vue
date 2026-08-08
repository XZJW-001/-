<template>
  <div class="profile-container">
    <div class="page-header">
      <h2>个人中心</h2>
      <p>查看和修改个人信息</p>
    </div>
    
    <el-row :gutter="20">
      <el-col :xs="24" :lg="8">
        <div class="card-container profile-card">
          <el-avatar :size="100" :src="userStore.userInfo.avatar" class="avatar">
            {{ userStore.userName?.charAt(0) }}
          </el-avatar>
          <h3>{{ userStore.userName }}</h3>
          <p class="user-type">{{ getUserTypeText(userStore.userInfo.userType) }}</p>
          <el-descriptions :column="1" class="info-list">
            <el-descriptions-item label="用户名">{{ userStore.userInfo.username }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userStore.userInfo.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ userStore.userInfo.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ getDeptName(userStore.userInfo.deptId) }}</el-descriptions-item>
            <el-descriptions-item label="职位">{{ userStore.userInfo.position || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>
      
      <el-col :xs="24" :lg="16">
        <div class="card-container">
          <h3>修改密码</h3>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" class="password-form">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="submitting">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <div class="card-container">
          <h3>我的签到统计</h3>
          <div class="my-stats">
            <div class="stat-item">
              <div class="stat-value">{{ myStats.totalMeetings || 0 }}</div>
              <div class="stat-label">参加会议</div>
            </div>
            <div class="stat-item success">
              <div class="stat-value">{{ myStats.signedCount || 0 }}</div>
              <div class="stat-label">正常签到</div>
            </div>
            <div class="stat-item warning">
              <div class="stat-value">{{ myStats.lateCount || 0 }}</div>
              <div class="stat-label">迟到签到</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ myStats.attendanceRate || 0 }}%</div>
              <div class="stat-label">出勤率</div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { changePassword } from '@/api/auth'
import { getUserStatistics } from '@/api/statistics'
import { getDeptList } from '@/api/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const pwdFormRef = ref(null)
const submitting = ref(false)
const deptList = ref([])

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const myStats = reactive({
  totalMeetings: 0,
  signedCount: 0,
  lateCount: 0,
  attendanceRate: 0
})

const loadDepts = async () => {
  try {
    const res = await getDeptList()
    deptList.value = res.data || []
  } catch (error) {
    console.error('加载部门列表失败:', error)
  }
}

const loadMyStats = async () => {
  try {
    const res = await getUserStatistics(userStore.userId)
    Object.assign(myStats, res.data)
  } catch (error) {
    console.error('加载个人统计失败:', error)
  }
}

const handleChangePassword = async () => {
  try {
    await pwdFormRef.value.validate()
    submitting.value = true
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    submitting.value = false
  }
}

const getUserTypeText = (type) => ['', '普通用户', '管理员', '会议领导'][type] || '未知'
const getDeptName = (deptId) => {
  const dept = deptList.value.find(d => d.id === deptId)
  return dept ? dept.deptName : '-'
}

onMounted(() => {
  loadDepts()
  loadMyStats()
})
</script>

<style lang="scss" scoped>
.profile-card {
  text-align: center;
  padding: 32px 20px;
  
  .avatar {
    margin-bottom: 16px;
    background: $color-primary;
  }
  
  h3 {
    margin: 0 0 8px;
  }
  
  .user-type {
    color: #909399;
    margin-bottom: 20px;
  }
  
  .info-list {
    text-align: left;
  }
}

.password-form {
  max-width: 400px;
}

.my-stats {
  display: flex;
  gap: 16px;
  
  .stat-item {
    flex: 1;
    text-align: center;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 8px;
    
    .stat-value {
      font-size: 28px;
      font-weight: 600;
      color: $color-primary;
    }
    
    .stat-label {
      margin-top: 8px;
      color: #909399;
      font-size: 13px;
    }
    
    &.success .stat-value { color: #67C23A; }
    &.warning .stat-value { color: #E6A23C; }
  }
}
</style>

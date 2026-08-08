<template>
  <div class="user-list page-container">
    <div class="page-header">
      <div>
        <span class="page-kicker">组织与权限</span>
        <h2>用户管理</h2>
        <p>维护系统用户资料、角色与账号状态</p>
      </div>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
    </div>
    
    <div class="card-container">
      <div class="card-heading">
        <div>
          <h3>用户列表</h3>
          <p>当前共 {{ pagination.total }} 位系统用户</p>
        </div>
        <el-tag type="info" effect="plain">{{ userList.length }} 条当前记录</el-tag>
      </div>

      <div class="filter-panel">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="姓名">
            <el-input v-model="searchForm.realName" placeholder="请输入姓名" clearable class="name-input" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="部门">
            <el-select v-model="searchForm.deptId" placeholder="选择部门" clearable class="filter-select">
              <el-option v-for="dept in deptList" :key="dept.id" :label="dept.deptName || dept.name" :value="dept.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="用户类型">
            <el-select v-model="searchForm.userType" placeholder="全部类型" clearable class="filter-select">
              <el-option label="普通用户" :value="1" />
              <el-option label="管理员" :value="2" />
              <el-option label="会议领导" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <div class="table-shell">
      <el-table :data="userList" v-loading="loading" empty-text="暂无符合条件的用户" stripe>
        <el-table-column label="用户名" :min-width="isMobileTable ? 120 : 130">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="user-initial" :class="`tone-${row.userType || 1}`">{{ getUserInitial(row) }}</span>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" :width="isMobileTable ? 90 : 100" show-overflow-tooltip />
        <el-table-column v-if="!isMobileTable" prop="phone" label="手机号" width="125" />
        <el-table-column v-if="isWideTable" prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
        <el-table-column v-if="isWideTable" prop="position" label="职位" width="100" />
        <el-table-column v-if="!isMobileTable" label="用户类型" width="96">
          <template #default="{ row }">
            <el-tag :type="getUserTypeTag(row.userType)">{{ getUserTypeText(row.userType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="!isMobileTable" label="状态" width="72">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isMobileTable ? 104 : 110" :fixed="isMobileTable ? false : 'right'">
          <template #default="{ row }">
            <el-button type="primary" link @click="showEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>
      
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        class="user-pagination"
        @size-change="loadUsers"
        @current-change="loadUsers"
      />
    </div>
    
    <!-- 用户编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px">
      <el-form ref="formRef" :model="userForm" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="userForm.deptId" placeholder="选择部门" style="width: 100%;">
            <el-option v-for="dept in deptList" :key="dept.id" :label="dept.deptName" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="userForm.position" />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="userForm.userType" style="width: 100%;">
            <el-option label="普通用户" :value="1" />
            <el-option label="管理员" :value="2" />
            <el-option label="会议领导" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser, getDeptList } from '@/api/user'

const loading = ref(false)
const submitting = ref(false)
const userList = ref([])
const deptList = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const editId = ref(null)
const viewportWidth = ref(window.innerWidth)

const isEdit = computed(() => !!editId.value)
const isMobileTable = computed(() => viewportWidth.value <= 768)
const isWideTable = computed(() => viewportWidth.value > 1260)

const searchForm = reactive({
  realName: '',
  deptId: null,
  userType: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const userForm = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  deptId: null,
  position: '',
  userType: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const loadDepts = async () => {
  try {
    const res = await getDeptList()
    deptList.value = res.data || []
  } catch (error) {
    console.error('加载部门列表失败:', error)
  }
}

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    if (searchForm.realName) params.realName = searchForm.realName
    if (searchForm.deptId) params.deptId = searchForm.deptId
    if (searchForm.userType) params.userType = searchForm.userType
    
    const res = await getUserList(params)
    userList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('加载用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadUsers()
}

const handleReset = () => {
  searchForm.realName = ''
  searchForm.deptId = null
  searchForm.userType = null
  handleSearch()
}

const showCreateDialog = () => {
  editId.value = null
  Object.assign(userForm, {
    username: '', password: '', realName: '', phone: '', 
    email: '', deptId: null, position: '', userType: 1
  })
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  editId.value = row.id
  Object.assign(userForm, {
    username: row.username,
    password: '',
    realName: row.realName,
    phone: row.phone,
    email: row.email,
    deptId: row.deptId,
    position: row.position,
    userType: row.userType
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    
    if (isEdit.value) {
      await updateUser(editId.value, userForm)
      ElMessage.success('更新成功')
    } else {
      await createUser(userForm)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除用户 "${row.realName}" 吗？`, '提示', { type: 'error' })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch {}
}

const handleStatusChange = async (row) => {
  try {
    await updateUser(row.id, { status: row.status })
    ElMessage.success('状态更新成功')
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
  }
}

const getUserTypeTag = (type) => ['info', '', 'warning', 'success'][type] || 'info'
const getUserTypeText = (type) => ['', '普通用户', '管理员', '会议领导'][type] || '未知'
const getUserInitial = (user) => (user.realName || user.username || '用').trim().charAt(0).toUpperCase()
const handleViewportResize = () => {
  viewportWidth.value = window.innerWidth
}

onMounted(() => {
  loadDepts()
  loadUsers()
  window.addEventListener('resize', handleViewportResize)
})
onBeforeUnmount(() => window.removeEventListener('resize', handleViewportResize))
</script>

<style lang="scss" scoped>
.user-list {
  min-height: 100%;
}

.page-kicker {
  display: block;
  margin-bottom: 6px;
  color: $color-primary;
  font-size: 11px;
  font-weight: 650;
}

.card-container {
  padding: $spacing-lg;
  border: 1px solid $border-light;
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

.name-input,
.filter-select {
  width: 170px;
}

.table-shell {
  width: 100%;
  overflow: hidden;
  border-radius: $radius-base;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.user-initial {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  flex: 0 0 28px;
  color: $color-primary;
  background: $color-primary-bg;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 650;

  &.tone-2 {
    color: $color-warning;
    background: $color-warning-bg;
  }

  &.tone-3 {
    color: $color-success;
    background: $color-success-bg;
  }
}

.user-pagination {
  justify-content: flex-end;
  margin-top: $spacing-lg;
  overflow-x: auto;
  padding-bottom: 2px;
}

@media (max-width: 768px) {
  .card-container {
    padding: 12px;
  }

  .card-heading {
    align-items: flex-start;
  }

  .search-form {
    :deep(.el-form-item) {
      width: 100%;
      margin-right: 0;
    }

    :deep(.el-form-item__content),
    .el-input,
    .el-select {
      width: 100%;
    }
  }

  .user-pagination {
    justify-content: flex-start;
  }
}
</style>

<template>
  <div class="group-list page-container">
    <div class="page-header">
      <div class="header-left">
        <span class="page-kicker">协作空间</span>
        <h2>群组管理</h2>
        <p>集中管理群组成员、会议安排与签到任务</p>
      </div>
      <div class="header-actions">
        <el-tooltip content="刷新群组" placement="bottom">
          <el-button class="refresh-button" :icon="RefreshRight" circle aria-label="刷新群组" @click="refreshGroups" />
        </el-tooltip>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          创建群组
        </el-button>
      </div>
    </div>

    <!-- 数据概览 -->
    <div class="overview-row" v-if="activeTab === 'my'">
      <div class="overview-card">
        <div class="ov-icon ov-primary"><el-icon><ChatLineRound /></el-icon></div>
        <div class="ov-info">
          <div class="ov-value">{{ myGroups.length }}</div>
          <div class="ov-label">我的群组</div>
        </div>
      </div>
      <div class="overview-card">
        <div class="ov-icon ov-success"><el-icon><UserFilled /></el-icon></div>
        <div class="ov-info">
          <div class="ov-value">{{ totalMembers }}</div>
          <div class="ov-label">总成员数</div>
        </div>
      </div>
      <div class="overview-card">
        <div class="ov-icon ov-warning"><el-icon><Tickets /></el-icon></div>
        <div class="ov-info">
          <div class="ov-value">{{ activeGroups }}</div>
          <div class="ov-label">活跃群组</div>
        </div>
      </div>
    </div>

    <section class="groups-section">
      <el-tabs v-model="activeTab">
        <el-tab-pane name="my">
          <template #label>
            <span class="tab-label">我的群组 <b>{{ myGroups.length }}</b></span>
          </template>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :xl="8" v-for="(group, index) in myGroups" :key="group.id">
              <div class="card-wrap" @click="viewGroup(group)">
                <div class="group-card">
                  <div class="card-top">
                    <div class="group-avatar" :class="`tone-${index % 4}`">{{ getGroupInitial(group.groupName) }}</div>
                    <div class="group-detail">
                      <h3>{{ group.groupName }}</h3>
                      <p>{{ group.description || '暂无描述' }}</p>
                    </div>
                    <el-icon class="arrow"><ArrowRight /></el-icon>
                  </div>
                  <div class="card-foot">
                    <div class="meta-item">
                      <el-icon><User /></el-icon>
                      <span>{{ group.memberCount }} 人</span>
                    </div>
                    <div class="meta-item">
                      <el-icon><Key /></el-icon>
                      <span class="code">{{ group.groupCode }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
            <el-col :span="24" v-if="myGroups.length === 0">
              <div class="empty-panel">
                <el-empty description="暂无群组">
                  <el-button type="primary" @click="showCreateDialog = true">创建第一个群组</el-button>
                </el-empty>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane name="all">
          <template #label>
            <span class="tab-label">全部群组 <b>{{ total }}</b></span>
          </template>
          <div class="search-bar">
            <el-input v-model="searchKeyword" clearable placeholder="搜索群组名称或编号" @keyup.enter="loadGroups">
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" plain @click="loadGroups">查询</el-button>
          </div>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :xl="8" v-for="(group, index) in allGroups" :key="group.id">
              <div class="card-wrap" @click="viewGroup(group)">
                <div class="group-card">
                  <div class="card-top">
                    <div class="group-avatar" :class="`tone-${index % 4}`">{{ getGroupInitial(group.groupName) }}</div>
                    <div class="group-detail">
                      <h3>{{ group.groupName }}</h3>
                      <p>{{ group.description || '暂无描述' }}</p>
                    </div>
                    <el-icon class="arrow"><ArrowRight /></el-icon>
                  </div>
                  <div class="card-foot">
                    <div class="meta-item">
                      <el-icon><User /></el-icon>
                      <span>{{ group.memberCount }} 人</span>
                    </div>
                    <div class="meta-item">
                      <el-icon><Key /></el-icon>
                      <span class="code">{{ group.groupCode }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
          <el-pagination
            v-if="total > 0"
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            @current-change="loadGroups"
            layout="prev, pager, next"
            class="pagination"
          />
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="showCreateDialog" title="创建群组" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="群组名称">
          <el-input v-model="createForm.groupName" placeholder="请输入群组名称" />
        </el-form-item>
        <el-form-item label="群组描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入群组描述" />
        </el-form-item>
        <el-form-item label="最大成员数">
          <el-input-number v-model="createForm.maxMembers" :min="2" :max="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateGroup">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, ArrowRight, Search, User, UserFilled, Key, ChatLineRound, Tickets, RefreshRight } from '@element-plus/icons-vue'
import { createGroup, getMyGroups, getGroupList } from '@/api/group'

const router = useRouter()
const activeTab = ref('my')
const myGroups = ref([])
const allGroups = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showCreateDialog = ref(false)

const createForm = reactive({
  groupName: '',
  description: '',
  maxMembers: 500
})

// 计算属性：统计概览
const totalMembers = computed(() => {
  return myGroups.value.reduce((sum, g) => sum + (g.memberCount || 0), 0)
})

const activeGroups = computed(() => {
  return myGroups.value.filter(g => (g.memberCount || 0) > 0).length
})

const getGroupInitial = (name) => name?.trim().charAt(0).toUpperCase() || '群'

const loadMyGroups = async () => {
  try {
    const res = await getMyGroups()
    myGroups.value = res.data || []
  } catch (error) {
    console.error('加载我的群组失败:', error)
  }
}

const loadGroups = async () => {
  try {
    const res = await getGroupList({
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    allGroups.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载群组列表失败:', error)
  }
}

const refreshGroups = () => {
  if (activeTab.value === 'all') {
    loadGroups()
  } else {
    loadMyGroups()
  }
}

const handleCreateGroup = async () => {
  if (!createForm.groupName) {
    ElMessage.warning('请输入群组名称')
    return
  }
  try {
    await createGroup(createForm)
    ElMessage.success('群组创建成功')
    showCreateDialog.value = false
    createForm.groupName = ''
    createForm.description = ''
    createForm.maxMembers = 500
    loadMyGroups()
  } catch (error) {
    console.error('创建群组失败:', error)
  }
}

const viewGroup = (group) => {
  router.push(`/groups/${group.id}`)
}

onMounted(() => {
  loadMyGroups()
})

watch(activeTab, (tab) => {
  if (tab === 'all' && allGroups.value.length === 0) loadGroups()
})
</script>

<style lang="scss" scoped>
.group-list {
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xl;

  .page-kicker {
    display: block;
    margin-bottom: 6px;
    color: $color-primary;
    font-size: 11px;
    font-weight: 650;
  }

  .header-left {
    h2 {
      margin: 0 0 6px 0;
      font-size: 24px;
      font-weight: 650;
      color: $text-primary;
    }
    p {
      margin: 0;
      font-size: 13px;
      color: $text-secondary;
    }
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.refresh-button {
  width: 34px;
  min-height: 34px;
}

// 数据概览
.overview-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-lg;
}

.overview-card {
  background: $bg-white;
  border-radius: $radius-base;
  padding: $spacing-lg;
  border: 1px solid $border-light;
  display: flex;
  align-items: center;
  gap: $spacing-md;
  box-shadow: $shadow-base;
  transition: border-color 0.16s ease-out, box-shadow 0.16s ease-out;

  &:hover {
    border-color: $border-base;
    box-shadow: $shadow-light;
  }

  .ov-icon {
    width: 48px;
    height: 48px;
    border-radius: $radius-base;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    color: $color-primary;

    &.ov-primary { background: $color-primary-bg; color: $color-primary; }
    &.ov-success { background: $color-success-bg; color: $color-success; }
    &.ov-warning { background: $color-warning-bg; color: $color-warning; }
  }

  .ov-info {
    .ov-value {
      font-size: 24px;
      font-weight: 600;
      color: $text-primary;
      line-height: 1.2;
    }
    .ov-label {
      font-size: 13px;
      color: $text-secondary;
      margin-top: 2px;
    }
  }
}

.groups-section {
  min-height: 320px;

  :deep(.el-tabs__header) {
    margin-bottom: $spacing-lg;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background: $border-light;
  }
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 7px;

  b {
    min-width: 20px;
    height: 20px;
    display: inline-grid;
    place-items: center;
    padding: 0 5px;
    color: $text-secondary;
    background: $border-extra-light;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
  }
}

// 群组卡片
.card-wrap {
  cursor: pointer;
  margin-bottom: $spacing-lg;
}

.group-card {
  min-height: 152px;
  background: $bg-white;
  border: 1px solid $border-light;
  border-radius: $radius-base;
  padding: $spacing-lg;
  box-shadow: $shadow-base;
  transition: border-color 0.16s ease-out, box-shadow 0.16s ease-out;

  .card-top {
    display: flex;
    align-items: flex-start;
    gap: $spacing-md;
    margin-bottom: $spacing-md;
  }

  .card-foot {
    display: flex;
    gap: $spacing-lg;
    padding-top: $spacing-md;
    border-top: 1px dashed $border-lighter;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: $text-regular;

      .el-icon { color: $text-secondary; }

      .code {
        font-family: 'Courier New', monospace;
        color: $color-success;
        font-weight: 600;
      }
    }
  }
}

.card-wrap:hover .group-card {
  box-shadow: $shadow-light;
  border-color: #b9cdfa;
}

.card-wrap .arrow {
  color: $text-placeholder;
  flex-shrink: 0;
  align-self: center;
  transition: $transition-fast;
}

.card-wrap:hover .arrow {
  color: $color-primary;
  transform: translateX(3px);
}

.group-avatar {
  width: 56px;
  height: 56px;
  background: $color-primary-bg;
  border-radius: $radius-base;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-primary;
  font-size: 24px;
  font-weight: 600;
  flex-shrink: 0;
  box-shadow: none;

  &.tone-1 {
    color: $color-success;
    background: $color-success-bg;
  }

  &.tone-2 {
    color: $color-warning;
    background: $color-warning-bg;
  }

  &.tone-3 {
    color: $color-danger;
    background: $color-danger-bg;
  }
}

.group-detail {
  flex: 1;
  min-width: 0;

  h3 {
    margin: 0 0 6px;
    font-size: 16px;
    color: $text-primary;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  p {
    margin: 0;
    color: $text-secondary;
    font-size: 13px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.search-bar {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
  max-width: 520px;

  .el-input {
    flex: 1;
  }
}

.pagination {
  margin-top: $spacing-lg;
  display: flex;
  justify-content: center;
}

.empty-panel {
  min-height: 320px;
  display: grid;
  place-items: center;
  background: $bg-white;
  border: 1px dashed $border-base;
  border-radius: $radius-base;
}

// 响应式
@media (max-width: 768px) {
  .overview-row {
    grid-template-columns: 1fr;
  }

  .page-header {
    align-items: flex-start;

    .header-actions {
      align-self: stretch;
    }
  }

  .search-bar {
    max-width: none;
  }
}
</style>

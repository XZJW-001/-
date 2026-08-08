<template>
  <el-container class="layout-shell" :class="{ 'mobile-menu-open': mobileMenuOpen }">
    <div class="sidebar-scrim" @click="mobileMenuOpen = false"></div>

    <el-aside :width="isCollapse ? '72px' : '236px'" class="sidebar">
      <div class="brand" @click="goHome">
        <div class="brand-mark">
          <el-icon :size="22"><FullScreen /></el-icon>
        </div>
        <div v-show="!isCollapse" class="brand-copy">
          <strong>会议签到中心</strong>
          <span>Meeting Console</span>
        </div>
      </div>

      <div v-show="!isCollapse" class="menu-label">工作空间</div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        :router="true"
        class="menu"
      >
        <template v-for="routeItem in menuRoutes" :key="routeItem.path">
          <el-menu-item
            v-if="!routeItem.meta?.roles || hasRole(routeItem.meta.roles)"
            :index="'/' + routeItem.path"
            @click="mobileMenuOpen = false"
          >
            <el-icon><component :is="routeItem.meta?.icon" /></el-icon>
            <template #title>{{ routeItem.meta?.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="sidebar-footer" :class="{ compact: isCollapse }">
        <span class="health-dot"></span>
        <div v-show="!isCollapse" class="health-copy">
          <strong>服务运行正常</strong>
          <span>版本 1.0.0</span>
        </div>
      </div>
    </el-aside>

    <el-container class="workspace">
      <el-header class="topbar">
        <div class="topbar-left">
          <el-tooltip :content="sidebarToggleText" placement="bottom">
            <button class="icon-button menu-trigger" type="button" @click="toggleSidebar">
              <el-icon :size="19">
                <Expand v-if="isCollapse" />
                <Fold v-else />
              </el-icon>
            </button>
          </el-tooltip>

          <div class="route-context">
            <span class="route-title">{{ currentTitle }}</span>
            <el-breadcrumb separator="/" class="breadcrumb">
              <el-breadcrumb-item :to="{ path: '/dashboard' }">工作台</el-breadcrumb-item>
              <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
                {{ item.meta.title }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </div>

        <div class="topbar-right">
          <el-tooltip content="系统通知" placement="bottom">
            <button class="icon-button" type="button" @click="showNotifications">
              <el-icon :size="18"><Bell /></el-icon>
              <span class="notice-dot"></span>
            </button>
          </el-tooltip>

          <span class="topbar-divider"></span>

          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-menu" type="button">
              <el-avatar :size="34" :src="userStore.userInfo.avatar" class="user-avatar">
                {{ userStore.userName?.charAt(0) }}
              </el-avatar>
              <span class="user-copy">
                <strong>{{ userStore.userName || '用户' }}</strong>
                <small>{{ roleText }}</small>
              </span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const mobileMenuOpen = ref(false)
const isMobile = ref(false)

const activeMenu = computed(() => route.meta?.activeMenu || route.path)
const currentTitle = computed(() => route.meta?.title || '会议签到中心')
const breadcrumbs = computed(() => route.matched.filter(item => item.meta?.title && item.path !== '/dashboard'))
const sidebarToggleText = computed(() => {
  if (isMobile.value) return mobileMenuOpen.value ? '关闭导航' : '打开导航'
  return isCollapse.value ? '展开导航' : '收起导航'
})

const menuRoutes = computed(() => {
  const mainRoute = router.options.routes.find(item => item.path === '/')
  return mainRoute?.children?.filter(item => !item.meta?.hidden) || []
})

const roleText = computed(() => {
  const roleMap = { ADMIN: '系统管理员', SUPER_ADMIN: '超级管理员', LEADER: '会议负责人', USER: '普通成员' }
  return roleMap[userStore.roleCode] || '普通成员'
})

const hasRole = (roles) => !roles || roles.includes(userStore.roleCode)

const toggleSidebar = () => {
  if (window.innerWidth <= 900) {
    mobileMenuOpen.value = !mobileMenuOpen.value
    return
  }
  isCollapse.value = !isCollapse.value
}

const handleResize = () => {
  isMobile.value = window.innerWidth <= 900
  if (!isMobile.value) mobileMenuOpen.value = false
}

const goHome = () => router.push('/dashboard')
const showNotifications = () => ElMessage.info('当前没有新的系统通知')

const handleCommand = async (command) => {
  if (command === 'profile') {
    router.push('/profile')
    return
  }

  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
      userStore.logout()
      router.push('/login')
    } catch {
      // 用户取消退出
    }
  }
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => window.removeEventListener('resize', handleResize))
</script>

<style lang="scss" scoped>
.layout-shell {
  height: 100vh;
  background: $bg-base;
}

.sidebar {
  position: relative;
  z-index: 30;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #17202d;
  border-right: 0;
  box-shadow: 1px 0 0 rgba(12, 20, 31, 0.42);
  transition: width 0.24s cubic-bezier(0.2, 0, 0, 1);
}

.brand {
  display: flex;
  align-items: center;
  min-height: 72px;
  padding: 0 16px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  flex: 0 0 40px;
  color: #fff;
  background: $color-primary;
  border-radius: $radius-base;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-left: 12px;
  white-space: nowrap;

  strong {
    color: #fff;
    font-size: 15px;
    font-weight: 650;
  }

  span {
    color: #8fa0b5;
    font-size: 10px;
    text-transform: uppercase;
  }
}

.menu-label {
  padding: 22px 20px 8px;
  color: #718096;
  font-size: 11px;
  font-weight: 600;
}

.menu {
  flex: 1;
  padding: 0 10px;
  overflow-y: auto;
  background: transparent;
  border-right: 0;

  :deep(.el-menu-item) {
    height: 42px;
    margin: 4px 0;
    padding: 0 14px !important;
    color: #aeb9c8;
    border-radius: 6px;

    .el-icon {
      width: 20px;
      margin-right: 12px;
      font-size: 18px;
    }

    &:hover {
      color: #fff;
      background: rgba(255, 255, 255, 0.07);
    }

    &.is-active {
      color: #fff;
      background: $color-primary;
      box-shadow: inset 3px 0 0 #8fb4ff;
    }
  }

  &.el-menu--collapse {
    width: auto;
    padding: 10px;

    :deep(.el-menu-item) {
      justify-content: center;
      padding: 0 !important;

      .el-icon {
        margin: 0;
      }
    }
  }
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 70px;
  margin: 10px;
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);

  &.compact {
    justify-content: center;
  }
}

.health-dot {
  width: 8px;
  height: 8px;
  flex: 0 0 8px;
  background: #35c38f;
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(53, 195, 143, 0.12);
}

.health-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  white-space: nowrap;

  strong {
    color: #c9d2de;
    font-size: 11px;
    font-weight: 500;
  }

  span {
    color: #718096;
    font-size: 10px;
  }
}

.workspace {
  min-width: 0;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid $border-light;
}

.topbar-left,
.topbar-right,
.user-menu {
  display: flex;
  align-items: center;
}

.topbar-left {
  min-width: 0;
  gap: 14px;
}

.route-context {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 3px;
}

.route-title {
  color: $text-primary;
  font-size: 14px;
  font-weight: 650;
  line-height: 1;
}

.breadcrumb {
  font-size: 11px;

  :deep(.el-breadcrumb__inner) {
    color: $text-placeholder;
    font-weight: 400;
  }
}

.topbar-right {
  gap: 10px;
}

.icon-button {
  position: relative;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  padding: 0;
  color: $text-secondary;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;

  &:hover {
    color: $color-primary;
    background: $color-primary-bg;
    border-color: #d9e5ff;
  }
}

.notice-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 6px;
  height: 6px;
  background: $color-danger;
  border: 1px solid #fff;
  border-radius: 50%;
}

.topbar-divider {
  width: 1px;
  height: 24px;
  background: $border-light;
}

.user-menu {
  gap: 9px;
  min-width: 0;
  padding: 4px 6px;
  color: inherit;
  background: transparent;
  border: 0;
  border-radius: 6px;
  cursor: pointer;

  &:hover {
    background: $bg-soft;
  }
}

.user-avatar {
  color: #fff;
  font-weight: 600;
  background: $color-success;
}

.user-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;

  strong {
    color: $text-primary;
    font-size: 13px;
    font-weight: 600;
  }

  small {
    color: $text-secondary;
    font-size: 10px;
  }
}

.user-arrow {
  color: $text-placeholder;
  font-size: 12px;
}

.main-content {
  padding: 0;
  overflow: auto;
  background: $bg-base;
}

.sidebar-scrim {
  display: none;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.14s ease-out;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    width: 236px !important;
    transform: translateX(-100%);
    box-shadow: 16px 0 40px rgba(16, 24, 40, 0.18);
  }

  .layout-shell.mobile-menu-open .sidebar {
    transform: translateX(0);
  }

  .sidebar-scrim {
    position: fixed;
    inset: 0;
    z-index: 20;
    display: block;
    visibility: hidden;
    background: rgba(16, 24, 40, 0.42);
    opacity: 0;
    transition: opacity 0.18s ease-out;
  }

  .layout-shell.mobile-menu-open .sidebar-scrim {
    visibility: visible;
    opacity: 1;
  }

  .brand-copy,
  .menu-label,
  .health-copy {
    display: flex !important;
  }

  .menu.el-menu--collapse {
    width: auto;
  }

  .topbar {
    padding: 0 14px;
  }

  .breadcrumb,
  .topbar-divider {
    display: none;
  }
}

@media (max-width: 560px) {
  .topbar {
    height: 58px;
  }

  .route-title {
    max-width: 140px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .user-copy,
  .user-arrow {
    display: none;
  }

  .topbar-right {
    gap: 2px;
  }
}
</style>

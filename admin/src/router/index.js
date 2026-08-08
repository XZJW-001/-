import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'Monitor' }
      },
      {
        path: 'groups',
        name: 'GroupList',
        component: () => import('@/views/group/GroupList.vue'),
        meta: { title: '群聊', icon: 'ChatLineRound' }
      },
      {
        path: 'groups/:id',
        name: 'GroupDetail',
        component: () => import('@/views/group/GroupDetail.vue'),
        meta: { title: '群工作台', hidden: true, activeMenu: '/groups' }
      },
      {
        path: 'groups/:gid/meetings/create',
        name: 'GroupMeetingCreate',
        component: () => import('@/views/meeting/MeetingCreate.vue'),
        meta: { title: '创建会议', hidden: true, activeMenu: '/groups' }
      },
      {
        path: 'groups/:gid/meetings/:mid',
        name: 'GroupMeetingDetail',
        component: () => import('@/views/meeting/MeetingDetail.vue'),
        meta: { title: '会议详情', hidden: true, activeMenu: '/groups' }
      },
      {
        path: 'approvals',
        name: 'Approvals',
        component: () => import('@/views/approval/ApprovalCenter.vue'),
        meta: { title: '审批中心', icon: 'DocumentChecked', roles: ['ADMIN', 'LEADER'] }
      },
      {
        path: 'checkins',
        name: 'CheckInList',
        component: () => import('@/views/checkin/CheckInList.vue'),
        meta: { title: '签到记录', icon: 'Checked' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/Statistics.vue'),
        meta: { title: '数据统计', icon: 'TrendCharts' }
      },
      {
        path: 'innovation',
        name: 'InnovationCenter',
        component: () => import('@/views/innovation/InnovationCenter.vue'),
        meta: { title: '智慧运营', icon: 'MagicStick', roles: ['ADMIN', 'SUPER_ADMIN', 'LEADER'] }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', icon: 'UserFilled', roles: ['ADMIN'] }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  document.title = to.meta.title ? `${to.meta.title} - 会议签到系统` : '会议签到系统'
  
  if (to.meta.requiresAuth !== false) {
    if (!userStore.token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    
    if (to.meta.roles && to.meta.roles.length > 0) {
      const hasRole = to.meta.roles.includes(userStore.roleCode)
      if (!hasRole) {
        ElMessage.error('您没有访问此页面的权限')
        next({ path: '/groups' })
        return
      }
    }
  } else {
    if (to.path === '/login' && userStore.token) {
      next({ path: '/dashboard' })
      return
    }
  }
  
  next()
})

export default router

import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    roleCode: localStorage.getItem('roleCode') || ''
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    userName: (state) => state.userInfo?.realName || state.userInfo?.username || '',
    isAdmin: (state) => state.roleCode === 'ADMIN' || state.roleCode === 'SUPER_ADMIN',
    isLeader: (state) => state.roleCode === 'LEADER',
    userType: (state) => state.userInfo?.userType || 0
  },
  
  actions: {
    async login(loginForm) {
      const res = await request.post('/auth/login', loginForm)
      const data = res.data
      
      this.token = data.token
      this.userId = data.user.id
      this.userInfo = data.user
      this.roleCode = data.user.roleCode
      
      localStorage.setItem('token', data.token)
      localStorage.setItem('userId', data.user.id)
      localStorage.setItem('userInfo', JSON.stringify(data.user))
      localStorage.setItem('roleCode', data.user.roleCode)
      
      return data
    },
    
    async getUserInfo() {
      try {
        const res = await request.get('/auth/userInfo')
        this.userInfo = res.data
        return res.data
      } catch (error) {
        console.error('获取用户信息失败:', error)
        throw error
      }
    },
    
    logout() {
      this.token = ''
      this.userId = ''
      this.userInfo = {}
      this.roleCode = ''
      
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('roleCode')
    }
  }
})

// pages/profile/profile.js
const app = getApp()

Page({
  data: {
    userInfo: null,
    myStats: null,
    proxyPendingCount: 0,
    loading: true
  },

  onLoad() {
    this.loadData()
  },

  onShow() {
    this.loadData()
  },

  async loadData() {
    this.setData({ loading: true })
    try {
      const userRes = await app.request({
        url: '/auth/userInfo',
        method: 'GET'
      })
      
      const [statsRes, proxyRes] = await Promise.all([
        app.request({
          url: `/statistics/user/${userRes.data.id}`,
          method: 'GET'
        }).catch((error) => {
          console.error('加载统计数据失败:', error)
          return null
        }),
        app.request({
          url: '/checkin/proxy/applications/my',
          method: 'GET',
          silent: true
        }).catch((error) => {
          console.error('加载代签申请数量失败:', error)
          return null
        })
      ])

      const proxyApplications = proxyRes?.data || []
      
      this.setData({
        userInfo: userRes.data,
        myStats: statsRes?.data || { totalMeetings: 0, signedCount: 0, lateCount: 0, attendanceRate: 0 },
        proxyPendingCount: proxyApplications.filter((item) => item.status === 0).length
      })
    } catch (error) {
      console.error('加载用户信息失败:', error)
      if (error.code === 401) {
        wx.reLaunch({ url: '/pages/login/login' })
      }
    } finally {
      this.setData({ loading: false })
    }
  },

  handleLogin() {
    wx.reLaunch({ url: '/pages/login/login' })
  },

  handleLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('userId')
          wx.removeStorageSync('userInfo')
          app.globalData.token = ''
          app.globalData.userId = ''
          app.globalData.userInfo = null
          wx.reLaunch({ url: '/pages/login/login' })
        }
      }
    })
  },

  goToStatistics() {
    wx.navigateTo({ url: '/pages/statistics/statistics' })
  },

  goToSmartCenter() {
    wx.navigateTo({ url: '/pages/smart/smart' })
  },

  goToMyRecords() {
    wx.navigateTo({ url: '/pages/checkin/checkin' })
  },

  goToProxyApplications() {
    wx.navigateTo({ url: '/pages/proxy/proxy' })
  },

  getUserTypeText(type) {
    return ['', '普通用户', '管理员', '会议领导'][type] || '未知'
  }
})

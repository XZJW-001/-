const app = getApp()

Page({
  data: {
    profile: {},
    reminders: [],
    offlineCount: 0,
    loading: true
  },

  onLoad() {
    this.loadData()
  },

  onShow() {
    this.updateOfflineCount()
  },

  onPullDownRefresh() {
    this.loadData().finally(() => wx.stopPullDownRefresh())
  },

  async loadData() {
    this.setData({ loading: true })
    try {
      const [profileRes, reminderRes] = await Promise.all([
        app.request({ url: '/innovation/profile/me', method: 'GET' }),
        app.request({ url: '/innovation/reminders/my', method: 'GET' })
      ])
      this.setData({
        profile: profileRes.data || {},
        reminders: reminderRes.data || []
      })
      this.updateOfflineCount()
    } catch (error) {
      console.error('加载智慧中心失败:', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  updateOfflineCount() {
    const queue = wx.getStorageSync('offlineCheckinQueue') || []
    this.setData({ offlineCount: queue.length })
  },

  async syncOffline() {
    if (!this.data.offlineCount) {
      wx.showToast({ title: '没有待同步记录', icon: 'none' })
      return
    }
    wx.showLoading({ title: '同步中...' })
    await app.syncOfflineCheckins()
    wx.hideLoading()
    this.updateOfflineCount()
  },

  async openReminder(e) {
    const item = e.currentTarget.dataset.item
    if (!item.read) {
      try {
        await app.request({ url: `/innovation/reminders/${item.id}/read`, method: 'PUT', silent: true })
      } catch (error) {
        console.warn('标记提醒失败:', error)
      }
    }
    if (item.meetingId) {
      wx.navigateTo({ url: `/pages/checkin/checkin?meetingId=${item.meetingId}` })
    }
  },

  methodName(value) {
    return ({ qrcode: '二维码', photo: '拍照', gesture: '手势', location: '定位' })[value] || '二维码'
  }
})

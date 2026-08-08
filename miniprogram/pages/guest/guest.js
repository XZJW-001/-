const app = getApp()

Page({
  data: {
    token: '',
    meeting: null,
    form: { guestName: '', organization: '', phone: '' },
    loading: true,
    submitted: false,
    result: null
  },

  onLoad(options) {
    const token = options.token ? decodeURIComponent(options.token) : ''
    this.setData({ token })
    this.loadInvite(token)
  },

  async loadInvite(token) {
    if (!token) {
      wx.showToast({ title: '访客邀请无效', icon: 'none' })
      return
    }
    try {
      const res = await app.request({ url: `/innovation/public/guest/${token}`, method: 'GET' })
      this.setData({ meeting: res.data })
    } catch (error) {
      console.error('加载访客邀请失败:', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  inputName(e) { this.setData({ 'form.guestName': e.detail.value }) },
  inputOrg(e) { this.setData({ 'form.organization': e.detail.value }) },
  inputPhone(e) { this.setData({ 'form.phone': e.detail.value }) },

  async submit() {
    const form = this.data.form
    if (!form.guestName.trim()) {
      wx.showToast({ title: '请填写姓名', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      const res = await app.request({
        url: `/innovation/public/guest/${this.data.token}/checkin`,
        method: 'POST',
        data: { ...form, deviceInfo: app.globalData.deviceId }
      })
      this.setData({ submitted: true, result: res.data })
    } catch (error) {
      console.error('访客签到失败:', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  close() {
    wx.navigateBack({ fail: () => wx.switchTab({ url: '/pages/scan/scan' }) })
  }
})

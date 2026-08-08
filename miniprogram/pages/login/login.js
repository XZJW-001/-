// pages/login/login.js
const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  async handleLogin() {
    const { username, password } = this.data
    
    if (!username || !password) {
      wx.showToast({
        title: '请输入用户名和密码',
        icon: 'none'
      })
      return
    }

    this.setData({ loading: true })
    try {
      const res = await app.login(username, password)
      
      // 保存登录信息
      wx.setStorageSync('token', res.data.token)
      wx.setStorageSync('userId', res.data.user.id)
      wx.setStorageSync('userInfo', res.data.user)
      
      app.globalData.token = res.data.token
      app.globalData.userId = res.data.user.id
      app.globalData.userInfo = res.data.user
      
      wx.showToast({
        title: '登录成功',
        icon: 'success'
      })
      
      setTimeout(() => {
        wx.switchTab({
          url: '/pages/group/group'
        })
      }, 1500)
    } catch (error) {
      // 错误已在app.js处理
    } finally {
      this.setData({ loading: false })
    }
  },

  handleWechatLogin() {
    wx.showToast({
      title: '微信登录开发中',
      icon: 'none'
    })
  }
})

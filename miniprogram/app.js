// app.js
App({
  onLaunch() {
    // 自动检测环境：开发者工具用localhost，真机用局域网IP
    try {
      const systemInfo = wx.getSystemInfoSync()
      const platform = systemInfo.platform
      const isDevtools = platform === 'devtools' || platform === 'windows' || platform === 'mac'
      this.globalData.baseUrl = isDevtools ? this.globalData.localIp : this.globalData.lanIp
      this.globalData.systemInfo = systemInfo
    } catch (e) {
      console.error('获取系统信息失败:', e)
      this.globalData.baseUrl = this.globalData.lanIp
    }

    // 检查登录状态
    const token = wx.getStorageSync('token')
    const userId = wx.getStorageSync('userId')
    if (token) {
      this.globalData.token = token
    }
    if (userId) {
      this.globalData.userId = userId
    }

    this.globalData.deviceId = this.getDeviceId()
    wx.onNetworkStatusChange((res) => {
      if (res.isConnected) this.syncOfflineCheckins()
    })
    if (token) this.syncOfflineCheckins()
  },

  onShow() {
    if (this.globalData.token) this.syncOfflineCheckins()
  },
  
  // 全局请求方法
  request(options) {
    return new Promise((resolve, reject) => {
      const url = this.globalData.baseUrl + options.url
      
      wx.request({
        url: url,
        method: options.method || 'GET',
        data: options.data || {},
        header: {
          'Content-Type': 'application/json',
          'Authorization': this.globalData.token ? `Bearer ${this.globalData.token}` : ''
        },
        success: (res) => {
          if (res.statusCode === 401 || res.statusCode === 403) {
            // 未授权或禁止访问，清除token并跳转登录
            wx.removeStorageSync('token')
            wx.removeStorageSync('userId')
            this.globalData.token = ''
            this.globalData.userId = ''
            wx.showModal({
              title: '登录已过期',
              content: '请重新登录',
              showCancel: false,
              success: () => {
                wx.reLaunch({ url: '/pages/login/login' })
              }
            })
            reject({ code: res.statusCode, message: '未授权' })
          } else if (res.statusCode === 200 && res.data) {
            if (res.data.code === 200) {
              resolve(res.data)
            } else {
              if (!options.silent) {
                wx.showToast({
                  title: res.data.message || '请求失败',
                  icon: 'none',
                  duration: 2000
                })
              }
              reject(res.data)
            }
          } else {
            if (!options.silent) {
              wx.showToast({
                title: `请求失败(${res.statusCode})`,
                icon: 'none',
                duration: 2000
              })
            }
            reject(res.data || { code: res.statusCode })
          }
        },
        fail: (err) => {
          console.error('网络请求失败:', err)
          if (!options.silent) {
            wx.showModal({
              title: '网络错误',
              content: `无法连接到服务器，请检查网络设置或后端服务是否启动。\n错误信息: ${err.errMsg || '未知错误'}`,
              showCancel: false,
              confirmText: '我知道了'
            })
          }
          reject(err)
        }
      })
    })
  },
  
  // 登录方法
  login(username, password) {
    return this.request({
      url: '/auth/login',
      method: 'POST',
      data: { username, password }
    })
  },

  getDeviceId() {
    let deviceId = wx.getStorageSync('meetingDeviceId')
    if (!deviceId) {
      deviceId = `MP-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
      wx.setStorageSync('meetingDeviceId', deviceId)
    }
    return deviceId
  },

  queueOfflineCheckin(meetingId, data) {
    const queue = wx.getStorageSync('offlineCheckinQueue') || []
    queue.push({ meetingId, data, queuedAt: Date.now() })
    wx.setStorageSync('offlineCheckinQueue', queue.slice(-50))
    return queue.length
  },

  async syncOfflineCheckins() {
    if (this.syncingOffline || !this.globalData.token) return
    const queue = wx.getStorageSync('offlineCheckinQueue') || []
    if (!queue.length) return
    this.syncingOffline = true
    try {
      const res = await this.request({
        url: '/innovation/offline/sync',
        method: 'POST',
        data: { items: queue },
        silent: true
      })
      const records = res.data?.records || []
      const succeeded = new Set(records.filter(item => item.success).map(item => item.clientRequestId))
      const remaining = queue.filter(item => !succeeded.has(item.data.clientRequestId))
      wx.setStorageSync('offlineCheckinQueue', remaining)
      if (succeeded.size) {
        wx.showToast({ title: `已同步${succeeded.size}条签到`, icon: 'success' })
      }
    } catch (error) {
      console.warn('离线签到暂未同步:', error)
    } finally {
      this.syncingOffline = false
    }
  },
  
  globalData: {
    // 真机访问需使用电脑局域网IP，开发环境在开发者工具中可用localhost
    // 如果在开发者工具中测试，会自动切换为localhost
    baseUrl: '',
    // cpolar内网穿透公网地址（手机用流量也能访问）
    lanIp: 'https://23e9ff4e.r38.cpolar.top/api',
    localIp: 'http://localhost:8080/api',
    token: '',
    userId: '',
    userInfo: null,
    systemInfo: null,
    deviceId: '',
    signMethods: {
      qrcode: '二维码',
      photo: '拍照签到',
      gesture: '手势签到',
      location: '定位签到'
    }
  }
})


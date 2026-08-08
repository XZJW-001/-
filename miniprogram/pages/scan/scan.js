// pages/scan/scan.js
const app = getApp()

Page({
  data: {
    scanning: false,
    scanResult: null
  },

  onLoad(options) {
    // 扫码页面入口
  },

  // 开始扫码
  startScan() {
    wx.scanCode({
      onlyFromCamera: false,
      scanType: ['qrCode'],
      success: (res) => {
        this.setData({
          scanResult: res.result,
          scanning: false
        })
        this.handleScanResult(res.result)
      },
      fail: (err) => {
        console.error('扫码失败:', err)
        this.setData({ scanning: false })
        if (err.errMsg !== 'cancel') {
          wx.showToast({
            title: '扫码失败',
            icon: 'none'
          })
        }
      }
    })
  },

  // 处理扫码结果
  handleScanResult(result) {
    try {
      if (result.startsWith('MEETING_DYNAMIC:')) {
        const ticket = result.substring('MEETING_DYNAMIC:'.length)
        wx.navigateTo({
          url: `/pages/checkin/checkin?dynamicTicket=${encodeURIComponent(ticket)}`
        })
        return
      }

      if (result.startsWith('MEETING_GUEST:')) {
        const guestToken = result.substring('MEETING_GUEST:'.length)
        wx.navigateTo({
          url: `/pages/guest/guest?token=${encodeURIComponent(guestToken)}`
        })
        return
      }

      let token = result
      // 解析二维码内容格式: MEETING_CHECKIN:{token}
      if (result.startsWith('MEETING_CHECKIN:')) {
        token = result.substring('MEETING_CHECKIN:'.length)
      } else if (result.startsWith('http')) {
        // URL格式
        const queryPart = result.split('?')[1] || ''
        const params = {}
        queryPart.split('&').forEach(p => {
          const [k, v] = p.split('=')
          if (k) params[k] = decodeURIComponent(v || '')
        })
        token = params['token'] || params['t'] || result
      } else if (result.startsWith('GROUP_JOIN:')) {
        // 群聊邀请码
        wx.showModal({
          title: '群聊邀请',
          content: '这是一个群聊邀请码，请使用添加群聊功能扫描',
          showCancel: false
        })
        return
      }
      
      if (!token || token === result) {
        wx.showModal({
          title: '扫码成功',
          content: '未识别的二维码格式，是否继续？',
          success: (res) => {
            if (res.confirm) {
              wx.navigateTo({
                url: `/pages/checkin/checkin?token=${encodeURIComponent(token)}`
              })
            }
          }
        })
        return
      }
      
      wx.showModal({
        title: '扫码成功',
        content: '是否前往签到页面？',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({
              url: `/pages/checkin/checkin?token=${encodeURIComponent(token)}`
            })
          }
        }
      })
    } catch (e) {
      wx.navigateTo({
        url: `/pages/checkin/checkin?token=${encodeURIComponent(result)}`
      })
    }
  },

  // 打开相机扫码
  startCameraScan() {
    this.setData({ scanning: true })
    this.startScan()
  },

  // 跳转到指定签到方式
  goToCheckin(e) {
    const method = e.currentTarget.dataset.method
    wx.navigateTo({
      url: `/pages/checkin/checkin?method=${method}`
    })
  },

  // 重置
  reset() {
    this.setData({
      scanning: false,
      scanResult: null
    })
  }
})

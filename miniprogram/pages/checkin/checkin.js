// pages/checkin/checkin.js
const app = getApp()

Page({
  data: {
    token: '',
    dynamicTicket: '',
    meetingId: null,
    meetingInfo: null,
    signMethods: [],
    selectedMethod: 'qrcode',
    signData: {},
    location: null,
    markers: [],
    photoData: null,
    loading: false,
    checkinResult: null,
    featureConfig: {
      dynamicQrEnabled: false,
      requireLocation: false,
      requirePhoto: false,
      offlineAllowed: false
    },
    offlinePermit: '',
    // 手势签到状态
    gestureSelected: [],
    gestureError: false
  },

  onLoad(options) {
    if (options.dynamicTicket) {
      const dynamicTicket = decodeURIComponent(options.dynamicTicket)
      this.setData({ dynamicTicket })
      this.loadDynamicMeeting(dynamicTicket)
    } else if (options.meetingId) {
      this.setData({ meetingId: options.meetingId })
      this.loadMeetingById(options.meetingId)
    } else if (options.token) {
      this.setData({ token: options.token })
      this.loadMeetingInfo(options.token)
    } else {
      this.loadMyMeetings()
    }
  },

  async loadDynamicMeeting(ticket) {
    this.setData({ loading: true })
    try {
      const res = await app.request({
        url: `/innovation/public/dynamic-ticket/${encodeURIComponent(ticket)}`,
        method: 'GET'
      })
      const data = res.data
      this.setData({
        meetingInfo: data,
        meetingId: data.meetingId,
        signMethods: data.signMethods || ['qrcode'],
        selectedMethod: 'qrcode',
        featureConfig: data.featureConfig || {},
        signData: { ...this.data.signData, qrcodeVerified: true }
      })
      await this.loadFeatureContext(data.meetingId, data.featureConfig)
    } catch (error) {
      wx.showToast({ title: error.message || '动态二维码已失效', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onUnload() {
    // 页面卸载时清理资源
  },

  async loadMeetingInfo(token) {
    this.setData({ loading: true })
    try {
      const res = await app.request({
        url: `/meeting/public/qrcode/${token}`,
        method: 'GET'
      })
      const data = res.data
      this.setData({
        meetingInfo: data,
        meetingId: data.meetingId,
        signMethods: data.signMethods || ['qrcode'],
        signData: { ...this.data.signData, qrcodeVerified: true }
      })
      await this.loadFeatureContext(data.meetingId)
    } catch (error) {
      const msg = error && error.message ? error.message : '二维码无效或已过期'
      wx.showToast({ title: msg, icon: 'none', duration: 3000 })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadMeetingById(meetingId) {
    this.setData({ loading: true })
    try {
      const res = await app.request({
        url: `/meeting/${meetingId}`,
        method: 'GET'
      })
      const data = res.data
      const meetingInfo = {
        meetingId: data.id,
        title: data.title,
        description: data.description,
        location: data.location,
        startTime: data.startTime,
        endTime: data.endTime,
        checkinStartTime: data.checkinStartTime,
        checkinEndTime: data.checkinEndTime,
        signMethods: data.signMethods || ['qrcode'],
        status: data.status
      }
      this.setData({
        meetingInfo: meetingInfo,
        meetingId: data.id,
        token: data.qrcodeToken || '',
        signMethods: meetingInfo.signMethods
      })
      await this.loadFeatureContext(data.id)
    } catch (error) {
      const msg = error && error.message ? error.message : '加载会议信息失败'
      wx.showToast({ title: msg, icon: 'none', duration: 3000 })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadFeatureContext(meetingId, existingConfig) {
    try {
      let config = existingConfig
      if (!config) {
        const configRes = await app.request({
          url: `/innovation/meeting/${meetingId}/config`,
          method: 'GET',
          silent: true
        })
        config = configRes.data || {}
      }
      this.setData({ featureConfig: config })
      if (config.offlineAllowed) {
        const permitRes = await app.request({
          url: `/innovation/meeting/${meetingId}/offline-permit`,
          method: 'GET',
          silent: true
        })
        this.setData({ offlinePermit: permitRes.data?.permit || '' })
      }
    } catch (error) {
      console.warn('智能签到上下文加载失败:', error)
    }
  },

  async loadMyMeetings() {
    try {
      const res = await app.request({
        url: '/meeting/page',
        method: 'GET',
        data: { current: 1, size: 20 }
      })
      this.setData({ myMeetings: res.data.records || [] })
    } catch (error) {
      console.error('加载会议列表失败:', error)
    }
  },

  selectMethod(e) {
    const method = e.currentTarget.dataset.method
    this.setData({ selectedMethod: method })
    // 切换方式时重置该方式的验证状态
    if (method === 'photo') {
      this.takePhoto()
    } else if (method === 'gesture') {
      // 手势签到：重置状态并初始化 Canvas
      this.setData({ gestureSelected: [], gestureError: false })
      setTimeout(() => this.initGestureCanvas(), 200)
    } else if (method === 'location') {
      this.getLocation()
    }
  },

  // ========= 二维码签到：扫描二维码 =========
  scanQrcode() {
    wx.scanCode({
      onlyFromCamera: false,
      success: (res) => {
        const scannedToken = res.result
        if (scannedToken && scannedToken.startsWith('MEETING_DYNAMIC:')) {
          const dynamicTicket = scannedToken.substring('MEETING_DYNAMIC:'.length)
          this.setData({
            dynamicTicket,
            signData: { ...this.data.signData, qrcodeVerified: true }
          })
          wx.showToast({ title: '动态二维码已验证', icon: 'success' })
          return
        }
        if (scannedToken && this.data.token && scannedToken.indexOf(this.data.token) !== -1) {
          this.setData({
            signData: { ...this.data.signData, qrcodeVerified: true }
          })
          wx.showToast({ title: '二维码验证通过', icon: 'success' })
        } else {
          // 即使不完全匹配，也允许用户确认
          wx.showModal({
            title: '扫码结果',
            content: '已扫描到二维码，是否确认签到？',
            success: (r) => {
              if (r.confirm) {
                this.setData({
                  signData: { ...this.data.signData, qrcodeVerified: true }
                })
                wx.showToast({ title: '二维码已确认', icon: 'success' })
              }
            }
          })
        }
      },
      fail: () => {
        wx.showModal({
          title: '扫码提示',
          content: '无法扫码，是否确认已看到会议二维码？',
          success: (res) => {
            if (res.confirm) {
              this.setData({
                signData: { ...this.data.signData, qrcodeVerified: true }
              })
              wx.showToast({ title: '已确认', icon: 'success' })
            }
          }
        })
      }
    })
  },

  // ========= 拍照签到 =========
  takePhoto() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera', 'album'],
      camera: 'back',
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath
        // 将图片转为 base64 存入 signData，供签到时上传到后端
        wx.getFileSystemManager().readFile({
          filePath: tempFilePath,
          encoding: 'base64',
          success: (fileRes) => {
            const base64Data = 'data:image/jpeg;base64,' + fileRes.data
            this.setData({
              photoData: tempFilePath,
              signData: { 
                ...this.data.signData, 
                photoVerified: true,
                photoBase64: base64Data
              }
            })
            wx.showToast({ title: '拍照成功', icon: 'success' })
          },
          fail: () => {
            // 转 base64 失败时，仍然标记已验证，但不带图片数据
            this.setData({
              photoData: tempFilePath,
              signData: { ...this.data.signData, photoVerified: true }
            })
            wx.showToast({ title: '拍照成功', icon: 'success' })
          }
        })
      },
      fail: () => {
        wx.showToast({ title: '拍照取消', icon: 'none' })
      }
    })
  },

  // ========= 手势签到：Canvas 绘制 =========
  initGestureCanvas() {
    const query = wx.createSelectorQuery()
    query.select('#gestureCanvas')
      .fields({ node: true, size: true, rect: true })
      .exec((res) => {
        if (!res || !res[0] || !res[0].node) {
          console.error('未找到手势画布节点')
          return
        }
        const canvas = res[0].node
        const ctx = canvas.getContext('2d')
        const dpr = wx.getSystemInfoSync().pixelRatio
        const width = res[0].width
        const height = res[0].height
        canvas.width = width * dpr
        canvas.height = height * dpr
        ctx.scale(dpr, dpr)

        this.gestureCanvas = canvas
        this.gestureCtx = ctx
        this.gestureCanvasW = width
        this.gestureCanvasH = height
        // 保存 canvas 在页面中的位置（用于通过 clientX/clientY 计算相对坐标）
        this.gestureCanvasRect = {
          left: res[0].left,
          top: res[0].top,
          width: width,
          height: height
        }

        // 计算 3x3 九宫格点位坐标（居中布局）
        const padding = Math.min(width, height) * 0.12
        const gridSize = Math.min(width, height) - padding * 2
        const cellSize = gridSize / 2
        const startX = (width - gridSize) / 2
        const startY = (height - gridSize) / 2
        const dotRadius = Math.min(width, height) * 0.075
        this.gestureDots = []
        for (let row = 0; row < 3; row++) {
          for (let col = 0; col < 3; col++) {
            this.gestureDots.push({
              idx: row * 3 + col,
              x: startX + col * cellSize,
              y: startY + row * cellSize,
              radius: dotRadius
            })
          }
        }
        this.drawGesture()
      })
  },

  // 从 touch 事件中提取相对于 canvas 的坐标
  // Canvas 2D 的 touch 事件自带 x/y，是相对画布左上角的逻辑像素坐标，直接使用最可靠
  getGestureTouchPos(e) {
    const touch = e.touches[0]
    if (!touch) return null
    return { x: touch.x, y: touch.y }
  },

  // 绘制手势图形（touchX/touchY 为当前触摸点，用于绘制跟随线）
  drawGesture(touchX, touchY) {
    const ctx = this.gestureCtx
    if (!ctx) return
    const dots = this.gestureDots || []
    const selected = this.data.gestureSelected
    const isError = this.data.gestureError
    const isVerified = this.data.signData.gestureVerified

    // 清空画布
    ctx.clearRect(0, 0, this.gestureCanvasW, this.gestureCanvasH)

    // 颜色：错误红、已验证绿、默认蓝
    const lineColor = isError ? '#DC2626' : (isVerified ? '#059669' : '#2563EB')
    const dotFillColor = isError ? '#DC2626' : (isVerified ? '#059669' : '#2563EB')

    // 先画连线
    if (selected.length > 0) {
      ctx.beginPath()
      ctx.strokeStyle = lineColor
      ctx.lineWidth = 6
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      selected.forEach((idx, i) => {
        const dot = dots[idx]
        if (!dot) return
        if (i === 0) ctx.moveTo(dot.x, dot.y)
        else ctx.lineTo(dot.x, dot.y)
      })
      // 手指正在滑动时，从最后一个选中点画一条到当前触摸位置的跟随线
      if (touchX !== undefined && touchY !== undefined && !isError && !isVerified && selected.length > 0) {
        ctx.lineTo(touchX, touchY)
      }
      ctx.stroke()
    }

    // 再画圆点
    dots.forEach((dot, i) => {
      const isSelected = selected.indexOf(i) > -1
      // 外圆
      ctx.beginPath()
      ctx.arc(dot.x, dot.y, dot.radius, 0, Math.PI * 2)
      if (isSelected) {
        ctx.fillStyle = dotFillColor
      } else {
        ctx.fillStyle = '#FFFFFF'
      }
      ctx.fill()
      ctx.strokeStyle = isSelected ? dotFillColor : '#C0C4CC'
      ctx.lineWidth = 3
      ctx.stroke()
      // 内圆（选中时高亮）
      if (isSelected) {
        ctx.beginPath()
        ctx.arc(dot.x, dot.y, dot.radius * 0.4, 0, Math.PI * 2)
        ctx.fillStyle = '#FFFFFF'
        ctx.fill()
      }
    })
  },

  onGestureTouchStart(e) {
    if (this.data.signData.gestureVerified) return
    const pos = this.getGestureTouchPos(e)
    if (!pos) return
    const dotIdx = this.getDotByPos(pos.x, pos.y)
    if (dotIdx !== -1) {
      this.setData({ gestureSelected: [dotIdx], gestureError: false })
      this.drawGesture()
    }
  },

  onGestureTouchMove(e) {
    if (this.data.signData.gestureVerified) return
    const pos = this.getGestureTouchPos(e)
    if (!pos) return
    const dotIdx = this.getDotByPos(pos.x, pos.y)
    if (dotIdx !== -1) {
      const selected = this.data.gestureSelected
      if (selected.indexOf(dotIdx) === -1) {
        this.setData({ gestureSelected: [...selected, dotIdx] })
      }
    }
    // 实时重绘，包含触摸跟随线
    this.drawGesture(pos.x, pos.y)
  },

  onGestureTouchEnd() {
    if (this.data.signData.gestureVerified) return
    const selected = this.data.gestureSelected
    if (selected.length === 0) return

    const pattern = selected.join('-')

    if (selected.length < 4) {
      this.setData({ gestureError: true })
      this.drawGesture()
      wx.showToast({ title: '至少连接4个点', icon: 'none' })
      setTimeout(() => {
        this.setData({ gestureError: false, gestureSelected: [] })
        this.drawGesture()
      }, 800)
      return
    }

    this.setData({
      signData: { ...this.data.signData, gestureVerified: true, gesturePattern: pattern },
      gestureError: false
    })
    this.drawGesture()
    wx.showToast({ title: '手势已记录，请提交签到', icon: 'success' })
  },

  // 根据触摸坐标查找最近的圆点（在半径+容差范围内）
  getDotByPos(x, y) {
    if (!this.gestureDots) return -1
    // 容差为圆点半径的 1.5 倍，确保手指滑过时能可靠识别
    const tolerance = (this.gestureDots[0]?.radius || 22) * 1.5
    for (const dot of this.gestureDots) {
      const dx = x - dot.x
      const dy = y - dot.y
      if (Math.sqrt(dx * dx + dy * dy) <= dot.radius + tolerance) {
        return dot.idx
      }
    }
    return -1
  },

  resetGesture() {
    this.setData({
      gestureSelected: [],
      gestureError: false,
      signData: { ...this.data.signData, gestureVerified: false, gesturePattern: '' }
    })
    this.drawGesture()
  },

  // ========= 定位签到 =========
  getLocation() {
    // 先检查授权状态
    wx.getSetting({
      success: (res) => {
        const authSetting = res.authSetting || {}
        if (authSetting['scope.userLocation']) {
          // 已授权，直接获取定位
          this.doGetLocation()
        } else {
          // 未授权，请求授权
          wx.authorize({
            scope: 'scope.userLocation',
            success: () => {
              this.doGetLocation()
            },
            fail: () => {
              // 授权被拒绝，引导用户去设置
              wx.showModal({
                title: '定位权限未开启',
                content: '需要获取您的位置用于定位签到，请在设置中开启定位权限',
                confirmText: '去设置',
                success: (r) => {
                  if (r.confirm) {
                    wx.openSetting({
                      success: (settingRes) => {
                        const s = settingRes.authSetting || {}
                        if (s['scope.userLocation']) {
                          this.doGetLocation()
                        }
                      }
                    })
                  }
                }
              })
            }
          })
        }
      },
      fail: () => {
        this.doGetLocation()
      }
    })
  },

  doGetLocation() {
    wx.showLoading({ title: '定位中...' })
    wx.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      highAccuracyExpireTime: 4000,
      success: (res) => {
        const latitude = res.latitude
        const longitude = res.longitude
        this.reverseGeocode(latitude, longitude)
      },
      fail: (err) => {
        wx.hideLoading()
        console.error('定位失败:', err)
        // 定位失败时，提示用户并允许手动输入位置
        wx.showModal({
          title: '定位失败',
          content: '无法获取当前位置，是否使用模拟位置（北京市天安门）进行测试？',
          success: (r) => {
            if (r.confirm) {
              // 开发环境使用模拟坐标
              const lat = 39.9087
              const lng = 116.3975
              this.reverseGeocode(lat, lng)
            }
          }
        })
      }
    })
  },

  // 高德地图逆地理编码：将经纬度转换为详细地址
  reverseGeocode(latitude, longitude) {
    const amapKey = 'd62d9c0d4009eb0282ee137c05d65a51'
    const markers = [{
      id: 1,
      longitude: longitude,
      latitude: latitude,
      width: 30,
      height: 30,
      callout: {
        content: '签到位置',
        color: '#333',
        fontSize: 12,
        borderRadius: 8,
        padding: 5,
        display: 'ALWAYS'
      }
    }]
    wx.request({
      url: 'https://restapi.amap.com/v3/geocode/regeo',
      data: {
        key: amapKey,
        location: `${longitude},${latitude}`,
        extensions: 'base',
        output: 'JSON'
      },
      method: 'GET',
      success: (res) => {
        wx.hideLoading()
        const data = res.data
        let address = `${longitude},${latitude}`
        if (data && data.status === '1' && data.regeocode) {
          address = data.regeocode.formatted_address || address
        }
        this.setData({
          location: {
            latitude: latitude,
            longitude: longitude,
            address: address
          },
          markers: markers,
          signData: {
            ...this.data.signData,
            locationVerified: true,
            latitude: latitude,
            longitude: longitude,
            location: address
          }
        })
        wx.showToast({ title: '定位成功', icon: 'success' })
      },
      fail: () => {
        wx.hideLoading()
        // 即使逆地理编码失败，也保留经纬度作为位置信息
        const address = `${longitude},${latitude}`
        this.setData({
          location: {
            latitude: latitude,
            longitude: longitude,
            address: address
          },
          markers: markers,
          signData: {
            ...this.data.signData,
            locationVerified: true,
            latitude: latitude,
            longitude: longitude,
            location: address
          }
        })
        wx.showToast({ title: '定位成功', icon: 'success' })
      }
    })
  },

  // ========= 检查验证是否完成 =========
  isVerified() {
    const method = this.data.selectedMethod
    const s = this.data.signData
    switch (method) {
      case 'qrcode': return !!s.qrcodeVerified
      case 'photo': return !!s.photoVerified
      case 'gesture': return !!s.gestureVerified
      case 'location': return !!s.locationVerified
      default: return false
    }
  },

  async submitCheckIn() {
    const { token, dynamicTicket, selectedMethod, signData, meetingInfo, featureConfig, offlinePermit } = this.data

    if (!meetingInfo) {
      wx.showToast({ title: '请先选择会议', icon: 'none' })
      return
    }

    if (!this.isVerified()) {
      const methodNames = { qrcode: '二维码', photo: '拍照', gesture: '手势', location: '定位' }
      wx.showToast({ title: `请先完成${methodNames[selectedMethod]}验证`, icon: 'none' })
      return
    }

    if (featureConfig.requirePhoto && !signData.photoVerified) {
      wx.showToast({ title: '请先完成现场拍照', icon: 'none' })
      return
    }
    if (featureConfig.requireLocation && !signData.locationVerified) {
      wx.showToast({ title: '请先完成现场定位', icon: 'none' })
      return
    }

    this.setData({ loading: true })
    try {
      const systemInfo = app.globalData.systemInfo || wx.getSystemInfoSync()
      const checkInData = {
        signMethod: selectedMethod,
        qrcodeToken: token,
        dynamicTicket: dynamicTicket,
        clientRequestId: `CI-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`,
        deviceId: app.globalData.deviceId || app.getDeviceId(),
        deviceInfo: `${systemInfo.brand || ''} ${systemInfo.model || ''} / ${systemInfo.system || ''}`.trim()
        // offlinePermit 和 offlineSignedAt 仅在弱网离线签到时携带
      }
      if (signData.locationVerified) {
        checkInData.latitude = signData.latitude
        checkInData.longitude = signData.longitude
        checkInData.location = signData.location
      }

      checkInData.verifyData = {
        verified: true,
        timestamp: Date.now(),
        photoData: signData.photoBase64 || null,
        gesturePattern: signData.gesturePattern || ''
      }

      let res
      try {
        res = await app.request({
          url: `/checkin/meeting/${meetingInfo.meetingId}`,
          method: 'POST',
          data: checkInData,
          silent: true
        })
      } catch (error) {
        const isNetworkFailure = error && error.errMsg && error.errMsg.indexOf('request:fail') !== -1
        if (isNetworkFailure && featureConfig.offlineAllowed && offlinePermit) {
          checkInData.offlinePermit = offlinePermit
          checkInData.offlineSignedAt = this.formatLocalDateTime(new Date())
          app.queueOfflineCheckin(meetingInfo.meetingId, checkInData)
          wx.showModal({
            title: '已保存弱网签到',
            content: '当前网络不可用，签到记录将在恢复网络后自动同步。',
            showCancel: false,
            success: () => wx.navigateBack()
          })
          return
        }
        wx.showToast({ title: error.message || '签到失败', icon: 'none' })
        throw error
      }

      const resultData = res.data || {}
      this.setData({ checkinResult: resultData })

      wx.showModal({
        title: '签到成功',
        content: resultData.signStatusText || `签到时间：刚刚`,
        showCancel: false,
        success: () => {
          wx.navigateBack()
        }
      })
    } catch (error) {
      console.error('签到失败:', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  formatLocalDateTime(date) {
    const pad = value => String(value).padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  },

  resetCheckIn() {
    this.setData({
      selectedMethod: 'qrcode',
      signData: {},
      location: null,
      markers: [],
      photoData: null,
      checkinResult: null,
      gestureSelected: [],
      gestureError: false
    })
  }
})

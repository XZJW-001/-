// pages/meeting/detail.js
const app = getApp()

Page({
  data: {
    mode: 'view', // view | create
    meetingId: null,
    groupId: null,
    meeting: null,
    attendees: [],
    statistics: null,
    loading: true,
    // 创建表单
    form: {
      title: '',
      description: '',
      location: '',
      startDate: '',
      startTimeOnly: '',
      endDate: '',
      endTimeOnly: '',
      checkinMode: 'auto',
      checkinStartOffset: 30,
      checkinEndOffset: 15,
      checkinStartDate: '',
      checkinStartTimeOnly: '',
      checkinEndDate: '',
      checkinEndTimeOnly: '',
      lateTime: 15,
      signMethods: ['qrcode']
    },
    methodOptions: [
      { value: 'qrcode', label: '二维码', checked: true },
      { value: 'location', label: '定位', checked: false },
      { value: 'photo', label: '拍照', checked: false },
      { value: 'gesture', label: '手势', checked: false }
    ],
    submitting: false
  },

  onLoad(options) {
    const mode = options.mode || 'view'
    this.setData({
      mode,
      meetingId: options.id,
      groupId: options.groupId || null
    })
    if (mode === 'create') {
      this.initCreateForm()
    } else if (options.id && options.id !== 'undefined') {
      this.loadData()
    }
  },

  onShow() {
    if (this.data.mode === 'view' && this.data.meetingId && this.data.meetingId !== 'undefined') {
      this.loadData()
    }
  },

  // 初始化创建表单默认时间（拆分为日期+时间两个字段）
  initCreateForm() {
    const pad = n => String(n).padStart(2, '0')
    const now = new Date()
    const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000)
    const end = new Date(tomorrow.getTime() + 60 * 60 * 1000)
    const checkStart = new Date(tomorrow.getTime() - 10 * 60 * 1000)
    const checkEnd = new Date(tomorrow.getTime() + 30 * 60 * 1000)
    const ymd = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
    const hm = (d) => `${pad(d.getHours())}:${pad(d.getMinutes())}`

    this.setData({
      'form.startDate': ymd(tomorrow),
      'form.startTimeOnly': hm(tomorrow),
      'form.endDate': ymd(end),
      'form.endTimeOnly': hm(end),
      'form.checkinStartDate': ymd(checkStart),
      'form.checkinStartTimeOnly': hm(checkStart),
      'form.checkinEndDate': ymd(checkEnd),
      'form.checkinEndTimeOnly': hm(checkEnd),
      loading: false
    })
  },

  async loadData() {
    this.setData({ loading: true })
    try {
      const id = this.data.meetingId
      const [meetingRes, statsRes] = await Promise.all([
        app.request({ url: `/meeting/${id}`, method: 'GET' }),
        app.request({ url: `/statistics/meeting/${id}`, method: 'GET' })
      ])
      this.setData({
        meeting: meetingRes.data,
        statistics: statsRes.data
      })
    } catch (error) {
      console.error('加载会议详情失败:', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  // 文本输入
  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  // 日期变化
  onDateChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  // 时间变化
  onTimeOnlyChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  // 迟到分钟
  onLateTimeChange(e) {
    this.setData({ 'form.lateTime': e.detail.value })
  },

  // 签到方式切换
  toggleMethod(e) {
    const value = e.currentTarget.dataset.value
    const options = this.data.methodOptions.map(o => {
      if (o.value === value) o.checked = !o.checked
      return o
    })
    const signMethods = options.filter(o => o.checked).map(o => o.value)
    this.setData({ methodOptions: options, 'form.signMethods': signMethods })
  },

  // 切换签到时间模式
  switchCheckinMode(e) {
    this.setData({ 'form.checkinMode': e.currentTarget.dataset.mode })
  },

  // 调整偏移量
  adjustOffset(e) {
    const field = e.currentTarget.dataset.field
    const delta = parseInt(e.currentTarget.dataset.delta)
    let val = this.data.form[field] + delta
    if (val < 0) val = 0
    if (val > 180) val = 180
    this.setData({ [`form.${field}`]: val })
  },

  // 组合日期+时间（返回空格分隔格式，匹配后端 @JsonFormat）
  combineDateTime(date, time) {
    if (!date || !time) return null
    return `${date} ${time}:00`
  },

  // 格式化 Date 对象为 yyyy-MM-dd HH:mm:ss
  formatDateTimeStr(d) {
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  },

  // 提交创建
  async submitCreate() {
    const { form, groupId } = this.data
    if (!form.title) {
      wx.showToast({ title: '请输入会议标题', icon: 'none' })
      return
    }
    if (!form.startDate || !form.startTimeOnly) {
      wx.showToast({ title: '请选择会议开始时间', icon: 'none' })
      return
    }
    if (!form.endDate || !form.endTimeOnly) {
      wx.showToast({ title: '请选择会议结束时间', icon: 'none' })
      return
    }

    // 根据签到模式计算签到时间
    let checkinStartTime = null
    let checkinEndTime = null
    if (form.checkinMode === 'auto') {
      // 快捷模式：基于会议开始时间偏移
      const meetingStart = this.combineDateTime(form.startDate, form.startTimeOnly)
      if (!meetingStart) {
        wx.showToast({ title: '请先选择会议开始时间', icon: 'none' })
        return
      }
      const startMs = new Date(meetingStart.replace(/-/g, '/')).getTime()
      checkinStartTime = this.formatDateTimeStr(new Date(startMs - form.checkinStartOffset * 60000))
      checkinEndTime = this.formatDateTimeStr(new Date(startMs + form.checkinEndOffset * 60000))
    } else {
      // 自定义模式
      if (!form.checkinStartDate || !form.checkinStartTimeOnly) {
        wx.showToast({ title: '请选择签到开始时间', icon: 'none' })
        return
      }
      if (!form.checkinEndDate || !form.checkinEndTimeOnly) {
        wx.showToast({ title: '请选择签到结束时间', icon: 'none' })
        return
      }
      checkinStartTime = this.combineDateTime(form.checkinStartDate, form.checkinStartTimeOnly)
      checkinEndTime = this.combineDateTime(form.checkinEndDate, form.checkinEndTimeOnly)
    }

    if (form.signMethods.length === 0) {
      wx.showToast({ title: '请至少选择一种签到方式', icon: 'none' })
      return
    }
    if (!groupId) {
      wx.showToast({ title: '缺少群组信息', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    try {
      const payload = {
        title: form.title,
        description: form.description,
        location: form.location,
        startTime: this.combineDateTime(form.startDate, form.startTimeOnly),
        endTime: this.combineDateTime(form.endDate, form.endTimeOnly),
        checkinStartTime: checkinStartTime,
        checkinEndTime: checkinEndTime,
        lateTime: parseInt(form.lateTime) || 15,
        signMethods: form.signMethods,
        groupId: parseInt(groupId)
      }
      await app.request({
        url: '/meeting',
        method: 'POST',
        data: payload
      })
      wx.showToast({ title: '创建成功', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({
          url: `/pages/group/detail?id=${groupId}`
        })
      }, 800)
    } catch (error) {
      console.error('创建会议失败:', error)
    } finally {
      this.setData({ submitting: false })
    }
  },

  cancelCreate() {
    wx.navigateBack()
  },

  goToCheckIn() {
    const meeting = this.data.meeting
    if (meeting && meeting.qrcodeToken) {
      wx.navigateTo({
        url: `/pages/checkin/checkin?token=${meeting.qrcodeToken}`
      })
    } else {
      wx.showToast({
        title: '二维码未生成',
        icon: 'none'
      })
    }
  },

  shareMeeting() {
    const meeting = this.data.meeting
    wx.showShareMenu({
      withShareTicket: true
    })
    wx.showModal({
      title: '分享会议',
      content: `邀请参会人员扫码签到：${meeting.qrcodeToken}`,
      confirmText: '复制链接',
      success: (res) => {
        if (res.confirm) {
          wx.setClipboardData({
            data: `${app.globalData.baseUrl}/meeting/checkin?token=${meeting.qrcodeToken}`,
            success: () => {
              wx.showToast({
                title: '链接已复制',
                icon: 'success'
              })
            }
          })
        }
      }
    })
  },

  onShareAppMessage() {
    const meeting = this.data.meeting
    return {
      title: `邀请参加：${meeting?.title}`,
      path: `/pages/checkin/checkin?token=${meeting?.qrcodeToken}`
    }
  }
})

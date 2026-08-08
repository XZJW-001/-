// pages/group/detail.js
const app = getApp()

const AVATAR_COLORS = ['#2563EB', '#0F766E', '#7C3AED', '#475569', '#DC2626', '#D97706', '#059669', '#9333EA', '#0891B2', '#64748B']
const AVATAR_GRADIENTS = [
  '#2563EB',
  '#0F766E',
  '#7C3AED',
  '#475569',
  '#D97706'
]

const STATUS_TEXT = ['草稿','已发布','进行中','已结束']
const CHECKIN_STATUS_TEXT = ['未知','正常','迟到','缺勤','补签','代签']
const METHOD_TEXT = { qrcode:'二维码', photo:'拍照', gesture:'手势', location:'定位', makeup:'补签', proxy:'代签' }

function getAvatarColor(uid) {
  return AVATAR_COLORS[Number(uid || 0) % AVATAR_COLORS.length]
}
function getAvatarChar(name) {
  return (name || '?').charAt(0)
}
function pad2(n) { return n < 10 ? '0'+n : ''+n }
function formatShort(t) {
  if (!t) return '-'
  // 兼容数组格式 [year, month, day, hour, minute]
  if (Array.isArray(t)) {
    if (t.length < 5) return '-'
    return pad2(t[1]) + '-' + pad2(t[2]) + ' ' + pad2(t[3]) + ':' + pad2(t[4])
  }
  // 兼容字符串格式
  if (typeof t === 'string') {
    const s = t.replace('T', ' ').replace(/-/g, '/')
    const d = new Date(s)
    if (isNaN(d.getTime())) return t
    return pad2(d.getMonth()+1)+'-'+pad2(d.getDate())+' '+pad2(d.getHours())+':'+pad2(d.getMinutes())
  }
  return '-'
}

function formatMsgTime(t) {
  if (!t) return ''
  if (Array.isArray(t)) {
    if (t.length < 5) return ''
    return pad2(t[3])+':'+pad2(t[4])
  }
  if (typeof t === 'string') {
    const s = t.replace('T', ' ').replace(/-/g,'/')
    const d = new Date(s)
    if (isNaN(d.getTime())) return ''
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const msgDay = new Date(d.getFullYear(), d.getMonth(), d.getDate())
    if (today.getTime() === msgDay.getTime()) {
      return pad2(d.getHours())+':'+pad2(d.getMinutes())
    }
    return pad2(d.getMonth()+1)+'-'+pad2(d.getDate())+' '+pad2(d.getHours())+':'+pad2(d.getMinutes())
  }
  return ''
}

Page({
  data: {
    groupId: null,
    group: null,
    members: [],
    meetings: [],
    messages: [],
    myCheckinRecords: [],
    groupInitial: '',
    avatarGradient: AVATAR_GRADIENTS[0],

    activeSegment: 'chat',
    statusText: STATUS_TEXT,

    userId: null,
    userName: '',
    avatarChar: '',
    selfAvatarBg: AVATAR_COLORS[0],

    isOwner: false,
    isManager: false,

    // 聊天
    inputText: '',
    sending: false,
    polling: false,
    scrollIntoId: '',
    lastPollId: 0,
    pollTimer: null,

    // 工具弹窗
    pickMeetingShow: false,
    pickMode: 'card',
    pickedIdx: -1,

    qrcodeShow: false,
    groupQrcodeImg: '',
    moreMenuShow: false
  },

  onLoad(options) {
    const groupId = options.id
    const userInfo = wx.getStorageSync('userInfo') || {}
    this.setData({
      groupId: Number(groupId),
      userId: Number(userInfo.id || wx.getStorageSync('userId') || 0),
      userName: userInfo.realName || userInfo.username || '我',
      avatarChar: getAvatarChar(userInfo.realName || userInfo.username),
      selfAvatarBg: getAvatarColor(userInfo.id || wx.getStorageSync('userId') || 0),
      avatarGradient: AVATAR_GRADIENTS[Number(groupId) % AVATAR_GRADIENTS.length]
    })
    this.loadAll()
    this.startPolling()
  },

  onShow() {
    this.loadAll()
  },

  onUnload() {
    this.stopPolling()
  },

  onPullDownRefresh() {
    this.loadAll().then(() => wx.stopPullDownRefresh())
  },

  // ========= 加载 =========
  async loadAll() {
    try {
      const [groupRes, membersRes] = await Promise.all([
        app.request({ url: `/group/${this.data.groupId}`, method: 'GET' }),
        app.request({ url: `/group/${this.data.groupId}/members`, method: 'GET' })
      ])
      const group = groupRes.data
      const members = (membersRes.data || []).map(m => ({
        ...m,
        avatarBg: getAvatarColor(m.userId),
        avatarChar: getAvatarChar(m.userName)
      }))
      const me = members.find(x => Number(x.userId) === this.data.userId)
      const myRole = me ? me.role : 0
      const isOwner = Number(group.ownerId) === this.data.userId
      const uInfo = wx.getStorageSync('userInfo') || {}
      const userType = Number(uInfo.userType || 0)
      const isManager = isOwner || myRole >= 2 || userType === 2 || userType === 3

      this.setData({
        group,
        groupInitial: (group.groupName || '?').charAt(0),
        members,
        isOwner,
        isManager
      })
      await this.loadMeetings()
      this.loadMessages()
    } catch (e) {
      console.error(e)
    }
  },

  async loadMeetings() {
    try {
      const res = await app.request({
        url: '/meeting/page',
        method: 'GET',
        data: { groupId: this.data.groupId, current: 1, size: 50 }
      })
      const list = (res.data?.records || []).map(m => ({
        ...m,
        startTimeText: formatShort(m.startTime)
      }))
      this.setData({ meetings: list })
    } catch (e) {
      console.error('加载会议失败', e)
    }
  },

  async loadMessages() {
    try {
      const res = await app.request({
        url: `/group/message/${this.data.groupId}`,
        method: 'GET',
        data: { limit: 150 }
      })
      const msgs = (res.data || []).map(m => this.processMsg(m))
      const maxId = msgs.length ? Math.max(...msgs.map(m=>Number(m.id))) : 0
      this.setData({ messages: msgs, lastPollId: maxId })
      this.scrollToBottom()
    } catch (e) {
      console.error('加载消息失败', e)
    }
  },

  async loadMyCheckinRecords() {
    try {
      const res = await app.request({ url: '/checkin/my/records', method: 'GET' })
      // 过滤属于本群会议的记录
      const meetingIds = this.data.meetings.map(m=>Number(m.id))
      const list = (res.data || [])
        .filter(r => meetingIds.includes(Number(r.meetingId)))
        .map(r => {
          const mt = this.data.meetings.find(x => Number(x.id) === Number(r.meetingId))
          const st = Number(r.signStatus)
          return {
            ...r,
            meetingTitle: mt ? mt.title : r.meetingTitle || '会议',
            signStatusText: CHECKIN_STATUS_TEXT[st] || '未知',
            signMethodText: METHOD_TEXT[r.signMethod] || r.signMethod || '-'
          }
        })
      this.setData({ myCheckinRecords: list })
    } catch (e) {
      console.error(e)
    }
  },

  processMsg(m) {
    const extraObj = this.parseExtra(m.extra)
    if (extraObj.startTime) extraObj.startTimeText = formatShort(extraObj.startTime)
    if (extraObj.checkinEndTime) extraObj.checkinEndTimeText = formatShort(extraObj.checkinEndTime)
    if (typeof extraObj.status === 'number') extraObj.statusText = STATUS_TEXT[extraObj.status] || '未知'
    // 格式化签到方式文本
    if (Array.isArray(extraObj.signMethods) && extraObj.signMethods.length) {
      extraObj.signMethodsText = extraObj.signMethods.map(method => METHOD_TEXT[method] || method).join('、')
    } else {
      extraObj.signMethodsText = '二维码'
    }
    const uid = Number(m.userId)
    return {
      ...m,
      userId: uid,
      extraObj,
      avatarBg: getAvatarColor(uid),
      avatarChar: getAvatarChar(m.userName),
      timeText: formatMsgTime(m.createTime)
    }
  },

  parseExtra(extra) {
    if (!extra) return {}
    try {
      return typeof extra === 'string' ? JSON.parse(extra) : extra
    } catch { return {} }
  },

  // ========= 轮询 =========
  startPolling() {
    this.stopPolling()
    this.data.pollTimer = setInterval(() => this.doPoll(), 3500)
  },
  stopPolling() {
    if (this.data.pollTimer) {
      clearInterval(this.data.pollTimer)
      this.data.pollTimer = null
    }
  },
  async doPoll() {
    if (this.data.activeSegment !== 'chat') return
    const lastId = this.data.lastPollId
    if (!lastId) return
    try {
      this.setData({ polling: true })
      const res = await app.request({
        url: `/group/message/${this.data.groupId}/latest`,
        method: 'GET',
        data: { lastId }
      })
      const arr = res.data || []
      if (arr.length) {
        const msgs = arr.map(m => this.processMsg(m))
        const newMax = Math.max(lastId, ...msgs.map(m=>Number(m.id)))
        this.setData({
          messages: this.data.messages.concat(msgs),
          lastPollId: newMax
        })
        this.scrollToBottom()
      }
    } catch (e) { /* ignore */ } finally {
      this.setData({ polling: false })
    }
  },

  scrollToBottom() {
    setTimeout(() => {
      this.setData({ scrollIntoId: 'scroll-bottom' })
    }, 50)
  },

  // ========= Tab =========
  switchSegment(e) {
    const seg = e.currentTarget.dataset.segment
    this.setData({ activeSegment: seg })
    if (seg === 'checkin' && this.data.myCheckinRecords.length === 0) {
      this.loadMyCheckinRecords()
    }
  },

  // ========= 聊天输入 =========
  onInput(e) {
    this.setData({ inputText: e.detail.value })
  },

  async sendMessage() {
    const text = (this.data.inputText || '').trim()
    if (!text || this.data.sending) return
    this.setData({ sending: true })
    try {
      // 先本地展示
      const tempId = -Date.now()
      const tempMsg = this.processMsg({
        id: tempId, userId: this.data.userId, type: 'text',
        content: text, userName: this.data.userName, createTime: new Date().toISOString()
      })
      const nextMsgs = this.data.messages.concat([tempMsg])
      this.setData({ messages: nextMsgs, inputText: '' })
      this.scrollToBottom()

      await app.request({
        url: `/group/message/${this.data.groupId}/send`,
        method: 'POST',
        data: { content: text }
      })
      // 同步最新
      const lastId = this.data.lastPollId
      const res = await app.request({
        url: `/group/message/${this.data.groupId}/latest`,
        method: 'GET',
        data: { lastId }
      })
      const arr = res.data || []
      const base = this.data.messages.filter(m => Number(m.id) > 0)
      const add = arr.map(m => this.processMsg(m))
      const all = base.concat(add)
      const newMax = all.length ? Math.max(...all.map(m=>Number(m.id))) : 0
      this.setData({ messages: all, lastPollId: newMax })
      this.scrollToBottom()
    } catch (e) {
      console.error(e)
      wx.showToast({ title: '发送失败', icon: 'none' })
      // 回滚
      const filtered = this.data.messages.filter(m => Number(m.id) > 0)
      this.setData({ messages: filtered })
    } finally {
      this.setData({ sending: false })
    }
  },

  // ========= 工具栏 =========
  handleTool(e) {
    const tool = e.currentTarget.dataset.tool
    if (!this.data.isManager && (tool === 'checkin' || tool === 'card' || tool === 'create')) {
      wx.showToast({ title: '无权限', icon: 'none' })
      return
    }
    if (tool === 'checkin' || tool === 'card') {
      if (this.data.meetings.length === 0) {
        wx.showToast({ title: '请先创建会议', icon: 'none' })
        return
      }
      this.setData({ pickMode: tool, pickMeetingShow: true, pickedIdx: -1 })
    } else if (tool === 'create') {
      wx.navigateTo({ url: `/pages/meeting/detail?groupId=${this.data.groupId}&mode=create` })
    } else if (tool === 'qr') {
      this.showQrcode()
    }
  },

  pickMeetingItem(e) {
    this.setData({ pickedIdx: Number(e.currentTarget.dataset.idx) })
  },
  closePickModal() {
    this.setData({ pickMeetingShow: false })
  },
  async confirmPickMeeting() {
    const idx = this.data.pickedIdx
    if (idx < 0) return
    const meeting = this.data.meetings[idx]
    const url = this.data.pickMode === 'checkin'
      ? `/group/message/${this.data.groupId}/checkin-card`
      : `/group/message/${this.data.groupId}/meeting-card`
    try {
      await app.request({ url, method: 'POST', data: { meetingId: meeting.id } })
      wx.showToast({ title: '已发送', icon: 'success' })
      this.setData({ pickMeetingShow: false })
      this.loadMessages()
    } catch (e) {
      console.error(e)
    }
  },

  // ========= 二维码 =========
  async showQrcode() {
    try {
      const res = await app.request({
        url: `/qrcode/group/${this.data.groupId}`,
        method: 'GET',
        data: { t: Date.now() }
      })
      const d = res.data || res
      const img = typeof d === 'string' ? d : (d.image || d.url || d.img)
      this.setData({ groupQrcodeImg: img, qrcodeShow: true })
    } catch (e) {
      console.error(e)
      wx.showToast({ title: '生成失败', icon: 'none' })
    }
  },
  closeQrcode() { this.setData({ qrcodeShow: false }) },

  // ========= 更多操作 =========
  showMoreMenu() {
    this.setData({ moreMenuShow: true })
  },
  closeMoreMenu() {
    this.setData({ moreMenuShow: false })
  },
  confirmClearMessages() {
    this.setData({ moreMenuShow: false })
    wx.showModal({
      title: '清空聊天记录',
      content: '确定清空本群所有聊天记录吗？此操作不可恢复！',
      confirmColor: '#F56C6C',
      success: async (res) => {
        if (!res.confirm) return
        wx.showLoading({ title: '清空中...' })
        try {
          await app.request({
            url: `/group/message/${this.data.groupId}/clear`,
            method: 'DELETE'
          })
          wx.hideLoading()
          wx.showToast({ title: '已清空', icon: 'success' })
          this.loadMessages()
        } catch (err) {
          wx.hideLoading()
          wx.showToast({ title: err.message || '清空失败', icon: 'none' })
        }
      }
    })
  },
  confirmClearMeetings() {
    this.setData({ moreMenuShow: false })
    wx.showModal({
      title: '清空会议记录',
      content: '确定清空本群所有会议记录吗？相关签到记录也将删除，此操作不可恢复！',
      confirmColor: '#F56C6C',
      success: async (res) => {
        if (!res.confirm) return
        wx.showLoading({ title: '清空中...' })
        try {
          await app.request({
            url: `/meeting/group/${this.data.groupId}/clear`,
            method: 'DELETE'
          })
          wx.hideLoading()
          wx.showToast({ title: '已清空', icon: 'success' })
          this.loadMeetings()
          this.loadMessages()
        } catch (err) {
          wx.hideLoading()
          wx.showToast({ title: err.message || '清空失败', icon: 'none' })
        }
      }
    })
  },

  // ========= 会议操作 =========
  openMeeting(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/meeting/detail?id=${id}&groupId=${this.data.groupId}` })
  },
  stopBubbleOpenCheckin(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/checkin/checkin?meetingId=${id}` })
  },
  confirmDeleteMeeting(e) {
    const id = e.currentTarget.dataset.id
    const title = e.currentTarget.dataset.title
    wx.showModal({
      title: '删除确认',
      content: `确定删除会议「${title}」吗？相关签到记录也将删除。`,
      confirmColor: '#F56C6C',
      success: async (res) => {
        if (!res.confirm) return
        wx.showLoading({ title: '删除中...' })
        try {
          await app.request({ url: `/meeting/${id}`, method: 'DELETE' })
          wx.hideLoading()
          wx.showToast({ title: '已删除', icon: 'success' })
          this.loadMeetings()
          this.loadMessages()
        } catch (err) {
          wx.hideLoading()
          wx.showToast({ title: err.message || '删除失败', icon: 'none' })
        }
      }
    })
  },
  openMeetingFromCard(e) {
    const extra = this.parseExtra(e.currentTarget.dataset.extra)
    if (extra.meetingId) {
      wx.navigateTo({ url: `/pages/meeting/detail?id=${extra.meetingId}&groupId=${this.data.groupId}` })
    }
  },
  openCheckinFromCard(e) {
    const extra = e.currentTarget.dataset.extraObj || this.parseExtra(e.currentTarget.dataset.extra)
    const meetingId = extra.meetingId
    if (!meetingId) {
      wx.showToast({ title: '无法获取会议信息', icon: 'none' })
      return
    }
    // 跳转到签到页面，由签到页面根据会议配置的签到方式展示对应UI
    wx.navigateTo({ url: `/pages/checkin/checkin?meetingId=${meetingId}` })
  },

  // 申请补签
  openMakeupApply(e) {
    e.stopPropagation && e.stopPropagation()
    const extra = e.currentTarget.dataset.extraObj || this.parseExtra(e.currentTarget.dataset.extra)
    const meetingId = extra.meetingId
    if (!meetingId) {
      wx.showToast({ title: '无法获取会议信息', icon: 'none' })
      return
    }
    const meetingTitle = extra.title || '未知会议'
    wx.showModal({
      title: '申请补签',
      editable: true,
      placeholderText: '请输入补签理由（如：迟到、网络故障等）',
      success: async (res) => {
        if (!res.confirm) return
        const reason = (res.content || '').trim()
        if (!reason) {
          wx.showToast({ title: '请填写补签理由', icon: 'none' })
          return
        }
        wx.showLoading({ title: '提交中...' })
        try {
          await app.request({
            url: `/checkin/meeting/${meetingId}/makeup`,
            method: 'POST',
            data: { reason, proofUrl: '' }
          })
          wx.hideLoading()
          wx.showModal({
            title: '申请已提交',
            content: `会议「${meetingTitle}」的补签申请已提交，请等待管理员审批。`,
            showCancel: false
          })
          this.loadMessages()
        } catch (err) {
          wx.hideLoading()
          const msg = err && err.message ? err.message : '提交失败'
          wx.showModal({
            title: '提交失败',
            content: msg,
            showCancel: false
          })
        }
      }
    })
  },
  goCreateMeeting() {
    wx.navigateTo({ url: `/pages/meeting/detail?groupId=${this.data.groupId}&mode=create` })
  },

  noop() {}
})

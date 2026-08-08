// pages/meeting/meeting.js
const app = getApp()

Page({
  data: {
    meetingList: [],
    filter: 'all',
    loading: true,
    showCreateModal: false,
    form: {
      title: '',
      location: '',
      startTime: '',
      endTime: '',
      groupId: null,
      groupName: '',
      groupIndex: -1
    },
    myGroups: []
  },

  onLoad() {
    this.loadMeetings()
    this.loadMyGroups()
  },

  onShow() {
    this.loadMeetings()
  },

  async loadMyGroups() {
    try {
      const res = await app.request({
        url: '/group/my',
        method: 'GET'
      })
      this.setData({ myGroups: res.data || [] })
    } catch (e) {
      console.error('加载群组失败:', e)
    }
  },

  showCreateModal() {
    this.setData({ showCreateModal: true })
  },

  closeCreateModal() {
    this.setData({
      showCreateModal: false,
      form: { title: '', location: '', startTime: '', endTime: '', groupId: null, groupName: '', groupIndex: -1 }
    })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  onGroupChange(e) {
    const index = e.detail.value
    const group = this.data.myGroups[index]
    this.setData({
      'form.groupIndex': index,
      'form.groupId': group ? group.id : null,
      'form.groupName': group ? group.groupName : ''
    })
  },

  async submitCreateMeeting() {
    const { title, location, startTime, endTime, groupId } = this.data.form
    if (!title) {
      wx.showToast({ title: '请输入会议标题', icon: 'none' })
      return
    }
    if (!startTime || !endTime) {
      wx.showToast({ title: '请填写开始和结束时间', icon: 'none' })
      return
    }
    try {
      await app.request({
        url: '/meeting',
        method: 'POST',
        data: { title, location, startTime, endTime, groupId }
      })
      wx.showToast({ title: '创建成功' })
      this.closeCreateModal()
      this.loadMeetings()
    } catch (error) {
      console.error('创建会议失败:', error)
    }
  },

  async loadMeetings() {
    this.setData({ loading: true })
    try {
      const res = await app.request({
        url: '/meeting/page',
        method: 'GET',
        data: { current: 1, size: 50 }
      })
      this.setData({
        meetingList: res.data.records || []
      })
    } catch (error) {
      console.error('加载会议列表失败:', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  filterMeetings(e) {
    const filter = e.currentTarget.dataset.filter
    this.setData({ filter })
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/meeting/detail?id=${id}`
    })
  },

  goToCheckIn(e) {
    const token = e.currentTarget.dataset.token
    if (token) {
      wx.navigateTo({
        url: `/pages/checkin/checkin?token=${token}`
      })
    } else {
      wx.showToast({
        title: '二维码未生成',
        icon: 'none'
      })
    }
  },

  pullDownRefresh() {
    this.loadMeetings()
    wx.stopPullDownRefresh()
  }
})

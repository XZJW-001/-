const app = getApp()

const STATUS_META = {
  0: { text: '待审批', className: 'warning' },
  1: { text: '已通过', className: 'success' },
  2: { text: '已驳回', className: 'danger' },
  3: { text: '已撤销', className: 'info' }
}

function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

Page({
  data: {
    activeTab: 'apply',
    meetings: [],
    selectedMeetingIndex: -1,
    selectedMeeting: null,
    candidates: [],
    selectedProxyUserId: null,
    reason: '',
    reasonLength: 0,
    applications: [],
    applicationStats: { pending: 0, approved: 0, finished: 0 },
    loadingMeetings: true,
    loadingCandidates: false,
    loadingApplications: true,
    submitting: false
  },

  onShow() {
    this.loadPageData()
  },

  onPullDownRefresh() {
    this.loadPageData().finally(() => wx.stopPullDownRefresh())
  },

  async loadPageData() {
    await Promise.all([
      this.loadEligibleMeetings(),
      this.loadApplications()
    ])
  },

  async loadEligibleMeetings() {
    this.setData({ loadingMeetings: true })
    try {
      const res = await app.request({
        url: '/checkin/proxy/eligible-meetings',
        method: 'GET'
      })
      const meetings = (res.data || []).map((item) => ({
        ...item,
        startTimeText: formatDateTime(item.startTime),
        checkinEndTimeText: formatDateTime(item.checkinEndTime)
      }))
      const currentId = this.data.selectedMeeting?.id
      const selectedMeetingIndex = meetings.findIndex((item) => item.id === currentId)
      const selectedMeeting = selectedMeetingIndex >= 0 ? meetings[selectedMeetingIndex] : null

      this.setData({
        meetings,
        selectedMeetingIndex,
        selectedMeeting,
        selectedProxyUserId: selectedMeeting ? this.data.selectedProxyUserId : null,
        candidates: selectedMeeting ? this.data.candidates : []
      })

      if (selectedMeeting) {
        await this.loadCandidates(selectedMeeting.id)
      }
    } catch (error) {
      console.error('加载可申请会议失败:', error)
      this.setData({ meetings: [], selectedMeetingIndex: -1, selectedMeeting: null })
    } finally {
      this.setData({ loadingMeetings: false })
    }
  },

  async loadApplications() {
    this.setData({ loadingApplications: true })
    try {
      const res = await app.request({
        url: '/checkin/proxy/applications/my',
        method: 'GET'
      })
      const applications = (res.data || []).map((item) => {
        const meta = STATUS_META[item.status] || { text: '未知', className: 'info' }
        return {
          ...item,
          statusText: item.statusText || meta.text,
          statusClass: meta.className,
          createTimeText: formatDateTime(item.createTime),
          approveTimeText: formatDateTime(item.approveTime)
        }
      })
      this.setData({
        applications,
        applicationStats: {
          pending: applications.filter((item) => item.status === 0).length,
          approved: applications.filter((item) => item.status === 1).length,
          finished: applications.filter((item) => item.status === 2 || item.status === 3).length
        }
      })
    } catch (error) {
      console.error('加载代签申请失败:', error)
      this.setData({ applications: [] })
    } finally {
      this.setData({ loadingApplications: false })
    }
  },

  switchTab(event) {
    this.setData({ activeTab: event.currentTarget.dataset.tab })
  },

  async onMeetingChange(event) {
    const selectedMeetingIndex = Number(event.detail.value)
    const selectedMeeting = this.data.meetings[selectedMeetingIndex]
    this.setData({
      selectedMeetingIndex,
      selectedMeeting,
      candidates: [],
      selectedProxyUserId: null
    })
    if (selectedMeeting) {
      await this.loadCandidates(selectedMeeting.id)
    }
  },

  async loadCandidates(meetingId) {
    this.setData({ loadingCandidates: true })
    try {
      const res = await app.request({
        url: `/checkin/meeting/${meetingId}/proxy/candidates`,
        method: 'GET'
      })
      const candidates = (res.data || []).map((item) => ({
        ...item,
        initial: (item.realName || '代').slice(0, 1)
      }))
      const selectedExists = candidates.some((item) => item.id === this.data.selectedProxyUserId)
      this.setData({
        candidates,
        selectedProxyUserId: selectedExists ? this.data.selectedProxyUserId : null
      })
    } catch (error) {
      console.error('加载代签人失败:', error)
      this.setData({ candidates: [], selectedProxyUserId: null })
    } finally {
      this.setData({ loadingCandidates: false })
    }
  },

  selectCandidate(event) {
    this.setData({ selectedProxyUserId: Number(event.currentTarget.dataset.id) })
  },

  onReasonInput(event) {
    const reason = event.detail.value
    this.setData({ reason, reasonLength: reason.length })
  },

  submitApplication() {
    const { selectedMeeting, selectedProxyUserId, reason, submitting } = this.data
    if (submitting) return
    if (!selectedMeeting) {
      wx.showToast({ title: '请选择会议', icon: 'none' })
      return
    }
    if (!selectedProxyUserId) {
      wx.showToast({ title: '请选择代签人', icon: 'none' })
      return
    }
    if (!reason.trim()) {
      wx.showToast({ title: '请填写代签原因', icon: 'none' })
      return
    }

    const candidate = this.data.candidates.find((item) => item.id === selectedProxyUserId)
    wx.showModal({
      title: '确认提交申请',
      content: `会议：${selectedMeeting.title}\n代签人：${candidate?.realName || '已选择'}`,
      confirmText: '提交',
      success: (result) => {
        if (result.confirm) this.doSubmitApplication()
      }
    })
  },

  async doSubmitApplication() {
    const { selectedMeeting, selectedProxyUserId, reason } = this.data
    this.setData({ submitting: true })
    try {
      await app.request({
        url: `/checkin/meeting/${selectedMeeting.id}/proxy/apply`,
        method: 'POST',
        data: {
          proxyUserId: selectedProxyUserId,
          reason: reason.trim()
        }
      })
      wx.showToast({ title: '申请已提交', icon: 'success' })
      this.setData({
        activeTab: 'records',
        selectedMeetingIndex: -1,
        selectedMeeting: null,
        candidates: [],
        selectedProxyUserId: null,
        reason: '',
        reasonLength: 0
      })
      await this.loadPageData()
    } catch (error) {
      console.error('提交代签申请失败:', error)
    } finally {
      this.setData({ submitting: false })
    }
  },

  cancelApplication(event) {
    const applyId = event.currentTarget.dataset.id
    const meetingTitle = event.currentTarget.dataset.title
    wx.showModal({
      title: '撤销申请',
      content: `确定撤销“${meetingTitle}”的代签申请吗？`,
      confirmText: '撤销',
      confirmColor: '#BE3654',
      success: async (result) => {
        if (!result.confirm) return
        try {
          await app.request({
            url: `/checkin/proxy/applications/${applyId}`,
            method: 'DELETE'
          })
          wx.showToast({ title: '已撤销', icon: 'success' })
          await this.loadPageData()
        } catch (error) {
          console.error('撤销代签申请失败:', error)
        }
      }
    })
  }
})

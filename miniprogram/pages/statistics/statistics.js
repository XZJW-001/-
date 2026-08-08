// pages/statistics/statistics.js
const app = getApp()

Page({
  data: {
    loading: true,
    meetingList: [],
    currentMeetingId: null,
    overview: {
      totalMeetings: 0,
      totalCheckins: 0,
      totalUsers: 0,
      attendanceRate: 0
    },
    attendanceStats: {
      checked: 0,
      absent: 0,
      late: 0,
      leave: 0
    },
    trendData: [],
    departmentStats: []
  },

  onLoad() {
    this.loadOverview()
  },

  onShow() {
    this.loadOverview()
  },

  async loadOverview() {
    this.setData({ loading: true })
    try {
      // 加载统计概览
      const overviewRes = await app.request({
        url: '/statistics/overview',
        method: 'GET'
      })
      const data = overviewRes.data || {}
      this.setData({
        overview: {
          totalMeetings: data.totalMeetings || 0,
          totalCheckins: data.totalSignCount || 0,
          totalUsers: data.totalUsers || 0,
          attendanceRate: data.todayAttendanceRate || data.attendanceRate || 0
        }
      })
    } catch (e) {
      console.error('加载概览失败:', e)
      this.loadMockData()
    }

    try {
      // 加载会议列表
      const meetingRes = await app.request({
        url: '/meeting/page',
        method: 'GET',
        data: { current: 1, size: 10 }
      })
      const meetings = meetingRes.data.records || []
      const firstMeetingId = meetings.length > 0 ? meetings[0].id : null
      this.setData({
        meetingList: meetings,
        currentMeetingId: firstMeetingId
      })
      
      if (firstMeetingId) {
        this.loadMeetingStats(firstMeetingId)
      } else {
        this.setData({ loading: false })
      }
    } catch (e) {
      console.error('加载会议列表失败:', e)
      this.setData({ loading: false })
    }
  },

  async loadMeetingStats(meetingId) {
    try {
      const [statsRes, detailRes] = await Promise.all([
        app.request({
          url: `/statistics/meeting/${meetingId}`,
          method: 'GET'
        }),
        app.request({
          url: `/statistics/meeting/${meetingId}/stats`,
          method: 'GET'
        })
      ])
      const data = statsRes.data || {}
      const detail = detailRes.data || {}
      // 应到人数：取 statsRes 的 totalCount（含群成员数）
      const totalCount = data.totalCount || detail.totalCount || 0
      const signedCount = data.signedCount || detail.signedCount || 0
      const notSignedCount = data.notSignedCount != null ? data.notSignedCount : (totalCount - signedCount - (data.lateCount || 0))
      const attendanceRate = data.attendanceRate || 0

      this.setData({
        attendanceStats: {
          total: totalCount,
          checked: signedCount,
          absent: notSignedCount,
          late: data.lateCount || detail.lateCount || 0,
          makeup: detail.makeupCount || 0,
          proxy: detail.proxyCount || 0,
          leave: 0
        },
        'overview.attendanceRate': attendanceRate
      })
    } catch (e) {
      console.error('加载会议统计失败:', e)
      this.setData({
        attendanceStats: { total: 0, checked: 0, absent: 0, late: 0, makeup: 0, proxy: 0, leave: 0 }
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  loadMockData() {
    this.setData({
      overview: {
        totalMeetings: 12,
        totalCheckins: 348,
        totalUsers: 56,
        attendanceRate: 92.5
      },
      attendanceStats: {
        checked: 48,
        absent: 5,
        late: 3,
        leave: 2
      }
    })
  },

  switchMeeting(e) {
    const meetingId = e.currentTarget.dataset.id
    this.setData({ currentMeetingId: meetingId })
    this.loadMeetingStats(meetingId)
  },

  goToDetail(e) {
    const type = e.currentTarget.dataset.type
    wx.navigateTo({
      url: `/pages/checkin/checkin?type=${type}`
    })
  },

  exportReport() {
    wx.showToast({
      title: '导出功能开发中',
      icon: 'none'
    })
  }
})

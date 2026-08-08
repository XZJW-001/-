const app = getApp()

Page({
  data: {
    groups: [],
    totalMembers: 0,
    showCreateModal: false,
    showJoinModal: false,
    form: {
      groupName: '',
      description: ''
    },
    joinCode: '',
    searchKeyword: '',
    isLoggedIn: false,
    loaded: false
  },

  onLoad() {
    this.initPage()
  },

  onShow() {
    if (this.data.loaded) {
      this.loadGroups()
    }
  },

  initPage() {
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.showModal({
        title: '提示',
        content: '请先登录',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) {
            wx.reLaunch({ url: '/pages/login/login' })
          }
        }
      })
      this.setData({ isLoggedIn: false })
      return
    }
    this.setData({ isLoggedIn: true, loaded: true })
    this.loadGroups()
  },

  onPullDownRefresh() {
    this.loadGroups()
    wx.stopPullDownRefresh()
  },

  async loadGroups() {
    try {
      const res = await app.request({
        url: '/group/my',
        method: 'GET'
      })
      const groups = res.data || []
      const totalMembers = groups.reduce((sum, g) => sum + (g.memberCount || 0), 0)
      this.setData({ groups, totalMembers })
    } catch (error) {
      console.error('加载群组失败:', error)
    }
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value })
  },

  searchGroup() {
    if (!this.data.searchKeyword) {
      this.loadGroups()
      return
    }
    wx.showLoading({ title: '搜索中...' })
    setTimeout(() => {
      wx.hideLoading()
      wx.showToast({ title: '搜索功能开发中', icon: 'none' })
    }, 500)
  },

  createGroup() {
    this.setData({ showCreateModal: true })
  },

  closeCreateModal() {
    this.setData({ 
      showCreateModal: false,
      form: { groupName: '', description: '' }
    })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({
      [`form.${field}`]: e.detail.value
    })
  },

  async submitCreateGroup() {
    const { groupName, description } = this.data.form
    if (!groupName) {
      wx.showToast({ title: '请输入群组名称', icon: 'none' })
      return
    }
    try {
      await app.request({
        url: '/group',
        method: 'POST',
        data: { groupName, description }
      })
      wx.showToast({ title: '创建成功' })
      this.closeCreateModal()
      this.loadGroups()
    } catch (error) {
      console.error('创建群组失败:', error)
    }
  },

  viewGroup(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/group/detail?id=${id}`
    })
  },

  joinGroup() {
    this.setData({ showJoinModal: true })
  },

  closeJoinModal() {
    this.setData({ showJoinModal: false, joinCode: '' })
  },

  onJoinCodeInput(e) {
    this.setData({ joinCode: e.detail.value })
  },

  async submitJoinGroup() {
    const code = this.data.joinCode
    if (!code || code.length !== 6) {
      wx.showToast({ title: '请输入6位编号', icon: 'none' })
      return
    }
    try {
      const searchRes = await app.request({
        url: `/group/code/${code}`,
        method: 'GET'
      })
      if (searchRes.data) {
        await app.request({
          url: `/group/${searchRes.data.id}/join`,
          method: 'POST'
        })
        wx.showToast({ title: '加入成功' })
        this.closeJoinModal()
        this.loadGroups()
      }
    } catch (error) {
      console.error('加入群组失败:', error)
    }
  }
})
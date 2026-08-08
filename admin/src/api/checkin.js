import request from '@/utils/request'

// 用户签到
export const checkIn = (meetingId, data) => request.post(`/checkin/meeting/${meetingId}`, data)

// 快速签到（群内一键签到）
export const quickCheckIn = (meetingId) => request.post(`/checkin/meeting/${meetingId}/quick`)

// 获取会议签到记录
export const getCheckInRecords = (meetingId) => request.get(`/checkin/meeting/${meetingId}/records`)

// 获取我的签到记录
export const getMyCheckInRecords = () => request.get('/checkin/my/records')

// 获取会议签到状态
export const getMeetingCheckInStatus = (meetingId) => request.get(`/checkin/meeting/${meetingId}/status`)

// 申请补签
export const applyMakeUp = (meetingId, data) => request.post(`/checkin/meeting/${meetingId}/makeup`, data)

// 审批补签
export const approveMakeUp = (applyId, status, remark) => 
  request.put(`/checkin/makeup/${applyId}/approve`, null, { params: { status, remark } })

// 获取补签申请列表（按会议）
export const getMakeUpList = (meetingId, status) => {
  if (meetingId === null || meetingId === undefined) {
    return request.get('/checkin/makeup/list', { params: { status } })
  }
  return request.get(`/checkin/meeting/${meetingId}/makeup/list`, { params: { status } })
}

// 获取所有补签申请列表（跨会议，管理员用）
export const getAllMakeUpList = (status) => request.get('/checkin/makeup/list', { params: { status } })

// 代签申请
export const applyProxySign = (meetingId, data) => request.post(`/checkin/meeting/${meetingId}/proxy/apply`, data)
export const getProxyCandidates = (meetingId) => request.get(`/checkin/meeting/${meetingId}/proxy/candidates`)
export const getMyProxyEligibleMeetings = () => request.get('/checkin/proxy/eligible-meetings')
export const getMyProxySignApplications = () => request.get('/checkin/proxy/applications/my')
export const cancelProxySignApplication = (applyId) => request.delete(`/checkin/proxy/applications/${applyId}`)
export const approveProxySignApplication = (applyId, status, remark) =>
  request.put(`/checkin/proxy/applications/${applyId}/approve`, null, { params: { status, remark } })
export const getProxySignApplications = (meetingId, status) =>
  request.get(`/checkin/meeting/${meetingId}/proxy/applications`, { params: { status } })
export const getAllProxySignApplications = (status) =>
  request.get('/checkin/proxy/applications', { params: { status } })

// 代签
export const proxySign = (meetingId, data) => request.post(`/checkin/meeting/${meetingId}/proxy`, data)

// 获取代签记录（按会议）
export const getProxySignList = (meetingId) => {
  if (meetingId === null || meetingId === undefined) {
    return request.get('/checkin/proxy/list')
  }
  return request.get(`/checkin/meeting/${meetingId}/proxy/list`)
}

// 获取所有代签记录（跨会议，管理员用）
export const getAllProxySignList = () => request.get('/checkin/proxy/list')

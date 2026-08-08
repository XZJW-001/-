import request from '@/utils/request'

// 获取会议签到统计
export const getMeetingStatistics = (meetingId) => request.get(`/statistics/meeting/${meetingId}`)

// 获取会议详细统计（含签到方式分布）
export const getMeetingStats = (meetingId) => request.get(`/statistics/meeting/${meetingId}/stats`)

// 获取人员状态分布
export const getStatusDistribution = (meetingId) => request.get(`/statistics/meeting/${meetingId}/status-distribution`)

// 获取补签统计
export const getMakeUpStatistics = (meetingId) => request.get(`/statistics/meeting/${meetingId}/makeup`)

// 获取代签统计
export const getProxyStatistics = (meetingId) => request.get(`/statistics/meeting/${meetingId}/proxy`)

// 获取用户签到统计
export const getUserStatistics = (userId) => request.get(`/statistics/user/${userId}`)

// 获取时间段签到统计
export const getTimeRangeStatistics = (startTime, endTime) => 
  request.get('/statistics/time-range', { params: { startTime, endTime } })

// 获取部门签到统计
export const getDeptStatistics = (startTime, endTime) => 
  request.get('/statistics/dept', { params: { startTime, endTime } })

// 导出会议签到报表
export const exportMeetingReport = (meetingId, format = 'csv') => 
  request.get(`/statistics/meeting/${meetingId}/export`, { params: { format }, responseType: 'blob' })

// 获取会议定位签到位置列表（地图展示）
export const getMeetingCheckInLocations = (meetingId) => 
  request.get(`/statistics/meeting/${meetingId}/locations`)

// 获取会议拍照签到照片列表
export const getMeetingCheckInPhotos = (meetingId) => 
  request.get(`/statistics/meeting/${meetingId}/photos`)

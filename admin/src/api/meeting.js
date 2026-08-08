import request from '@/utils/request'

// 创建会议
export const createMeeting = (data) => request.post('/meeting', data)

// 更新会议
export const updateMeeting = (id, data) => request.put(`/meeting/${id}`, data)

// 删除会议
export const deleteMeeting = (id) => request.delete(`/meeting/${id}`)

// 获取会议详情
export const getMeetingDetail = (id) => request.get(`/meeting/${id}`)

// 分页查询会议列表
export const getMeetingList = (params) => request.get('/meeting/page', { params })

// 生成会议二维码
export const generateQrcode = (id) => request.post(`/meeting/${id}/qrcode`)

// 根据二维码Token获取会议信息（公开接口）
export const getMeetingByQrcode = (token) => request.get(`/meeting/public/qrcode/${token}`)

// 发布会议
export const publishMeeting = (id) => request.put(`/meeting/${id}/publish`)

// 开始会议
export const startMeeting = (id) => request.put(`/meeting/${id}/start`)

// 结束会议
export const endMeeting = (id) => request.put(`/meeting/${id}/end`)

// 生成群组二维码
export const generateGroupQrcode = (content) => request.post('/qrcode/generate', { content })

// 获取会议参会人员列表
export const getMeetingAttendees = (id) => request.get(`/meeting/${id}/attendees`)

// PC端模拟签到（管理员代签到）
export const mockCheckIn = (meetingId, data) => request.post(`/checkin/meeting/${meetingId}/mock`, data)

// 清空群聊会议记录
export const clearGroupMeetings = (groupId) => request.delete(`/meeting/group/${groupId}/clear`)

// 清空群聊消息记录
export const clearGroupMessages = (groupId) => request.delete(`/group/message/${groupId}/clear`)

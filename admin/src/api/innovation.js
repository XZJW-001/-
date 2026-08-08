import request from '@/utils/request'

export const getFeatureConfig = (meetingId) => request.get(`/innovation/meeting/${meetingId}/config`)
export const saveFeatureConfig = (meetingId, data) => request.put(`/innovation/meeting/${meetingId}/config`, data)
export const getDynamicQrcode = (meetingId) => request.get(`/innovation/meeting/${meetingId}/dynamic-qrcode`)
export const getLiveSnapshot = (meetingId) => request.get(`/innovation/meeting/${meetingId}/live`)
export const getRisks = (meetingId, level) => request.get(`/innovation/meeting/${meetingId}/risks`, { params: { level } })
export const reviewRisk = (riskId, data) => request.put(`/innovation/risk/${riskId}/review`, data)
export const sendMeetingReminders = (meetingId, data) => request.post(`/innovation/meeting/${meetingId}/reminders/send`, data)
export const createGuestInvite = (meetingId, data = {}) => request.post(`/innovation/meeting/${meetingId}/guest-invite`, data)
export const getGuests = (meetingId) => request.get(`/innovation/meeting/${meetingId}/guests`)
export const getAlerts = () => request.get('/innovation/alerts')
export const generateMinutes = (meetingId, sourceText) => request.post(`/innovation/meeting/${meetingId}/minutes/generate`, { sourceText })
export const getMinutes = (meetingId) => request.get(`/innovation/meeting/${meetingId}/minutes`)

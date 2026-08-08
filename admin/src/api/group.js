import request from '@/utils/request'

export function createGroup(data) {
  return request({
    url: '/group',
    method: 'post',
    data
  })
}

export function updateGroup(id, data) {
  return request({
    url: `/group/${id}`,
    method: 'put',
    data
  })
}

export function deleteGroup(id) {
  return request({
    url: `/group/${id}`,
    method: 'delete'
  })
}

export function getGroup(id) {
  return request({
    url: `/group/${id}`,
    method: 'get'
  })
}

export function getGroupList(params) {
  return request({
    url: '/group/page',
    method: 'get',
    params
  })
}

export function getMyGroups() {
  return request({
    url: '/group/my',
    method: 'get'
  })
}

export function joinGroup(id) {
  return request({
    url: `/group/${id}/join`,
    method: 'post'
  })
}

export function leaveGroup(id) {
  return request({
    url: `/group/${id}/leave`,
    method: 'post'
  })
}

export function getGroupMembers(id) {
  return request({
    url: `/group/${id}/members`,
    method: 'get'
  })
}

export function updateMemberRole(groupId, userId, role) {
  return request({
    url: `/group/${groupId}/members/${userId}/role`,
    method: 'put',
    params: { role }
  })
}

export function removeMember(groupId, userId) {
  return request({
    url: `/group/${groupId}/members/${userId}`,
    method: 'delete'
  })
}

export function applyJoinGroup(id, remark) {
  return request({
    url: `/group/${id}/apply`,
    method: 'post',
    data: { remark }
  })
}

export function inviteToGroup(groupId, targetUserId, remark) {
  return request({
    url: `/group/${groupId}/invite`,
    method: 'post',
    data: { targetUserId, remark }
  })
}

export function handleInvite(id, accept) {
  return request({
    url: `/group/invite/${id}/handle`,
    method: 'put',
    params: { accept }
  })
}

export function getMyInvites() {
  return request({
    url: '/group/invite/my',
    method: 'get'
  })
}

// ====== 群聊消息 ======
export function getMessages(groupId, limit) {
  return request({
    url: `/group/message/${groupId}`,
    method: 'get',
    params: { limit }
  })
}

export function getLatestMessages(groupId, lastId) {
  return request({
    url: `/group/message/${groupId}/latest`,
    method: 'get',
    params: { lastId }
  })
}

export function sendTextMessage(groupId, content) {
  return request({
    url: `/group/message/${groupId}/send`,
    method: 'post',
    data: { content }
  })
}

export function sendMeetingCard(groupId, meetingId) {
  return request({
    url: `/group/message/${groupId}/meeting-card`,
    method: 'post',
    data: { meetingId }
  })
}

export function sendCheckinCard(groupId, meetingId) {
  return request({
    url: `/group/message/${groupId}/checkin-card`,
    method: 'post',
    data: { meetingId }
  })
}
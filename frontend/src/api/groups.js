import request from '../utils/request'

export const sendGroupMessage = (data) => request.post('/api/group/send', data)
export const getOnlineMembers = (groupId) => request.get(`/api/group/online/${groupId}`)
export const listGroups = () => request.get('/api/groups')
export const createGroup = (data) => request.post('/api/groups', data)
export const updateGroup = (groupId, data) => request.put(`/api/groups/${groupId}`, data)
export const getGroupDetail = (groupId) => request.get(`/api/groups/${groupId}`)
export const joinGroup = (groupId) => request.post(`/api/groups/${groupId}/join`)
export const leaveGroup = (groupId) => request.post(`/api/groups/${groupId}/leave`)
export const getGroupMembers = (groupId) => request.get(`/api/groups/${groupId}/members`)
export const removeMember = (groupId, memberId) => request.delete(`/api/groups/${groupId}/members/${memberId}`)
export const getGroupMessages = (groupId, params) => request.get(`/api/groups/${groupId}/messages`, { params })
export const exportGroupMessages = (groupId) =>
  request.get(`/api/groups/${groupId}/export`, { responseType: 'blob' })

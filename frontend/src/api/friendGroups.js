import request from '../utils/request'

export const listFriendGroups = () => request.get('/api/friend-groups')
export const createFriendGroup = (data) => request.post('/api/friend-groups', data)
export const updateFriendGroup = (groupId, data) => request.put(`/api/friend-groups/${groupId}`, data)
export const deleteFriendGroup = (groupId) => request.delete(`/api/friend-groups/${groupId}`)

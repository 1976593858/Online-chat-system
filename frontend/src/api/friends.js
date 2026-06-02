import request from '../utils/request'

export const listFriends = (params) => request.get('/api/friends', { params })
export const getFriendDetail = (friendId) => request.get(`/api/friends/${friendId}`)
export const updateFriendRemark = (friendId, data) => request.put(`/api/friends/${friendId}/remark`, data)
export const moveFriendGroup = (friendId, data) => request.put(`/api/friends/${friendId}/group`, data)
export const deleteFriend = (friendId) => request.delete(`/api/friends/${friendId}`)

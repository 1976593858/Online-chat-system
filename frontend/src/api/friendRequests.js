import request from '../utils/request'

export const createFriendRequest = (data) => request.post('/api/friend-requests', data)
export const listFriendRequests = (params) => request.get('/api/friend-requests', { params })
export const acceptFriendRequest = (requestId, data = {}) => request.put(`/api/friend-requests/${requestId}/accept`, data)
export const rejectFriendRequest = (requestId, data = {}) => request.put(`/api/friend-requests/${requestId}/reject`, data)

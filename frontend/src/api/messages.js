import request from '../utils/request'

export const openPrivateConversation = (targetUserId) => request.get(`/api/conversations/private/${targetUserId}`)
export const fetchPrivateMessages = (targetUserId, params) => request.get(`/api/messages/private/${targetUserId}`, { params })
export const sendPrivateMessage = (data) => request.post('/api/messages/private', data)

export const exportPrivateMessages = (targetUserId) =>
  request.get(`/api/messages/private/${targetUserId}/export`, {
    responseType: 'blob'
  })


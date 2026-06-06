import request from '../utils/request'

export const listRecentConversations = (params) => request.get('/api/conversations/recent', { params })
export const markConversationRead = (conversationId) => request.put(`/api/conversations/${conversationId}/read`)
export const toggleMuteConversation = (conversationId, muted) => request.put(`/api/conversations/${conversationId}/mute?muted=${muted}`)

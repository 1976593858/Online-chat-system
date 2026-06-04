import request from '../utils/request'

export const searchChatHistory = (params) => request.get('/api/chat-history/search', { params })

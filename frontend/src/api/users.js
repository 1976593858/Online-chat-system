import request from '../utils/request'

export const searchUsers = (params) => request.get('/api/users/search', { params })

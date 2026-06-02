import request from '../utils/request'

export const login = (data) => request.post('/api/auth/login', data)
export const register = (data) => request.post('/api/auth/register', data)
export const fetchCurrentUser = () => request.get('/api/auth/me')

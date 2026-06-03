import { defineStore } from 'pinia'
import { fetchCurrentUser, login, register } from '../api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  actions: {
    async login(payload) {
      const data = await login(payload)
      this.setSession(data)
      return data
    },
    async register(payload) {
      const data = await register(payload)
      this.setSession(data)
      return data
    },
    async loadCurrentUser() {
      this.user = await fetchCurrentUser()
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    setSession(data) {
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})

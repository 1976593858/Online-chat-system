import { defineStore } from 'pinia'
import { connectWS, disconnect, addHandler, removeHandler, send, isConnected } from '../api/chat'
import { useAuthStore } from './auth'

export const useWebSocketStore = defineStore('websocket', {
  state: () => ({
    connected: false
  }),

  actions: {
    connect() {
      const authStore = useAuthStore()
      const userId = authStore.user?.id
      if (!userId) {
        console.warn('WebSocket: 未登录，跳过连接')
        return
      }
      connectWS(String(userId))
      this.connected = true
    },

    disconnect() {
      disconnect()
      this.connected = false
    },

    addHandler(handler) {
      addHandler(handler)
    },

    removeHandler(handler) {
      removeHandler(handler)
    },

    send(data) {
      send(data)
    },

    isConnected() {
      return isConnected()
    },

    onOpen(callback) {
      const check = setInterval(() => {
        if (isConnected()) {
          clearInterval(check)
          callback()
        }
      }, 100)
    }
  }
})

import { defineStore } from 'pinia'
import { listRecentConversations, markConversationRead } from '../api/conversations'

export const useConversationStore = defineStore('conversation', {
  state: () => ({
    page: { records: [], total: 0, pageNo: 1, pageSize: 20, pages: 0 },
    loading: false
  }),
  actions: {
    async loadRecent(params = {}) {
      this.loading = true
      try {
        this.page = await listRecentConversations({ pageNo: 1, pageSize: 20, ...params })
      } finally {
        this.loading = false
      }
    },
    async markRead(conversationId) {
      await markConversationRead(conversationId)
      await this.loadRecent({ pageNo: this.page.pageNo, pageSize: this.page.pageSize })
    }
  }
})

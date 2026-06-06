import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import FriendManagementView from '../views/FriendManagementView.vue'
import ConversationListView from '../views/ConversationListView.vue'
import ChatView from '../views/ChatView.vue'
import ChatHistoryView from '../views/ChatHistoryView.vue'
import GroupChat from '../views/group/GroupChat.vue'
import GroupListView from '../views/group/GroupListView.vue'

const routes = [
  { path: '/', redirect: '/friends' },
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/friends', name: 'friends', component: FriendManagementView },
  { path: '/conversations', name: 'conversations', component: ConversationListView },
  { path: '/chat/:targetUserId', name: 'chat', component: ChatView },
  { path: '/chat-history', name: 'chatHistory', component: ChatHistoryView },
  { path: '/groups', name: 'groups', component: GroupListView },
  { path: '/group/:groupId', name: 'group', component: GroupChat, props: true }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (!to.meta.public && !authStore.token) {
    return { name: 'login' }
  }
  if (to.name === 'login' && authStore.token) {
    return { name: 'friends' }
  }
  return true
})

export default router

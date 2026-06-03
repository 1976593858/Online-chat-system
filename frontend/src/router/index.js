import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import FriendManagementView from '../views/FriendManagementView.vue'
import ConversationListView from '../views/ConversationListView.vue'
import ChatView from '../views/ChatView.vue'

const routes = [
  { path: '/', redirect: '/friends' },
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/friends', name: 'friends', component: FriendManagementView },
  { path: '/conversations', name: 'conversations', component: ConversationListView },
  { path: '/chat/:targetUserId', name: 'chat', component: ChatView }
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

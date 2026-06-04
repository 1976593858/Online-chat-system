<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <div class="brand-mark">Online Chat</div>
        <h1 class="page-title">私聊</h1>
        <p class="page-subtitle">与 {{ targetDisplayName }} 的聊天记录。</p>
      </div>
      <nav class="nav-links">
        <RouterLink class="nav-link" to="/friends">好友</RouterLink>
        <RouterLink class="nav-link" to="/conversations">消息列表</RouterLink>
        <RouterLink class="nav-link" to="/chat-history">搜索</RouterLink>
        <el-button plain @click="logout">退出</el-button>
      </nav>
    </header>

    <section class="chat-layout glass-card">
      <header class="chat-header">
        <div class="chat-peer">
          <el-avatar :src="conversation?.targetAvatar" :size="44">{{ firstLetter(targetDisplayName) }}</el-avatar>
          <div>
            <div class="chat-peer-name">{{ targetDisplayName }}</div>
            <div class="muted">@{{ conversation?.targetUsername || '-' }}</div>
          </div>
        </div>
        <div class="chat-header-actions">
          <el-button :disabled="!conversation" @click="downloadHistory">导出记录</el-button>
          <el-button type="primary" :loading="loadingConversation" @click="reload">刷新</el-button>
        </div>
      </header>

      <div class="chat-body">
        <div class="history-toolbar">
          <el-button
            v-if="hasMore"
            plain
            size="small"
            :loading="loadingMessages"
            @click="loadMore"
          >
            加载更多
          </el-button>
          <span v-else class="muted">没有更多记录</span>
        </div>

        <div class="message-list" ref="listRef">
          <div v-for="message in messages" :key="message.id" class="message-row" :class="{ self: isSelf(message) }">
            <div class="message-bubble">
              <div class="message-meta muted">{{ message.createdAt }}</div>
              <div class="message-text">{{ message.content }}</div>
            </div>
          </div>
        </div>
      </div>

      <footer class="chat-input">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-word-limit
          placeholder="输入消息，Ctrl+Enter 发送"
          @keydown.ctrl.enter.prevent="send"
        />
        <div class="chat-input-actions">
          <el-button type="primary" :disabled="!draft.trim()" :loading="sending" @click="send">发送</el-button>
        </div>
      </footer>
    </section>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { markConversationRead } from '../api/conversations'
import { exportPrivateMessages, fetchPrivateMessages, openPrivateConversation, sendPrivateMessage } from '../api/messages'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const targetUserId = computed(() => Number(route.params.targetUserId))
const conversation = ref(null)
const loadingConversation = ref(false)

const messages = ref([])
const pageNo = ref(1)
const pageSize = 20
const pages = ref(0)
const loadingMessages = ref(false)

const draft = ref('')
const sending = ref(false)
const listRef = ref(null)

const targetDisplayName = computed(() => conversation.value?.targetNickname || conversation.value?.targetUsername || `用户 ${targetUserId.value}`)
const hasMore = computed(() => pages.value === 0 || pageNo.value < pages.value)

onMounted(async () => {
  await init()
})

function firstLetter(value) {
  return value ? value.slice(0, 1).toUpperCase() : '?'
}

function isSelf(message) {
  return authStore.user && Number(message.fromUserId) === Number(authStore.user.id)
}

async function init() {
  await openConversation()
  await loadFirstPage()
  await scrollToBottom()
}

async function reload() {
  messages.value = []
  pageNo.value = 1
  pages.value = 0
  await init()
}

async function openConversation() {
  loadingConversation.value = true
  try {
    conversation.value = await openPrivateConversation(targetUserId.value)
    if (conversation.value?.id) {
      await markConversationRead(conversation.value.id)
    }
  } finally {
    loadingConversation.value = false
  }
}

async function loadFirstPage() {
  pageNo.value = 1
  const page = await loadPage(pageNo.value)
  pages.value = page.pages
  messages.value = page.records.slice().reverse()
}

async function loadMore() {
  if (!hasMore.value || loadingMessages.value) return
  pageNo.value += 1
  const page = await loadPage(pageNo.value)
  pages.value = page.pages
  const older = page.records.slice().reverse()
  messages.value = [...older, ...messages.value]
}

async function loadPage(no) {
  loadingMessages.value = true
  try {
    return await fetchPrivateMessages(targetUserId.value, { pageNo: no, pageSize })
  } finally {
    loadingMessages.value = false
  }
}

async function send() {
  const content = draft.value.trim()
  if (!content) return
  sending.value = true
  try {
    const message = await sendPrivateMessage({ toUserId: targetUserId.value, content, messageType: 'TEXT' })
    messages.value = [...messages.value, message]
    draft.value = ''
    await nextTick()
    await scrollToBottom()
  } finally {
    sending.value = false
  }
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

async function downloadHistory() {
  try {
    const blob = await exportPrivateMessages(targetUserId.value)
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `chat-${targetUserId.value}.txt`
    anchor.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e?.message || '导出失败')
  }
}

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.chat-layout {
  max-width: 1080px;
  margin: 0 auto;
  border-radius: 26px;
  padding: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid var(--line);
}

.chat-peer {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-peer-name {
  font-weight: 800;
}

.chat-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-body {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: min(66vh, 720px);
}

.history-toolbar {
  padding: 14px 20px;
  border-bottom: 1px solid var(--line);
}

.message-list {
  padding: 18px 20px;
  overflow-y: auto;
  display: grid;
  gap: 12px;
  background: rgba(255, 255, 255, 0.45);
}

.message-row {
  display: flex;
  justify-content: flex-start;
}

.message-row.self {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(520px, 78%);
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.9);
  display: grid;
  gap: 6px;
}

.message-row.self .message-bubble {
  background: rgba(14, 124, 123, 0.12);
  border-color: rgba(14, 124, 123, 0.22);
}

.message-meta {
  font-size: 12px;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-input {
  padding: 18px 20px;
  border-top: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.72);
  display: grid;
  gap: 10px;
}

.chat-input-actions {
  display: flex;
  justify-content: flex-end;
}
</style>


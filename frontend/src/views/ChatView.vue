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
          <el-button type="success" :disabled="!conversation" @click="startVoiceCall">📞 语音通话</el-button>
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
import { useVoiceCallStore } from '../stores/voiceCall'
import { markConversationRead } from '../api/conversations'
import { exportPrivateMessages, fetchPrivateMessages, openPrivateConversation, sendPrivateMessage } from '../api/messages'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const voiceCallStore = useVoiceCallStore()

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

function startVoiceCall() {
  if (!conversation.value) return
  const name = conversation.value.targetNickname || conversation.value.targetUsername || `用户 ${targetUserId.value}`
  voiceCallStore.startCall(targetUserId.value, name)
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
  border-radius: 30px;
  padding: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--line);
  background: var(--glass-bg);
  backdrop-filter: var(--blur-glass);
  -webkit-backdrop-filter: var(--blur-glass);
}

.chat-peer {
  display: flex;
  align-items: center;
  gap: 14px;
}

.chat-peer-name {
  font-weight: 800;
  font-size: 17px;
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
  padding: 14px 24px;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.25);
}

/* Message area — extra transparent to show background */
.message-list {
  padding: 20px 24px;
  overflow-y: auto;
  display: grid;
  gap: 14px;
  background: rgba(255, 255, 255, 0.18);
}

.message-row {
  display: flex;
  justify-content: flex-start;
  animation: msgIn 0.25s cubic-bezier(0.22, 0.61, 0.36, 1);
}

@keyframes msgIn {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

.message-row.self {
  justify-content: flex-end;
}

/* Glass bubbles */
.message-bubble {
  max-width: min(520px, 78%);
  padding: 14px 16px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: var(--glass-bg-hover);
  backdrop-filter: var(--blur-light);
  -webkit-backdrop-filter: var(--blur-light);
  display: grid;
  gap: 6px;
  box-shadow: var(--shadow-xs);
}

.message-row.self .message-bubble {
  background: rgba(7, 193, 96, 0.15);
  border-color: rgba(7, 193, 96, 0.25);
}

.message-meta {
  font-size: 11px;
  color: var(--muted);
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.55;
  font-size: 15px;
}

/* Input area — glass footer */
.chat-input {
  padding: 18px 24px;
  border-top: 1px solid var(--line);
  background: var(--glass-bg);
  backdrop-filter: var(--blur-glass);
  -webkit-backdrop-filter: var(--blur-glass);
  display: grid;
  gap: 12px;
}

.chat-input-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .chat-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .chat-header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .message-bubble {
    max-width: 88%;
  }
}
</style>


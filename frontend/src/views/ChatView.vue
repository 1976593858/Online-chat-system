<template>
  <main class="chat-shell">
    <!-- Header: floating glass bar -->
    <header class="chat-topbar">
      <button class="back-btn" @click="router.push('/conversations')">
        <span>←</span>
      </button>
      <div class="chat-peer" @click="router.push('/conversations')">
        <div class="chat-avatar" :style="avatarStyle">
          {{ firstLetter(targetDisplayName) }}
        </div>
        <div class="chat-peer-info">
          <div class="chat-peer-name">{{ targetDisplayName }}</div>
          <div class="chat-peer-username">@{{ conversation?.targetUsername || '-' }}</div>
        </div>
      </div>
      <div class="chat-topbar-actions">
        <button class="action-btn" :disabled="!conversation" @click="downloadHistory" title="导出记录">
          <span>↓</span>
        </button>
        <button class="action-btn primary" :disabled="!conversation" @click="startVoiceCall" title="语音通话">
          <span>📞</span>
        </button>
      </div>
    </header>

    <!-- Messages: floating glass cards -->
    <div class="chat-messages" ref="listRef">
      <div class="messages-load-more" v-if="hasMore">
        <button class="load-more-btn" :disabled="loadingMessages" @click="loadMore">
          {{ loadingMessages ? '加载中…' : '加载更早消息' }}
        </button>
      </div>
      <div v-else class="messages-end muted">— 这是对话的开始 —</div>

      <div
        v-for="message in messages"
        :key="message.id"
        :class="['message-row', isSelf(message) ? 'self' : 'other']"
      >
        <div v-if="!isSelf(message)" class="message-avatar" :style="otherAvatarStyle">
          {{ firstLetter(targetDisplayName) }}
        </div>

        <div class="message-card">
          <div class="message-time muted">{{ message.createdAt }}</div>
          <div class="message-text">{{ message.content }}</div>
        </div>

        <div v-if="isSelf(message)" class="message-avatar self-avatar">
          {{ firstLetter(myName) }}
        </div>
      </div>
    </div>

    <!-- Input: premium glass bar -->
    <footer class="chat-input-bar">
      <div class="input-wrapper">
        <textarea
          v-model="draft"
          class="chat-textarea"
          placeholder="输入消息…"
          rows="1"
          maxlength="2000"
          @keydown.ctrl.enter.prevent="send"
          @keydown.enter.exact.prevent="send"
          ref="inputRef"
        ></textarea>
        <button
          class="send-btn"
          :class="{ ready: draft.trim() }"
          :disabled="!draft.trim() || sending"
          @click="send"
        >
          <span>↑</span>
        </button>
      </div>
    </footer>
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
const inputRef = ref(null)

const myName = computed(() => authStore.user?.nickname || authStore.user?.username || '我')
const targetDisplayName = computed(() => conversation.value?.targetNickname || conversation.value?.targetUsername || `用户 ${targetUserId.value}`)
const hasMore = computed(() => pages.value === 0 || pageNo.value < pages.value)

const avatarStyle = computed(() => ({
  background: `linear-gradient(135deg, #5E6AD2 0%, #7B83E8 100%)`,
  boxShadow: `0 6px 20px rgba(94, 106, 210, 0.3)`
}))

const otherAvatarStyle = computed(() => ({
  background: `linear-gradient(135deg, #5E6AD2 0%, #7B83E8 100%)`,
  boxShadow: `0 4px 14px rgba(94, 106, 210, 0.25)`
}))

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
</script>

<style scoped>
/* ================================================
   Chat Shell
   ================================================ */

.chat-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: relative;
}

/* ================================================
   Top Bar — floating glass
   ================================================ */

.chat-topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin: 12px 16px 0;
  flex-shrink: 0;
  background: var(--glass-1);
  backdrop-filter: var(--blur-xl);
  -webkit-backdrop-filter: var(--blur-xl);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  z-index: 10;
}

.back-btn {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: var(--glass-3);
  backdrop-filter: var(--blur-md);
  -webkit-backdrop-filter: var(--blur-md);
  color: var(--text-secondary);
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-fast) var(--ease-out-expo);
  flex-shrink: 0;
}

.back-btn:hover {
  background: var(--glass-2);
  color: var(--text-primary);
  transform: scale(1.05);
}

.chat-peer {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast) var(--ease-out-expo);
}

.chat-peer:hover {
  background: var(--glass-3);
}

.chat-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.chat-peer-info {
  min-width: 0;
}

.chat-peer-name {
  font-weight: 680;
  font-size: 16px;
  color: var(--text-primary);
  line-height: 1.2;
}

.chat-peer-username {
  font-size: 12px;
  color: var(--text-tertiary);
}

.chat-topbar-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: var(--glass-3);
  backdrop-filter: var(--blur-md);
  -webkit-backdrop-filter: var(--blur-md);
  color: var(--text-secondary);
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-fast) var(--ease-spring-soft);
}

.action-btn:hover:not(:disabled) {
  background: var(--glass-2);
  color: var(--text-primary);
  transform: scale(1.08);
  box-shadow: var(--shadow-sm);
}

.action-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.action-btn.primary {
  background: var(--brand);
  color: #fff;
  box-shadow: 0 2px 8px var(--brand-glow);
}

.action-btn.primary:hover:not(:disabled) {
  background: var(--brand-hover);
  box-shadow: 0 6px 20px var(--brand-glow);
}

/* ================================================
   Messages Area
   ================================================ */

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.messages-load-more {
  text-align: center;
  padding: 4px 0 8px;
}

.load-more-btn {
  border: none;
  background: var(--glass-3);
  backdrop-filter: var(--blur-md);
  -webkit-backdrop-filter: var(--blur-md);
  color: var(--text-secondary);
  padding: 6px 18px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-expo);
  font-family: inherit;
}

.load-more-btn:hover {
  background: var(--glass-2);
  color: var(--text-primary);
}

.messages-end {
  text-align: center;
  font-size: 12px;
  padding: 8px 0;
}

/* === Message Row === */

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  animation: messageIn 0.4s var(--ease-spring-soft) both;
}

.message-row.self {
  justify-content: flex-end;
}

.message-row.other {
  justify-content: flex-start;
}

/* === Avatar === */

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
  margin-bottom: 2px;
}

.self-avatar {
  width: 28px;
  height: 28px;
  font-size: 11px;
  background: linear-gradient(135deg, var(--brand) 0%, var(--brand-active) 100%) !important;
  box-shadow: 0 2px 8px var(--brand-glow) !important;
}

/* ================================================
   Message Card — floating glass, not a boxy bubble
   28px radius, 68% max-width
   ================================================ */

.message-card {
  display: inline-flex;
  flex-direction: column;
  width: fit-content;
  max-width: 68%;
  padding: 10px 16px;
  border-radius: 28px;
  background: var(--glass-2);
  backdrop-filter: var(--blur-lg);
  -webkit-backdrop-filter: var(--blur-lg);
  border: 1px solid var(--glass-border-3);
  box-shadow: var(--shadow-sm);
  transition: all var(--duration-normal) var(--ease-spring-soft);
}

.message-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

/* Other's cards: slightly larger radius bottom-left */
.message-row.other .message-card {
  border-bottom-left-radius: 10px;
  background: var(--glass-1);
}

/* Self cards: green-tinted glass */
.message-row.self .message-card {
  border-bottom-right-radius: 10px;
  background: rgba(26, 173, 94, 0.13);
  border-color: rgba(26, 173, 94, 0.18);
}

.message-time {
  font-size: 10px;
  margin-bottom: 2px;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.45;
  font-size: 15px;
  color: var(--text-primary);
}

/* ================================================
   Input Bar — premium glass, floating
   ================================================ */

.chat-input-bar {
  flex-shrink: 0;
  padding: 12px 16px 16px;
  margin: 0 12px 12px;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 8px 8px 8px 18px;
  background: var(--glass-1);
  backdrop-filter: var(--blur-xl);
  -webkit-backdrop-filter: var(--blur-xl);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  transition: all var(--duration-normal) var(--ease-out-expo);
}

.input-wrapper:focus-within {
  border-color: rgba(26, 173, 94, 0.3);
  box-shadow: 0 0 0 3px var(--brand-glow), var(--shadow-md);
}

.chat-textarea {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  font-family: inherit;
  color: var(--text-primary);
  resize: none;
  min-height: 24px;
  max-height: 120px;
  line-height: 1.45;
  padding: 4px 0;
}

.chat-textarea::placeholder {
  color: var(--text-tertiary);
}

.send-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: var(--glass-3);
  color: var(--text-tertiary);
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all var(--duration-normal) var(--ease-spring-smooth);
  font-family: inherit;
}

.send-btn.ready {
  background: var(--brand);
  color: #fff;
  box-shadow: 0 2px 8px var(--brand-glow);
}

.send-btn.ready:hover {
  background: var(--brand-hover);
  box-shadow: 0 6px 20px var(--brand-glow);
  transform: scale(1.08);
}

.send-btn:active {
  transform: scale(0.92);
}

/* ================================================
   Scrollbar
   ================================================ */

.chat-messages::-webkit-scrollbar {
  width: 3px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.08);
  border-radius: 999px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: rgba(0,0,0,0.15);
}

/* ================================================
   Responsive
   ================================================ */

@media (max-width: 640px) {
  .chat-topbar {
    margin: 8px 8px 0;
    padding: 10px 12px;
    border-radius: var(--radius-md);
  }

  .chat-messages {
    padding: 10px 10px;
    gap: 6px;
  }

  .message-card {
    max-width: 82%;
    padding: 8px 14px;
    border-radius: 24px;
  }

  .message-row.other .message-card {
    border-bottom-left-radius: 8px;
  }

  .message-row.self .message-card {
    border-bottom-right-radius: 8px;
  }

  .chat-input-bar {
    margin: 0 6px 6px;
    padding: 8px 10px 10px;
  }

  .chat-avatar {
    width: 38px;
    height: 38px;
    font-size: 15px;
  }

  .message-avatar {
    width: 30px;
    height: 30px;
    font-size: 12px;
  }
}
</style>

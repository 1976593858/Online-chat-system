<template>
  <div class="group-shell">
    <!-- Header -->
    <header class="group-topbar">
      <button class="back-btn" @click="$router.push('/friends')">
        <span>←</span>
      </button>
      <div class="group-header-info">
        <div class="group-name">{{ groupName }}</div>
        <div class="group-label muted">群聊</div>
      </div>
      <div class="group-header-spacer"></div>
    </header>

    <!-- Messages -->
    <div class="group-messages" ref="bodyRef">
      <div
        v-for="(m, i) in messages"
        :key="i"
        :class="['msg-row', Number(m.userId) === myId ? 'self' : 'other']"
      >
        <div v-if="Number(m.userId) !== myId" class="msg-avatar" :style="memberAvatarStyle(m.userId)">
          {{ String(m.userId).slice(-2) }}
        </div>

        <div class="msg-card">
          <div class="msg-sender" v-if="Number(m.userId) !== myId">
            {{ String(m.userId).slice(-2) }}
          </div>
          <div class="msg-text">{{ m.content }}</div>
        </div>

        <div v-if="Number(m.userId) === myId" class="msg-avatar self-avatar">
          {{ String(myId).slice(-2) }}
        </div>
      </div>
    </div>

    <!-- Input -->
    <footer class="group-input-bar">
      <div class="group-input-wrapper">
        <input
          v-model="text"
          class="group-input"
          placeholder="输入群消息…"
          @keyup.enter="send"
        />
        <button class="group-send-btn" :class="{ ready: text.trim() }" :disabled="!text.trim()" @click="send">
          <span>↑</span>
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { sendGroupMessage } from '../../api/groups.js'
import { useAuthStore } from '../../stores/auth'
import { useWebSocketStore } from '../../stores/websocket'

const props = defineProps({
  groupId: { type: String, default: 'group_001' },
  groupName: { type: String, default: '全栈开发交流群' }
})

const authStore = useAuthStore()
const wsStore = useWebSocketStore()
const myId = authStore.user?.id || 0
const text = ref('')
const messages = ref([])
const bodyRef = ref(null)

const avatarColors = ['#5E6AD2', '#FF8C42', '#00A8CC', '#1aad5e', '#E84040', '#8C52D2', '#F0A030', '#4A90D9']

function memberAvatarStyle(userId) {
  const color = avatarColors[Number(userId) % avatarColors.length]
  return {
    background: `linear-gradient(135deg, ${color} 0%, ${color}dd 100%)`,
    boxShadow: `0 3px 12px ${color}40`
  }
}

function handleMessage(data) {
  if (data.type === 'group_message' && String(data.groupId) === String(props.groupId)) {
    messages.value.push(data)
    scrollBottom()
  }
}

onMounted(() => {
  if (!wsStore.isConnected()) {
    wsStore.connect()
  }
  wsStore.addHandler(handleMessage)
})

onUnmounted(() => {
  wsStore.removeHandler(handleMessage)
})

async function send() {
  if (!text.value.trim()) return
  const payload = {
    groupId: props.groupId,
    content: text.value
  }
  try {
    await sendGroupMessage(payload)
  } catch (e) {
    console.warn('群消息发送HTTP失败，仅本地回显', e)
  }
  messages.value.push({
    type: 'group_message',
    groupId: props.groupId,
    userId: String(myId),
    content: text.value,
    timestamp: Date.now()
  })
  text.value = ''
  scrollBottom()
}

function scrollBottom() {
  nextTick(() => {
    const el = bodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style scoped>
.group-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

/* ================================================
   Header
   ================================================ */

.group-topbar {
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

.group-header-info {
  min-width: 0;
}

.group-name {
  font-weight: 680;
  font-size: 16px;
  color: var(--text-primary);
}

.group-label {
  font-size: 12px;
}

.group-header-spacer {
  width: 38px;
  flex-shrink: 0;
}

/* ================================================
   Messages
   ================================================ */

.group-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  animation: messageIn 0.4s var(--ease-spring-soft) both;
}

.msg-row.self { justify-content: flex-end; }
.msg-row.other { justify-content: flex-start; }

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  margin-bottom: 2px;
}

.self-avatar {
  width: 26px;
  height: 26px;
  font-size: 10px;
  background: linear-gradient(135deg, var(--brand) 0%, var(--brand-active) 100%) !important;
  box-shadow: 0 2px 8px var(--brand-glow) !important;
}

/* ================================================
   Message Card
   ================================================ */

.msg-card {
  display: inline-flex;
  flex-direction: column;
  width: fit-content;
  max-width: 68%;
  padding: 9px 15px;
  border-radius: 24px;
  background: var(--glass-2);
  backdrop-filter: var(--blur-lg);
  -webkit-backdrop-filter: var(--blur-lg);
  border: 1px solid var(--glass-border-3);
  box-shadow: var(--shadow-sm);
  transition: all var(--duration-normal) var(--ease-spring-soft);
}

.msg-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.msg-row.other .msg-card {
  border-bottom-left-radius: 8px;
  background: var(--glass-1);
}

.msg-row.self .msg-card {
  border-bottom-right-radius: 8px;
  background: rgba(26, 173, 94, 0.13);
  border-color: rgba(26, 173, 94, 0.18);
}

.msg-sender {
  font-size: 10px;
  color: var(--text-tertiary);
  margin-bottom: 2px;
  font-weight: 600;
}

.msg-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.45;
  font-size: 15px;
  color: var(--text-primary);
}

/* ================================================
   Input
   ================================================ */

.group-input-bar {
  flex-shrink: 0;
  padding: 12px 16px 16px;
  margin: 0 12px 12px;
}

.group-input-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 6px 6px 18px;
  background: var(--glass-1);
  backdrop-filter: var(--blur-xl);
  -webkit-backdrop-filter: var(--blur-xl);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  transition: all var(--duration-normal) var(--ease-out-expo);
}

.group-input-wrapper:focus-within {
  border-color: rgba(26, 173, 94, 0.3);
  box-shadow: 0 0 0 3px var(--brand-glow), var(--shadow-md);
}

.group-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  font-family: inherit;
  color: var(--text-primary);
  padding: 8px 0;
}

.group-input::placeholder {
  color: var(--text-tertiary);
}

.group-send-btn {
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

.group-send-btn.ready {
  background: var(--brand);
  color: #fff;
  box-shadow: 0 2px 8px var(--brand-glow);
}

.group-send-btn.ready:hover {
  background: var(--brand-hover);
  box-shadow: 0 6px 20px var(--brand-glow);
  transform: scale(1.08);
}

.group-send-btn:active {
  transform: scale(0.92);
}

/* Scrollbar */
.group-messages::-webkit-scrollbar { width: 3px; }
.group-messages::-webkit-scrollbar-track { background: transparent; }
.group-messages::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.08); border-radius: 999px; }
.group-messages::-webkit-scrollbar-thumb:hover { background: rgba(0,0,0,0.15); }

/* ================================================
   Responsive
   ================================================ */

@media (max-width: 640px) {
  .group-topbar {
    margin: 8px 8px 0;
    padding: 10px 12px;
    border-radius: var(--radius-md);
  }

  .group-messages {
    padding: 10px 10px;
    gap: 5px;
  }

  .msg-card {
    max-width: 82%;
    padding: 8px 13px;
    border-radius: 22px;
  }

  .msg-row.other .msg-card { border-bottom-left-radius: 6px; }
  .msg-row.self .msg-card { border-bottom-right-radius: 6px; }

  .group-input-bar {
    margin: 0 6px 6px;
    padding: 8px 10px 10px;
  }
}
</style>

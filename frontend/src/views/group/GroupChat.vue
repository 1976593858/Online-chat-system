<template>
  <div class="chat-box">
    <div class="chat-header">群聊 - {{ groupName }}</div>

    <div class="chat-body" ref="bodyRef">
      <div
        v-for="(m, i) in messages"
        :key="i"
        :class="['msg-row', Number(m.userId) === myId ? 'mine' : 'other']"
      >
        <span class="avatar">{{ String(m.userId).slice(-2) }}</span>
        <div class="bubble">{{ m.content }}</div>
      </div>
    </div>

    <div class="chat-input">
      <input v-model="text" @keyup.enter="send" placeholder="输入群消息…" />
      <button @click="send">发送</button>
    </div>
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
    // 接口失败也本地回显，避免丢失消息体验
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
.chat-box {
  display: flex;
  flex-direction: column;
  height: 100vh;
  font-family: inherit;
  background:
    radial-gradient(ellipse 65% 45% at 20% 10%, rgba(7, 193, 96, 0.15),  transparent 50%),
    radial-gradient(ellipse 55% 50% at 80% 15%, rgba(255, 107, 53, 0.10), transparent 50%),
    radial-gradient(ellipse 50% 40% at 60% 85%, rgba(0, 122, 255, 0.10),  transparent 50%),
    linear-gradient(175deg, #f5f5f7 0%, #efeff4 35%, #e8e8ed 100%);
}

/* Glass header */
.chat-header {
  padding: 16px 24px;
  background: var(--glass-bg);
  backdrop-filter: var(--blur-heavy);
  -webkit-backdrop-filter: var(--blur-heavy);
  border-bottom: 1px solid var(--line);
  color: var(--ink);
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.02em;
  z-index: 1;
}

/* Message area — very transparent, colorful background bleeds through */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  animation: msgIn 0.28s cubic-bezier(0.22, 0.61, 0.36, 1);
}

@keyframes msgIn {
  from { opacity: 0; transform: translateY(14px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.msg-row.mine {
  flex-direction: row-reverse;
}

/* Gradient avatars */
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--brand) 0%, var(--brand-strong) 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 2px 8px var(--brand-glow);
}

.msg-row.mine .avatar {
  background: linear-gradient(135deg, var(--accent) 0%, #e85d2a 100%);
  box-shadow: 0 2px 8px var(--accent-glow);
}

/* Glass chat bubbles */
.bubble {
  max-width: 60%;
  padding: 13px 18px;
  border-radius: 22px;
  background: var(--glass-bg-hover);
  backdrop-filter: var(--blur-light);
  -webkit-backdrop-filter: var(--blur-light);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-xs);
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 15px;
  line-height: 1.55;
  transition: all var(--transition-fast);
}

.bubble:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.msg-row.mine .bubble {
  background: rgba(7, 193, 96, 0.14);
  border-color: rgba(7, 193, 96, 0.22);
}

/* Glass input bar */
.chat-input {
  display: flex;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--line);
  background: var(--glass-bg);
  backdrop-filter: var(--blur-heavy);
  -webkit-backdrop-filter: var(--blur-heavy);
}

.chat-input input {
  flex: 1;
  padding: 11px 18px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: var(--glass-bg);
  backdrop-filter: var(--blur-subtle);
  -webkit-backdrop-filter: var(--blur-subtle);
  outline: none;
  font-size: 15px;
  font-family: inherit;
  transition: all var(--transition-fast);
}

.chat-input input:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-glow);
}

.chat-input input::placeholder {
  color: var(--muted);
}

.chat-input button {
  padding: 11px 28px;
  border: none;
  border-radius: 14px;
  background: var(--brand);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--transition-spring);
  box-shadow: 0 2px 10px var(--brand-glow);
}

.chat-input button:hover {
  background: var(--brand-light);
  box-shadow: 0 6px 22px var(--brand-glow);
  transform: translateY(-1px);
}

.chat-input button:active {
  transform: scale(0.96);
}

@media (max-width: 640px) {
  .chat-body {
    padding: 14px 16px;
    gap: 10px;
  }

  .chat-input {
    padding: 12px 16px;
    gap: 8px;
  }

  .bubble {
    max-width: 78%;
  }
}
</style>

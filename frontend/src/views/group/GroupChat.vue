<template>
  <div class="chat-box">
    <!-- 头部 -->
    <div class="chat-header">💬 {{ groupName }}</div>

    <!-- 消息区 -->
    <div class="chat-body" ref="bodyRef">
      <div
        v-for="(m, i) in messages"
        :key="i"
        :class="['msg-row', m.userId === myId ? 'mine' : 'other']"
      >
        <span class="avatar">{{ m.userId?.slice(-2) }}</span>
        <div class="bubble">{{ m.content }}</div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="chat-input">
      <input v-model="text" @keyup.enter="send" placeholder="输入群消息…" />
      <button @click="send">发送</button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { connectWS } from '../../api/chat.js'

const props = defineProps({
  groupId: { type: String, default: 'group_001' },
  myId:    { type: String, default: 'user_001' }
})

const groupName = ref('全栈开发交流群')
const text = ref('')
const messages = ref([])
const bodyRef = ref(null)
let ws = null

onMounted(() => {
  ws = connectWS(props.myId, (data) => {
    messages.value.push(data)
    scrollBottom()
  })
})

onUnmounted(() => { ws && ws.close() })

async function send() {
  if (!text.value.trim()) return
  const payload = {
    type: 'group_message',
    groupId: props.groupId,
    userId: props.myId,
    content: text.value,
    timestamp: Date.now()
  }
  // 走 HTTP 存库 + 触发广播
  await fetch('http://localhost:8080/api/group/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  // 本地立刻回显
  messages.value.push(payload)
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
.chat-box  { display:flex; flex-direction:column; height:100vh; font-family:sans-serif; }
.chat-header{ padding:12px 16px; background:#2b2b2b; color:#fff; }
.chat-body  { flex:1; overflow:auto; padding:16px; background:#f5f5f5; }
.msg-row    { display:flex; align-items:flex-start; margin-bottom:10px; gap:8px; }
.msg-row.mine{ flex-direction:row-reverse; }
.avatar     { width:36px;height:36px;border-radius:50%;background:#409eff;color:#fff;
              display:flex;align-items:center;justify-content:center;font-size:12px;flex-shrink:0;}
.bubble     { max-width:65%; padding:10px 14px; border-radius:12px; background:#fff;
              box-shadow:0 1px 3px rgba(0,0,0,.08); white-space:pre-wrap; word-break:break-all; }
.mine .bubble{ background:#95ec69; }
.chat-input { display:flex; gap:8px; padding:10px; border-top:1px solid #ddd; background:#fff; }
.chat-input input{ flex:1; padding:8px 12px; border-radius:8px; border:1px solid #ccc; outline:none; }
.chat-input button{ padding:8px 18px;border:none;border-radius:8px;background:#07c160;color:#fff;cursor:pointer;}
</style>
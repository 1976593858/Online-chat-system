<template>
  <main class="group-shell">
    <!-- Header -->
    <header class="group-topbar">
      <button class="back-btn" @click="router.push('/groups')">
        <span>←</span>
      </button>
      <div class="group-header-info">
        <div class="group-name">
          {{ groupName || '加载中…' }}
          <span class="group-id-tag">#{{ groupId }}</span>
        </div>
        <div class="group-label muted">
          {{ memberCount }} 位成员
          <span v-if="inviteCode" class="invite-code-chip">码 {{ inviteCode }}</span>
        </div>
      </div>
      <div class="group-topbar-actions">
        <button
          class="action-btn"
          :class="{ muted: groupMuted }"
          :title="groupMuted ? '取消免打扰' : '开启免打扰'"
          @click="toggleGroupMute"
        >
          <span>{{ groupMuted ? '🔕' : '🔔' }}</span>
        </button>
        <button class="action-btn primary" @click="startGroupVoiceCall" title="群语音通话">
          <span>📞</span>
        </button>
        <button class="action-btn" @click="showInviteDialog = true" title="邀请入群">
          <span>📩</span>
        </button>
        <button class="action-btn" :class="{ active: showMembers }" @click="showMembers = !showMembers" title="群成员">
          <span>👥</span>
        </button>
      </div>
    </header>

    <!-- Body -->
    <div class="group-body">
      <!-- Messages -->
      <div class="group-messages" ref="listRef">
        <div class="messages-load-more" v-if="hasMore">
          <button class="load-more-btn" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中…' : '加载更早消息' }}
          </button>
        </div>
        <div v-else class="messages-end muted">— 这是群聊的开始 —</div>

        <div
          v-for="message in messages"
          :key="message.id || message.messageId"
          :class="['message-row', isSelf(message) ? 'self' : 'other']"
        >
          <div v-if="!isSelf(message)" class="message-avatar" :style="memberAvatarStyle(message)">
            {{ firstLetter(getSenderName(message)) }}
          </div>

          <div class="message-card">
            <div class="message-sender" v-if="!isSelf(message)">{{ getSenderName(message) }}</div>
            <div class="message-text">{{ message.content }}</div>
            <div class="message-time muted">{{ formatTime(message.createdAt || message.timestamp) }}</div>
          </div>

          <div v-if="isSelf(message)" class="message-avatar self-avatar">
            {{ firstLetter(myName) }}
          </div>
        </div>
      </div>

      <!-- Member sidebar -->
      <aside v-if="showMembers" class="member-sidebar glass-highlight">
        <div class="member-sidebar-head">
          <h3 class="member-sidebar-title">群成员</h3>
          <button class="member-sidebar-close" @click="showMembers = false">×</button>
        </div>
        <div class="member-list">
          <div v-for="member in members" :key="member.userId" class="member-item">
            <div class="member-avatar" :style="memberAvatarById(member.userId)">
              {{ firstLetter(member.nickname || member.username || String(member.userId)) }}
            </div>
            <div class="member-info">
              <span class="member-name">{{ member.nickname || member.username || '用户 ' + member.userId }}</span>
              <span v-if="member.role === 'OWNER'" class="member-role owner">群主</span>
              <span v-else-if="member.role === 'ADMIN'" class="member-role admin">管理员</span>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- Input -->
    <footer class="group-input-bar">
      <div class="input-wrapper">
        <textarea
          v-model="draft"
          class="group-textarea"
          placeholder="输入群消息…"
          rows="1"
          maxlength="2000"
          @keydown.ctrl.enter.prevent="send"
          @keydown.enter.exact.prevent="send"
          ref="inputRef"
        ></textarea>
        <button class="send-btn" :class="{ ready: draft.trim() }" :disabled="!draft.trim() || sending" @click="send">
          <span>↑</span>
        </button>
      </div>
    </footer>

    <!-- Invite dialog with user search -->
    <div v-if="showInviteDialog" class="dialog-overlay" @click.self="closeInviteDialog">
      <div class="dialog-card glass-highlight">
        <h3 class="dialog-title">邀请用户入群</h3>
        <input
          v-model="inviteSearchKeyword"
          class="dialog-input"
          placeholder="输入用户名或昵称搜索"
          maxlength="64"
          @input="onInviteSearchInput"
          @keyup.enter="doInviteSearch"
        />
        <div v-if="searchingUsers" class="search-hint muted">搜索中…</div>
        <div class="search-results" v-else-if="searchResults.length">
          <div
            v-for="user in searchResults"
            :key="user.id"
            class="search-result-item"
          >
            <div class="search-result-avatar" :style="memberAvatarById(user.id)">
              {{ firstLetter(user.nickname || user.username) }}
            </div>
            <div class="search-result-info">
              <span class="search-result-name">{{ user.nickname || user.username }}</span>
              <span class="search-result-username muted">@{{ user.username }}</span>
            </div>
            <button
              class="primary-btn"
              :disabled="invitingUserIds.has(user.id)"
              @click="doInviteUser(user)"
            >
              {{ invitingUserIds.has(user.id) ? '邀请中…' : '邀请' }}
            </button>
          </div>
        </div>
        <div v-else-if="inviteSearchKeyword.trim() && !searchingUsers" class="search-hint muted">
          未找到用户
        </div>
        <div v-else class="search-hint muted">输入关键词搜索用户</div>
        <div class="dialog-actions">
          <button class="secondary-btn" @click="closeInviteDialog">关闭</button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { useWebSocketStore } from '../../stores/websocket'
import { useVoiceCallStore } from '../../stores/voiceCall'
import {
  getGroupDetail, getGroupMembers, getGroupMessages, sendGroupMessage,
  inviteToGroup, toggleMuteGroup
} from '../../api/groups'
import { searchUsers } from '../../api/users'

const props = defineProps({
  groupId: { type: String, required: true }
})

const router = useRouter()
const authStore = useAuthStore()
const wsStore = useWebSocketStore()
const voiceCallStore = useVoiceCallStore()

const myId = computed(() => Number(authStore.user?.id))
const myName = computed(() => authStore.user?.nickname || authStore.user?.username || '我')

const groupName = ref('')
const memberCount = ref(0)
const members = ref([])
const messages = ref([])
const showMembers = ref(false)
const inviteCode = ref('')
const groupMuted = ref(false)

const draft = ref('')
const sending = ref(false)
const listRef = ref(null)
const inputRef = ref(null)

const pageNo = ref(1)
const pageSize = ref(20)
const pages = ref(0)
const loading = ref(false)
const hasMore = computed(() => pages.value === 0 || pageNo.value < pages.value)

const msgIds = ref(new Set())
const avatarColors = ['#5E6AD2', '#FF8C42', '#00A8CC', '#1aad5e', '#E84040', '#8C52D2', '#F0A030', '#4A90D9']

// Invite dialog
const showInviteDialog = ref(false)
const inviteSearchKeyword = ref('')
const searchingUsers = ref(false)
const searchResults = ref([])
const invitingUserIds = ref(new Set())
let inviteSearchTimer = null

function closeInviteDialog() {
  showInviteDialog.value = false
  inviteSearchKeyword.value = ''
  searchResults.value = []
  invitingUserIds.value = new Set()
}

function onInviteSearchInput() {
  if (inviteSearchTimer) clearTimeout(inviteSearchTimer)
  inviteSearchTimer = setTimeout(() => doInviteSearch(), 400)
}

async function doInviteSearch() {
  const keyword = inviteSearchKeyword.value.trim()
  if (!keyword) {
    searchResults.value = []
    return
  }
  searchingUsers.value = true
  try {
    const page = await searchUsers({ keyword, pageNo: 1, pageSize: 20 })
    // Filter out self and existing members
    const memberIds = new Set(members.value.map(m => Number(m.userId)))
    searchResults.value = (page.records || []).filter(
      u => Number(u.id) !== myId.value && !memberIds.has(Number(u.id))
    )
  } catch {
    searchResults.value = []
  } finally {
    searchingUsers.value = false
  }
}

async function doInviteUser(user) {
  const userId = Number(user.id)
  const s = new Set(invitingUserIds.value)
  s.add(userId)
  invitingUserIds.value = s
  try {
    await inviteToGroup(Number(props.groupId), { inviteeId: userId })
    ElMessage.success(`已向 ${user.nickname || user.username} 发送邀请`)
    // Remove user from search results
    searchResults.value = searchResults.value.filter(u => Number(u.id) !== userId)
  } catch (e) {
    ElMessage.error(e?.message || '邀请失败')
  } finally {
    const s2 = new Set(invitingUserIds.value)
    s2.delete(userId)
    invitingUserIds.value = s2
  }
}

function firstLetter(value) {
  return value ? String(value).slice(0, 1).toUpperCase() : '?'
}

function isSelf(message) {
  const uid = Number(message.userId || message.fromUserId)
  return uid === myId.value
}

const memberMap = computed(() => {
  const map = {}
  for (const m of members.value) {
    map[m.userId] = m
  }
  return map
})

function getSenderName(message) {
  const uid = Number(message.userId || message.fromUserId)
  const m = memberMap.value[uid]
  if (m) return m.nickname || m.username || '用户 ' + uid
  return '用户 ' + uid
}

function memberAvatarStyle(message) {
  const uid = Number(message.userId || message.fromUserId)
  return memberAvatarById(uid)
}

function memberAvatarById(userId) {
  const color = avatarColors[Number(userId) % avatarColors.length]
  return {
    background: `linear-gradient(135deg, ${color} 0%, ${color}dd 100%)`,
    boxShadow: `0 4px 14px ${color}40`
  }
}

function formatTime(value) {
  if (!value) return ''
  if (typeof value === 'number') {
    const d = new Date(value)
    const h = String(d.getHours()).padStart(2, '0')
    const m = String(d.getMinutes()).padStart(2, '0')
    return `${h}:${m}`
  }
  return String(value).slice(11, 16) || value
}

function handleWsMessage(data) {
  if (data.type === 'group_message' && String(data.groupId) === String(props.groupId)) {
    const id = data.messageId || (data.userId + '_' + data.timestamp)
    if (msgIds.value.has(id)) return
    msgIds.value.add(id)
    messages.value = [...messages.value, data]
    scrollToBottom()
  }

  // Handle group invite notification
  if (data.type === 'group_invite' && data.action === 'invited') {
    ElMessage.info(`你被邀请加入群聊 "${data.groupName}"`)
  }
}

onMounted(async () => {
  if (!wsStore.isConnected()) wsStore.connect()
  wsStore.addHandler(handleWsMessage)
  // Register voice call signaling handler so we receive group call notifications
  voiceCallStore.setupSignaling()
  await loadGroupInfo()
  await loadMembers()
  await loadFirstPage()
})

onUnmounted(() => {
  wsStore.removeHandler(handleWsMessage)
})

async function loadGroupInfo() {
  try {
    const g = await getGroupDetail(Number(props.groupId))
    groupName.value = g.name
    memberCount.value = g.memberCount || 0
    inviteCode.value = g.inviteCode || ''
  } catch (e) {
    groupName.value = '群聊 ' + props.groupId
  }
}

async function loadMembers() {
  try {
    members.value = await getGroupMembers(Number(props.groupId))
    memberCount.value = members.value.length
    // Check if current user has muted this group
    const me = members.value.find(m => String(m.userId) === String(myId.value))
    if (me) {
      groupMuted.value = me.muted === 1 || me.muted === true
    }
  } catch (e) {
    // silently fail
  }
}

async function loadFirstPage() {
  try {
    pageNo.value = 1
    const page = await loadPage(pageNo.value)
    pages.value = page.pages
    messages.value = page.records.slice().reverse()
    for (const m of messages.value) {
      msgIds.value.add(m.id || (m.userId + '_' + m.timestamp))
    }
    await scrollToBottom()
  } catch {
    // error already shown by interceptor
  }
}

async function loadMore() {
  if (!hasMore.value || loading.value) return
  try {
    pageNo.value += 1
    const page = await loadPage(pageNo.value)
    pages.value = page.pages
    const older = page.records.slice().reverse()
    for (const m of older) {
      if (msgIds.value.has(m.id || (m.userId + '_' + m.timestamp))) continue
      msgIds.value.add(m.id || (m.userId + '_' + m.timestamp))
      messages.value.unshift(m)
    }
  } catch {
    pageNo.value -= 1
  }
}

async function loadPage(no) {
  loading.value = true
  try {
    return await getGroupMessages(Number(props.groupId), { pageNo: no, pageSize: pageSize.value })
  } finally {
    loading.value = false
  }
}

async function send() {
  const content = draft.value.trim()
  if (!content) return
  sending.value = true
  try {
    await sendGroupMessage({ groupId: props.groupId, content })
    draft.value = ''
    inputRef.value?.focus()
  } catch (e) {
    console.warn('群消息发送失败', e)
  } finally {
    sending.value = false
  }
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

// Mute toggle
async function toggleGroupMute() {
  try {
    const newMuted = !groupMuted.value
    await toggleMuteGroup(Number(props.groupId), newMuted)
    groupMuted.value = newMuted
    ElMessage.success(newMuted ? '已开启免打扰' : '已取消免打扰')
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

// Voice call
function startGroupVoiceCall() {
  voiceCallStore.startGroupCall(props.groupId, groupName.value)
}

</script>

<style scoped>
.group-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

/* ================================================
   Top Bar
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
  flex: 1;
  min-width: 0;
}

.group-name {
  font-weight: 680;
  font-size: 16px;
  color: var(--text-primary);
  line-height: 1.2;
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-id-tag {
  font-size: 11px;
  font-weight: 600;
  color: var(--brand);
  background: var(--brand-soft);
  padding: 1px 8px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.group-label {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.invite-code-chip {
  padding: 1px 8px;
  border-radius: var(--radius-full);
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  user-select: all;
}

.group-topbar-actions {
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

.action-btn:hover {
  background: var(--glass-2);
  color: var(--text-primary);
  transform: scale(1.08);
}

.action-btn.active {
  background: var(--brand);
  color: #fff;
  box-shadow: 0 2px 8px var(--brand-glow);
}

.action-btn.muted {
  opacity: 0.6;
  background: rgba(0,0,0,0.04);
}

.action-btn.primary {
  background: var(--brand);
  color: #fff;
  box-shadow: 0 2px 8px var(--brand-glow);
}

.action-btn.primary:hover {
  background: var(--brand-hover);
  box-shadow: 0 6px 20px var(--brand-glow);
}

/* ================================================
   Body: messages + sidebar
   ================================================ */

.group-body {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

/* === Messages === */

.group-messages {
  flex: 1;
  min-width: 0;
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

.message-row.self { justify-content: flex-end; }
.message-row.other { justify-content: flex-start; }

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

/* === Message Card === */

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

.message-row.other .message-card {
  border-bottom-left-radius: 10px;
  background: var(--glass-1);
}

.message-row.self .message-card {
  border-bottom-right-radius: 10px;
  background: rgba(26, 173, 94, 0.13);
  border-color: rgba(26, 173, 94, 0.18);
}

.message-sender {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-bottom: 3px;
  font-weight: 600;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.45;
  font-size: 15px;
  color: var(--text-primary);
}

.message-time {
  font-size: 10px;
  margin-top: 3px;
  align-self: flex-end;
}

/* === Member Sidebar === */

.member-sidebar {
  width: 240px;
  flex-shrink: 0;
  border-left: 1px solid var(--glass-border-2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideInRight 0.3s var(--ease-spring-smooth) both;
}

@keyframes slideInRight {
  from { opacity: 0; transform: translateX(20px); }
  to { opacity: 1; transform: translateX(0); }
}

.member-sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--glass-border-1);
}

.member-sidebar-title {
  font-weight: 680;
  font-size: 14px;
  color: var(--text-primary);
  margin: 0;
}

.member-sidebar-close {
  border: none;
  background: var(--glass-3);
  color: var(--text-secondary);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-fast) var(--ease-out-expo);
}

.member-sidebar-close:hover {
  background: var(--glass-2);
  color: var(--text-primary);
}

.member-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  transition: background var(--duration-fast) var(--ease-out-expo);
}

.member-item:hover {
  background: var(--glass-3);
}

.member-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.member-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.member-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-role {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  font-weight: 600;
  flex-shrink: 0;
}

.member-role.owner {
  background: rgba(240, 160, 48, 0.15);
  color: #e6a01e;
}

.member-role.admin {
  background: rgba(94, 106, 210, 0.15);
  color: #7B83E8;
}

.invite-member-btn {
  width: 28px;
  height: 28px;
  border: 1px solid var(--glass-border-2);
  border-radius: 50%;
  background: transparent;
  color: var(--text-tertiary);
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-fast) var(--ease-out-expo);
  flex-shrink: 0;
  opacity: 0;
}

.member-item:hover .invite-member-btn {
  opacity: 1;
}

.invite-member-btn:hover {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}

/* ================================================
   Input Bar
   ================================================ */

.group-input-bar {
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

.group-textarea {
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

.group-textarea::placeholder {
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
   Dialog
   ================================================ */

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.dialog-card {
  width: 400px;
  max-width: calc(100vw - 48px);
  padding: 28px;
  border-radius: var(--radius-xl);
  border: 1px solid var(--glass-border-2);
  display: flex;
  flex-direction: column;
  gap: 16px;
  animation: springIn 0.45s var(--ease-spring-smooth) both;
}

.dialog-title {
  font-weight: 700;
  font-size: 18px;
  color: var(--text-primary);
  margin: 0;
}

.dialog-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-md);
  background: var(--glass-2);
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  transition: border-color var(--duration-fast) var(--ease-out-expo);
}

.dialog-input:focus {
  border-color: rgba(26, 173, 94, 0.3);
  box-shadow: 0 0 0 3px var(--brand-glow);
}

.dialog-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.primary-btn {
  padding: 8px 18px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--brand);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-expo);
  font-family: inherit;
  box-shadow: 0 2px 8px var(--brand-glow);
}

.primary-btn:hover:not(:disabled) {
  background: var(--brand-hover);
  box-shadow: 0 6px 20px var(--brand-glow);
  transform: translateY(-1px);
}

.primary-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.secondary-btn {
  padding: 8px 18px;
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-full);
  background: var(--glass-2);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-expo);
  font-family: inherit;
}

.secondary-btn:hover {
  background: var(--glass-1);
  color: var(--text-primary);
  border-color: var(--glass-border-3);
}

/* ================================================
   Scrollbar
   ================================================ */

.group-messages::-webkit-scrollbar,
.member-list::-webkit-scrollbar { width: 3px; }
.group-messages::-webkit-scrollbar-track,
.member-list::-webkit-scrollbar-track { background: transparent; }
.group-messages::-webkit-scrollbar-thumb,
.member-list::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.08); border-radius: 999px; }
.group-messages::-webkit-scrollbar-thumb:hover,
.member-list::-webkit-scrollbar-thumb:hover { background: rgba(0,0,0,0.15); }

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
    gap: 6px;
  }

  .message-card {
    max-width: 82%;
    padding: 8px 14px;
    border-radius: 24px;
  }

  .message-row.other .message-card { border-bottom-left-radius: 8px; }
  .message-row.self .message-card { border-bottom-right-radius: 8px; }

  .group-input-bar {
    margin: 0 6px 6px;
    padding: 8px 10px 10px;
  }

  .member-sidebar {
    width: 200px;
  }
}

/* ================================================
   Search Results (in invite dialog)
   ================================================ */

.search-hint {
  text-align: center;
  padding: 16px 0;
  font-size: 13px;
}
.search-results {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 280px;
  overflow-y: auto;
}
.search-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: var(--radius-md);
  transition: background var(--duration-fast) var(--ease-out-expo);
}
.search-result-item:hover {
  background: var(--glass-3);
}
.search-result-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}
.search-result-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.search-result-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.search-result-username {
  font-size: 11px;
  color: var(--text-tertiary);
}
</style>

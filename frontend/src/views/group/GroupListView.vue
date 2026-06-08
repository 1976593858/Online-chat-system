<template>
  <main class="groups-shell">
    <!-- Header -->
    <header class="groups-topbar">
      <div class="groups-brand">
        <div class="groups-brand-icon">◇</div>
        <div class="groups-brand-text">Online Chat</div>
      </div>
      <nav class="nav-links">
        <RouterLink class="nav-link" to="/friends">好友</RouterLink>
        <RouterLink class="nav-link" to="/conversations">消息</RouterLink>
        <RouterLink class="nav-link active" to="/groups">群聊</RouterLink>
        <RouterLink class="nav-link" to="/chat-history">搜索</RouterLink>
        <button class="nav-logout" @click="logout">退出</button>
      </nav>
    </header>

    <!-- Content -->
    <section class="groups-main glass-highlight">
      <div class="groups-head">
        <h2 class="groups-title">群聊</h2>
        <div class="groups-head-actions">
          <button
            v-if="pendingInvites.length"
            class="invite-badge-btn"
            @click="showInvitesDialog = true"
          >
            {{ pendingInvites.length }} 个邀请
          </button>
          <button class="primary-btn" @click="showCreateDialog = true">创建群聊</button>
          <button class="secondary-btn" @click="showJoinDialog = true">加入群聊</button>
        </div>
      </div>

      <!-- Group list -->
      <div class="groups-stack" v-if="groups.length">
        <article
          v-for="group in groups"
          :key="group.id"
          class="group-card"
          @click="router.push(`/group/${group.id}`)"
        >
          <div class="group-card-avatar" :style="groupAvatarStyle(group)">
            {{ firstLetter(group.name) }}
          </div>
          <div class="group-card-content">
            <div class="group-card-name">{{ group.name }}</div>
            <div class="group-card-meta muted">
              <span class="group-id-label">#{{ group.id }}</span>
              · {{ group.memberCount ?? 0 }} 位成员
              <span v-if="group.announcement"> · {{ group.announcement }}</span>
              <span v-if="group.inviteCode" class="invite-code-label"> · 码: {{ group.inviteCode }}</span>
            </div>
          </div>
          <div class="group-card-arrow">→</div>
        </article>
      </div>

      <div v-else-if="!loading" class="groups-empty">
        <div class="groups-empty-icon">◇</div>
        <div class="groups-empty-text">暂无群聊</div>
        <div class="groups-empty-sub muted">创建或加入一个群聊开始交流</div>
      </div>

      <div v-if="loading" class="groups-loading muted">加载中…</div>
    </section>

    <!-- Create Group Dialog -->
    <div v-if="showCreateDialog" class="dialog-overlay" @click.self="showCreateDialog = false">
      <div class="dialog-card glass-highlight">
        <h3 class="dialog-title">创建群聊</h3>
        <input v-model="createForm.name" class="dialog-input" placeholder="群聊名称" maxlength="64" @keyup.enter="doCreate" />
        <textarea v-model="createForm.announcement" class="dialog-textarea" placeholder="群公告（可选）" rows="2" maxlength="500"></textarea>
        <div class="dialog-actions">
          <button class="secondary-btn" @click="showCreateDialog = false">取消</button>
          <button class="primary-btn" :disabled="!createForm.name.trim() || creating" @click="doCreate">
            {{ creating ? '创建中…' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Join Group Dialog -->
    <div v-if="showJoinDialog" class="dialog-overlay" @click.self="closeJoinDialog">
      <div class="dialog-card glass-highlight">
        <h3 class="dialog-title">加入群聊</h3>
        <input
          v-model="joinInput"
          class="dialog-input"
          placeholder="输入群聊 ID 或邀请码"
          @keyup.enter="doJoinPreview"
        />

        <!-- Preview card after input -->
        <div v-if="previewGroup" class="preview-card glass-highlight">
          <div class="preview-avatar" :style="groupAvatarStyle(previewGroup)">
            {{ firstLetter(previewGroup.name) }}
          </div>
          <div class="preview-content">
            <div class="preview-name">{{ previewGroup.name }}</div>
            <div class="preview-meta muted">{{ previewGroup.memberCount ?? 0 }} 位成员</div>
            <div v-if="previewGroup.announcement" class="preview-announce muted">{{ previewGroup.announcement }}</div>
          </div>
        </div>
        <div v-if="previewError" class="preview-error">{{ previewError }}</div>

        <div class="dialog-actions">
          <button class="secondary-btn" @click="closeJoinDialog">取消</button>
          <button
            v-if="!previewGroup"
            class="primary-btn"
            :disabled="!joinInput.trim() || joinPreviewing"
            @click="doJoinPreview"
          >
            {{ joinPreviewing ? '查找中…' : '查找' }}
          </button>
          <button
            v-else
            class="primary-btn"
            :disabled="joining"
            @click="doJoin"
          >
            {{ joining ? '加入中…' : '加入群聊' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Pending Invites Dialog -->
    <div v-if="showInvitesDialog" class="dialog-overlay" @click.self="showInvitesDialog = false">
      <div class="dialog-card glass-highlight" style="max-height: 70vh; overflow-y: auto;">
        <h3 class="dialog-title">待处理的邀请</h3>
        <div v-if="pendingInvites.length" class="invites-stack">
          <div v-for="inv in pendingInvites" :key="inv.id" class="invite-card">
            <div class="invite-card-info">
              <div class="invite-group-name">{{ inv.groupName || '群聊 ' + inv.groupId }}</div>
              <div class="invite-sender muted">
                来自: {{ inv.senderNickname || inv.senderUsername || '用户 ' + inv.senderId }}
              </div>
            </div>
            <div class="invite-card-actions">
              <button class="accept-btn" @click="handleAccept(inv.id)">接受</button>
              <button class="reject-btn" @click="handleReject(inv.id)">拒绝</button>
            </div>
          </div>
        </div>
        <div v-else class="invites-empty muted">暂无待处理的邀请</div>
        <div class="dialog-actions">
          <button class="secondary-btn" @click="showInvitesDialog = false">关闭</button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { useWebSocketStore } from '../../stores/websocket'
import {
  createGroup, joinGroup, listGroups, getGroupPreview,
  joinByInviteCode, getPendingInvites, acceptInvite, rejectInvite
} from '../../api/groups'

const router = useRouter()
const authStore = useAuthStore()
const wsStore = useWebSocketStore()

const groups = ref([])
const loading = ref(false)

const showCreateDialog = ref(false)
const showJoinDialog = ref(false)
const showInvitesDialog = ref(false)
const joinInput = ref('')
const creating = ref(false)
const joining = ref(false)
const joinPreviewing = ref(false)
const createForm = ref({ name: '', announcement: '' })

const previewGroup = ref(null)
const previewError = ref('')
const pendingInvites = ref([])

const avatarColors = ['#5E6AD2', '#FF8C42', '#00A8CC', '#1aad5e', '#E84040', '#8C52D2', '#F0A030', '#4A90D9']

function firstLetter(value) {
  return value ? String(value).slice(0, 1).toUpperCase() : '?'
}

function groupAvatarStyle(group) {
  const color = avatarColors[Number(group.id) % avatarColors.length]
  return {
    background: `linear-gradient(135deg, ${color} 0%, ${color}dd 100%)`,
    boxShadow: `0 6px 20px ${color}40`
  }
}

function logout() {
  authStore.logout()
  router.push('/login')
}

async function loadGroups() {
  loading.value = true
  try {
    groups.value = await listGroups()
  } catch (e) {
    ElMessage.error(e?.message || '加载群聊列表失败')
  } finally {
    loading.value = false
  }
}

async function loadPendingInvites() {
  try {
    pendingInvites.value = await getPendingInvites()
  } catch (e) {
    // silently fail
  }
}

async function doCreate() {
  const name = createForm.value.name.trim()
  if (!name) return
  creating.value = true
  try {
    const group = await createGroup({ name, announcement: createForm.value.announcement.trim() })
    ElMessage.success('群聊创建成功')
    showCreateDialog.value = false
    createForm.value = { name: '', announcement: '' }
    router.push(`/group/${group.id}`)
  } catch (e) {
    ElMessage.error(e?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

function closeJoinDialog() {
  showJoinDialog.value = false
  joinInput.value = ''
  previewGroup.value = null
  previewError.value = ''
}

async function doJoinPreview() {
  const input = joinInput.value.trim()
  if (!input) return
  joinPreviewing.value = true
  previewGroup.value = null
  previewError.value = ''
  try {
    // Try as numeric ID first, then as invite code
    const isNumeric = /^\d+$/.test(input)
    if (isNumeric) {
      previewGroup.value = await getGroupPreview(Number(input))
    } else {
      previewGroup.value = await joinByInviteCode(input)
    }
  } catch (e) {
    previewError.value = e?.message || '未找到群聊，请检查 ID 或邀请码'
  } finally {
    joinPreviewing.value = false
  }
}

async function doJoin() {
  if (!previewGroup.value) return
  joining.value = true
  try {
    await joinGroup(Number(previewGroup.value.id))
    ElMessage.success('已加入群聊')
    closeJoinDialog()
    await loadGroups()
  } catch (e) {
    ElMessage.error(e?.message || '加入失败')
  } finally {
    joining.value = false
  }
}

async function handleAccept(inviteId) {
  try {
    await acceptInvite(inviteId)
    ElMessage.success('已加入群聊')
    await Promise.all([loadPendingInvites(), loadGroups()])
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function handleReject(inviteId) {
  try {
    await rejectInvite(inviteId)
    ElMessage.success('已拒绝邀请')
    await loadPendingInvites()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function handleWsInvite(data) {
  if (data.type === 'group_invite' && data.action === 'invited') {
    ElMessage.info(`你被邀请加入群聊 "${data.groupName}"`)
    loadPendingInvites()
  }
}

onMounted(() => {
  loadGroups()
  loadPendingInvites()
  wsStore.addHandler(handleWsInvite)
})

onUnmounted(() => {
  wsStore.removeHandler(handleWsInvite)
})
</script>

<style scoped>
.groups-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

/* ================================================
   Top Bar
   ================================================ */

.groups-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  margin: 12px 16px 0;
  flex-shrink: 0;
  background: var(--glass-1);
  backdrop-filter: var(--blur-xl);
  -webkit-backdrop-filter: var(--blur-xl);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.groups-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.groups-brand-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--brand) 0%, var(--brand-active) 100%);
  color: #fff;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.groups-brand-text {
  font-weight: 700;
  font-size: 16px;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-link {
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all var(--duration-fast) var(--ease-out-expo);
}

.nav-link:hover { color: var(--text-primary); background: var(--glass-3); }
.nav-link.active { color: var(--text-primary); background: var(--glass-2); }

.nav-logout {
  margin-left: 12px;
  padding: 6px 14px;
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-expo);
  font-family: inherit;
}

.nav-logout:hover { color: var(--danger); border-color: var(--danger); background: rgba(232,64,64,0.06); }

/* ================================================
   Main
   ================================================ */

.groups-main {
  flex: 1;
  margin: 16px;
  padding: 24px;
  border-radius: var(--radius-xl);
  overflow-y: auto;
}

.groups-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.groups-title {
  font-weight: 720;
  font-size: 20px;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  margin: 0;
}

.groups-head-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.invite-badge-btn {
  padding: 8px 16px;
  border: 1px solid var(--brand);
  border-radius: var(--radius-full);
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-expo);
  font-family: inherit;
  animation: pulseBadge 2s ease infinite;
}

.invite-badge-btn:hover {
  background: var(--brand);
  color: #fff;
}

@keyframes pulseBadge {
  0%, 100% { box-shadow: 0 0 0 0 rgba(26, 173, 94, 0.3); }
  50% { box-shadow: 0 0 0 8px rgba(26, 173, 94, 0); }
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
   Group Cards
   ================================================ */

.groups-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.group-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: var(--glass-2);
  backdrop-filter: var(--blur-lg);
  -webkit-backdrop-filter: var(--blur-lg);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-spring-soft);
}

.group-card:hover {
  background: var(--glass-1);
  border-color: var(--glass-border-3);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.group-card:active {
  transform: translateY(0);
}

.group-card-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.group-card-content {
  flex: 1;
  min-width: 0;
}

.group-card-name {
  font-weight: 680;
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.3;
}

.group-card-meta {
  font-size: 12px;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-id-label {
  color: var(--brand);
  font-weight: 700;
}

.invite-code-label {
  color: var(--brand);
  font-weight: 600;
}

.group-card-arrow {
  font-size: 18px;
  color: var(--text-tertiary);
  flex-shrink: 0;
  transition: transform var(--duration-fast) var(--ease-out-expo);
}

.group-card:hover .group-card-arrow {
  transform: translateX(4px);
}

/* ================================================
   Empty / Loading
   ================================================ */

.groups-empty {
  text-align: center;
  padding: 64px 0;
}

.groups-empty-icon {
  font-size: 48px;
  color: var(--text-tertiary);
  margin-bottom: 12px;
}

.groups-empty-text {
  font-size: 18px;
  font-weight: 680;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.groups-empty-sub {
  font-size: 13px;
}

.groups-loading {
  text-align: center;
  padding: 32px;
  font-size: 13px;
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

.dialog-textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-md);
  background: var(--glass-2);
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  resize: vertical;
  transition: border-color var(--duration-fast) var(--ease-out-expo);
}

.dialog-textarea:focus {
  border-color: rgba(26, 173, 94, 0.3);
  box-shadow: 0 0 0 3px var(--brand-glow);
}

.dialog-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

/* ================================================
   Preview
   ================================================ */

.preview-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--glass-border-2);
}

.preview-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.preview-content {
  flex: 1;
  min-width: 0;
}

.preview-name {
  font-weight: 680;
  font-size: 16px;
  color: var(--text-primary);
}

.preview-meta {
  font-size: 12px;
  margin-top: 2px;
}

.preview-announce {
  font-size: 12px;
  margin-top: 4px;
  font-style: italic;
}

.preview-error {
  font-size: 13px;
  color: var(--danger);
  text-align: center;
  padding: 4px 0;
}

/* ================================================
   Invites
   ================================================ */

.invites-stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.invite-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--glass-2);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-md);
}

.invite-card-info {
  flex: 1;
  min-width: 0;
}

.invite-group-name {
  font-weight: 680;
  font-size: 15px;
  color: var(--text-primary);
}

.invite-sender {
  font-size: 12px;
  margin-top: 2px;
}

.invite-card-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.accept-btn {
  padding: 6px 14px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--brand);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-expo);
  font-family: inherit;
}

.accept-btn:hover {
  background: var(--brand-hover);
}

.reject-btn {
  padding: 6px 14px;
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-expo);
  font-family: inherit;
}

.reject-btn:hover {
  border-color: var(--danger);
  color: var(--danger);
  background: rgba(232,64,64,0.06);
}

.invites-empty {
  text-align: center;
  padding: 16px 0;
  font-size: 13px;
}
</style>

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
              {{ group.memberCount ?? 0 }} 位成员
              <span v-if="group.announcement"> · {{ group.announcement }}</span>
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
    <div v-if="showJoinDialog" class="dialog-overlay" @click.self="showJoinDialog = false">
      <div class="dialog-card glass-highlight">
        <h3 class="dialog-title">加入群聊</h3>
        <input v-model="joinGroupId" class="dialog-input" placeholder="输入群聊 ID" @keyup.enter="doJoin" />
        <div class="dialog-actions">
          <button class="secondary-btn" @click="showJoinDialog = false">取消</button>
          <button class="primary-btn" :disabled="!joinGroupId.trim() || joining" @click="doJoin">
            {{ joining ? '加入中…' : '加入' }}
          </button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { createGroup, joinGroup, listGroups } from '../../api/groups'

const router = useRouter()
const authStore = useAuthStore()

const groups = ref([])
const loading = ref(false)

const showCreateDialog = ref(false)
const showJoinDialog = ref(false)
const joinGroupId = ref('')
const creating = ref(false)
const joining = ref(false)
const createForm = ref({ name: '', announcement: '' })

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

async function doJoin() {
  const id = joinGroupId.value.trim()
  if (!id) return
  joining.value = true
  try {
    await joinGroup(Number(id))
    ElMessage.success('已加入群聊')
    showJoinDialog.value = false
    joinGroupId.value = ''
    await loadGroups()
  } catch (e) {
    ElMessage.error(e?.message || '加入失败')
  } finally {
    joining.value = false
  }
}

onMounted(() => {
  loadGroups()
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
</style>

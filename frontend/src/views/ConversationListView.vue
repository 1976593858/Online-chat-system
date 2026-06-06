<template>
  <main class="conv-shell">
    <!-- Header -->
    <header class="conv-topbar">
      <div class="conv-brand">
        <div class="conv-brand-icon">◇</div>
        <div class="conv-brand-text">Online Chat</div>
      </div>
      <nav class="nav-links">
        <RouterLink class="nav-link" to="/friends">好友</RouterLink>
        <RouterLink class="nav-link active" to="/conversations">消息</RouterLink>
        <RouterLink class="nav-link" to="/groups">群聊</RouterLink>
        <RouterLink class="nav-link" to="/chat-history">搜索</RouterLink>
        <button class="nav-logout" @click="logout">退出</button>
      </nav>
    </header>

    <!-- List -->
    <section class="conv-list">
      <div class="conv-list-header">
        <h2 class="conv-title">消息</h2>
        <button class="conv-refresh" :disabled="conversationStore.loading" @click="loadRecent(1)">
          {{ conversationStore.loading ? '刷新中…' : '刷新' }}
        </button>
      </div>

      <div class="conv-stack">
        <article
          v-for="conversation in conversationStore.page.records"
          :key="conversation.id"
          class="conv-card"
          :class="{ unread: conversation.unreadCount }"
          @click="openChat(conversation)"
        >
          <!-- Avatar with badge -->
          <div class="conv-avatar-wrap">
            <div class="conv-avatar" :style="avatarStyle(conversation)">
              {{ firstLetter(conversation.targetNickname || conversation.targetUsername) }}
            </div>
            <span v-if="conversation.unreadCount" class="conv-badge">
              {{ conversation.unreadCount > 99 ? '99+' : conversation.unreadCount }}
            </span>
          </div>

          <!-- Content -->
          <div class="conv-content">
            <div class="conv-name-row">
              <span class="conv-name">{{ conversation.targetNickname || conversation.targetUsername || '未知' }}</span>
              <span v-if="conversation.pinned" class="conv-tag">置顶</span>
              <span v-if="conversation.muted" class="conv-tag muted-tag">免打扰</span>
            </div>
            <div class="conv-preview">
              <span class="conv-type">[{{ conversation.lastMessageType }}]</span>
              {{ conversation.lastMessageContent || '暂无消息' }}
            </div>
          </div>

          <!-- Actions -->
          <div class="conv-meta">
            <button
              class="conv-mute-btn"
              :class="{ muted: conversation.muted }"
              :title="conversation.muted ? '取消免打扰' : '开启免打扰'"
              @click.stop="toggleMute(conversation)"
            >
              {{ conversation.muted ? '🔕' : '🔔' }}
            </button>
            <div class="conv-time">{{ conversation.lastMessageAt }}</div>
          </div>
        </article>

        <div v-if="!conversationStore.loading && conversationStore.page.records.length === 0" class="conv-empty">
          <div class="conv-empty-icon">◇</div>
          <div class="conv-empty-text">暂无会话</div>
          <div class="conv-empty-sub muted">去好友列表开始聊天</div>
        </div>
      </div>

      <el-pagination
        v-if="conversationStore.page.total > conversationStore.page.pageSize"
        layout="prev, pager, next"
        :total="conversationStore.page.total"
        :current-page="conversationStore.page.pageNo"
        :page-size="conversationStore.page.pageSize"
        @current-change="loadRecent"
        small
      />
    </section>
  </main>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { useConversationStore } from '../stores/conversation'
import { toggleMuteConversation } from '../api/conversations'

const router = useRouter()
const authStore = useAuthStore()
const conversationStore = useConversationStore()

onMounted(() => loadRecent(1))

function firstLetter(value) {
  return value ? value.slice(0, 1).toUpperCase() : '?'
}

function avatarStyle(conv) {
  const name = conv.targetNickname || conv.targetUsername || '?'
  const hue = (name.charCodeAt(0) || 65) * 137 % 360
  return {
    background: `linear-gradient(135deg, hsl(${hue}, 55%, 55%) 0%, hsl(${hue}, 60%, 42%) 100%)`,
    boxShadow: `0 4px 14px hsla(${hue}, 55%, 50%, 0.3)`
  }
}

async function loadRecent(pageNo = 1) {
  await conversationStore.loadRecent({
    pageNo,
    pageSize: conversationStore.page.pageSize
  })
}

function logout() {
  authStore.logout()
  router.push('/login')
}

async function toggleMute(conv) {
  try {
    const newMuted = !conv.muted
    await toggleMuteConversation(conv.id, newMuted)
    conv.muted = newMuted
    ElMessage.success(newMuted ? '已开启免打扰' : '已取消免打扰')
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function openChat(conversation) {
  if (!conversation?.targetUserId) return
  router.push({ name: 'chat', params: { targetUserId: conversation.targetUserId } })
}
</script>

<style scoped>
/* ================================================
   Shell
   ================================================ */

.conv-shell {
  max-width: 720px;
  margin: 0 auto;
  padding: 20px 20px 40px;
  min-height: 100vh;
}

/* ================================================
   Header
   ================================================ */

.conv-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.conv-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.conv-brand-icon {
  font-size: 28px;
  color: var(--brand);
  filter: drop-shadow(0 2px 8px var(--brand-glow));
}

.conv-brand-text {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

/* ================================================
   List
   ================================================ */

.conv-list {
  background: var(--glass-1);
  backdrop-filter: var(--blur-xl);
  -webkit-backdrop-filter: var(--blur-xl);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  animation: springIn 0.5s var(--ease-spring-soft) both;
}

.conv-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 14px;
}

.conv-title {
  margin: 0;
  font-size: 22px;
  font-weight: 780;
  letter-spacing: -0.03em;
}

.conv-refresh {
  border: none;
  background: var(--glass-3);
  backdrop-filter: var(--blur-md);
  -webkit-backdrop-filter: var(--blur-md);
  color: var(--text-secondary);
  padding: 7px 16px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-expo);
  font-family: inherit;
}

.conv-refresh:hover:not(:disabled) {
  background: var(--glass-2);
  color: var(--text-primary);
}

.conv-refresh:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ================================================
   Conversation Cards — Spotify/Telegram style
   ================================================ */

.conv-stack {
  padding: 0 12px 12px;
}

.conv-card {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-spring-smooth);
  border: 1px solid transparent;
}

.conv-card:hover {
  background: var(--glass-2);
  border-color: var(--glass-border-3);
  box-shadow: var(--shadow-sm);
  transform: translateX(3px);
}

.conv-card:active {
  transform: scale(0.985);
}

/* Breathing highlight for unread */
.conv-card.unread {
  background: rgba(26, 173, 94, 0.06);
  border-color: rgba(26, 173, 94, 0.10);
}

/* ================================================
   Avatar
   ================================================ */

.conv-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.conv-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  transition: transform var(--duration-normal) var(--ease-spring-soft);
}

.conv-card:hover .conv-avatar {
  transform: scale(1.06);
}

.conv-badge {
  position: absolute;
  top: -4px;
  right: -6px;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  background: var(--danger);
  color: #fff;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(232, 64, 64, 0.4);
}

/* ================================================
   Content
   ================================================ */

.conv-content {
  min-width: 0;
}

.conv-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.conv-name {
  font-weight: 680;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-tag {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  background: var(--brand-soft);
  color: var(--brand);
  flex-shrink: 0;
}

.muted-tag {
  background: rgba(0,0,0,0.05);
  color: var(--text-tertiary);
}

.conv-preview {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-card.unread .conv-preview {
  color: var(--text-secondary);
}

.conv-type {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-tertiary);
}

/* ================================================
   Meta
   ================================================ */

.conv-meta {
  text-align: right;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.conv-mute-btn {
  border: none;
  background: transparent;
  font-size: 14px;
  cursor: pointer;
  opacity: 0;
  transition: opacity var(--duration-fast) var(--ease-out-expo);
  padding: 2px;
  line-height: 1;
}

.conv-card:hover .conv-mute-btn {
  opacity: 1;
}

.conv-mute-btn:hover {
  transform: scale(1.15);
}

.conv-mute-btn.muted {
  opacity: 0.7;
}

.conv-time {
  font-size: 11px;
  color: var(--text-tertiary);
  white-space: nowrap;
}

/* ================================================
   Empty State
   ================================================ */

.conv-empty {
  text-align: center;
  padding: 60px 20px;
}

.conv-empty-icon {
  font-size: 48px;
  color: var(--text-tertiary);
  margin-bottom: 16px;
  opacity: 0.4;
}

.conv-empty-text {
  font-size: 18px;
  font-weight: 680;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.conv-empty-sub {
  font-size: 14px;
}

/* ================================================
   Pagination
   ================================================ */

:deep(.el-pagination) {
  justify-content: center;
  padding: 16px 0;
}

/* ================================================
   Responsive
   ================================================ */

@media (max-width: 560px) {
  .conv-shell {
    padding: 12px 10px 24px;
  }

  .conv-topbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .conv-card {
    grid-template-columns: 44px minmax(0, 1fr);
    gap: 10px;
    padding: 12px 12px;
  }

  .conv-avatar {
    width: 44px;
    height: 44px;
    font-size: 17px;
  }

  .conv-meta {
    grid-column: 1 / -1;
    text-align: left;
    padding-left: 54px;
  }
}
</style>

<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <div class="brand-mark">Online Chat</div>
        <h1 class="page-title">消息列表</h1>
        <p class="page-subtitle">最近会话按置顶和最后消息时间排序，直接展示最后一条消息与未读数。</p>
      </div>
      <nav class="nav-links">
        <RouterLink class="nav-link" to="/friends">好友</RouterLink>
        <RouterLink class="nav-link" to="/conversations">消息列表</RouterLink>
        <RouterLink class="nav-link" to="/chat-history">搜索</RouterLink>
        <el-button plain @click="logout">退出</el-button>
      </nav>
    </header>

    <section class="main-panel glass-card">
      <div class="toolbar">
        <strong>最近会话</strong>
        <el-button type="primary" :loading="conversationStore.loading" @click="loadRecent(1)">刷新</el-button>
      </div>

      <div class="stack">
        <article
          v-for="conversation in conversationStore.page.records"
          :key="conversation.id"
          class="conversation-card conversation-clickable"
          @click="openChat(conversation)"
        >
          <el-badge :value="conversation.unreadCount" :hidden="!conversation.unreadCount">
            <el-avatar :src="conversation.targetAvatar" :size="52">{{ firstLetter(conversation.targetNickname || conversation.targetUsername) }}</el-avatar>
          </el-badge>

          <div class="conversation-content">
            <div class="conversation-title">
              <span>{{ conversation.targetNickname || conversation.targetUsername || '未知会话' }}</span>
              <el-tag v-if="conversation.pinned" size="small">置顶</el-tag>
              <el-tag v-if="conversation.muted" type="info" size="small">免打扰</el-tag>
            </div>
            <div class="conversation-message">
              <span class="muted">[{{ conversation.lastMessageType }}]</span>
              {{ conversation.lastMessageContent || '暂无消息' }}
            </div>
          </div>

          <div class="conversation-actions">
            <div class="muted">{{ conversation.lastMessageAt }}</div>
            <el-button v-if="conversation.unreadCount" link @click.stop="conversationStore.markRead(conversation.id)">标为已读</el-button>
          </div>
        </article>

        <el-empty v-if="!conversationStore.loading && conversationStore.page.records.length === 0" description="暂无会话" />
      </div>

      <el-pagination
        layout="prev, pager, next, total"
        :total="conversationStore.page.total"
        :current-page="conversationStore.page.pageNo"
        :page-size="conversationStore.page.pageSize"
        @current-change="loadRecent"
      />
    </section>
  </main>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useConversationStore } from '../stores/conversation'

const router = useRouter()
const authStore = useAuthStore()
const conversationStore = useConversationStore()

onMounted(() => loadRecent(1))

function firstLetter(value) {
  return value ? value.slice(0, 1).toUpperCase() : '?'
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

function openChat(conversation) {
  if (!conversation?.targetUserId) return
  router.push({ name: 'chat', params: { targetUserId: conversation.targetUserId } })
}
</script>

<style scoped>
.main-panel {
  max-width: 1080px;
  margin: 0 auto;
}

.toolbar strong {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.conversation-actions {
  display: grid;
  justify-items: end;
  gap: 6px;
  white-space: nowrap;
}

.conversation-clickable {
  cursor: pointer;
}

.conversation-clickable:active {
  transform: scale(0.993);
}

:deep(.el-pagination) {
  justify-content: flex-end;
  margin-top: 20px;
}

:deep(.el-empty) {
  padding: 56px 0;
}

:deep(.el-empty__description) {
  color: var(--muted);
  font-weight: 600;
}

@media (max-width: 720px) {
  .conversation-card {
    grid-template-columns: 44px minmax(0, 1fr);
    gap: 10px;
    padding: 14px 16px;
  }

  .conversation-actions {
    grid-column: 1 / -1;
    justify-items: start;
  }
}
</style>

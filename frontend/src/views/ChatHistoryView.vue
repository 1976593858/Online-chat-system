<template>
  <main class="history-shell">
    <!-- Header -->
    <header class="history-topbar">
      <div class="history-brand">
        <div class="history-brand-icon">◇</div>
        <div class="history-brand-text">Online Chat</div>
      </div>
      <nav class="nav-links">
        <RouterLink class="nav-link" to="/friends">好友</RouterLink>
        <RouterLink class="nav-link" to="/conversations">消息</RouterLink>
        <RouterLink class="nav-link" to="/groups">群聊</RouterLink>
        <RouterLink class="nav-link active" to="/chat-history">搜索</RouterLink>
        <button class="nav-logout" @click="logout">退出</button>
      </nav>
    </header>

    <!-- Search — Spotlight style -->
    <section class="search-spotlight">
      <div class="spotlight-bar">
        <span class="spotlight-icon">⌘</span>
        <input
          v-model="keyword"
          class="spotlight-input"
          placeholder="搜索聊天记录…"
          @keyup.enter="doSearch(1)"
        />
        <button v-if="keyword" class="spotlight-clear" @click="keyword = ''; doSearch(1)">×</button>
      </div>
      <div class="spotlight-filters">
        <input
          v-model="userIdFilter"
          class="filter-input"
          placeholder="用户ID（可选）"
          @keyup.enter="doSearch(1)"
        />
        <input v-model="dateFrom" type="date" class="filter-date" @change="doSearch(1)" />
        <span class="filter-sep muted">至</span>
        <input v-model="dateTo" type="date" class="filter-date" @change="doSearch(1)" />
        <button class="search-btn" :disabled="loading" @click="doSearch(1)">
          {{ loading ? '搜索中…' : '搜索' }}
        </button>
      </div>
    </section>

    <!-- Results -->
    <section class="results-panel glass-highlight" v-if="hasSearched">
      <div class="results-header">
        <h3 class="results-title">找到 {{ total }} 条记录</h3>
      </div>

      <div class="results-stack" v-if="results.length">
        <div
          v-for="(item, i) in results"
          :key="item.id || i"
          class="result-card"
          :style="{ animationDelay: `${i * 0.03}s` }"
        >
          <div class="result-meta">
            <span class="result-sender">{{ item.fromUserNickname || item.fromUserUsername || '用户 '+item.fromUserId }}</span>
            <span class="result-tag">{{ item.messageType }}</span>
            <span class="result-time muted">{{ item.createdAt }}</span>
          </div>
          <div class="result-content">{{ item.content }}</div>
          <button
            v-if="item.toUserId"
            class="result-go"
            @click="goChat(item.toUserId)"
          >前往聊天 →</button>
        </div>
      </div>

      <div v-else class="results-empty muted">未找到匹配的记录</div>

      <el-pagination
        v-if="total > pageSize"
        layout="prev, pager, next"
        :total="total"
        :current-page="pageNo"
        :page-size="pageSize"
        @current-change="doSearch"
        small
      />
    </section>

    <div v-else class="search-hint">
      <div class="hint-icon">⌘</div>
      <div class="hint-text">输入关键词搜索聊天记录</div>
    </div>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { searchChatHistory } from '../api/chatHistory'

const router = useRouter()
const authStore = useAuthStore()

const keyword = ref('')
const userIdFilter = ref('')
const dateFrom = ref('')
const dateTo = ref('')
const results = ref([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 12
const loading = ref(false)
const hasSearched = ref(false)

async function doSearch(p = 1) {
  pageNo.value = p
  loading.value = true
  hasSearched.value = true
  try {
    const res = await searchChatHistory({
      keyword: keyword.value || undefined,
      userId: userIdFilter.value || undefined,
      dateFrom: dateFrom.value || undefined,
      dateTo: dateTo.value || undefined,
      pageNo: p,
      pageSize
    })
    results.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.warn('搜索失败', e)
  } finally {
    loading.value = false
  }
}

function goChat(targetUserId) {
  router.push({ name: 'chat', params: { targetUserId } })
}

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.history-shell {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px 20px 40px;
  min-height: 100vh;
}

/* ================================================
   Header
   ================================================ */

.history-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.history-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.history-brand-icon {
  font-size: 28px;
  color: var(--brand);
  filter: drop-shadow(0 2px 8px var(--brand-glow));
}

.history-brand-text {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.nav-logout {
  background: none;
  border: 1px solid var(--glass-border-3);
  color: var(--text-secondary);
  padding: 8px 16px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-expo);
  font-family: inherit;
  margin-left: 4px;
}

.nav-logout:hover {
  background: var(--glass-3);
  color: var(--text-primary);
}

/* ================================================
   Spotlight Search
   ================================================ */

.search-spotlight {
  margin-bottom: 24px;
  animation: springIn 0.5s var(--ease-spring-soft) both;
}

.spotlight-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  background: var(--glass-1);
  backdrop-filter: var(--blur-xl);
  -webkit-backdrop-filter: var(--blur-xl);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  transition: all var(--duration-normal) var(--ease-out-expo);
}

.spotlight-bar:focus-within {
  border-color: rgba(26, 173, 94, 0.3);
  box-shadow: 0 0 0 4px var(--brand-glow), var(--shadow-lg);
}

.spotlight-icon {
  font-size: 20px;
  color: var(--text-tertiary);
  font-weight: 600;
}

.spotlight-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 18px;
  font-family: inherit;
  color: var(--text-primary);
}

.spotlight-input::placeholder {
  color: var(--text-tertiary);
}

.spotlight-clear {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: rgba(0,0,0,0.08);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-fast) var(--ease-out-expo);
}

.spotlight-clear:hover {
  background: rgba(0,0,0,0.14);
  color: var(--text-primary);
}

/* Filters */
.spotlight-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.filter-input {
  flex: 1;
  min-width: 140px;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--glass-border-3);
  background: var(--glass-3);
  backdrop-filter: var(--blur-md);
  -webkit-backdrop-filter: var(--blur-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--text-primary);
  outline: none;
  transition: all var(--duration-fast) var(--ease-out-expo);
}

.filter-input:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-glow);
}

.filter-date {
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--glass-border-3);
  background: var(--glass-3);
  backdrop-filter: var(--blur-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--text-primary);
  outline: none;
  transition: all var(--duration-fast) var(--ease-out-expo);
}

.filter-date:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-glow);
}

.filter-sep {
  font-size: 13px;
}

.search-btn {
  padding: 8px 20px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--brand);
  color: #fff;
  font-size: 14px;
  font-weight: 680;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-spring-soft);
  font-family: inherit;
  box-shadow: 0 2px 8px var(--brand-glow);
}

.search-btn:hover:not(:disabled) {
  background: var(--brand-hover);
  box-shadow: 0 6px 20px var(--brand-glow);
  transform: translateY(-1px);
}

.search-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ================================================
   Results
   ================================================ */

.results-panel {
  background: var(--glass-1);
  backdrop-filter: var(--blur-xl);
  -webkit-backdrop-filter: var(--blur-xl);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  padding: 20px 24px;
  animation: springIn 0.45s 0.1s var(--ease-spring-soft) both;
}

.results-header {
  margin-bottom: 16px;
}

.results-title {
  margin: 0;
  font-size: 16px;
  font-weight: 680;
}

.results-stack {
  display: grid;
  gap: 8px;
}

.result-card {
  padding: 14px 18px;
  border-radius: var(--radius-md);
  background: var(--glass-3);
  border: 1px solid var(--glass-border-3);
  transition: all var(--duration-normal) var(--ease-spring-smooth);
  animation: messageIn 0.35s var(--ease-spring-soft) both;
}

.result-card:hover {
  background: var(--glass-2);
  box-shadow: var(--shadow-sm);
  transform: translateX(3px);
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.result-sender {
  font-weight: 680;
  font-size: 14px;
}

.result-tag {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  background: var(--brand-soft);
  color: var(--brand);
}

.result-time {
  font-size: 11px;
}

.result-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.result-go {
  display: inline-block;
  margin-top: 8px;
  border: none;
  background: transparent;
  color: var(--brand);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
  transition: all var(--duration-fast) var(--ease-out-expo);
}

.result-go:hover {
  color: var(--brand-hover);
}

.results-empty {
  text-align: center;
  padding: 40px 0;
  font-size: 15px;
}

/* ================================================
   Empty State
   ================================================ */

.search-hint {
  text-align: center;
  padding: 80px 20px;
}

.hint-icon {
  font-size: 56px;
  color: var(--text-tertiary);
  opacity: 0.3;
  margin-bottom: 16px;
}

.hint-text {
  font-size: 16px;
  color: var(--text-tertiary);
}

/* ================================================
   Pagination
   ================================================ */

:deep(.el-pagination) {
  justify-content: center;
  padding-top: 16px;
}

/* ================================================
   Responsive
   ================================================ */

@media (max-width: 560px) {
  .history-shell {
    padding: 12px 10px 24px;
  }

  .history-topbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .spotlight-bar {
    padding: 12px 16px;
  }

  .spotlight-input {
    font-size: 16px;
  }

  .spotlight-filters {
    flex-direction: column;
  }

  .filter-input {
    width: 100%;
  }
}
</style>

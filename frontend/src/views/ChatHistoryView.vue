<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <div class="brand-mark">Online Chat</div>
        <h1 class="page-title">聊天记录查询</h1>
        <p class="page-subtitle">基于全文检索，快速定位历史消息。</p>
      </div>
      <nav class="nav-links">
        <RouterLink class="nav-link" to="/friends">好友</RouterLink>
        <RouterLink class="nav-link" to="/conversations">消息列表</RouterLink>
        <RouterLink class="nav-link" to="/chat-history">搜索</RouterLink>
        <el-button plain @click="logout">退出</el-button>
      </nav>
    </header>

    <section class="main-panel glass-card">
      <div class="search-form">
        <div class="search-row">
          <el-input
            v-model="form.keyword"
            placeholder="输入关键词搜索聊天记录"
            clearable
            size="large"
            @keyup.enter="doSearch(1)"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" size="large" :loading="loading" @click="doSearch(1)">搜索</el-button>
        </div>
        <div class="filter-row">
          <div class="filter-item">
            <span class="filter-label">用户ID</span>
            <el-input-number v-model="form.userId" :min="0" placeholder="筛选指定用户" clearable style="width:140px" />
          </div>
          <div class="filter-item">
            <span class="filter-label">开始日期</span>
            <el-date-picker v-model="form.fromDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:160px" />
          </div>
          <div class="filter-item">
            <span class="filter-label">结束日期</span>
            <el-date-picker v-model="form.toDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:160px" />
          </div>
          <el-button text @click="resetFilter">重置</el-button>
        </div>
      </div>

      <div class="result-info" v-if="searched">
        共找到 <strong>{{ page.total }}</strong> 条匹配记录
      </div>

      <div class="stack">
        <article
          v-for="item in page.records"
          :key="item.id"
          class="message-card"
        >
          <div class="msg-header">
            <span class="msg-sender">
              {{ displayName(item.fromUserNickname, item.fromUserUsername) }}
              <span class="muted">(ID: {{ item.fromUserId }})</span>
            </span>
            <el-tag size="small" type="info">{{ item.messageType }}</el-tag>
            <span class="msg-time muted">{{ item.createdAt }}</span>
          </div>
          <div class="msg-body">{{ item.content }}</div>
          <div class="msg-footer muted">
            发送给 {{ displayName(item.toUserNickname, item.toUserUsername) }} (ID: {{ item.toUserId }})
            <el-button link size="small" @click="goChat(item)">去聊天</el-button>
          </div>
        </article>

        <el-empty v-if="!loading && searched && page.records.length === 0" description="未找到匹配的记录" />
        <el-empty v-if="!searched && !loading" description="输入关键词开始搜索" />
      </div>

      <el-pagination
        v-if="page.pages > 1"
        layout="prev, pager, next, total"
        :total="page.total"
        :current-page="page.pageNo"
        :page-size="page.pageSize"
        @current-change="doSearch"
      />
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { searchChatHistory } from '../api/chatHistory'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const searched = ref(false)
const page = reactive({
  records: [],
  total: 0,
  pageNo: 1,
  pageSize: 20,
  pages: 0
})

const form = reactive({
  keyword: '',
  userId: null,
  fromDate: null,
  toDate: null
})

function resetFilter() {
  form.userId = null
  form.fromDate = null
  form.toDate = null
}

function displayName(nickname, username) {
  return nickname || username || '未知'
}

async function doSearch(pageNo = 1) {
  if (!form.keyword.trim() && !form.userId && !form.fromDate && !form.toDate) return
  loading.value = true
  searched.value = true
  try {
    const params = { pageNo, pageSize: page.pageSize }
    if (form.keyword.trim()) params.keyword = form.keyword.trim()
    if (form.userId) params.userId = form.userId
    if (form.fromDate) params.fromDate = form.fromDate
    if (form.toDate) params.toDate = form.toDate

    const res = await searchChatHistory(params)
    page.records = res.records
    page.total = res.total
    page.pageNo = res.pageNo
    page.pageSize = res.pageSize
    page.pages = res.pages
  } finally {
    loading.value = false
  }
}

function goChat(item) {
  const targetId = authStore.user && Number(item.fromUserId) === Number(authStore.user.id)
    ? item.toUserId
    : item.fromUserId
  router.push({ name: 'chat', params: { targetUserId: targetId } })
}

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.main-panel {
  max-width: 1080px;
  margin: 0 auto;
}

.search-form {
  margin-bottom: 20px;
}

.search-row {
  display: flex;
  gap: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: var(--muted);
  white-space: nowrap;
}

.result-info {
  margin-bottom: 14px;
  font-size: 14px;
}

.message-card {
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  display: grid;
  gap: 8px;
}

.msg-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.msg-sender {
  font-weight: 700;
}

.msg-body {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  padding: 8px 0;
}

.msg-footer {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.msg-time {
  font-size: 12px;
  margin-left: auto;
}

:deep(.el-pagination) {
  justify-content: flex-end;
  margin-top: 18px;
}
</style>

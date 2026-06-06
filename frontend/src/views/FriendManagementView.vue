<template>
  <main class="friends-shell">
    <!-- Header -->
    <header class="friends-topbar">
      <div class="friends-brand">
        <div class="friends-brand-icon">◇</div>
        <div class="friends-brand-text">Online Chat</div>
      </div>
      <nav class="nav-links">
        <RouterLink class="nav-link active" to="/friends">好友</RouterLink>
        <RouterLink class="nav-link" to="/conversations">消息</RouterLink>
        <RouterLink class="nav-link" to="/chat-history">搜索</RouterLink>
        <button class="nav-logout" @click="logout">退出</button>
      </nav>
    </header>

    <!-- Layout: sidebar + main -->
    <div class="friends-layout">
      <!-- Sidebar — Groups -->
      <aside class="friends-sidebar glass-highlight">
        <div class="sidebar-head">
          <h3 class="sidebar-title">分组</h3>
          <button class="sidebar-add" @click="promptCreateGroup">+</button>
        </div>
        <div class="sidebar-groups">
          <div
            v-for="group in friendStore.groups"
            :key="group.id"
            :class="['group-card', { active: activeGroupId === group.id }]"
            @click="selectGroup(group.id)"
          >
            <div class="group-card-main">
              <span class="group-card-name">{{ group.name }}</span>
              <span class="group-card-count">{{ group.friendCount || 0 }}</span>
            </div>
            <div class="group-card-actions" @click.stop>
              <template v-if="group.isDefault === 1">
                <span class="group-default-tag">默认</span>
              </template>
              <template v-else>
                <button class="group-action-btn" @click="promptRenameGroup(group)">改名</button>
                <button class="group-action-btn danger" @click="confirmDeleteGroup(group)">删除</button>
              </template>
            </div>
          </div>
        </div>
      </aside>

      <!-- Main Panel -->
      <section class="friends-main glass-highlight">
        <!-- Custom tab pills -->
        <div class="main-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            :class="['main-tab', { active: activeTab === tab.key }]"
            @click="switchTab(tab.key)"
          >{{ tab.label }}</button>
        </div>

        <!-- Friends Tab -->
        <div v-show="activeTab === 'friends'" class="tab-content">
          <div class="tab-toolbar">
            <input
              v-model="friendKeyword"
              class="tab-search"
              placeholder="搜索好友…"
              @keyup.enter="loadFriends(1)"
            />
            <button class="tab-action-btn" @click="loadFriends(1)">查询</button>
          </div>

          <div class="friend-list" v-if="friendStore.friendsPage.records.length">
            <div
              v-for="friend in friendStore.friendsPage.records"
              :key="friend.friendId"
              class="friend-card"
            >
              <div class="friend-avatar" :style="friendAvatarStyle(friend)">
                {{ firstLetter(friend.remark || friend.nickname || friend.username) }}
              </div>
              <div class="friend-info">
                <div class="friend-name">{{ friend.remark || friend.nickname || friend.username }}</div>
                <div class="friend-meta">
                  <span class="muted">@{{ friend.username }}</span>
                  <span class="friend-group-tag">{{ friend.groupName }}</span>
                </div>
              </div>
              <div class="friend-actions">
                <button class="friend-btn" @click="openDetail(friend.friendId)">详情</button>
                <button class="friend-btn primary" @click="openChat(friend.friendId)">私聊</button>
                <button class="friend-btn" @click="promptRemark(friend)">备注</button>
                <button class="friend-btn" @click="openMoveDialog(friend)">移动</button>
                <button class="friend-btn danger" @click="confirmDeleteFriend(friend)">删除</button>
              </div>
            </div>
          </div>
          <div v-else class="tab-empty muted">暂无好友</div>

          <el-pagination
            v-if="friendStore.friendsPage.total > friendStore.friendsPage.pageSize"
            layout="prev, pager, next"
            :total="friendStore.friendsPage.total"
            :current-page="friendStore.friendsPage.pageNo"
            :page-size="friendStore.friendsPage.pageSize"
            @current-change="loadFriends"
            small
          />
        </div>

        <!-- Search Tab -->
        <div v-show="activeTab === 'search'" class="tab-content">
          <div class="tab-toolbar">
            <input
              v-model="searchKeyword"
              class="tab-search"
              placeholder="搜索用户…"
              @keyup.enter="searchUsers(1)"
            />
            <button class="tab-action-btn" @click="searchUsers(1)">搜索</button>
          </div>

          <div class="friend-list" v-if="friendStore.searchPage.records.length">
            <div
              v-for="user in friendStore.searchPage.records"
              :key="user.id"
              class="friend-card"
            >
              <div class="friend-avatar" :style="friendAvatarStyle(user)">
                {{ firstLetter(user.nickname || user.username) }}
              </div>
              <div class="friend-info">
                <div class="friend-name">{{ user.nickname || user.username }}</div>
                <div class="friend-meta">
                  <span class="muted">@{{ user.username }}</span>
                  <span class="relation-tag" :class="relationClass(user.relationStatus)">
                    {{ relationText(user.relationStatus) }}
                  </span>
                </div>
              </div>
              <div class="friend-actions">
                <button
                  class="friend-btn primary"
                  :disabled="user.relationStatus !== 'NONE'"
                  @click="promptSendRequest(user)"
                >添加好友</button>
              </div>
            </div>
          </div>
          <div v-else class="tab-empty muted">搜索用户以添加好友</div>

          <el-pagination
            v-if="friendStore.searchPage.total > friendStore.searchPage.pageSize"
            layout="prev, pager, next"
            :total="friendStore.searchPage.total"
            :current-page="friendStore.searchPage.pageNo"
            :page-size="friendStore.searchPage.pageSize"
            @current-change="searchUsers"
            small
          />
        </div>

        <!-- Requests Tab -->
        <div v-show="activeTab === 'requests'" class="tab-content">
          <div class="tab-toolbar">
            <select v-model="requestDirection" class="tab-select" @change="loadRequests(1)">
              <option value="received">收到的申请</option>
              <option value="sent">发出的申请</option>
            </select>
            <select v-model="requestStatus" class="tab-select" @change="loadRequests(1)">
              <option value="">全部状态</option>
              <option value="PENDING">待处理</option>
              <option value="ACCEPTED">已同意</option>
              <option value="REJECTED">已拒绝</option>
            </select>
            <button class="tab-action-btn" @click="loadRequests(1)">刷新</button>
          </div>

          <div class="friend-list" v-if="friendStore.requestsPage.records.length">
            <div
              v-for="req in friendStore.requestsPage.records"
              :key="req.id"
              class="friend-card"
            >
              <div class="friend-avatar" :style="requestAvatarStyle(req)">
                {{ firstLetter(requestDirection === 'received' ? (req.senderNickname || req.senderUsername) : (req.receiverNickname || req.receiverUsername)) }}
              </div>
              <div class="friend-info">
                <div class="friend-name">
                  {{ requestDirection === 'received' ? (req.senderNickname || req.senderUsername) : (req.receiverNickname || req.receiverUsername) }}
                </div>
                <div class="friend-meta">
                  <span class="status-tag" :class="'status-' + req.status">{{ req.status }}</span>
                  <span class="muted" v-if="req.message">{{ req.message }}</span>
                </div>
              </div>
              <div class="friend-actions" v-if="requestDirection === 'received'">
                <button class="friend-btn primary" :disabled="req.status !== 'PENDING'" @click="openAcceptDialog(req)">同意</button>
                <button class="friend-btn danger" :disabled="req.status !== 'PENDING'" @click="promptReject(req)">拒绝</button>
              </div>
            </div>
          </div>
          <div v-else class="tab-empty muted">暂无申请</div>

          <el-pagination
            v-if="friendStore.requestsPage.total > friendStore.requestsPage.pageSize"
            layout="prev, pager, next"
            :total="friendStore.requestsPage.total"
            :current-page="friendStore.requestsPage.pageNo"
            :page-size="friendStore.requestsPage.pageSize"
            @current-change="loadRequests"
            small
          />
        </div>
      </section>
    </div>

    <!-- Move Dialog -->
    <el-dialog v-model="moveDialog.visible" title="移动到分组" width="400px" align-center>
      <select v-model="moveDialog.groupId" class="dialog-select">
        <option v-for="group in friendStore.groups" :key="group.id" :value="group.id">{{ group.name }}</option>
      </select>
      <template #footer>
        <button class="dialog-btn cancel" @click="moveDialog.visible = false">取消</button>
        <button class="dialog-btn confirm" @click="submitMoveFriend">确认移动</button>
      </template>
    </el-dialog>

    <!-- Accept Dialog -->
    <el-dialog v-model="acceptDialog.visible" title="同意好友申请" width="440px" align-center>
      <div class="dialog-field">
        <label class="dialog-label">放入分组</label>
        <select v-model="acceptDialog.groupId" class="dialog-select">
          <option v-for="group in friendStore.groups" :key="group.id" :value="group.id">{{ group.name }}</option>
        </select>
      </div>
      <div class="dialog-field">
        <label class="dialog-label">备注</label>
        <input v-model="acceptDialog.remark" class="dialog-input" maxlength="50" placeholder="可选" />
      </div>
      <template #footer>
        <button class="dialog-btn cancel" @click="acceptDialog.visible = false">取消</button>
        <button class="dialog-btn confirm" @click="submitAcceptRequest">同意</button>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <el-drawer v-model="detailDrawerVisible" title="好友详情" size="400px">
      <div v-if="friendStore.selectedFriend" class="detail-card">
        <div class="detail-avatar" :style="friendAvatarStyle(friendStore.selectedFriend)">
          {{ firstLetter(friendStore.selectedFriend.nickname || friendStore.selectedFriend.username) }}
        </div>
        <div class="detail-name">{{ friendStore.selectedFriend.nickname || friendStore.selectedFriend.username }}</div>
        <div class="detail-username muted">@{{ friendStore.selectedFriend.username }}</div>

        <div class="detail-fields">
          <div class="detail-field">
            <span class="detail-label">备注</span>
            <span>{{ friendStore.selectedFriend.remark || '-' }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">分组</span>
            <span>{{ friendStore.selectedFriend.groupName }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">邮箱</span>
            <span>{{ friendStore.selectedFriend.email || '-' }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">手机号</span>
            <span>{{ friendStore.selectedFriend.phone || '-' }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">好友时间</span>
            <span>{{ friendStore.selectedFriend.friendSince }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">最后登录</span>
            <span>{{ friendStore.selectedFriend.lastLoginAt || '-' }}</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { useFriendStore } from '../stores/friend'

const router = useRouter()
const authStore = useAuthStore()
const friendStore = useFriendStore()

const tabs = [
  { key: 'friends', label: '好友列表' },
  { key: 'search', label: '搜索用户' },
  { key: 'requests', label: '好友申请' }
]

const activeTab = ref('friends')
const activeGroupId = ref(null)
const friendKeyword = ref('')
const searchKeyword = ref('')
const requestDirection = ref('received')
const requestStatus = ref('')
const detailDrawerVisible = ref(false)

const moveDialog = reactive({ visible: false, friendId: null, groupId: null })
const acceptDialog = reactive({ visible: false, requestId: null, groupId: null, remark: '' })

onMounted(async () => {
  await Promise.all([friendStore.loadGroups(), loadFriends(1), loadRequests(1)])
})

function firstLetter(value) {
  return value ? value.slice(0, 1).toUpperCase() : '?'
}

function friendAvatarStyle(row) {
  const name = row.nickname || row.username || '?'
  const hue = (name.charCodeAt(0) || 65) * 137 % 360
  return {
    background: `linear-gradient(135deg, hsl(${hue}, 55%, 55%) 0%, hsl(${hue}, 60%, 42%) 100%)`,
    boxShadow: `0 4px 14px hsla(${hue}, 55%, 50%, 0.3)`
  }
}

function requestAvatarStyle(req) {
  const name = requestDirection.value === 'received'
    ? (req.senderNickname || req.senderUsername || '?')
    : (req.receiverNickname || req.receiverUsername || '?')
  const hue = (name.charCodeAt(0) || 65) * 137 % 360
  return {
    background: `linear-gradient(135deg, hsl(${hue}, 45%, 55%) 0%, hsl(${hue}, 50%, 40%) 100%)`,
    boxShadow: `0 4px 14px hsla(${hue}, 45%, 50%, 0.25)`
  }
}

function relationText(status) {
  const map = { SELF: '自己', FRIEND: '已是好友', NONE: '可添加', PENDING_SENT: '已发送申请', PENDING_RECEIVED: '待你处理' }
  return map[status] || status
}

function relationClass(status) {
  if (status === 'NONE') return 'rel-ok'
  if (status === 'FRIEND') return 'rel-friend'
  if (status?.startsWith('PENDING')) return 'rel-pending'
  return ''
}

async function switchTab(name) {
  activeTab.value = name
  if (name === 'search' && friendStore.searchPage.records.length === 0) await searchUsers(1)
  if (name === 'requests') await loadRequests(1)
}

function selectGroup(groupId) {
  activeGroupId.value = activeGroupId.value === groupId ? null : groupId
  loadFriends(1)
}

async function loadFriends(pageNo = 1) {
  await friendStore.loadFriends({ pageNo, pageSize: friendStore.friendsPage.pageSize, groupId: activeGroupId.value || undefined, keyword: friendKeyword.value || undefined })
}

async function searchUsers(pageNo = 1) {
  await friendStore.searchUsers({ pageNo, pageSize: friendStore.searchPage.pageSize, keyword: searchKeyword.value || undefined })
}

async function loadRequests(pageNo = 1) {
  await friendStore.loadRequests({ pageNo, pageSize: friendStore.requestsPage.pageSize, direction: requestDirection.value, status: requestStatus.value || undefined })
}

async function promptCreateGroup() {
  const { value } = await ElMessageBox.prompt('请输入分组名称', '创建分组', { inputPattern: /^.{1,32}$/, inputErrorMessage: '分组名称不能为空且不能超过32个字符' })
  await friendStore.createGroup(value)
}

async function promptRenameGroup(group) {
  const { value } = await ElMessageBox.prompt('请输入新的分组名称', '修改分组', { inputValue: group.name, inputPattern: /^.{1,32}$/, inputErrorMessage: '分组名称不能为空且不能超过32个字符' })
  await friendStore.updateGroup(group.id, value)
}

async function confirmDeleteGroup(group) {
  await ElMessageBox.confirm(`删除分组「${group.name}」后，好友将移动到默认分组。`, '删除分组', { type: 'warning' })
  await friendStore.deleteGroup(group.id)
  if (activeGroupId.value === group.id) activeGroupId.value = null
}

async function promptSendRequest(row) {
  const { value } = await ElMessageBox.prompt(`向 ${row.nickname || row.username} 发送好友申请`, '添加好友', { inputPlaceholder: '申请备注，可选' })
  await friendStore.sendRequest(row.id, value)
  await searchUsers(friendStore.searchPage.pageNo)
}

async function promptRemark(row) {
  const { value } = await ElMessageBox.prompt('请输入好友备注', '修改备注', { inputValue: row.remark || '', inputPlaceholder: '最多50个字符' })
  await friendStore.updateRemark(row.friendId, value)
}

function openMoveDialog(row) {
  moveDialog.friendId = row.friendId
  moveDialog.groupId = row.groupId
  moveDialog.visible = true
}

async function submitMoveFriend() {
  await friendStore.moveFriend(moveDialog.friendId, moveDialog.groupId)
  moveDialog.visible = false
}

async function confirmDeleteFriend(row) {
  await ElMessageBox.confirm(`确认删除好友「${row.remark || row.nickname || row.username}」？`, '删除好友', { type: 'warning' })
  await friendStore.removeFriend(row.friendId)
}

async function openDetail(friendId) {
  await friendStore.loadFriendDetail(friendId)
  detailDrawerVisible.value = true
}

function openAcceptDialog(row) {
  const defaultGroup = friendStore.groups.find((group) => group.isDefault === 1)
  acceptDialog.requestId = row.id
  acceptDialog.groupId = defaultGroup?.id || null
  acceptDialog.remark = row.senderNickname || row.senderUsername || ''
  acceptDialog.visible = true
}

async function submitAcceptRequest() {
  await friendStore.acceptRequest(acceptDialog.requestId, { groupId: acceptDialog.groupId, remark: acceptDialog.remark })
  acceptDialog.visible = false
}

async function promptReject(row) {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝好友申请', { inputPlaceholder: '可选' })
  await friendStore.rejectRequest(row.id, value)
}

function logout() {
  authStore.logout()
  router.push('/login')
}

function openChat(friendId) {
  router.push({ name: 'chat', params: { targetUserId: friendId } })
}
</script>

<style scoped>
/* ================================================
   Shell
   ================================================ */

.friends-shell {
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px 20px 40px;
  min-height: 100vh;
}

/* ================================================
   Header
   ================================================ */

.friends-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.friends-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.friends-brand-icon {
  font-size: 28px;
  color: var(--brand);
  filter: drop-shadow(0 2px 8px var(--brand-glow));
}

.friends-brand-text {
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
   Layout
   ================================================ */

.friends-layout {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
  animation: springIn 0.5s var(--ease-spring-soft) both;
}

/* ================================================
   Sidebar — Groups
   ================================================ */

.friends-sidebar {
  background: var(--glass-1);
  backdrop-filter: var(--blur-xl);
  -webkit-backdrop-filter: var(--blur-xl);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  padding: 20px;
  position: sticky;
  top: 20px;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.sidebar-title {
  margin: 0;
  font-size: 15px;
  font-weight: 680;
}

.sidebar-add {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: var(--brand);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-fast) var(--ease-spring-soft);
  box-shadow: 0 2px 6px var(--brand-glow);
}

.sidebar-add:hover {
  background: var(--brand-hover);
  transform: scale(1.08);
  box-shadow: 0 4px 14px var(--brand-glow);
}

.sidebar-groups {
  display: grid;
  gap: 6px;
}

.group-card {
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: var(--glass-3);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-spring-smooth);
}

.group-card:hover {
  background: var(--glass-2);
  border-color: var(--glass-border-3);
  transform: translateX(2px);
}

.group-card.active {
  background: var(--brand-soft);
  border-color: rgba(26, 173, 94, 0.2);
}

.group-card-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.group-card-name {
  font-weight: 600;
  font-size: 14px;
}

.group-card-count {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-tertiary);
  background: rgba(0,0,0,0.05);
  padding: 2px 8px;
  border-radius: 999px;
}

.group-card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
}

.group-default-tag {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-tertiary);
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(0,0,0,0.04);
}

.group-action-btn {
  border: none;
  background: none;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all var(--duration-fast) var(--ease-out-expo);
  font-family: inherit;
}

.group-action-btn:hover {
  background: rgba(0,0,0,0.05);
  color: var(--text-primary);
}

.group-action-btn.danger:hover {
  background: var(--danger-soft);
  color: var(--danger);
}

/* ================================================
   Main Panel
   ================================================ */

.friends-main {
  background: var(--glass-1);
  backdrop-filter: var(--blur-xl);
  -webkit-backdrop-filter: var(--blur-xl);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  min-height: 500px;
}

/* ================================================
   Tab Pills
   ================================================ */

.main-tabs {
  display: flex;
  gap: 4px;
  padding: 6px;
  margin: 16px 20px;
  background: var(--glass-3);
  border-radius: var(--radius-full);
}

.main-tab {
  flex: 1;
  padding: 9px;
  border: none;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-tertiary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-spring-smooth);
  font-family: inherit;
}

.main-tab.active {
  background: var(--glass-1);
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
}

/* ================================================
   Tab Content
   ================================================ */

.tab-content {
  padding: 0 20px 20px;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.tab-search {
  flex: 1;
  min-width: 160px;
  padding: 9px 14px;
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

.tab-search:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-glow);
}

.tab-select {
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--glass-border-3);
  background: var(--glass-3);
  font-size: 13px;
  font-family: inherit;
  color: var(--text-primary);
  outline: none;
  cursor: pointer;
}

.tab-action-btn {
  padding: 9px 18px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--brand);
  color: #fff;
  font-size: 13px;
  font-weight: 680;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-spring-soft);
  font-family: inherit;
  box-shadow: 0 2px 6px var(--brand-glow);
}

.tab-action-btn:hover {
  background: var(--brand-hover);
  box-shadow: 0 4px 14px var(--brand-glow);
  transform: translateY(-1px);
}

.tab-empty {
  text-align: center;
  padding: 48px 0;
  font-size: 14px;
}

/* ================================================
   Friend Cards
   ================================================ */

.friend-list {
  display: grid;
  gap: 8px;
}

.friend-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--glass-3);
  border: 1px solid transparent;
  transition: all var(--duration-normal) var(--ease-spring-smooth);
}

.friend-card:hover {
  background: var(--glass-2);
  border-color: var(--glass-border-3);
  box-shadow: var(--shadow-sm);
}

.friend-avatar {
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

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-weight: 680;
  font-size: 15px;
  margin-bottom: 2px;
}

.friend-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  flex-wrap: wrap;
}

.friend-group-tag {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
  background: var(--brand-soft);
  color: var(--brand);
}

.relation-tag {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
}

.rel-ok { background: rgba(46,204,113,0.12); color: var(--success); }
.rel-friend { background: rgba(0,0,0,0.04); color: var(--text-tertiary); }
.rel-pending { background: rgba(240,160,48,0.12); color: var(--warning); }

.status-tag {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
}

.status-PENDING { background: rgba(240,160,48,0.12); color: var(--warning); }
.status-ACCEPTED { background: rgba(46,204,113,0.12); color: var(--success); }
.status-REJECTED { background: rgba(232,64,64,0.08); color: var(--danger); }

.friend-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.friend-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 999px;
  background: rgba(0,0,0,0.04);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-expo);
  font-family: inherit;
  white-space: nowrap;
}

.friend-btn:hover:not(:disabled) {
  background: rgba(0,0,0,0.08);
  color: var(--text-primary);
}

.friend-btn.primary {
  background: var(--brand-soft);
  color: var(--brand);
}

.friend-btn.primary:hover:not(:disabled) {
  background: var(--brand);
  color: #fff;
}

.friend-btn.danger:hover:not(:disabled) {
  background: var(--danger-soft);
  color: var(--danger);
}

.friend-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ================================================
   Dialogs
   ================================================ */

.dialog-select {
  width: 100%;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--glass-border-3);
  background: var(--glass-3);
  font-size: 14px;
  font-family: inherit;
  color: var(--text-primary);
  outline: none;
}

.dialog-input {
  width: 100%;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--glass-border-3);
  background: var(--glass-3);
  font-size: 14px;
  font-family: inherit;
  color: var(--text-primary);
  outline: none;
}

.dialog-field {
  margin-bottom: 16px;
}

.dialog-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.dialog-btn {
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 680;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-spring-soft);
  font-family: inherit;
  margin-left: 8px;
}

.dialog-btn.cancel {
  background: rgba(0,0,0,0.05);
  color: var(--text-secondary);
}

.dialog-btn.cancel:hover {
  background: rgba(0,0,0,0.1);
}

.dialog-btn.confirm {
  background: var(--brand);
  color: #fff;
  box-shadow: 0 2px 6px var(--brand-glow);
}

.dialog-btn.confirm:hover {
  background: var(--brand-hover);
  box-shadow: 0 4px 14px var(--brand-glow);
}

/* ================================================
   Detail Drawer
   ================================================ */

.detail-card {
  text-align: center;
  padding: 20px 0;
}

.detail-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  margin: 0 auto 16px;
}

.detail-name {
  font-size: 22px;
  font-weight: 780;
  margin-bottom: 4px;
}

.detail-username {
  font-size: 14px;
  margin-bottom: 24px;
}

.detail-fields {
  text-align: left;
}

.detail-field {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--glass-border-3);
  font-size: 14px;
}

.detail-label {
  font-weight: 600;
  color: var(--text-secondary);
}

/* ================================================
   Pagination
   ================================================ */

:deep(.el-pagination) {
  justify-content: center;
  padding-top: 14px;
}

/* ================================================
   Responsive
   ================================================ */

@media (max-width: 860px) {
  .friends-layout {
    grid-template-columns: 1fr;
  }

  .friends-sidebar {
    position: static;
    padding: 16px;
  }

  .sidebar-groups {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }

  .friend-card {
    flex-wrap: wrap;
  }

  .friend-actions {
    width: 100%;
    justify-content: flex-start;
    padding-left: 62px;
  }

  .friend-avatar {
    width: 42px;
    height: 42px;
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .friends-shell {
    padding: 10px 8px 20px;
  }

  .friends-topbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .main-tabs {
    margin: 12px 12px;
  }

  .tab-content {
    padding: 0 12px 16px;
  }
}
</style>

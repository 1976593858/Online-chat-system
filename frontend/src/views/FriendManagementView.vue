<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <div class="brand-mark">Online Chat</div>
        <h1 class="page-title">好友管理</h1>
        <p class="page-subtitle">搜索用户、处理申请、维护分组和好友资料。</p>
      </div>
      <nav class="nav-links">
        <RouterLink class="nav-link" to="/friends">好友</RouterLink>
        <RouterLink class="nav-link" to="/conversations">消息列表</RouterLink>
        <el-button plain @click="logout">退出</el-button>
      </nav>
    </header>

    <section class="workspace-grid">
      <aside class="side-panel glass-card">
        <div class="toolbar">
          <strong>好友分组</strong>
          <el-button type="primary" size="small" @click="promptCreateGroup">新建</el-button>
        </div>
        <div class="stack">
          <el-card
            v-for="group in friendStore.groups"
            :key="group.id"
            shadow="never"
            :class="{ active: activeGroupId === group.id }"
            class="group-card"
            @click="selectGroup(group.id)"
          >
            <div class="group-row">
              <span>{{ group.name }}</span>
              <el-tag size="small">{{ group.friendCount || 0 }}</el-tag>
            </div>
            <div class="group-actions" @click.stop>
              <el-tag v-if="group.isDefault === 1" type="info" size="small">默认</el-tag>
              <el-button v-else link size="small" @click="promptRenameGroup(group)">改名</el-button>
              <el-button v-if="group.isDefault !== 1" link type="danger" size="small" @click="confirmDeleteGroup(group)">删除</el-button>
            </div>
          </el-card>
        </div>
      </aside>

      <section class="main-panel glass-card">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="好友列表" name="friends">
            <div class="toolbar">
              <el-input v-model="friendKeyword" clearable placeholder="按用户名、昵称、备注搜索" style="max-width: 360px" @keyup.enter="loadFriends(1)" />
              <el-button type="primary" @click="loadFriends(1)">查询</el-button>
            </div>
            <el-table :data="friendStore.friendsPage.records" row-key="friendId">
              <el-table-column label="好友">
                <template #default="{ row }">
                  <el-avatar :src="row.avatar">{{ firstLetter(row.nickname || row.username) }}</el-avatar>
                  <span class="friend-name">{{ row.remark || row.nickname || row.username }}</span>
                  <span class="muted">@{{ row.username }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="groupName" label="分组" width="140" />
              <el-table-column prop="createdAt" label="添加时间" width="180" />
              <el-table-column label="操作" width="280">
                <template #default="{ row }">
                  <el-button link @click="openDetail(row.friendId)">详情</el-button>
                  <el-button link @click="promptRemark(row)">备注</el-button>
                  <el-button link @click="openMoveDialog(row)">移动</el-button>
                  <el-button link type="danger" @click="confirmDeleteFriend(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              layout="prev, pager, next, total"
              :total="friendStore.friendsPage.total"
              :current-page="friendStore.friendsPage.pageNo"
              :page-size="friendStore.friendsPage.pageSize"
              @current-change="loadFriends"
            />
          </el-tab-pane>

          <el-tab-pane label="搜索用户" name="search">
            <div class="toolbar">
              <el-input v-model="searchKeyword" clearable placeholder="输入用户名、昵称或邮箱" style="max-width: 420px" @keyup.enter="searchUsers(1)" />
              <el-button type="primary" @click="searchUsers(1)">搜索</el-button>
            </div>
            <el-table :data="friendStore.searchPage.records" row-key="id">
              <el-table-column label="用户">
                <template #default="{ row }">
                  <el-avatar :src="row.avatar">{{ firstLetter(row.nickname || row.username) }}</el-avatar>
                  <span class="friend-name">{{ row.nickname || row.username }}</span>
                  <span class="muted">@{{ row.username }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="email" label="邮箱" />
              <el-table-column label="关系" width="160">
                <template #default="{ row }">
                  <el-tag :type="relationTagType(row.relationStatus)">{{ relationText(row.relationStatus) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160">
                <template #default="{ row }">
                  <el-button type="primary" size="small" :disabled="row.relationStatus !== 'NONE'" @click="promptSendRequest(row)">添加好友</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              layout="prev, pager, next, total"
              :total="friendStore.searchPage.total"
              :current-page="friendStore.searchPage.pageNo"
              :page-size="friendStore.searchPage.pageSize"
              @current-change="searchUsers"
            />
          </el-tab-pane>

          <el-tab-pane label="好友申请" name="requests">
            <div class="toolbar">
              <div>
                <el-select v-model="requestDirection" style="width: 140px" @change="loadRequests(1)">
                  <el-option label="收到的申请" value="received" />
                  <el-option label="发出的申请" value="sent" />
                </el-select>
                <el-select v-model="requestStatus" clearable style="width: 140px; margin-left: 8px" @change="loadRequests(1)">
                  <el-option label="待处理" value="PENDING" />
                  <el-option label="已同意" value="ACCEPTED" />
                  <el-option label="已拒绝" value="REJECTED" />
                </el-select>
              </div>
              <el-button @click="loadRequests(1)">刷新</el-button>
            </div>
            <el-table :data="friendStore.requestsPage.records" row-key="id">
              <el-table-column label="申请人/接收人">
                <template #default="{ row }">
                  <span v-if="requestDirection === 'received'">{{ row.senderNickname || row.senderUsername }}</span>
                  <span v-else>{{ row.receiverNickname || row.receiverUsername }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="message" label="申请备注" />
              <el-table-column prop="status" label="状态" width="120" />
              <el-table-column prop="createdAt" label="申请时间" width="180" />
              <el-table-column v-if="requestDirection === 'received'" label="操作" width="180">
                <template #default="{ row }">
                  <el-button link :disabled="row.status !== 'PENDING'" @click="openAcceptDialog(row)">同意</el-button>
                  <el-button link type="danger" :disabled="row.status !== 'PENDING'" @click="promptReject(row)">拒绝</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              layout="prev, pager, next, total"
              :total="friendStore.requestsPage.total"
              :current-page="friendStore.requestsPage.pageNo"
              :page-size="friendStore.requestsPage.pageSize"
              @current-change="loadRequests"
            />
          </el-tab-pane>
        </el-tabs>
      </section>
    </section>

    <el-dialog v-model="moveDialog.visible" title="移动好友到分组" width="420px">
      <el-select v-model="moveDialog.groupId" placeholder="选择分组" style="width: 100%">
        <el-option v-for="group in friendStore.groups" :key="group.id" :label="group.name" :value="group.id" />
      </el-select>
      <template #footer>
        <el-button @click="moveDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitMoveFriend">确认移动</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="acceptDialog.visible" title="同意好友申请" width="460px">
      <el-form label-position="top">
        <el-form-item label="放入分组">
          <el-select v-model="acceptDialog.groupId" placeholder="默认分组" style="width: 100%">
            <el-option v-for="group in friendStore.groups" :key="group.id" :label="group.name" :value="group.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="acceptDialog.remark" maxlength="50" show-word-limit placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitAcceptRequest">同意</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawerVisible" title="好友详情" size="420px">
      <el-descriptions v-if="friendStore.selectedFriend" :column="1" border>
        <el-descriptions-item label="用户名">{{ friendStore.selectedFriend.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ friendStore.selectedFriend.nickname }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ friendStore.selectedFriend.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分组">{{ friendStore.selectedFriend.groupName }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ friendStore.selectedFriend.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ friendStore.selectedFriend.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="成为好友">{{ friendStore.selectedFriend.friendSince }}</el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ friendStore.selectedFriend.lastLoginAt || '-' }}</el-descriptions-item>
      </el-descriptions>
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

const activeTab = ref('friends')
const activeGroupId = ref(null)
const friendKeyword = ref('')
const searchKeyword = ref('')
const requestDirection = ref('received')
const requestStatus = ref('PENDING')
const detailDrawerVisible = ref(false)

const moveDialog = reactive({
  visible: false,
  friendId: null,
  groupId: null
})

const acceptDialog = reactive({
  visible: false,
  requestId: null,
  groupId: null,
  remark: ''
})

onMounted(async () => {
  await Promise.all([friendStore.loadGroups(), loadFriends(1), loadRequests(1)])
})

function firstLetter(value) {
  return value ? value.slice(0, 1).toUpperCase() : '?'
}

function relationText(status) {
  const map = {
    SELF: '自己',
    FRIEND: '已是好友',
    NONE: '可添加',
    PENDING_SENT: '已发送申请',
    PENDING_RECEIVED: '待你处理'
  }
  return map[status] || status
}

function relationTagType(status) {
  if (status === 'NONE') return 'success'
  if (status === 'FRIEND') return 'info'
  if (status?.startsWith('PENDING')) return 'warning'
  return ''
}

function selectGroup(groupId) {
  activeGroupId.value = activeGroupId.value === groupId ? null : groupId
  loadFriends(1)
}

async function loadFriends(pageNo = 1) {
  await friendStore.loadFriends({
    pageNo,
    pageSize: friendStore.friendsPage.pageSize,
    groupId: activeGroupId.value || undefined,
    keyword: friendKeyword.value || undefined
  })
}

async function searchUsers(pageNo = 1) {
  await friendStore.searchUsers({
    pageNo,
    pageSize: friendStore.searchPage.pageSize,
    keyword: searchKeyword.value || undefined
  })
}

async function loadRequests(pageNo = 1) {
  await friendStore.loadRequests({
    pageNo,
    pageSize: friendStore.requestsPage.pageSize,
    direction: requestDirection.value,
    status: requestStatus.value || undefined
  })
}

async function handleTabChange(name) {
  if (name === 'search' && friendStore.searchPage.records.length === 0) {
    await searchUsers(1)
  }
  if (name === 'requests') {
    await loadRequests(1)
  }
}

async function promptCreateGroup() {
  const { value } = await ElMessageBox.prompt('请输入分组名称', '创建分组', {
    inputPattern: /^.{1,32}$/,
    inputErrorMessage: '分组名称不能为空且不能超过32个字符'
  })
  await friendStore.createGroup(value)
}

async function promptRenameGroup(group) {
  const { value } = await ElMessageBox.prompt('请输入新的分组名称', '修改分组', {
    inputValue: group.name,
    inputPattern: /^.{1,32}$/,
    inputErrorMessage: '分组名称不能为空且不能超过32个字符'
  })
  await friendStore.updateGroup(group.id, value)
}

async function confirmDeleteGroup(group) {
  await ElMessageBox.confirm(`删除分组「${group.name}」后，好友将移动到默认分组。`, '删除分组', { type: 'warning' })
  await friendStore.deleteGroup(group.id)
  if (activeGroupId.value === group.id) {
    activeGroupId.value = null
  }
}

async function promptSendRequest(row) {
  const { value } = await ElMessageBox.prompt(`向 ${row.nickname || row.username} 发送好友申请`, '添加好友', {
    inputPlaceholder: '申请备注，可选'
  })
  await friendStore.sendRequest(row.id, value)
  await searchUsers(friendStore.searchPage.pageNo)
}

async function promptRemark(row) {
  const { value } = await ElMessageBox.prompt('请输入好友备注', '修改备注', {
    inputValue: row.remark || '',
    inputPlaceholder: '最多50个字符'
  })
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
  await friendStore.acceptRequest(acceptDialog.requestId, {
    groupId: acceptDialog.groupId,
    remark: acceptDialog.remark
  })
  acceptDialog.visible = false
}

async function promptReject(row) {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝好友申请', {
    inputPlaceholder: '可选'
  })
  await friendStore.rejectRequest(row.id, value)
}

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.group-card {
  cursor: pointer;
  border: 1px solid transparent;
  transition: border-color 0.18s ease, transform 0.18s ease;
}

.group-card:hover,
.group-card.active {
  border-color: var(--brand);
  transform: translateY(-1px);
}

.group-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-weight: 800;
}

.group-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
  margin-top: 8px;
}

.friend-name {
  margin-left: 10px;
  margin-right: 8px;
  font-weight: 800;
}

:deep(.el-pagination) {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

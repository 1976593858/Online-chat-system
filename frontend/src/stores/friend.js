import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { searchUsers } from '../api/users'
import { createFriendGroup, deleteFriendGroup, listFriendGroups, updateFriendGroup } from '../api/friendGroups'
import { deleteFriend, getFriendDetail, listFriends, moveFriendGroup, updateFriendRemark } from '../api/friends'
import { acceptFriendRequest, createFriendRequest, listFriendRequests, rejectFriendRequest } from '../api/friendRequests'

export const useFriendStore = defineStore('friend', {
  state: () => ({
    groups: [],
    friendsPage: { records: [], total: 0, pageNo: 1, pageSize: 10, pages: 0 },
    requestsPage: { records: [], total: 0, pageNo: 1, pageSize: 10, pages: 0 },
    searchPage: { records: [], total: 0, pageNo: 1, pageSize: 10, pages: 0 },
    selectedFriend: null,
    loading: false
  }),
  actions: {
    async loadGroups() {
      this.groups = await listFriendGroups()
    },
    async loadFriends(params = {}) {
      this.friendsPage = await listFriends({ pageNo: 1, pageSize: 10, ...params })
    },
    async searchUsers(params = {}) {
      this.searchPage = await searchUsers({ pageNo: 1, pageSize: 10, ...params })
    },
    async loadRequests(params = {}) {
      this.requestsPage = await listFriendRequests({ direction: 'received', pageNo: 1, pageSize: 10, ...params })
    },
    async sendRequest(receiverId, message) {
      await createFriendRequest({ receiverId, message })
      ElMessage.success('好友申请已发送')
    },
    async acceptRequest(requestId, data) {
      await acceptFriendRequest(requestId, data)
      ElMessage.success('已同意好友申请')
      await Promise.all([this.loadRequests(), this.loadFriends(), this.loadGroups()])
    },
    async rejectRequest(requestId, handleReason) {
      await rejectFriendRequest(requestId, { handleReason })
      ElMessage.success('已拒绝好友申请')
      await this.loadRequests()
    },
    async createGroup(name) {
      await createFriendGroup({ name })
      ElMessage.success('分组已创建')
      await this.loadGroups()
    },
    async updateGroup(groupId, name) {
      await updateFriendGroup(groupId, { name })
      ElMessage.success('分组已更新')
      await this.loadGroups()
    },
    async deleteGroup(groupId) {
      await deleteFriendGroup(groupId)
      ElMessage.success('分组已删除')
      await Promise.all([this.loadGroups(), this.loadFriends()])
    },
    async moveFriend(friendId, groupId) {
      await moveFriendGroup(friendId, { groupId })
      ElMessage.success('好友已移动')
      await Promise.all([this.loadGroups(), this.loadFriends()])
    },
    async updateRemark(friendId, remark) {
      await updateFriendRemark(friendId, { remark })
      ElMessage.success('备注已更新')
      await this.loadFriends()
    },
    async removeFriend(friendId) {
      await deleteFriend(friendId)
      ElMessage.success('好友已删除')
      await Promise.all([this.loadGroups(), this.loadFriends()])
    },
    async loadFriendDetail(friendId) {
      this.selectedFriend = await getFriendDetail(friendId)
    }
  }
})

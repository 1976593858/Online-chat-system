import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'
import { useWebSocketStore } from './websocket'
import { ElMessage } from 'element-plus'

const AUDIO_CONSTRAINTS = {
  audio: {
    echoCancellation: { ideal: true },
    noiseSuppression: { ideal: true },
    autoGainControl: { ideal: true },
    sampleRate: { ideal: 48000 },
    channelCount: { ideal: 1 },
    latency: { ideal: 0.005 }
  }
}

const ICE_SERVERS = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    { urls: 'stun:stun2.l.google.com:19302' },
    { urls: 'stun:stun3.l.google.com:19302' },
    { urls: 'stun:stun4.l.google.com:19302' }
  ],
  iceCandidatePoolSize: 2
}

const CONNECTION_TIMEOUT_MS = 30000

export const useVoiceCallStore = defineStore('voiceCall', () => {
  const authStore = useAuthStore()
  const wsStore = useWebSocketStore()

  // 1-on-1 state
  const callState = ref('idle') // idle | calling | ringing | connected | ended
  const remoteUserId = ref('')
  const remoteUsername = ref('')
  const errorMsg = ref('')
  const startTime = ref(null)
  const elapsed = ref(0)
  const micMuted = ref(false)

  let peerConnection = null
  let localStream = null
  let remoteAudio = null
  let elapsedTimer = null
  let pendingOffer = null
  let connectionTimeout = null

  // Group call state
  const groupCallActive = ref(false)
  const groupCallRoomId = ref('')
  const groupCallGroupId = ref('')
  const groupCallGroupName = ref('')
  const groupCallParticipants = ref([]) // [{ userId, username, state: 'connected'|'connecting' }]
  const groupCallInitiator = ref('')

  let groupPeerConnections = new Map() // userId -> RTCPeerConnection
  let groupLocalStream = null
  let groupRemoteAudios = new Map() // userId -> Audio element

  const myId = () => String(authStore.user?.id || '')
  const myName = () => authStore.user?.nickname || authStore.user?.username || ''

  // ============================================================
  // 1-on-1 signaling
  // ============================================================

  function setupSignaling() {
    wsStore.addHandler(handleSignal)
  }

  function teardownSignaling() {
    wsStore.removeHandler(handleSignal)
  }

  function handleSignal(data) {
    const type = data.type
    const from = String(data.fromUserId || '')

    switch (type) {
      case 'call_offer':
        if (callState.value !== 'idle') {
          wsStore.send({ type: 'call_rejected', fromUserId: myId(), toUserId: from })
          return
        }
        remoteUserId.value = from
        remoteUsername.value = data.fromUsername || `用户 ${from}`
        pendingOffer = data.sdp
        callState.value = 'ringing'
        break

      case 'call_answer':
        if (callState.value !== 'calling' || from !== remoteUserId.value) return
        if (peerConnection && data.sdp) {
          peerConnection.setRemoteDescription(new RTCSessionDescription(data.sdp))
        }
        break

      case 'call_rejected':
        if (from !== remoteUserId.value) return
        stopCall('对方拒绝了通话')
        break

      case 'ice_candidate':
        if (!peerConnection || from !== remoteUserId.value || !data.candidate) return
        peerConnection.addIceCandidate(new RTCIceCandidate(data.candidate)).catch(() => {})
        break

      case 'call_ended':
        if (from !== remoteUserId.value) return
        stopCall('对方已挂断')
        break

      case 'call_failed':
        if (from !== remoteUserId.value) return
        stopCall(data.reason || '通话连接失败')
        break

      // Group call signals
      case 'group_call_start':
        handleGroupCallStart(data)
        break

      case 'group_call_join':
        handleGroupCallJoin(data)
        break

      case 'group_call_leave':
        handleGroupCallLeave(data)
        break

      case 'group_call_end':
        handleGroupCallEnd(data)
        break
    }
  }

  // ============================================================
  // 1-on-1 call
  // ============================================================

  function startConnectionTimeout() {
    clearConnectionTimeout()
    connectionTimeout = setTimeout(() => {
      if (callState.value === 'calling') {
        stopCall('通话连接超时，请检查网络后重试')
      }
    }, CONNECTION_TIMEOUT_MS)
  }

  function clearConnectionTimeout() {
    if (connectionTimeout) {
      clearTimeout(connectionTimeout)
      connectionTimeout = null
    }
  }

  async function startCall(toUserId, toUsername) {
    if (callState.value !== 'idle') return

    remoteUserId.value = String(toUserId)
    remoteUsername.value = toUsername || `用户 ${toUserId}`

    try {
      setupSignaling()
      callState.value = 'calling'

      localStream = await navigator.mediaDevices.getUserMedia(AUDIO_CONSTRAINTS)
      peerConnection = new RTCPeerConnection(ICE_SERVERS)

      localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream))

      peerConnection.onicecandidate = (e) => {
        if (e.candidate) {
          wsStore.send({
            type: 'ice_candidate',
            fromUserId: myId(),
            toUserId: remoteUserId.value,
            candidate: e.candidate
          })
        }
      }

      peerConnection.ontrack = (e) => {
        if (!remoteAudio) {
          remoteAudio = new Audio()
          remoteAudio.autoplay = true
        }
        remoteAudio.srcObject = e.streams[0]
      }

      peerConnection.onconnectionstatechange = () => {
        if (peerConnection) {
          const state = peerConnection.connectionState
          if (state === 'connected' && callState.value === 'calling') {
            callState.value = 'connected'
            clearConnectionTimeout()
            startTimer()
          } else if (state === 'failed' || state === 'disconnected') {
            stopCall('连接断开')
          }
        }
      }

      startConnectionTimeout()

      const offer = await peerConnection.createOffer()
      await peerConnection.setLocalDescription(offer)

      wsStore.send({
        type: 'call_offer',
        fromUserId: myId(),
        toUserId: remoteUserId.value,
        fromUsername: myName(),
        sdp: offer
      })
    } catch (err) {
      console.error('发起通话失败', err)
      if (err.name === 'NotAllowedError') {
        stopCall('请允许浏览器使用麦克风')
      } else {
        stopCall('发起通话失败: ' + (err.message || '未知错误'))
      }
    }
  }

  async function acceptCall() {
    if (callState.value !== 'ringing') return

    try {
      callState.value = 'calling'

      localStream = await navigator.mediaDevices.getUserMedia(AUDIO_CONSTRAINTS)
      peerConnection = new RTCPeerConnection(ICE_SERVERS)

      localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream))

      peerConnection.onicecandidate = (e) => {
        if (e.candidate) {
          wsStore.send({
            type: 'ice_candidate',
            fromUserId: myId(),
            toUserId: remoteUserId.value,
            candidate: e.candidate
          })
        }
      }

      peerConnection.ontrack = (e) => {
        if (!remoteAudio) {
          remoteAudio = new Audio()
          remoteAudio.autoplay = true
        }
        remoteAudio.srcObject = e.streams[0]
      }

      peerConnection.onconnectionstatechange = () => {
        if (peerConnection) {
          const s = peerConnection.connectionState
          if (s === 'connected') {
            callState.value = 'connected'
            clearConnectionTimeout()
            startTimer()
          } else if (s === 'failed' || s === 'disconnected') {
            stopCall('连接断开')
          }
        }
      }

      startConnectionTimeout()

      if (pendingOffer) {
        await peerConnection.setRemoteDescription(new RTCSessionDescription(pendingOffer))
        pendingOffer = null
      }

      const answer = await peerConnection.createAnswer()
      await peerConnection.setLocalDescription(answer)

      wsStore.send({
        type: 'call_answer',
        fromUserId: myId(),
        toUserId: remoteUserId.value,
        sdp: answer
      })
    } catch (err) {
      console.error('接听通话失败', err)
      if (err.name === 'NotAllowedError') {
        stopCall('请允许浏览器使用麦克风')
      } else {
        stopCall('接听失败: ' + (err.message || '未知错误'))
      }
    }
  }

  function rejectCall() {
    wsStore.send({
      type: 'call_rejected',
      fromUserId: myId(),
      toUserId: remoteUserId.value
    })
    resetCall()
  }

  function endCall() {
    wsStore.send({
      type: 'call_ended',
      fromUserId: myId(),
      toUserId: remoteUserId.value
    })
    stopCall()
  }

  function toggleMute() {
    micMuted.value = !micMuted.value
    if (localStream) {
      localStream.getAudioTracks().forEach(track => {
        track.enabled = !micMuted.value
      })
    }
    if (groupLocalStream) {
      groupLocalStream.getAudioTracks().forEach(track => {
        track.enabled = !micMuted.value
      })
    }
  }

  function stopCall(reason) {
    if (reason) {
      errorMsg.value = reason
      ElMessage.warning(reason)
    }
    if (callState.value === 'connected' || callState.value === 'calling') {
      callState.value = 'ended'
      setTimeout(() => resetCall(), 2500)
    } else {
      resetCall()
    }
  }

  function resetCall() {
    cleanup()
    callState.value = 'idle'
    remoteUserId.value = ''
    remoteUsername.value = ''
    errorMsg.value = ''
    pendingOffer = null
  }

  function cleanup() {
    stopTimer()
    clearConnectionTimeout()
    if (peerConnection) {
      peerConnection.onicecandidate = null
      peerConnection.ontrack = null
      peerConnection.onconnectionstatechange = null
      peerConnection.close()
      peerConnection = null
    }
    if (localStream) {
      localStream.getTracks().forEach(t => t.stop())
      localStream = null
    }
    if (remoteAudio) {
      remoteAudio.srcObject = null
      remoteAudio = null
    }
  }

  // ============================================================
  // Group call
  // ============================================================

  function handleGroupCallStart(data) {
    if (groupCallActive.value || callState.value !== 'idle') return
    groupCallActive.value = true
    groupCallRoomId.value = data.callRoomId || ''
    groupCallGroupId.value = data.groupId || ''
    groupCallGroupName.value = data.groupName || ''
    groupCallInitiator.value = String(data.fromUserId || '')
    groupCallParticipants.value = [{
      userId: String(data.fromUserId || ''),
      username: data.fromUsername || '发起者',
      state: 'connected'
    }]
    ElMessage.info(`${groupCallGroupName.value || '群聊'} 语音通话邀请`)
  }

  function handleGroupCallJoin(data) {
    const uid = String(data.fromUserId || '')
    const existing = groupCallParticipants.value.find(p => p.userId === uid)
    if (existing) {
      existing.state = 'connected'
    } else {
      groupCallParticipants.value.push({
        userId: uid,
        username: data.fromUsername || `用户 ${uid}`,
        state: 'connected'
      })
    }
    // If we're the initiator, create a peer connection to this new joiner
    if (myId() === groupCallInitiator.value && !groupPeerConnections.has(uid)) {
      createGroupPeerConnection(uid)
    }
  }

  function handleGroupCallLeave(data) {
    const uid = String(data.fromUserId || '')
    removeGroupParticipant(uid)
  }

  function handleGroupCallEnd(_data) {
    if (groupCallActive.value) {
      stopGroupCall('通话已结束')
    }
  }

  async function startGroupCall(groupId, groupName, onlineMembers) {
    if (groupCallActive.value || callState.value !== 'idle') return

    const roomId = 'gc_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
    groupCallActive.value = true
    groupCallRoomId.value = roomId
    groupCallGroupId.value = String(groupId)
    groupCallGroupName.value = groupName || ''
    groupCallInitiator.value = myId()
    groupCallParticipants.value = [{ userId: myId(), username: myName(), state: 'connected' }]

    try {
      groupLocalStream = await navigator.mediaDevices.getUserMedia(AUDIO_CONSTRAINTS)

      // Create peer connections to each online member
      for (const memberId of (onlineMembers || [])) {
        const uid = String(memberId)
        if (uid === myId()) continue
        createGroupPeerConnection(uid)
        groupCallParticipants.value.push({ userId: uid, username: `用户 ${uid}`, state: 'connecting' })
      }

      wsStore.send({
        type: 'group_call_start',
        groupId: String(groupId),
        callRoomId: roomId,
        fromUserId: myId(),
        fromUsername: myName(),
        groupName: groupName || ''
      })
    } catch (err) {
      console.error('发起群通话失败', err)
      stopGroupCall('发起群通话失败')
    }
  }

  function createGroupPeerConnection(targetUserId) {
    const uid = String(targetUserId)
    if (groupPeerConnections.has(uid)) return

    const pc = new RTCPeerConnection(ICE_SERVERS)
    groupPeerConnections.set(uid, pc)

    if (groupLocalStream) {
      groupLocalStream.getTracks().forEach(track => pc.addTrack(track, groupLocalStream))
    }

    pc.onicecandidate = (e) => {
      if (e.candidate) {
        wsStore.send({
          type: 'ice_candidate',
          fromUserId: myId(),
          toUserId: uid,
          candidate: e.candidate
        })
      }
    }

    pc.ontrack = (e) => {
      let audio = groupRemoteAudios.get(uid)
      if (!audio) {
        audio = new Audio()
        audio.autoplay = true
        groupRemoteAudios.set(uid, audio)
      }
      audio.srcObject = e.streams[0]
    }

    pc.onconnectionstatechange = () => {
      if (pc.connectionState === 'connected') {
        const p = groupCallParticipants.value.find(p => p.userId === uid)
        if (p) p.state = 'connected'
      } else if (pc.connectionState === 'failed' || pc.connectionState === 'disconnected') {
        removeGroupParticipant(uid)
      }
    }

    // Create offer (initiator sends offer to each participant)
    pc.createOffer().then(offer => {
      return pc.setLocalDescription(offer)
    }).then(() => {
      wsStore.send({
        type: 'call_offer',
        fromUserId: myId(),
        toUserId: uid,
        fromUsername: myName(),
        sdp: pc.localDescription
      })
    }).catch(err => {
      console.error('群通话 offer 失败', uid, err)
    })
  }

  function removeGroupParticipant(uid) {
    const pc = groupPeerConnections.get(uid)
    if (pc) {
      pc.close()
      groupPeerConnections.delete(uid)
    }
    const audio = groupRemoteAudios.get(uid)
    if (audio) {
      audio.srcObject = null
      groupRemoteAudios.delete(uid)
    }
    groupCallParticipants.value = groupCallParticipants.value.filter(p => p.userId !== uid)
  }

  function leaveGroupCall() {
    wsStore.send({
      type: 'group_call_leave',
      groupId: groupCallGroupId.value,
      fromUserId: myId()
    })
    stopGroupCall()
  }

  function endGroupCall() {
    wsStore.send({
      type: 'group_call_end',
      groupId: groupCallGroupId.value,
      fromUserId: myId()
    })
    stopGroupCall('通话已结束')
  }

  function stopGroupCall(reason) {
    if (reason) {
      ElMessage.info(reason)
    }
    // Close all peer connections
    for (const [uid, pc] of groupPeerConnections) {
      pc.close()
    }
    groupPeerConnections.clear()

    for (const [uid, audio] of groupRemoteAudios) {
      audio.srcObject = null
    }
    groupRemoteAudios.clear()

    if (groupLocalStream) {
      groupLocalStream.getTracks().forEach(t => t.stop())
      groupLocalStream = null
    }

    groupCallActive.value = false
    groupCallRoomId.value = ''
    groupCallGroupId.value = ''
    groupCallGroupName.value = ''
    groupCallInitiator.value = ''
    groupCallParticipants.value = []
  }

  // ============================================================
  // Shared timer
  // ============================================================

  function startTimer() {
    startTime.value = Date.now()
    elapsed.value = 0
    elapsedTimer = setInterval(() => {
      elapsed.value = Math.floor((Date.now() - startTime.value) / 1000)
    }, 1000)
  }

  function stopTimer() {
    if (elapsedTimer) {
      clearInterval(elapsedTimer)
      elapsedTimer = null
    }
  }

  function formatTime(sec) {
    const m = Math.floor(sec / 60)
    const s = sec % 60
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }

  return {
    callState, remoteUserId, remoteUsername, errorMsg, elapsed, micMuted,
    groupCallActive, groupCallRoomId, groupCallGroupId, groupCallGroupName,
    groupCallParticipants, groupCallInitiator,
    startCall, acceptCall, rejectCall, endCall, stopCall, resetCall, toggleMute,
    formatTime, setupSignaling,
    startGroupCall, leaveGroupCall, endGroupCall, stopGroupCall
  }
})

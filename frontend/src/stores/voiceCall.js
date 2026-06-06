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

const ANSWER_TIMEOUT_MS = 30000

export const useVoiceCallStore = defineStore('voiceCall', () => {
  const authStore = useAuthStore()
  const wsStore = useWebSocketStore()

  // ============================================================
  // 1-on-1 state
  // ============================================================
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
  let callTimeout = null
  let ringingTimeout = null

  // ============================================================
  // Group call state
  // ============================================================
  const groupCallState = ref('idle') // idle | calling | ringing | connected | ended
  const groupCallRoomId = ref('')
  const groupCallGroupId = ref('')
  const groupCallGroupName = ref('')
  const groupCallParticipants = ref([]) // [{ userId, username, nickname, state: 'invited'|'connected' }]
  const groupCallInitiator = ref('')
  const groupCallInitiatorName = ref('')
  const groupCallError = ref('')

  let groupPeerConnections = new Map()
  let groupLocalStream = null
  let groupRemoteAudios = new Map()
  let groupAnswerTimeout = null

  const myId = () => String(authStore.user?.id || '')
  const myName = () => authStore.user?.nickname || authStore.user?.username || ''

  // ============================================================
  // Signaling dispatcher
  // ============================================================

  function setupSignaling() {
    wsStore.addHandler(handleSignal)
  }

  function handleSignal(data) {
    const type = data.type
    const from = String(data.fromUserId || '')

    switch (type) {
      // --- 1-on-1 signals ---
      case 'call_offer': {
        const callRoomId = data.callRoomId
        // If this offer belongs to a group call we accepted, route to group handler
        if (callRoomId && groupCallState.value === 'calling' && groupCallRoomId.value === callRoomId) {
          handleGroupOffer(data)
          return
        }
        // Otherwise treat as 1-on-1 incoming call
        if (callState.value !== 'idle') {
          if (groupCallState.value !== 'idle') return // busy in group call
          wsStore.send({ type: 'call_rejected', fromUserId: myId(), toUserId: from })
          return
        }
        remoteUserId.value = from
        remoteUsername.value = data.fromUsername || `用户 ${from}`
        pendingOffer = data.sdp
        callState.value = 'ringing'
        startRingingTimeout()
        break
      }

      case 'call_answer': {
        const callRoomId = data.callRoomId
        if (callRoomId && groupCallState.value === 'calling' && groupCallRoomId.value === callRoomId) {
          handleGroupAnswer(data)
          return
        }
        if (callState.value !== 'calling' || from !== remoteUserId.value) return
        if (peerConnection && data.sdp) {
          peerConnection.setRemoteDescription(new RTCSessionDescription(data.sdp))
        }
        break
      }

      case 'call_rejected':
        if (from !== remoteUserId.value) return
        stopCall('对方拒绝接听')
        break

      case 'ice_candidate': {
        // Route to group peer connection if applicable
        if ((groupCallState.value === 'calling' || groupCallState.value === 'connected') && groupPeerConnections.has(from)) {
          const pc = groupPeerConnections.get(from)
          if (pc && data.candidate) {
            pc.addIceCandidate(new RTCIceCandidate(data.candidate)).catch(() => {})
          }
          return
        }
        if (!peerConnection || from !== remoteUserId.value || !data.candidate) return
        peerConnection.addIceCandidate(new RTCIceCandidate(data.candidate)).catch(() => {})
        break
      }

      case 'call_ended':
        if (from !== remoteUserId.value) return
        stopCall('对方已挂断')
        break

      case 'call_failed':
        if (from !== remoteUserId.value) return
        stopCall(data.reason || '通话连接失败')
        break

      // --- Group call signals ---
      case 'group_call_start':
        handleGroupCallStart(data)
        break

      case 'group_call_accept':
        handleGroupCallAccept(data)
        break

      case 'group_call_reject':
        handleGroupCallReject(data)
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

  function startCallTimeout(reason) {
    clearCallTimeout()
    callTimeout = setTimeout(() => {
      if (callState.value === 'calling') {
        stopCall(reason || '无人接听')
      }
    }, ANSWER_TIMEOUT_MS)
  }

  function startRingingTimeout() {
    clearRingingTimeout()
    ringingTimeout = setTimeout(() => {
      if (callState.value === 'ringing') {
        // Auto-dismiss; caller side will time out separately
        wsStore.send({ type: 'call_rejected', fromUserId: myId(), toUserId: remoteUserId.value })
        resetCall()
      }
    }, ANSWER_TIMEOUT_MS)
  }

  function clearCallTimeout() {
    if (callTimeout) { clearTimeout(callTimeout); callTimeout = null }
  }

  function clearRingingTimeout() {
    if (ringingTimeout) { clearTimeout(ringingTimeout); ringingTimeout = null }
  }

  async function startCall(toUserId, toUsername) {
    if (callState.value !== 'idle' || groupCallState.value !== 'idle') return

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
          wsStore.send({ type: 'ice_candidate', fromUserId: myId(), toUserId: remoteUserId.value, candidate: e.candidate })
        }
      }

      peerConnection.ontrack = (e) => {
        if (!remoteAudio) { remoteAudio = new Audio(); remoteAudio.autoplay = true }
        remoteAudio.srcObject = e.streams[0]
      }

      peerConnection.onconnectionstatechange = () => {
        if (!peerConnection) return
        const s = peerConnection.connectionState
        if (s === 'connected' && callState.value === 'calling') {
          callState.value = 'connected'
          clearCallTimeout()
          startTimer()
        } else if (s === 'failed' || s === 'disconnected') {
          stopCall('连接断开')
        }
      }

      startCallTimeout('无人接听')

      const offer = await peerConnection.createOffer()
      await peerConnection.setLocalDescription(offer)

      wsStore.send({
        type: 'call_offer', fromUserId: myId(), toUserId: remoteUserId.value,
        fromUsername: myName(), sdp: offer
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
    clearRingingTimeout()

    try {
      callState.value = 'calling'

      localStream = await navigator.mediaDevices.getUserMedia(AUDIO_CONSTRAINTS)
      peerConnection = new RTCPeerConnection(ICE_SERVERS)

      localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream))

      peerConnection.onicecandidate = (e) => {
        if (e.candidate) {
          wsStore.send({ type: 'ice_candidate', fromUserId: myId(), toUserId: remoteUserId.value, candidate: e.candidate })
        }
      }

      peerConnection.ontrack = (e) => {
        if (!remoteAudio) { remoteAudio = new Audio(); remoteAudio.autoplay = true }
        remoteAudio.srcObject = e.streams[0]
      }

      peerConnection.onconnectionstatechange = () => {
        if (!peerConnection) return
        const s = peerConnection.connectionState
        if (s === 'connected') {
          callState.value = 'connected'
          clearCallTimeout()
          startTimer()
        } else if (s === 'failed' || s === 'disconnected') {
          stopCall('连接断开')
        }
      }

      startCallTimeout('连接超时')

      if (pendingOffer) {
        await peerConnection.setRemoteDescription(new RTCSessionDescription(pendingOffer))
        pendingOffer = null
      }

      const answer = await peerConnection.createAnswer()
      await peerConnection.setLocalDescription(answer)

      wsStore.send({ type: 'call_answer', fromUserId: myId(), toUserId: remoteUserId.value, sdp: answer })
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
    clearRingingTimeout()
    wsStore.send({ type: 'call_rejected', fromUserId: myId(), toUserId: remoteUserId.value })
    resetCall()
  }

  function endCall() {
    wsStore.send({ type: 'call_ended', fromUserId: myId(), toUserId: remoteUserId.value })
    stopCall()
  }

  function toggleMute() {
    micMuted.value = !micMuted.value
    if (localStream) localStream.getAudioTracks().forEach(t => { t.enabled = !micMuted.value })
    if (groupLocalStream) groupLocalStream.getAudioTracks().forEach(t => { t.enabled = !micMuted.value })
  }

  function stopCall(reason) {
    clearCallTimeout()
    clearRingingTimeout()
    if (reason) { errorMsg.value = reason; ElMessage.info(reason) }
    if (callState.value === 'connected' || callState.value === 'calling') {
      callState.value = 'ended'
      setTimeout(() => resetCall(), 2500)
    } else {
      resetCall()
    }
  }

  function resetCall() {
    cleanupPeer()
    callState.value = 'idle'
    remoteUserId.value = ''
    remoteUsername.value = ''
    errorMsg.value = ''
    pendingOffer = null
  }

  function cleanupPeer() {
    stopTimer()
    clearCallTimeout()
    clearRingingTimeout()
    if (peerConnection) {
      peerConnection.onicecandidate = null
      peerConnection.ontrack = null
      peerConnection.onconnectionstatechange = null
      peerConnection.close()
      peerConnection = null
    }
    if (localStream) { localStream.getTracks().forEach(t => t.stop()); localStream = null }
    if (remoteAudio) { remoteAudio.srcObject = null; remoteAudio = null }
  }

  // ============================================================
  // Group call — initiator helpers
  // ============================================================

  function startGroupAnswerTimeout() {
    clearGroupAnswerTimeout()
    groupAnswerTimeout = setTimeout(() => {
      // After 30s, anyone still "invited" never responded
      const answered = groupCallParticipants.value.filter(p => p.state === 'connected')
      if (answered.length <= 1) {
        // Only initiator is "connected", no one else answered
        stopGroupCall('无人接听')
      } else {
        // Some answered, remove the rest silently
        groupCallParticipants.value = groupCallParticipants.value.filter(p => p.state !== 'invited')
        if (groupCallParticipants.value.every(p => p.userId === myId())) {
          stopGroupCall('无人接听')
        }
      }
    }, ANSWER_TIMEOUT_MS)
  }

  function clearGroupAnswerTimeout() {
    if (groupAnswerTimeout) { clearTimeout(groupAnswerTimeout); groupAnswerTimeout = null }
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
        wsStore.send({ type: 'ice_candidate', fromUserId: myId(), toUserId: uid, candidate: e.candidate })
      }
    }

    pc.ontrack = (e) => {
      let audio = groupRemoteAudios.get(uid)
      if (!audio) { audio = new Audio(); audio.autoplay = true; groupRemoteAudios.set(uid, audio) }
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

    pc.createOffer().then(offer => pc.setLocalDescription(offer)).then(() => {
      wsStore.send({
        type: 'call_offer', fromUserId: myId(), toUserId: uid,
        fromUsername: myName(), sdp: pc.localDescription,
        callRoomId: groupCallRoomId.value
      })
    }).catch(err => { console.error('群通话 offer 失败', uid, err) })
  }

  function removeGroupParticipant(uid) {
    const pc = groupPeerConnections.get(uid)
    if (pc) { pc.close(); groupPeerConnections.delete(uid) }
    const audio = groupRemoteAudios.get(uid)
    if (audio) { audio.srcObject = null; groupRemoteAudios.delete(uid) }
    groupCallParticipants.value = groupCallParticipants.value.filter(p => p.userId !== uid)
  }

  // ============================================================
  // Group call — event handlers
  // ============================================================

  function handleGroupCallStart(data) {
    // Ignore self-broadcast (backend should filter, but guard here too)
    if (String(data.fromUserId || '') === myId()) return
    if (groupCallState.value !== 'idle' || callState.value !== 'idle') {
      // Busy — silently reject
      wsStore.send({ type: 'group_call_reject', fromUserId: myId(), toUserId: data.fromUserId, callRoomId: data.callRoomId })
      return
    }
    groupCallState.value = 'ringing'
    groupCallRoomId.value = data.callRoomId || ''
    groupCallGroupId.value = data.groupId || ''
    groupCallGroupName.value = data.groupName || ''
    groupCallInitiator.value = String(data.fromUserId || '')
    groupCallInitiatorName.value = data.fromUsername || '发起者'
    groupCallParticipants.value = [{
      userId: String(data.fromUserId || ''),
      username: data.fromUsername || '发起者',
      state: 'connected'
    }]
    groupCallError.value = ''
    startGroupRingingTimeout()
  }

  function handleGroupCallAccept(data) {
    if (groupCallState.value !== 'calling' || groupCallInitiator.value !== myId()) return
    const uid = String(data.fromUserId || '')
    if (!groupPeerConnections.has(uid)) {
      createGroupPeerConnection(uid)
    }
    const p = groupCallParticipants.value.find(p => p.userId === uid)
    if (p) p.state = 'connecting'
  }

  function handleGroupCallReject(data) {
    if (groupCallState.value !== 'calling' || groupCallInitiator.value !== myId()) return
    const uid = String(data.fromUserId || '')
    removeGroupParticipant(uid)
  }

  function handleGroupCallLeave(data) {
    const uid = String(data.fromUserId || '')
    removeGroupParticipant(uid)
    // If initiator left and I'm not initiator, close
    if (uid === groupCallInitiator.value && myId() !== uid) {
      stopGroupCall('通话已结束')
    }
  }

  function handleGroupCallEnd(_data) {
    if (groupCallState.value !== 'idle') {
      stopGroupCall('通话已结束')
    }
  }

  // Group offer: participant receives WebRTC offer from initiator
  async function handleGroupOffer(data) {
    const uid = String(data.fromUserId || '')
    try {
      if (!groupLocalStream) {
        groupLocalStream = await navigator.mediaDevices.getUserMedia(AUDIO_CONSTRAINTS)
      }
      const pc = new RTCPeerConnection(ICE_SERVERS)
      groupPeerConnections.set(uid, pc)

      if (groupLocalStream) {
        groupLocalStream.getTracks().forEach(track => pc.addTrack(track, groupLocalStream))
      }

      pc.onicecandidate = (e) => {
        if (e.candidate) {
          wsStore.send({ type: 'ice_candidate', fromUserId: myId(), toUserId: uid, candidate: e.candidate })
        }
      }

      pc.ontrack = (e) => {
        let audio = groupRemoteAudios.get(uid)
        if (!audio) { audio = new Audio(); audio.autoplay = true; groupRemoteAudios.set(uid, audio) }
        audio.srcObject = e.streams[0]
      }

      pc.onconnectionstatechange = () => {
        if (pc.connectionState === 'connected') {
          groupCallState.value = 'connected'
          clearGroupAnswerTimeout()
          startTimer()
          const p = groupCallParticipants.value.find(p => p.userId === uid)
          if (p) p.state = 'connected'
        } else if (pc.connectionState === 'failed' || pc.connectionState === 'disconnected') {
          stopGroupCall('连接断开')
        }
      }

      await pc.setRemoteDescription(new RTCSessionDescription(data.sdp))
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)

      wsStore.send({
        type: 'call_answer', fromUserId: myId(), toUserId: uid,
        sdp: answer, callRoomId: groupCallRoomId.value
      })
    } catch (err) {
      console.error('群通话接听失败', err)
      stopGroupCall('接听失败')
    }
  }

  // Group answer: initiator receives answer from participant
  function handleGroupAnswer(data) {
    const uid = String(data.fromUserId || '')
    const pc = groupPeerConnections.get(uid)
    if (pc && data.sdp) {
      pc.setRemoteDescription(new RTCSessionDescription(data.sdp)).catch(() => {})
    }
  }

  // ============================================================
  // Group call — user actions
  // ============================================================

  async function startGroupCall(groupId, groupName) {
    if (groupCallState.value !== 'idle' || callState.value !== 'idle') return

    const roomId = 'gc_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
    groupCallState.value = 'calling'
    groupCallRoomId.value = roomId
    groupCallGroupId.value = String(groupId)
    groupCallGroupName.value = groupName || ''
    groupCallInitiator.value = myId()
    groupCallInitiatorName.value = myName()
    groupCallParticipants.value = [{ userId: myId(), username: myName(), state: 'connected' }]
    groupCallError.value = ''

    try {
      groupLocalStream = await navigator.mediaDevices.getUserMedia(AUDIO_CONSTRAINTS)

      // Broadcast invitation — do NOT create peer connections yet
      wsStore.send({
        type: 'group_call_start',
        groupId: String(groupId),
        callRoomId: roomId,
        fromUserId: myId(),
        fromUsername: myName(),
        groupName: groupName || ''
      })

      startGroupAnswerTimeout()
    } catch (err) {
      console.error('发起群通话失败', err)
      stopGroupCall('发起群通话失败')
    }
  }

  function acceptGroupCall() {
    if (groupCallState.value !== 'ringing') return
    clearGroupRingingTimeout()
    groupCallState.value = 'calling' // waiting for initiator's WebRTC offer
    wsStore.send({
      type: 'group_call_accept',
      fromUserId: myId(),
      toUserId: groupCallInitiator.value,
      callRoomId: groupCallRoomId.value,
      fromUsername: myName()
    })
  }

  function rejectGroupCall() {
    if (groupCallState.value !== 'ringing') return
    clearGroupRingingTimeout()
    wsStore.send({
      type: 'group_call_reject',
      fromUserId: myId(),
      toUserId: groupCallInitiator.value,
      callRoomId: groupCallRoomId.value
    })
    resetGroupCall()
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

  function startGroupRingingTimeout() {
    clearGroupAnswerTimeout()
    groupAnswerTimeout = setTimeout(() => {
      if (groupCallState.value === 'ringing') {
        wsStore.send({
          type: 'group_call_reject',
          fromUserId: myId(),
          toUserId: groupCallInitiator.value,
          callRoomId: groupCallRoomId.value
        })
        resetGroupCall()
      }
    }, ANSWER_TIMEOUT_MS)
  }

  function stopGroupCall(reason) {
    clearGroupAnswerTimeout()
    groupCallError.value = reason || ''
    if (reason) ElMessage.info(reason)

    for (const [, pc] of groupPeerConnections) { pc.close() }
    groupPeerConnections.clear()
    for (const [, audio] of groupRemoteAudios) { audio.srcObject = null }
    groupRemoteAudios.clear()
    if (groupLocalStream) { groupLocalStream.getTracks().forEach(t => t.stop()); groupLocalStream = null }

    if (groupCallState.value === 'connected' || groupCallState.value === 'calling') {
      groupCallState.value = 'ended'
      setTimeout(() => resetGroupCall(), 2500)
    } else {
      resetGroupCall()
    }
  }

  function resetGroupCall() {
    stopTimer()
    clearGroupAnswerTimeout()
    for (const [, pc] of groupPeerConnections) { pc.close() }
    groupPeerConnections.clear()
    for (const [, audio] of groupRemoteAudios) { audio.srcObject = null }
    groupRemoteAudios.clear()
    if (groupLocalStream) { groupLocalStream.getTracks().forEach(t => t.stop()); groupLocalStream = null }

    groupCallState.value = 'idle'
    groupCallRoomId.value = ''
    groupCallGroupId.value = ''
    groupCallGroupName.value = ''
    groupCallInitiator.value = ''
    groupCallInitiatorName.value = ''
    groupCallParticipants.value = []
    groupCallError.value = ''
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
    if (elapsedTimer) { clearInterval(elapsedTimer); elapsedTimer = null }
  }

  function formatTime(sec) {
    const m = Math.floor(sec / 60)
    const s = sec % 60
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }

  return {
    // 1-on-1
    callState, remoteUserId, remoteUsername, errorMsg, elapsed, micMuted,
    startCall, acceptCall, rejectCall, endCall, stopCall, resetCall, toggleMute,
    formatTime, setupSignaling,
    // Group
    groupCallState, groupCallRoomId, groupCallGroupId, groupCallGroupName,
    groupCallParticipants, groupCallInitiator, groupCallInitiatorName, groupCallError,
    startGroupCall, acceptGroupCall, rejectGroupCall, leaveGroupCall, endGroupCall, stopGroupCall
  }
})

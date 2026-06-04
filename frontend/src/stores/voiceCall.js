import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'
import { useWebSocketStore } from './websocket'
import { ElMessage } from 'element-plus'

const ICE_SERVERS = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' }
  ]
}

export const useVoiceCallStore = defineStore('voiceCall', () => {
  const authStore = useAuthStore()
  const wsStore = useWebSocketStore()

  const callState = ref('idle') // idle | calling | ringing | connected | ended
  const remoteUserId = ref('')
  const remoteUsername = ref('')
  const errorMsg = ref('')
  const startTime = ref(null)
  const elapsed = ref(0)

  let peerConnection = null
  let localStream = null
  let remoteAudio = null
  let elapsedTimer = null
  let pendingOffer = null

  const myId = () => String(authStore.user?.id || '')
  const myName = () => authStore.user?.nickname || authStore.user?.username || ''

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
    }
  }

  async function startCall(toUserId, toUsername) {
    if (callState.value !== 'idle') return

    remoteUserId.value = String(toUserId)
    remoteUsername.value = toUsername || `用户 ${toUserId}`

    try {
      setupSignaling()
      callState.value = 'calling'

      localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false })
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
            startTimer()
          } else if (state === 'failed' || state === 'disconnected') {
            stopCall('连接断开')
          }
        }
      }

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

      localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false })
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
            startTimer()
          } else if (s === 'failed' || s === 'disconnected') {
            stopCall('连接断开')
          }
        }
      }

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
    teardownSignaling()
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
    callState, remoteUserId, remoteUsername, errorMsg, elapsed,
    startCall, acceptCall, rejectCall, endCall, stopCall, resetCall,
    formatTime, setupSignaling
  }
})

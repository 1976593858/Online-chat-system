<template>
  <RouterView />
  <VoiceCallDialog />
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useAuthStore } from './stores/auth'
import { useWebSocketStore } from './stores/websocket'
import { useFriendStore } from './stores/friend'
import { useVoiceCallStore } from './stores/voiceCall'
import VoiceCallDialog from './components/VoiceCallDialog.vue'

const authStore = useAuthStore()
const wsStore = useWebSocketStore()
const friendStore = useFriendStore()
const voiceCallStore = useVoiceCallStore()

onMounted(() => {
  if (authStore.token) {
    wsStore.connect()
    friendStore.initWSListener()
    voiceCallStore.setupSignaling()
  }
})

onUnmounted(() => {
  wsStore.disconnect()
})
</script>

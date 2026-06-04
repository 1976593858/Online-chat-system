<template>
  <RouterView />
  <VoiceCallDialog />
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useAuthStore } from './stores/auth'
import { useWebSocketStore } from './stores/websocket'
import { useFriendStore } from './stores/friend'
import VoiceCallDialog from './components/VoiceCallDialog.vue'

const authStore = useAuthStore()
const wsStore = useWebSocketStore()
const friendStore = useFriendStore()

onMounted(() => {
  if (authStore.token) {
    wsStore.connect()
    friendStore.initWSListener()
  }
})

onUnmounted(() => {
  wsStore.disconnect()
})
</script>

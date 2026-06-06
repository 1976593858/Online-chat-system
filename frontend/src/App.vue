<template>
  <div class="app-root">
    <RouterView v-slot="{ Component }">
      <transition name="view" mode="out-in">
        <component :is="Component" />
      </transition>
    </RouterView>
    <VoiceCallDialog />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, watch } from 'vue'
import { useAuthStore } from './stores/auth'
import { useWebSocketStore } from './stores/websocket'
import { useFriendStore } from './stores/friend'
import { useVoiceCallStore } from './stores/voiceCall'
import VoiceCallDialog from './components/VoiceCallDialog.vue'

const authStore = useAuthStore()
const wsStore = useWebSocketStore()
const friendStore = useFriendStore()
const voiceCallStore = useVoiceCallStore()

// Always register the signaling handler — it's a pure handler,
// doesn't need WebSocket to be connected. Messages will be
// routed through it once the WS connection is established.
voiceCallStore.setupSignaling()

onMounted(() => {
  if (authStore.token) {
    wsStore.connect()
    friendStore.initWSListener()
  }
})

// When the user logs in AFTER App has already mounted
// (e.g. first visit → login page → login success),
// the WebSocket connection needs to be established and
// the friend listener needs to be registered.
watch(
  () => authStore.token,
  (token, prev) => {
    if (token && !prev) {
      wsStore.connect()
      friendStore.initWSListener()
    }
  }
)

onUnmounted(() => {
  wsStore.disconnect()
})
</script>

<style>
/* View transitions */
.view-enter-active {
  animation: springIn 0.45s var(--ease-spring-soft) both;
}
.view-leave-active {
  animation: springOut 0.2s var(--ease-out-expo) both;
  position: absolute;
  width: 100%;
}

@keyframes springIn {
  0%   { opacity: 0; transform: scale(0.96) translateY(12px); }
  60%  { opacity: 1; transform: scale(1.01) translateY(-1px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}

@keyframes springOut {
  0%   { opacity: 1; transform: scale(1); }
  100% { opacity: 0; transform: scale(0.97) translateY(4px); }
}
</style>

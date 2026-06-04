<template>
  <Teleport to="body">
    <transition name="call-fade">
      <div v-if="callState !== 'idle'" class="voice-call-overlay">
        <!-- 主叫等待 -->
        <div v-if="callState === 'calling'" class="call-panel">
          <div class="call-avatar">{{ firstLetter(remoteUsername) }}</div>
          <div class="call-name">{{ remoteUsername }}</div>
          <div class="call-status ring-text">正在呼叫...</div>
          <div class="call-actions">
            <button class="btn-hangup" @click="endCall">
              <span class="btn-icon">📞</span> 取消
            </button>
          </div>
        </div>

        <!-- 被叫响铃 -->
        <div v-else-if="callState === 'ringing'" class="call-panel ringing">
          <div class="call-avatar ringing-avatar">{{ firstLetter(remoteUsername) }}</div>
          <div class="call-name">{{ remoteUsername }}</div>
          <div class="call-status">邀请你进行语音通话</div>
          <div class="call-actions">
            <button class="btn-accept" @click="acceptCall">
              <span class="btn-icon">📞</span> 接听
            </button>
            <button class="btn-hangup" @click="rejectCall">
              <span class="btn-icon">✕</span> 拒绝
            </button>
          </div>
        </div>

        <!-- 通话中 -->
        <div v-else-if="callState === 'connected'" class="call-panel connected">
          <div class="call-avatar connected-avatar">{{ firstLetter(remoteUsername) }}</div>
          <div class="call-name">{{ remoteUsername }}</div>
          <div class="call-status timer">{{ formatTime(elapsed) }}</div>
          <div class="call-actions">
            <button class="btn-hangup" @click="endCall">
              <span class="btn-icon">📞</span> 挂断
            </button>
          </div>
        </div>

        <!-- 通话结束 -->
        <div v-else-if="callState === 'ended'" class="call-panel ended">
          <div class="call-avatar ended-avatar">{{ firstLetter(remoteUsername) }}</div>
          <div class="call-name">{{ remoteUsername }}</div>
          <div class="call-status ended-text">{{ errorMsg || '通话已结束' }}</div>
          <div class="call-actions">
            <button class="btn-close" @click="resetCall">关闭</button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { useVoiceCallStore } from '../stores/voiceCall'
import { storeToRefs } from 'pinia'

const store = useVoiceCallStore()
const { callState, remoteUsername, errorMsg, elapsed } = storeToRefs(store)
const { acceptCall, rejectCall, endCall, resetCall, formatTime } = store

function firstLetter(name) {
  return name ? String(name).slice(0, 1).toUpperCase() : '?'
}
</script>

<style scoped>
/* Deep glass overlay — colorful background bleeds through */
.voice-call-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: saturate(200%) blur(44px);
  -webkit-backdrop-filter: saturate(200%) blur(44px);
}

/* Liquid glass panel */
.call-panel {
  position: relative;
  background: var(--glass-bg);
  backdrop-filter: var(--blur-heavy);
  -webkit-backdrop-filter: var(--blur-heavy);
  border: 1px solid var(--glass-border);
  border-radius: 36px;
  padding: 56px 64px;
  text-align: center;
  box-shadow: var(--shadow-xl);
  min-width: 360px;
  overflow: hidden;
  animation: slideUp 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* Top edge light reflection */
.call-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  border-radius: inherit;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.50) 0%,
    rgba(255, 255, 255, 0.14) 35%,
    transparent 60%
  );
}

.call-panel > * {
  position: relative;
  z-index: 1;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(48px) scale(0.92); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.call-avatar {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--brand) 0%, var(--brand-strong) 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 700;
  margin: 0 auto 20px;
  user-select: none;
  box-shadow: 0 10px 32px var(--brand-glow);
}

.ringing-avatar { animation: pulse 1.2s ease infinite; }
.connected-avatar { background: linear-gradient(135deg, var(--success) 0%, #28a745 100%); box-shadow: 0 10px 32px rgba(52, 199, 89, 0.35); }
.ended-avatar { background: linear-gradient(135deg, #8e8e93 0%, #636366 100%); box-shadow: 0 10px 32px rgba(0,0,0,0.12); }

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(7, 193, 96, 0.50); }
  50% { box-shadow: 0 0 0 26px rgba(7, 193, 96, 0); }
}

.call-name {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 10px;
  color: var(--ink);
  letter-spacing: -0.03em;
}

.call-status {
  font-size: 16px;
  color: var(--muted);
  margin-bottom: 36px;
  font-weight: 500;
}

.ring-text { animation: blink 1s ease infinite; }

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.30; }
}

.timer {
  font-size: 44px;
  font-weight: 300;
  font-variant-numeric: tabular-nums;
  color: var(--success);
  letter-spacing: 3px;
}

.ended-text { color: var(--danger); font-weight: 600; }

.call-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

button {
  border: none;
  border-radius: 999px;
  padding: 14px 40px;
  font-size: 16px;
  cursor: pointer;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all var(--transition-spring);
  font-family: inherit;
  letter-spacing: 0.01em;
}

button:active { transform: scale(0.94); }

.btn-hangup {
  background: var(--danger);
  color: #fff;
  box-shadow: 0 6px 20px rgba(255, 59, 48, 0.35);
}
.btn-hangup:hover { background: #e04040; transform: translateY(-2px); box-shadow: 0 8px 26px rgba(255, 59, 48, 0.45); }

.btn-accept {
  background: var(--success);
  color: #fff;
  box-shadow: 0 6px 20px rgba(52, 199, 89, 0.35);
}
.btn-accept:hover { background: #45d46a; transform: translateY(-2px); box-shadow: 0 8px 26px rgba(52, 199, 89, 0.45); }

.btn-close {
  background: rgba(0, 0, 0, 0.06);
  color: var(--ink);
  backdrop-filter: var(--blur-subtle);
  -webkit-backdrop-filter: var(--blur-subtle);
}
.btn-close:hover { background: rgba(0, 0, 0, 0.10); }

.btn-icon { font-size: 20px; }

.call-fade-enter-active, .call-fade-leave-active {
  transition: opacity 0.3s cubic-bezier(0.42, 0, 0.58, 1);
}
.call-fade-enter-from, .call-fade-leave-to {
  opacity: 0;
}

@media (max-width: 480px) {
  .call-panel {
    padding: 40px 28px;
    border-radius: 28px;
    min-width: auto;
    margin: 16px;
  }

  .call-avatar {
    width: 72px;
    height: 72px;
    font-size: 30px;
  }

  .call-name {
    font-size: 22px;
  }

  .timer {
    font-size: 36px;
  }

  button {
    padding: 12px 28px;
    font-size: 15px;
  }
}
</style>

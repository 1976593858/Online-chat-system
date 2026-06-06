<template>
  <Teleport to="body">
    <transition name="call-fade">
      <div v-if="callState !== 'idle'" class="voice-call-overlay">
        <!-- Calling -->
        <div v-if="callState === 'calling'" class="call-panel">
          <div class="call-avatar">{{ firstLetter(remoteUsername) }}</div>
          <div class="call-name">{{ remoteUsername }}</div>
          <div class="call-status ring-text">正在呼叫...</div>
          <div class="call-actions">
            <button class="btn-hangup" @click="endCall">
              <span>📞</span> 取消
            </button>
          </div>
        </div>

        <!-- Ringing -->
        <div v-else-if="callState === 'ringing'" class="call-panel ringing">
          <div class="call-avatar ringing-avatar">{{ firstLetter(remoteUsername) }}</div>
          <div class="call-name">{{ remoteUsername }}</div>
          <div class="call-status">邀请你进行语音通话</div>
          <div class="call-actions">
            <button class="btn-accept" @click="acceptCall">
              <span>📞</span> 接听
            </button>
            <button class="btn-hangup" @click="rejectCall">
              <span>✕</span> 拒绝
            </button>
          </div>
        </div>

        <!-- Connected -->
        <div v-else-if="callState === 'connected'" class="call-panel connected">
          <div class="call-avatar connected-avatar">{{ firstLetter(remoteUsername) }}</div>
          <div class="call-name">{{ remoteUsername }}</div>
          <div class="call-status timer">{{ formatTime(elapsed) }}</div>
          <div class="call-actions">
            <button class="btn-mute" :class="{ muted: micMuted }" @click="toggleMute">
              {{ micMuted ? '🔇' : '🎤' }}
            </button>
            <button class="btn-hangup" @click="endCall">
              <span>📞</span> 挂断
            </button>
          </div>
        </div>

        <!-- Ended -->
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
const { callState, remoteUsername, errorMsg, elapsed, micMuted } = storeToRefs(store)
const { acceptCall, rejectCall, endCall, resetCall, formatTime, toggleMute } = store

function firstLetter(name) {
  return name ? String(name).slice(0, 1).toUpperCase() : '?'
}
</script>

<style scoped>
/* ================================================
   Overlay — deep glass with aurora bleed
   ================================================ */

.voice-call-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: saturate(200%) blur(48px);
  -webkit-backdrop-filter: saturate(200%) blur(48px);
}

/* ================================================
   Panel — liquid glass card
   ================================================ */

.call-panel {
  position: relative;
  background: var(--glass-1);
  backdrop-filter: var(--blur-xl);
  -webkit-backdrop-filter: var(--blur-xl);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-xl);
  padding: 56px 64px;
  text-align: center;
  box-shadow: var(--shadow-xl);
  min-width: 360px;
  overflow: hidden;
  animation: springIn 0.5s var(--ease-spring) both;
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
    rgba(255, 255, 255, 0.45) 0%,
    rgba(255, 255, 255, 0.12) 35%,
    transparent 55%
  );
}

.call-panel > * {
  position: relative;
  z-index: 1;
}

/* ================================================
   Avatar — bold, ambient glow
   ================================================ */

.call-avatar {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--brand) 0%, var(--brand-active) 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 700;
  margin: 0 auto 20px;
  user-select: none;
  box-shadow: 0 12px 36px var(--brand-glow);
}

.ringing-avatar {
  animation: ringPulse 1.2s ease infinite;
}

.connected-avatar {
  background: linear-gradient(135deg, var(--success) 0%, #28a745 100%);
  box-shadow: 0 12px 36px rgba(46, 204, 113, 0.35);
}

.ended-avatar {
  background: linear-gradient(135deg, #8e8e93 0%, #636366 100%);
  box-shadow: 0 12px 36px rgba(0,0,0,0.1);
}

/* ================================================
   Text
   ================================================ */

.call-name {
  font-size: 26px;
  font-weight: 780;
  margin-bottom: 10px;
  color: var(--text-primary);
  letter-spacing: -0.03em;
}

.call-status {
  font-size: 16px;
  color: var(--text-tertiary);
  margin-bottom: 36px;
  font-weight: 500;
}

.ring-text { animation: blink 1s ease infinite; }

.timer {
  font-size: 44px;
  font-weight: 300;
  font-variant-numeric: tabular-nums;
  color: var(--success);
  letter-spacing: 3px;
}

.ended-text { color: var(--danger); font-weight: 600; }

/* ================================================
   Buttons — iOS style controls
   ================================================ */

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
  font-weight: 680;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all var(--duration-normal) var(--ease-spring-smooth);
  font-family: inherit;
  letter-spacing: 0.01em;
}

button:active { transform: scale(0.94); }

.btn-hangup {
  background: var(--danger);
  color: #fff;
  box-shadow: 0 6px 20px rgba(232, 64, 64, 0.35);
}
.btn-hangup:hover {
  background: #e04040;
  transform: translateY(-2px);
  box-shadow: 0 8px 26px rgba(232, 64, 64, 0.45);
}

.btn-accept {
  background: var(--success);
  color: #fff;
  box-shadow: 0 6px 20px rgba(46, 204, 113, 0.35);
}
.btn-accept:hover {
  background: #45d46a;
  transform: translateY(-2px);
  box-shadow: 0 8px 26px rgba(46, 204, 113, 0.45);
}

.btn-close {
  background: rgba(0, 0, 0, 0.06);
  color: var(--text-primary);
  backdrop-filter: var(--blur-sm);
  -webkit-backdrop-filter: var(--blur-sm);
}
.btn-close:hover { background: rgba(0, 0, 0, 0.10); }

/* Mute button — circular */
.btn-mute {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  padding: 0;
  background: rgba(0,0,0,0.04);
  color: var(--text-tertiary);
  font-size: 20px;
  backdrop-filter: var(--blur-sm);
  -webkit-backdrop-filter: var(--blur-sm);
  box-shadow: var(--shadow-xs);
  display: flex;
  align-items: center;
  justify-content: center;
}
.btn-mute:hover {
  background: rgba(0,0,0,0.08);
  color: var(--text-primary);
}
.btn-mute.muted {
  background: var(--danger-soft);
  color: var(--danger);
}

/* ================================================
   Animations
   ================================================ */

@keyframes ringPulse {
  0%, 100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(26, 173, 94, 0.5); }
  50%      { transform: scale(1.05); box-shadow: 0 0 0 22px rgba(26, 173, 94, 0); }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.30; }
}

/* Transition */
.call-fade-enter-active, .call-fade-leave-active {
  transition: opacity 0.35s var(--ease-out-expo);
}
.call-fade-enter-from, .call-fade-leave-to {
  opacity: 0;
}

/* ================================================
   Responsive
   ================================================ */

@media (max-width: 480px) {
  .call-panel {
    padding: 40px 28px;
    border-radius: var(--radius-lg);
    min-width: auto;
    margin: 16px;
  }

  .call-avatar {
    width: 72px;
    height: 72px;
    font-size: 30px;
  }

  .call-name { font-size: 22px; }
  .timer { font-size: 36px; }

  button {
    padding: 12px 28px;
    font-size: 15px;
  }
}
</style>

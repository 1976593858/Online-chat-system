<template>
  <main class="login-shell">
    <!-- Theme toggle — top right -->
    <div class="login-theme">
      <div class="theme-toggle">
        <button
          v-for="m in themeModes"
          :key="m.key"
          :class="['theme-toggle-btn', { active: theme === m.key }]"
          :title="m.label"
          @click="setTheme(m.key)"
        >{{ m.icon }}</button>
      </div>
    </div>

    <!-- Brand -->
    <div class="login-brand">
      <div class="brand-icon">◇</div>
      <div class="brand-label">Online Chat</div>
    </div>

    <!-- Floating glass card -->
    <section class="login-card glass-highlight">
      <!-- Tab pills -->
      <div class="login-tabs">
        <button
          :class="['login-tab', { active: mode === 'login' }]"
          @click="mode = 'login'"
        >登录</button>
        <button
          :class="['login-tab', { active: mode === 'register' }]"
          @click="mode = 'register'"
        >注册</button>
      </div>

      <!-- Error -->
      <div v-if="errorMsg" class="login-error">
        <span>{{ errorMsg }}</span>
        <button class="error-close" @click="errorMsg = ''">×</button>
      </div>

      <!-- Login Form -->
      <form v-if="mode === 'login'" @submit.prevent="submitLogin" class="login-form">
        <div class="field">
          <label class="field-label">用户名</label>
          <input
            v-model="loginForm.username"
            class="field-input"
            placeholder="输入用户名"
            @input="errorMsg = ''"
          />
        </div>
        <div class="field">
          <label class="field-label">密码</label>
          <input
            v-model="loginForm.password"
            class="field-input"
            type="password"
            placeholder="输入密码"
            @keyup.enter="submitLogin"
            @input="errorMsg = ''"
          />
        </div>
        <button type="submit" class="login-submit" :disabled="submitting">
          {{ submitting ? '登录中…' : '登录' }}
        </button>
      </form>

      <!-- Register Form -->
      <form v-else @submit.prevent="submitRegister" class="login-form">
        <div class="field">
          <label class="field-label">用户名</label>
          <input
            v-model="registerForm.username"
            class="field-input"
            placeholder="4-32位字母、数字、下划线"
            @input="errorMsg = ''"
          />
        </div>
        <div class="field">
          <label class="field-label">昵称</label>
          <input
            v-model="registerForm.nickname"
            class="field-input"
            placeholder="展示昵称"
            @input="errorMsg = ''"
          />
        </div>
        <div class="field">
          <label class="field-label">邮箱</label>
          <input
            v-model="registerForm.email"
            class="field-input"
            placeholder="name@example.com"
            @input="errorMsg = ''"
          />
        </div>
        <div class="field">
          <label class="field-label">手机号</label>
          <input
            v-model="registerForm.phone"
            class="field-input"
            placeholder="可选"
            @input="errorMsg = ''"
          />
        </div>
        <div class="field">
          <label class="field-label">密码</label>
          <input
            v-model="registerForm.password"
            class="field-input"
            type="password"
            placeholder="至少6位"
            @input="errorMsg = ''"
          />
        </div>
        <button type="submit" class="login-submit" :disabled="submitting">
          {{ submitting ? '注册中…' : '注册并登录' }}
        </button>
      </form>
    </section>

    <p class="login-footer muted">安全连接 · 端到端加密</p>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { useTheme } from '../composables/useTheme'

const router = useRouter()
const authStore = useAuthStore()
const { theme, setTheme, modes: themeModes } = useTheme()

const mode = ref('login')
const submitting = ref(false)
const errorMsg = ref('')

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  password: ''
})

function resolveError(err) {
  if (err?.response) {
    const status = err.response.status
    if (status === 401) return '用户名或密码错误'
    if (status === 403) return '账号已被禁用'
    if (status >= 500) return '服务器内部错误，请稍后重试'
    return err.response.data?.message || `请求失败 (${status})`
  }
  if (err?.code === 'ERR_NETWORK' || err?.message?.includes('Network Error')) {
    return '无法连接服务器，请确认后端服务已启动 (localhost:8080)'
  }
  return err?.message || '操作失败，请重试'
}

async function submitLogin() {
  errorMsg.value = ''
  if (!loginForm.username.trim() || !loginForm.password) {
    errorMsg.value = '请填写用户名和密码'
    return
  }
  submitting.value = true
  try {
    await authStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/friends')
  } catch (err) {
    errorMsg.value = resolveError(err)
  } finally {
    submitting.value = false
  }
}

async function submitRegister() {
  errorMsg.value = ''
  if (!registerForm.username.trim() || !registerForm.password || !registerForm.nickname.trim()) {
    errorMsg.value = '请填写用户名、昵称和密码'
    return
  }
  const usernameRule = /^[a-zA-Z0-9_]{4,32}$/
  if (!usernameRule.test(registerForm.username)) {
    errorMsg.value = '用户名只能包含字母、数字、下划线，长度4-32位'
    return
  }
  if (registerForm.password.length < 6) {
    errorMsg.value = '密码至少6位'
    return
  }
  if (registerForm.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) {
    errorMsg.value = '邮箱格式不正确'
    return
  }
  submitting.value = true
  try {
    await authStore.register(registerForm)
    ElMessage.success('注册成功')
    router.push('/friends')
  } catch (err) {
    errorMsg.value = resolveError(err)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* ================================================
   Login Shell — centered, immersive
   ================================================ */

.login-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

/* ================================================
   Theme Toggle — top right
   ================================================ */

.login-theme {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 100;
}

.theme-toggle {
  display: flex;
  gap: 2px;
  padding: 3px;
  background: var(--glass-2);
  backdrop-filter: var(--blur-lg);
  -webkit-backdrop-filter: var(--blur-lg);
  border: 1px solid var(--glass-border-3);
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-sm);
}

.theme-toggle-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-fast) var(--ease-out-expo);
  color: var(--text-tertiary);
}

.theme-toggle-btn:hover {
  color: var(--text-primary);
}

.theme-toggle-btn.active {
  background: var(--glass-1);
  color: var(--text-primary);
  box-shadow: var(--shadow-xs);
}

/* ================================================
   Brand
   ================================================ */

.login-brand {
  text-align: center;
  margin-bottom: 36px;
}

.brand-icon {
  font-size: 48px;
  color: var(--brand);
  margin-bottom: 12px;
  animation: springIn 0.6s var(--ease-spring) both;
  filter: drop-shadow(0 4px 12px var(--brand-glow));
}

.brand-label {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-tertiary);
  animation: springIn 0.6s 0.1s var(--ease-spring) both;
}

/* ================================================
   Login Card — floating glass
   ================================================ */

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 36px 32px 30px;
  background: var(--glass-1);
  backdrop-filter: var(--blur-xl);
  -webkit-backdrop-filter: var(--blur-xl);
  border: 1px solid var(--glass-border-2);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  animation: springIn 0.55s 0.15s var(--ease-spring-soft) both;
}

/* ================================================
   Tab Pills
   ================================================ */

.login-tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--glass-3);
  border-radius: var(--radius-full);
  margin-bottom: 28px;
}

.login-tab {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-tertiary);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-spring-smooth);
  font-family: inherit;
}

.login-tab.active {
  background: var(--glass-1);
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
}

/* ================================================
   Error
   ================================================ */

.login-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  margin-bottom: 18px;
  background: var(--danger-soft);
  border-radius: var(--radius-sm);
  color: var(--danger);
  font-size: 13px;
  font-weight: 500;
  animation: slideDown 0.25s var(--ease-out-expo) both;
}

.error-close {
  background: none;
  border: none;
  color: var(--danger);
  cursor: pointer;
  font-size: 18px;
  padding: 0 2px;
  opacity: 0.6;
}

.error-close:hover { opacity: 1; }

/* ================================================
   Form Fields
   ================================================ */

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding-left: 4px;
}

.field-input {
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--glass-border-3);
  background: var(--glass-3);
  backdrop-filter: var(--blur-md);
  -webkit-backdrop-filter: var(--blur-md);
  font-size: 15px;
  font-family: inherit;
  color: var(--text-primary);
  outline: none;
  transition: all var(--duration-fast) var(--ease-out-expo);
}

.field-input::placeholder {
  color: var(--text-tertiary);
}

.field-input:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-glow);
}

/* ================================================
   Submit Button — iOS style
   ================================================ */

.login-submit {
  margin-top: 6px;
  padding: 13px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--brand);
  color: #fff;
  font-size: 16px;
  font-weight: 680;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-spring-smooth);
  font-family: inherit;
  letter-spacing: 0.02em;
  box-shadow: 0 2px 8px var(--brand-glow);
}

.login-submit:hover:not(:disabled) {
  background: var(--brand-hover);
  box-shadow: 0 8px 24px var(--brand-glow);
  transform: translateY(-1px);
}

.login-submit:active:not(:disabled) {
  transform: scale(0.97);
}

.login-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ================================================
   Footer
   ================================================ */

.login-footer {
  margin-top: 28px;
  font-size: 11px;
  letter-spacing: 0.04em;
  animation: springIn 0.6s 0.25s var(--ease-spring) both;
}

/* ================================================
   Responsive
   ================================================ */

@media (max-width: 480px) {
  .login-shell {
    padding: 20px 16px;
  }

  .login-card {
    padding: 28px 20px 24px;
    border-radius: var(--radius-lg);
  }

  .brand-icon {
    font-size: 38px;
  }
}
</style>

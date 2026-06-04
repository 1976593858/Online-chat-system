<template>
  <main class="login-shell">
    <section class="login-hero">
      <p class="eyebrow">Online Chat System</p>
      <h1>把好友、申请和会话列表整理成可维护的工作台</h1>
      <p>登录后可搜索用户、发送好友申请、管理分组，并查看最近会话与未读数量。</p>
    </section>

    <section class="login-panel glass-card">
      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="登录" name="login">
          <el-alert v-if="loginError" :title="loginError" type="error" show-icon closable @close="loginError = ''" style="margin-bottom: 16px" />
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-position="top" @submit.prevent>
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="alice" @input="loginError = ''" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="123456" @keyup.enter="submitLogin" @input="loginError = ''" />
            </el-form-item>
            <el-button type="primary" :loading="submitting" class="full-button" @click="submitLogin">登录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-alert v-if="registerError" :title="registerError" type="error" show-icon closable @close="registerError = ''" style="margin-bottom: 16px" />
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-position="top" @submit.prevent>
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="4-32位字母、数字、下划线" @input="registerError = ''" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="展示昵称" @input="registerError = ''" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="registerForm.email" placeholder="name@example.com" @input="registerError = ''" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="可选" @input="registerError = ''" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="至少6位" @input="registerError = ''" />
            </el-form-item>
            <el-button type="primary" :loading="submitting" class="full-button" @click="submitRegister">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const mode = ref('login')
const submitting = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()
const loginError = ref('')
const registerError = ref('')

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

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,32}$/, message: '用户名只能包含字母、数字、下划线，长度4-32位', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }]
}

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
  return err?.message || '登录失败，请重试'
}

async function submitLogin() {
  loginError.value = ''
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await authStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/friends')
  } catch (err) {
    loginError.value = resolveError(err)
  } finally {
    submitting.value = false
  }
}

async function submitRegister() {
  registerError.value = ''
  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await authStore.register(registerForm)
    ElMessage.success('注册成功')
    router.push('/friends')
  } catch (err) {
    registerError.value = resolveError(err)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 56px;
  align-items: center;
  min-height: 100vh;
  padding: 48px 60px;
}

.login-hero {
  max-width: 760px;
  animation: heroFadeIn 0.8s cubic-bezier(0.22, 0.61, 0.36, 1);
}

@keyframes heroFadeIn {
  from { opacity: 0; transform: translateY(24px); }
  to   { opacity: 1; transform: translateY(0); }
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--brand);
  font-weight: 800;
  font-size: 13px;
  letter-spacing: 0.15em;
  text-transform: uppercase;
}

.eyebrow::before {
  content: "";
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--brand);
  box-shadow: 0 0 12px var(--brand-glow);
  animation: dotPulse 2s ease-in-out infinite;
}

@keyframes dotPulse {
  0%, 100% { box-shadow: 0 0 8px var(--brand-glow); }
  50% { box-shadow: 0 0 18px var(--brand-glow), 0 0 32px var(--brand-glow); }
}

.login-hero h1 {
  margin: 22px 0;
  font-size: clamp(44px, 7vw, 76px);
  font-weight: 800;
  line-height: 1.02;
  letter-spacing: -0.05em;
}

.login-hero p:last-child {
  max-width: 520px;
  color: var(--muted);
  font-size: 18px;
  line-height: 1.7;
}

/* Liquid glass login panel — low opacity, heavy blur, edge highlight */
.login-panel {
  position: relative;
  padding: 36px 32px 30px;
  border-radius: 34px;
  background: var(--glass-bg);
  backdrop-filter: var(--blur-heavy);
  -webkit-backdrop-filter: var(--blur-heavy);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-xl);
  overflow: hidden;
  animation: panelFadeIn 0.6s 0.15s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

/* Glass edge reflection */
.login-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  border-radius: inherit;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.55) 0%,
    rgba(255, 255, 255, 0.18) 35%,
    transparent 60%
  );
}

.login-panel > * {
  position: relative;
  z-index: 1;
}

@keyframes panelFadeIn {
  from { opacity: 0; transform: translateY(30px) scale(0.96); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.login-panel :deep(.el-tabs__header) {
  margin-bottom: 22px;
}

.login-panel :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 700;
  color: var(--muted);
}

.login-panel :deep(.el-tabs__item.is-active) {
  color: var(--brand);
  font-weight: 800;
}

.login-panel :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 2px;
  background: var(--brand);
}

.login-panel :deep(.el-form-item__label) {
  font-weight: 600;
  font-size: 13px;
  color: var(--ink-soft);
}

.full-button {
  width: 100%;
  margin-top: 6px;
  height: 46px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 14px;
  letter-spacing: 0.02em;
  transition: all var(--transition-spring);
}

.full-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px var(--brand-glow);
}

.full-button:active {
  transform: scale(0.97);
}

@media (max-width: 960px) {
  .login-shell {
    grid-template-columns: 1fr;
    gap: 36px;
    padding: 32px 24px;
    align-content: center;
  }

  .login-hero h1 {
    font-size: clamp(34px, 9vw, 56px);
  }
}

@media (max-width: 480px) {
  .login-shell {
    padding: 18px;
    gap: 22px;
  }

  .login-panel {
    padding: 26px 18px 20px;
    border-radius: 26px;
  }
}
</style>

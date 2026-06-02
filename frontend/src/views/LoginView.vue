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
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="alice" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="123456" />
            </el-form-item>
            <el-button type="primary" :loading="submitting" class="full-button" @click="submitLogin">登录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="4-32位字母、数字、下划线" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="展示昵称" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="registerForm.email" placeholder="name@example.com" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="可选" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="至少6位" />
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

async function submitLogin() {
  await loginFormRef.value.validate()
  submitting.value = true
  try {
    await authStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/friends')
  } finally {
    submitting.value = false
  }
}

async function submitRegister() {
  await registerFormRef.value.validate()
  submitting.value = true
  try {
    await authStore.register(registerForm)
    ElMessage.success('注册成功')
    router.push('/friends')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 40px;
  align-items: center;
  min-height: 100vh;
  padding: 48px;
}

.login-hero {
  max-width: 760px;
}

.eyebrow {
  color: var(--brand);
  font-weight: 900;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.login-hero h1 {
  margin: 16px 0;
  font-size: clamp(44px, 7vw, 86px);
  line-height: 0.95;
  letter-spacing: -0.07em;
}

.login-hero p:last-child {
  max-width: 560px;
  color: var(--muted);
  font-size: 18px;
  line-height: 1.8;
}

.login-panel {
  padding: 28px;
  border-radius: 30px;
}

.full-button {
  width: 100%;
  margin-top: 8px;
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
    padding: 24px;
  }
}
</style>

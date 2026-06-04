<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await authStore.login(username.value, password.value)
    router.push('/app')
  } catch (e: any) {
    if (e.code === 'ERR_NETWORK' || e.message?.includes('Network Error')) {
      error.value = '无法连接服务器，请确认后端已启动'
    } else if (e.response?.data?.message) {
      error.value = e.response.data.message
    } else {
      error.value = `登录失败: ${e.message || '未知错误'}`
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-bg">
      <div class="bg-shape shape-1" />
      <div class="bg-shape shape-2" />
    </div>

    <div class="auth-card">
      <div class="card-header">
        <div class="logo-mark">库</div>
        <h1>智能知识库</h1>
      </div>

      <form @submit.prevent="handleLogin">
        <div class="field">
          <input
            id="username"
            v-model="username"
            type="text"
            placeholder="用户名"
            autocomplete="username"
            required
          />
        </div>
        <div class="field">
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="密码"
            autocomplete="current-password"
            required
          />
        </div>

        <p v-if="error" class="error-msg">{{ error }}</p>

        <button type="submit" :disabled="loading" class="btn-login">
          <span v-if="loading" class="spinner" />
          <span>{{ loading ? '登录中...' : '登录' }}</span>
        </button>
      </form>

      <p class="footer-link">
        还没有账号？<router-link to="/register">注册</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  position: relative;
  overflow: hidden;
  padding: 24px;
}

/* ── Decorative shapes ── */
.auth-bg { position: absolute; inset: 0; pointer-events: none; }
.bg-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.35;
}
.shape-1 {
  width: 500px; height: 500px;
  background: var(--sage-light);
  top: -200px; right: -150px;
}
.shape-2 {
  width: 400px; height: 400px;
  background: var(--rose-light);
  bottom: -150px; left: -100px;
}

/* ── Card ── */
.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  background: var(--surface);
  border-radius: var(--radius-lg);
  padding: 48px 40px;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-light);
  animation: fadeIn 0.6s var(--ease);
}

.card-header { text-align: center; margin-bottom: 36px; }
.logo-mark {
  width: 52px; height: 52px;
  border-radius: var(--radius);
  background: linear-gradient(135deg, var(--sage), var(--dusty-blue));
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 16px;
  letter-spacing: 0;
}
.card-header h1 {
  font-size: var(--text-xl);
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--text);
}
.card-header p {
  color: var(--text-muted);
  font-size: var(--text-sm);
}

/* ── Fields ── */
.field { margin-bottom: 16px; }
.field input {
  width: 100%;
  padding: 14px 18px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius);
  font-size: var(--text-base);
  font-family: var(--font);
  background: var(--surface-alt);
  color: var(--text);
  transition: border-color 0.2s var(--ease), box-shadow 0.2s var(--ease);
  outline: none;
}
.field input:focus {
  border-color: var(--sage);
  box-shadow: 0 0 0 4px var(--sage-light);
  background: var(--surface);
}
.field input::placeholder { color: var(--text-muted); }

/* ── Error ── */
.error-msg {
  color: var(--error);
  font-size: var(--text-sm);
  margin: 8px 0;
  padding: 10px 14px;
  background: var(--rose-light);
  border-radius: var(--radius-sm);
  border: 1px solid #edd5ce;
}

/* ── Button ── */
.btn-login {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: var(--radius);
  background: linear-gradient(135deg, var(--sage), #7a8f74);
  color: #fff;
  font-size: var(--text-base);
  font-weight: 600;
  font-family: var(--font);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
  transition: transform 0.15s var(--ease), box-shadow 0.15s var(--ease);
}
.btn-login:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}
.btn-login:active:not(:disabled) { transform: translateY(0); }
.btn-login:disabled { opacity: 0.7; cursor: not-allowed; }

.spinner {
  width: 18px; height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.footer-link {
  text-align: center;
  margin-top: 24px;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
.footer-link a {
  color: var(--dusty-blue);
  font-weight: 500;
}
</style>

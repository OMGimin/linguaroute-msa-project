<template>
  <main class="callback-page">
    <p>{{ message }}</p>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const message = ref('로그인 정보를 확인하고 있습니다.')
const homeByRole = { PLATFORM_ADMIN: '/admin', COMPANY_ADMIN: '/company', EMPLOYEE: '/app' }

onMounted(async () => {
  const code = String(route.query.code || '')
  const state = String(route.query.state || '')
  if (!code || !state) {
    await router.replace('/login?reason=session-expired')
    return
  }
  try {
    const user = await auth.completeOAuthLogin(code, state)
    const requestedPath = sessionStorage.getItem('post_login_redirect')
    sessionStorage.removeItem('post_login_redirect')
    const safeRequestedPath = requestedPath?.startsWith('/') && !requestedPath.startsWith('//') ? requestedPath : null
    await router.replace(safeRequestedPath || homeByRole[user?.businessRole] || '/app')
  } catch (error) {
    message.value = '로그인에 실패했습니다. 다시 시도해 주세요.'
    window.setTimeout(() => router.replace('/login?reason=session-expired'), 1500)
  }
})
</script>

<style scoped>
.callback-page{min-height:100vh;display:grid;place-items:center;color:var(--muted);background:var(--paper);font-size:14px}
</style>

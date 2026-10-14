import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth.js'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(sessionStorage.getItem('access_token') || null)
  const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

  const isAuthenticated = computed(() => !!accessToken.value)
  const businessRole = computed(() => user.value?.businessRole ?? null)
  const isPlatformAdmin = computed(() => businessRole.value === 'PLATFORM_ADMIN')
  const isCompanyAdmin = computed(() => businessRole.value === 'COMPANY_ADMIN')
  const isEmployee = computed(() => businessRole.value === 'EMPLOYEE')
  const isActive = computed(() => user.value?.status === 'ACTIVE')

  function setToken(token) {
    accessToken.value = token
    sessionStorage.setItem('access_token', token)
  }

  function setUser(userData) {
    user.value = userData
    sessionStorage.setItem('user', JSON.stringify(userData))
  }

  async function fetchUser() {
    try {
      const res = await authApi.getMe()
      console.log('[AuthStore] /me response =', res.data)

      const userData = res?.data?.data ?? res?.data

      if (!userData || typeof userData !== 'object') {
        throw new Error('사용자 정보 형식이 올바르지 않습니다.')
      }

      setUser(userData)
      return userData
    } catch (error) {
      console.error('[AuthStore] 사용자 정보 조회 실패:', error)
      logout(false)
      throw error
    }
  }

  async function logout(redirect = true) {
    accessToken.value = null
    user.value = null
    sessionStorage.removeItem('access_token')
    sessionStorage.removeItem('user')

    if (!redirect) return

    const logoutUrl = import.meta.env.VITE_AUTH_LOGOUT_URL || 'http://localhost:9000/logout'
    try {
      await fetch(logoutUrl, {
        method: 'POST',
        credentials: 'include',
        mode: 'no-cors'
      })
    } finally {
      window.location.replace('/')
    }
  }

  function startOAuthLogin() {
    const state = crypto.randomUUID()
    const redirectUri = import.meta.env.VITE_AUTH_REDIRECT_URI || `${window.location.origin}/callback`
    const authServerBaseUrl = import.meta.env.VITE_AUTH_SERVER_BASE_URL || 'http://localhost:8080'
    sessionStorage.setItem('oauth_state', state)
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: import.meta.env.VITE_AUTH_WEB_CLIENT_ID || 'web-client',
      redirect_uri: redirectUri,
      scope: 'openid profile read write',
      state
    })
    window.location.assign(`${authServerBaseUrl}/oauth2/authorize?${params}`)
  }

  async function completeOAuthLogin(code, state) {
    const expectedState = sessionStorage.getItem('oauth_state')
    if (!expectedState || state !== expectedState) {
      throw new Error('로그인 요청 상태가 올바르지 않습니다.')
    }
    sessionStorage.removeItem('oauth_state')
    const res = await authApi.exchangeOAuthCode(code)
    const payload = res?.data?.data ?? res?.data
    const token = payload?.accessToken

    if (!token) {
      throw new Error('액세스 토큰을 받지 못했습니다.')
    }

    setToken(token)
    return fetchUser()
  }

  return {
    accessToken,
    user,
    isAuthenticated,
    businessRole,
    isPlatformAdmin,
    isCompanyAdmin,
    isEmployee,
    isActive,
    setToken,
    setUser,
    fetchUser,
    logout,
    startOAuthLogin,
    completeOAuthLogin
  }
})

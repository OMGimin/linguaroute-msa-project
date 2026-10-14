import axios from 'axios'
import { useAuthStore } from '@/store/auth.js'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      auth.logout(false)
      if (window.location.pathname !== '/login') {
        window.location.assign('/login?reason=session-expired')
      }
    }
    if (err.response?.status === 403 && err.response?.data?.code === 'USER_INACTIVE') {
      const auth = useAuthStore()
      auth.logout(false)
      window.location.assign('/login?reason=user-inactive')
    }
    return Promise.reject(err)
  }
)

export default api

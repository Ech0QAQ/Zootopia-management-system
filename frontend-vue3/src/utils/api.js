import axios from 'axios'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    // 调试信息
    console.log('API Request:', config.method?.toUpperCase(), config.url, config.data)
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  response => {
    // 调试信息
    console.log('API Response:', response.config.url, response.data)
    // 检查后端返回的 code，如果不是 200，当作错误处理
    if (response.data && typeof response.data === 'object' && 'code' in response.data) {
      if (response.data.code !== 200) {
        const error = new Error(response.data.message || '请求失败')
        error.response = {
          ...response,
          status: response.data.code >= 400 ? response.data.code : 400
        }
        console.error('API Error (code != 200):', response.data)
        return Promise.reject(error)
      }
    }
    return response
  },
  error => {
    // 调试信息
    console.error('API Error:', error.config?.url, error.response?.status, error.response?.data, error.message)
    // 处理 HTTP 错误状态码
    if (error.response?.status === 401) {
      // 如果是登录接口返回 401，不要清除 token（因为可能还没登录）
      if (!error.config?.url?.includes('/login') && !error.config?.url?.includes('/register')) {
        const userStore = useUserStore()
        userStore.clearAuth()
        router.push('/login')
      }
    }
    // 如果后端返回了错误信息，使用后端的错误信息
    if (error.response?.data?.message) {
      error.message = error.response.data.message
    } else if (error.response?.data && typeof error.response.data === 'string') {
      error.message = error.response.data
    } else if (!error.response) {
      // 网络错误或后端未启动
      error.message = '无法连接到服务器，请检查后端服务是否已启动'
    }
    return Promise.reject(error)
  }
)

export default api


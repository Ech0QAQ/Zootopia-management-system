import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/utils/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('zoo_token') || '')
  const user = ref(JSON.parse(localStorage.getItem('zoo_user') || 'null'))

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('zoo_token', newToken)
  }

  function setUser(newUser) {
    user.value = newUser
    localStorage.setItem('zoo_user', JSON.stringify(newUser))
  }

  function clearAuth() {
    token.value = ''
    user.value = null
    localStorage.removeItem('zoo_token')
    localStorage.removeItem('zoo_user')
  }

  async function login(username, password) {
    const res = await api.post('/login', { username, password })
    if (res.data.code === 200) {
      setToken(res.data.data.token)
      setUser(res.data.data)
      return true
    }
    throw new Error(res.data.message || '登录失败')
  }

  async function register(data) {
    const res = await api.post('/register', data)
    if (res.data.code === 200) {
      return true
    }
    throw new Error(res.data.message || '注册失败')
  }

  // 刷新用户信息
  async function refreshUser() {
    if (!token.value) {
      return
    }
    try {
      const role = user.value?.role
      let res
      if (role === 'worker') {
        res = await api.get('/worker/profile')
      } else if (role === 'admin') {
        // 管理员信息不需要刷新，从登录时获取
        return
      } else {
        res = await api.get('/me')
      }
      if (res.data.code === 200 && res.data.data) {
        const userData = res.data.data
        // 合并更新用户信息，保留原有字段
        const updatedUser = {
          ...user.value,
          ...userData,
          // 确保同时有驼峰和下划线格式
          employmentStatus: userData.employmentStatus || userData.employment_status,
          employment_status: userData.employmentStatus || userData.employment_status,
          liveArea: userData.liveArea || userData.live_area,
          live_area: userData.liveArea || userData.live_area,
          workArea: userData.workArea || userData.work_area,
          work_area: userData.workArea || userData.work_area
        }
        setUser(updatedUser)
      }
    } catch (error) {
      console.error('刷新用户信息失败:', error)
    }
  }

  return {
    token,
    user,
    setToken,
    setUser,
    clearAuth,
    login,
    register,
    refreshUser
  }
})


import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 用户状态管理（Pinia Store）
 *
 * 管理登录状态、用户信息、token 的存取
 * 刷新页面时从 localStorage 恢复 token 和用户信息
 */
export const useUserStore = defineStore('user', () => {
  // === 状态 ===
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  // === 计算属性 ===
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')

  // === 方法 ===
  function setLoginInfo(data) {
    token.value = data.token
    userInfo.value = data.userInfo
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    setLoginInfo,
    logout
  }
})

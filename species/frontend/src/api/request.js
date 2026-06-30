import axios from 'axios'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

/**
 * Axios 请求封装
 *
 * 统一处理：
 * 1. 请求拦截 — 自动注入 JWT token
 * 2. 响应拦截 — 统一处理错误（401 跳登录、500 弹错误提示）
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// === 请求拦截器 ===
// 每次请求前自动加上 Authorization 头
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// === 响应拦截器 ===
// 统一处理后端返回的错误
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端返回的 code 不为 200 → 业务错误
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        // token 过期或无效 → 清除登录信息，跳回登录页
        const userStore = useUserStore()
        userStore.logout()
        window.location.href = '/login'
        ElMessage.error('登录已过期，请重新登录')
      } else if (status >= 500) {
        ElMessage.error('服务器错误，请稍后重试')
      }
    } else {
      ElMessage.error('网络错误，请检查连接')
    }
    return Promise.reject(error)
  }
)

export default request

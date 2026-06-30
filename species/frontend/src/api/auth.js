import request from './request'

/**
 * 认证相关 API
 */
export const authApi = {
  /**
   * 用户登录
   * @param {string} username
   * @param {string} password
   * @returns {Promise} {code, msg, data: {token, userInfo}}
   */
  login(username, password) {
    return request.post('/auth/login', { username, password })
  },

  /**
   * 用户注册
   * @param {string} username
   * @param {string} password
   * @param {string} nickname 昵称（可选）
   */
  register(username, password, nickname) {
    return request.post('/auth/register', { username, password, nickname })
  },

  /**
   * 获取当前用户信息
   */
  getCurrentUser() {
    return request.get('/auth/me')
  }
}

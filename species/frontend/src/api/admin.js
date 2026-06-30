import request from './request'

/**
 * 管理员 API
 * 所有接口需要 admin 角色
 */
export const adminApi = {
  /**
   * 获取用户列表
   */
  getUsers() {
    return request.get('/admin/users')
  },

  /**
   * 重置用户密码
   * @param {number} userId
   * @param {string} newPassword
   */
  resetPassword(userId, newPassword) {
    return request.put(`/admin/users/${userId}/reset-password`, { newPassword })
  },

  /**
   * 启用/禁用用户
   * @param {number} userId
   * @param {number} status 1=启用 0=禁用
   */
  toggleStatus(userId, status) {
    return request.put(`/admin/users/${userId}/status`, { status })
  }
}

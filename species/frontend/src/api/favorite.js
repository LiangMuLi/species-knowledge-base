import request from './request'

/**
 * 收藏相关 API
 */
export const favoriteApi = {
  /**
   * 获取当前用户收藏的所有物种
   */
  list() {
    return request.get('/favorites')
  },

  /**
   * 获取收藏的物种 ID 集合
   */
  getIds() {
    return request.get('/favorites/ids')
  },

  /**
   * 添加收藏
   * @param {number} speciesId
   */
  add(speciesId) {
    return request.post(`/favorites/${speciesId}`)
  },

  /**
   * 取消收藏
   * @param {number} speciesId
   */
  remove(speciesId) {
    return request.delete(`/favorites/${speciesId}`)
  }
}

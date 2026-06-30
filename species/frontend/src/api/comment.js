import request from './request'

/**
 * 评论相关 API
 */
export const commentApi = {
  /**
   * 获取物种评论列表
   * @param {number} speciesId
   */
  list(speciesId) {
    return request.get(`/comments/${speciesId}`)
  },

  /**
   * 发表评论
   * @param {number} speciesId
   * @param {string} content
   * @param {number} [rating] 1-5
   */
  add(speciesId, content, rating) {
    return request.post(`/comments/${speciesId}`, { content, rating })
  },

  /**
   * 删除评论
   * @param {number} commentId
   */
  delete(commentId) {
    return request.delete(`/comments/${commentId}`)
  }
}

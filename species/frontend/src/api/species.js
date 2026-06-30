import request from './request'

/**
 * 物种相关 API
 */
export const speciesApi = {
  /**
   * 分页查询物种列表
   * @param {Object} params {page, size, keyword, categoryId, showAll}
   */
  list(params) {
    return request.get('/species', { params })
  },

  /**
   * 管理员查询全部（含草稿）
   * @param {Object} params
   */
  adminList(params) {
    return request.get('/species', { params: { ...params, showAll: true } })
  },

  /**
   * 获取物种详情
   * @param {number} id
   */
  getById(id) {
    return request.get(`/species/${id}`)
  },

  /**
   * 新增物种
   */
  create(data) {
    return request.post('/species', data)
  },

  /**
   * 更新物种
   * @param {number} id
   * @param {Object} data
   */
  update(id, data) {
    return request.put(`/species/${id}`, data)
  },

  /**
   * 删除物种
   * @param {number} id
   */
  delete(id) {
    return request.delete(`/species/${id}`)
  },

  /**
   * 批量删除物种
   * @param {number[]} ids
   */
  batchDelete(ids) {
    return request.delete('/species/batch', { data: { ids } })
  },

  /**
   * 获取分类列表
   */
  getCategories() {
    return request.get('/categories')
  },

  /**
   * 搜索建议（关键词自动补全）
   * @param {string} keyword
   */
  suggestions(keyword) {
    return request.get('/species/suggestions', { params: { keyword } })
  }
}

import request from './request'

/**
 * 爬虫相关 API
 */
export const crawlerApi = {
  /**
   * 启动爬取任务
   * @returns {Promise} { jobId, total }
   */
  start() {
    return request.post('/crawler/start')
  },

  /**
   * 查询任务状态
   * @param {string} jobId
   * @returns {Promise} { jobId, status, total, success, failed, skipped, logs }
   */
  status(jobId) {
    return request.get(`/crawler/status/${jobId}`)
  },

  /**
   * 获取种子数据统计
   * @returns {Promise}
   */
  seeds() {
    return request.get('/crawler/seeds')
  },

  /**
   * 批量删除爬虫添加的数据（保留原始 15 条）
   * @returns {Promise}
   */
  clear() {
    return request.delete('/crawler/clear')
  }
}

import request from '@/utils/request'

/**
 * 查询线程池列表
 */
export function queryThreadPoolList() {
  return request({
    url: '/query_thread_pool_list',
    method: 'get'
  })
}

/**
 * 查询线程池配置
 */
export function queryThreadPoolConfig(params) {
  return request({
    url: '/query_thread_pool_config',
    method: 'get',
    params
  })
}

/**
 * 更新线程池配置
 */
export function updateThreadPoolConfig(data) {
  return request({
    url: '/update_thread_pool_config',
    method: 'post',
    data
  })
}

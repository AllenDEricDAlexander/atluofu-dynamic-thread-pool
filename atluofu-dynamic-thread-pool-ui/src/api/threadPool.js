import request from '@/utils/request'

/**
 * 按应用名查询线程池列表（推荐，解决数据窗口期问题）
 */
export function queryThreadPoolListByApp(appName) {
  return request({
    url: '/query_thread_pool_list_by_app',
    method: 'get',
    params: { appName }
  })
}

/**
 * 查询线程池列表（兼容旧接口）
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

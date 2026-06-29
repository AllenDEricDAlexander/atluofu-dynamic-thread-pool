import request from '@/utils/request'

/**
 * 查询应用列表
 */
export function queryApps() {
  return request({
    url: '/apps',
    method: 'get'
  })
}

/**
 * 查询应用实例列表
 */
export function queryInstances(appName) {
  return request({
    url: `/apps/${appName}/instances`,
    method: 'get'
  })
}

/**
 * 查询执行器列表
 */
export function queryExecutors(appName, instanceId) {
  return request({
    url: `/apps/${appName}/instances/${instanceId}/executors`,
    method: 'get'
  })
}

/**
 * 查询执行器配置
 */
export function queryExecutor(appName, instanceId, executorName) {
  return request({
    url: `/apps/${appName}/instances/${instanceId}/executors/${executorName}`,
    method: 'get'
  })
}

/**
 * 调整传统线程池配置
 */
export function resizeExecutor(appName, instanceId, executorName, data) {
  return request({
    url: `/apps/${appName}/instances/${instanceId}/executors/${executorName}/resize`,
    method: 'post',
    data
  })
}

/**
 * 调整虚拟线程执行器并发限制
 */
export function updateVirtualLimit(appName, instanceId, executorName, data) {
  return request({
    url: `/apps/${appName}/instances/${instanceId}/executors/${executorName}/virtual-limit`,
    method: 'post',
    data
  })
}

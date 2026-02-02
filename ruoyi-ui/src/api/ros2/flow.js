import request from '@/utils/request'

// 查询机器人流程列表
export function listFlow(query) {
  return request({
    url: '/ros2/flow/list',
    method: 'get',
    params: query
  })
}

// 查询机器人流程详细
export function getFlow(flowId) {
  return request({
    url: '/ros2/flow/' + flowId,
    method: 'get'
  })
}

// 新增机器人流程
export function addFlow(data) {
  return request({
    url: '/ros2/flow',
    method: 'post',
    data: data
  })
}

// 修改机器人流程
export function updateFlow(data) {
  return request({
    url: '/ros2/flow',
    method: 'put',
    data: data
  })
}

// 删除机器人流程
export function delFlow(flowId) {
  return request({
    url: '/ros2/flow/' + flowId,
    method: 'delete'
  })
}

// 导出机器人流程
export function exportFlow(query) {
  return request({
    url: '/ros2/flow/export',
    method: 'post',
    params: query
  })
}

// 保存单条全局流程
export function saveSingletonFlow(data) {
  return request({
    url: '/ros2/flow/save',
    method: 'post',
    data: data
  })
}

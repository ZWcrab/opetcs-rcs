import request from '@/utils/request'

// 连接ROS2
export function connectRos2(rosBridgeUrl) {
  return request({
    url: '/ros2/navigation/connect',
    method: 'post',
    params: { rosBridgeUrl }
  })
}

// 断开ROS2连接
export function disconnectRos2() {
  return request({
    url: '/ros2/navigation/disconnect',
    method: 'post'
  })
}

// 获取ROS2连接状态
export function getRos2Status() {
  return request({
    url: '/ros2/navigation/status',
    method: 'get'
  })
}

// 发送导航目标
export function sendNavigationGoal(navigationGoal) {
  return request({
    url: '/ros2/navigation/sendGoal',
    method: 'post',
    data: navigationGoal
  })
}

// 取消导航目标
export function cancelNavigationGoal() {
  return request({
    url: '/ros2/navigation/cancelGoal',
    method: 'post'
  })
}
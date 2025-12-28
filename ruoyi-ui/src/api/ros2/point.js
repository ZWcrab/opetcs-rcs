import request from '@/utils/request'

// 保存导航点位
export function saveNav2Point(nav2Point) {
  return request({
    url: '/ros2/point/save',
    method: 'post',
    data: nav2Point
  })
}

// 获取所有导航点位
export function getAllNav2Points() {
  return request({
    url: '/ros2/point/list',
    method: 'get'
  })
}
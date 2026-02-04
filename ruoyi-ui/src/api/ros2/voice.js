import request from '@/utils/request'

// 获取全部语音配置
export function listVoices() {
  return request({
    url: '/ros2/voice/list',
    method: 'get'
  })
}

// 新增语音配置（保存到 MySQL，并在后端立刻向 ROS2 发送语音命令）
export function saveVoice(data) {
  return request({
    url: '/ros2/voice/save',
    method: 'post',
    data
  })
}


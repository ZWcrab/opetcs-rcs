import request from '@/utils/request'

// 生成 TTS 语音文件
export function generateTts(data) {
  return request({
    url: '/ros2/tts/generate',
    method: 'post',
    data: data
  })
}

// 播放语音
export function playVoice(data) {
  return request({
    url: '/ros2/tts/play',
    method: 'post',
    data: data
  })
}

// 查询语音列表
export function listVoice(query) {
  return request({
    url: '/ros2/tts/list',
    method: 'get',
    params: query
  })
}

// 新增语音
export function addVoice(data) {
  return request({
    url: '/ros2/tts/add',
    method: 'post',
    data: data
  })
}

// 删除语音
export function delVoice(id) {
  return request({
    url: '/ros2/tts/' + id,
    method: 'delete'
  })
}

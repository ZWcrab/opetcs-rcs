import request from '@/utils/request'

// 发送语音合成请求
export function sendTextToSpeech(textToSpeechRequest) {
  return request({
    url: '/ros2/tts/speak',
    method: 'post',
    data: textToSpeechRequest
  })
}

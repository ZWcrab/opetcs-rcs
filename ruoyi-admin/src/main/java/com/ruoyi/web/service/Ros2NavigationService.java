package com.ruoyi.web.service;

import com.ruoyi.web.model.dto.NavigationGoalDTO;
import com.ruoyi.web.model.dto.TextToSpeechDTO;

public interface Ros2NavigationService {

    /**
     * 发送导航目标到ROS2
     * @param navigationGoalDTO 导航目标
     * @return 是否发送成功
     */
    boolean sendNavigationGoal(NavigationGoalDTO navigationGoalDTO);

    /**
     * 取消当前导航目标
     * @return 是否取消成功
     */
    boolean cancelNavigationGoal();

    /**
     * 连接到ROS Bridge
     * @param rosBridgeUrl ROS Bridge地址
     * @return 是否连接成功
     */
    boolean connectToRosBridge(String rosBridgeUrl);

    /**
     * 断开与ROS Bridge的连接
     */
    void disconnectFromRosBridge();

    /**
     * 检查是否已连接到ROS Bridge
     * @return 是否已连接
     */
    boolean isConnected();

    /**
     * 发送文本转语音请求到ROS2
     * @param textToSpeechDTO 语音合成请求
     * @return 是否发送成功
     */
    boolean sendTextToSpeech(TextToSpeechDTO textToSpeechDTO);

    /**
     * 直接向 /voice_words 话题发布语音播放命令。
     * data 字段为 JSON 字符串：{"tts_text":"xxx","fileName":"voiceX"}
     *
     * @param ttsText  要合成/播放的文本
     * @param fileName 语音文件名标识
     * @return 是否发布成功
     */
    boolean publishVoiceWords(String ttsText, String fileName);
}
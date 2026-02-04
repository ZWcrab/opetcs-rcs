package com.ruoyi.web.service;

import com.ruoyi.web.model.entity.TtsVoice;

import java.util.List;

/**
 * 语音文本配置服务
 */
public interface TtsVoiceService {

    /**
     * 新增语音配置
     */
    int saveTtsVoice(TtsVoice ttsVoice);

    /**
     * 查询全部语音配置
     */
    List<TtsVoice> listAll();

    /**
     * 删除语音
     */
    int deleteTtsVoiceById(Long id);
}


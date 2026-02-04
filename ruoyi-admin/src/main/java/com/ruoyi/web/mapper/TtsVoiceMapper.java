package com.ruoyi.web.mapper;

import com.ruoyi.web.model.entity.TtsVoice;

import java.util.List;

/**
 * 语音文本配置 Mapper
 *
 * 使用 XML 映射自定义 SQL
 */
public interface TtsVoiceMapper {

    /**
     * 新增语音配置
     */
    int insertTtsVoice(TtsVoice ttsVoice);

    /**
     * 查询全部语音配置，按创建时间倒序
     */
    List<TtsVoice> selectAllTtsVoices();

    /**
     * 删除语音配置
     */
    int deleteTtsVoiceById(Long id);
}


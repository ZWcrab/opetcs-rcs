package com.ruoyi.web.service.impl;

import com.ruoyi.web.mapper.TtsVoiceMapper;
import com.ruoyi.web.model.entity.TtsVoice;
import com.ruoyi.web.service.TtsVoiceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 语音文本配置服务实现
 */
@Service
public class TtsVoiceServiceImpl implements TtsVoiceService {

    @Resource
    private TtsVoiceMapper ttsVoiceMapper;

    @Override
    public int saveTtsVoice(TtsVoice ttsVoice) {
        return ttsVoiceMapper.insertTtsVoice(ttsVoice);
    }

    @Override
    public List<TtsVoice> listAll() {
        return ttsVoiceMapper.selectAllTtsVoices();
    }

    @Override
    public int deleteTtsVoiceById(Long id) {
        return ttsVoiceMapper.deleteTtsVoiceById(id);
    }
}


package com.ruoyi.web.controller.openTCS;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.web.model.entity.TtsVoice;
import com.ruoyi.web.service.TtsVoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;

import java.util.List;

/**
 * 语音文本管理 Controller
 */
@RestController
@RequestMapping("/ros2/tts")
public class TtsVoiceController extends BaseController {

    @Autowired
    private TtsVoiceService ttsVoiceService;

    /**
     * 查询语音列表
     */
    @GetMapping("/list")
    public AjaxResult list() {
        List<TtsVoice> list = ttsVoiceService.listAll();
        return AjaxResult.success(list);
    }

    /**
     * 新增语音配置
     */
    @Log(title = "语音管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody TtsVoice ttsVoice) {
        // 保存到数据库
        return toAjax(ttsVoiceService.saveTtsVoice(ttsVoice));
    }

    /**
     * 删除语音配置
     */
    @Log(title = "语音管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(ttsVoiceService.deleteTtsVoiceById(id));
    }
}


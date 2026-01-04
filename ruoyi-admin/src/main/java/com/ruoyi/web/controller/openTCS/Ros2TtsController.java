package com.ruoyi.web.controller.openTCS;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.web.model.dto.TextToSpeechDTO;
import com.ruoyi.web.service.Ros2NavigationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Api(value = "ROS2语音合成相关接口", tags = "ROS2语音合成相关接口")
@RequestMapping("/ros2/tts")
public class Ros2TtsController extends BaseController {

    @Resource
    private Ros2NavigationService ros2NavigationService;

    @ApiOperation("发送语音合成请求")
    @PostMapping("/speak")
    public AjaxResult sendTextToSpeech(@RequestBody TextToSpeechDTO textToSpeechDTO) {
        try {
            boolean result = ros2NavigationService.sendTextToSpeech(textToSpeechDTO);
            if (result) {
                return AjaxResult.success("语音合成请求发送成功");
            } else {
                return AjaxResult.error("语音合成请求发送失败，请检查ROS2连接");
            }
        } catch (Exception e) {
            return AjaxResult.error("发送语音合成请求时发生异常: " + e.getMessage());
        }
    }
}

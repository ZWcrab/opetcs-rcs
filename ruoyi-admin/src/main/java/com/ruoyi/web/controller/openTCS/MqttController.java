package com.ruoyi.web.controller.openTCS;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.web.service.MqttService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * MQTT测试控制器
 * 用于发送AGV控制指令
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/mqtt")
public class MqttController {

    @Autowired
    private MqttService mqttService;

    /**
     * 发送AGV控制指令
     * 
     * @param vehicleId 车辆ID
     * @param command 指令类型：1-前进，2-后退，3-左转，4-右转，5-停止，6-重置
     * @return 结果
     */
    @PostMapping("/control")
    public AjaxResult control(@RequestParam String vehicleId, @RequestParam int command) {
        try {
            // 根据指令类型发送不同的MQTT消息
            switch (command) {
                case 1:
                    mqttService.sendForwardCommand(vehicleId);
                    return AjaxResult.success("前进指令已发送");
                case 2:
                    mqttService.sendBackwardCommand(vehicleId);
                    return AjaxResult.success("后退指令已发送");
                case 3:
                    mqttService.sendTurnLeftCommand(vehicleId);
                    return AjaxResult.success("左转指令已发送");
                case 4:
                    mqttService.sendTurnRightCommand(vehicleId);
                    return AjaxResult.success("右转指令已发送");
                case 5:
                    mqttService.sendStopCommand(vehicleId);
                    return AjaxResult.success("停止指令已发送");
                case 6:
                    mqttService.sendResetCommand(vehicleId);
                    return AjaxResult.success("重置指令已发送");
                default:
                    return AjaxResult.error("无效的指令类型，请使用1-6之间的数字");
            }
        } catch (Exception e) {
            return AjaxResult.error("发送失败：" + e.getMessage());
        }
    }

    /**
     * 测试MQTT连接
     * 
     * @return 结果
     */
    @GetMapping("/test")
    public AjaxResult test() {
        return AjaxResult.success("MQTT测试接口可用");
    }
}

package com.ruoyi.web.service;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * MQTT服务类
 * 
 * @author ruoyi
 */
@Service
public class MqttService {

    @Resource(name = "mqttOutputChannel")
    private MessageChannel mqttOutputChannel;

    /**
     * 发送MQTT消息
     * 
     * @param topic   主题
     * @param payload 消息内容
     */
    public void sendMessage(String topic, String payload) {
        boolean sent = mqttOutputChannel.send(MessageBuilder.withPayload(payload)
                .setHeader("mqtt_topic", topic)
                .setHeader("mqtt_qos", 1)
                .build());
        if (!sent) {
            throw new RuntimeException("Failed to send MQTT message: " + payload);
        }
    }

    /**
     * 发送AGV前进指令
     * 
     * @param vehicleId 车辆ID
     */
    public void sendForwardCommand(String vehicleId) {
        String payload = "{\"vehicleId\":\"" + vehicleId + "\",\"command\":\"forward\"}";
        sendMessage("vehicle/control", payload);
    }

    /**
     * 发送AGV后退指令
     * 
     * @param vehicleId 车辆ID
     */
    public void sendBackwardCommand(String vehicleId) {
        String payload = "{\"vehicleId\":\"" + vehicleId + "\",\"command\":\"backward\"}";
        sendMessage("vehicle/control", payload);
    }

    /**
     * 发送AGV左转指令
     * 
     * @param vehicleId 车辆ID
     */
    public void sendTurnLeftCommand(String vehicleId) {
        String payload = "{\"vehicleId\":\"" + vehicleId + "\",\"command\":\"left\"}";
        sendMessage("vehicle/control", payload);
    }

    /**
     * 发送AGV右转指令
     * 
     * @param vehicleId 车辆ID
     */
    public void sendTurnRightCommand(String vehicleId) {
        String payload = "{\"vehicleId\":\"" + vehicleId + "\",\"command\":\"right\"}";
        sendMessage("vehicle/control", payload);
    }

    /**
     * 发送AGV停止指令
     * 
     * @param vehicleId 车辆ID
     */
    public void sendStopCommand(String vehicleId) {
        String payload = "{\"vehicleId\":\"" + vehicleId + "\",\"command\":\"stop\"}";
        sendMessage("vehicle/control", payload);
    }

    /**
     * 发送AGV重置指令
     * 
     * @param vehicleId 车辆ID
     */
    public void sendResetCommand(String vehicleId) {
        String payload = "{\"vehicleId\":\"" + vehicleId + "\",\"command\":\"reset\"}";
        sendMessage("vehicle/control", payload);
    }
}

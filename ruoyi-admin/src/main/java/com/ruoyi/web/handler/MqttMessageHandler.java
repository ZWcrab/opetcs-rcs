package com.ruoyi.web.handler;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * MQTT消息处理器
 * 
 * @author ruoyi
 */
@Component
public class MqttMessageHandler {

    /**
     * 处理MQTT消息
     * 
     * @param message MQTT消息
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        String payload = message.getPayload().toString();
        String topic = message.getHeaders().get("mqtt_receivedTopic").toString();
        int qos = (int) message.getHeaders().get("mqtt_receivedQos");

        System.out.println("Received MQTT message:");
        System.out.println("  Topic: " + topic);
        System.out.println("  QoS: " + qos);
        System.out.println("  Payload: " + payload);

        // 这里可以添加消息处理逻辑，例如解析JSON格式的消息
        // 根据消息内容调用相应的服务方法
        // 示例：如果是车辆状态更新消息，可以更新数据库中的车辆状态
    }
}

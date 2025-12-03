package com.ruoyi.web.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * MQTT配置类
 * 
 * @author ruoyi
 */
@Configuration
public class MqttConfig {

    @Value("${spring.mqtt.broker}")
    private String broker;

    @Value("${spring.mqtt.client-id}")
    private String clientId;

    @Value("${spring.mqtt.username}")
    private String username;

    @Value("${spring.mqtt.password}")
    private String password;

    @Value("${spring.mqtt.topic.vehicle-control}")
    private String vehicleControlTopic;

    @Value("${spring.mqtt.topic.vehicle-status}")
    private String vehicleStatusTopic;

    @Value("${spring.mqtt.timeout}")
    private int timeout;

    @Value("${spring.mqtt.keep-alive-interval}")
    private int keepAliveInterval;

    @Value("${spring.mqtt.automatic-reconnect}")
    private boolean automaticReconnect;

    /**
     * MQTT客户端工厂
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { broker });
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setConnectionTimeout(timeout);
        options.setKeepAliveInterval(keepAliveInterval);
        options.setAutomaticReconnect(automaticReconnect);
        options.setCleanSession(true);
        factory.setConnectionOptions(options);
        return factory;
    }

    /**
     * MQTT消息输入通道
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息处理器
     */
    @Bean
    public MessageHandler mqttInboundHandler() {
        return message -> {
            String payload = message.getPayload().toString();
            String topic = message.getHeaders().get("mqtt_receivedTopic").toString();
            System.out.println("Received MQTT message: topic=" + topic + ", payload=" + payload);
            // 这里可以添加消息处理逻辑，例如解析MQTT消息并调用相应的服务
        };
    }

    /**
     * MQTT消息驱动通道适配器
     */
    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "-inbound", mqttClientFactory(), vehicleControlTopic, vehicleStatusTopic);
        adapter.setCompletionTimeout(timeout);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    /**
     * MQTT消息输出通道
     */
    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息处理器
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler mqttOutboundHandler() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(
                clientId + "-outbound", mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(vehicleControlTopic);
        messageHandler.setDefaultQos(1);
        return messageHandler;
    }
}

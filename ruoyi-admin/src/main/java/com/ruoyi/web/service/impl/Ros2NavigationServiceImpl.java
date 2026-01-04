package com.ruoyi.web.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.web.model.dto.NavigationGoalDTO;
import com.ruoyi.web.model.dto.TextToSpeechDTO;
import com.ruoyi.web.service.Ros2NavigationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class Ros2NavigationServiceImpl implements Ros2NavigationService {

    private static final Logger logger = LoggerFactory.getLogger(Ros2NavigationServiceImpl.class);

    private RosBridgeClientEndpoint clientEndpoint;
    private boolean connected = false;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String currentGoalId;

    @ClientEndpoint
    public static class RosBridgeClientEndpoint {
        private Session session;
        private Ros2NavigationServiceImpl parentService;
        private CountDownLatch connectionLatch;

        public RosBridgeClientEndpoint(Ros2NavigationServiceImpl parentService, CountDownLatch connectionLatch) {
            this.parentService = parentService;
            this.connectionLatch = connectionLatch;
        }

        @OnOpen
        public void onOpen(Session session) {
            this.session = session;
            parentService.onWebSocketOpen(session);
            connectionLatch.countDown();
        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            parentService.onWebSocketClose(session, closeReason);
        }

        @OnMessage
        public void onMessage(String message) {
            parentService.onWebSocketMessage(message);
        }

        @OnError
        public void onError(Session session, Throwable error) {
            parentService.onWebSocketError(session, error);
            connectionLatch.countDown();
        }

        public void sendMessage(String message) throws IOException {
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
        }

        public boolean isOpen() {
            return session != null && session.isOpen();
        }

        public void close() throws IOException {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public void onWebSocketOpen(Session session) {
        logger.info("ROS Bridge连接成功，会话ID: {}", session.getId());
        logger.info("ROS Bridge连接成功，最大文本消息大小: {}", session.getMaxTextMessageBufferSize());
        connected = true;
    }

    public void onWebSocketClose(Session session, CloseReason closeReason) {
        logger.warn("ROS Bridge连接已关闭: {}", closeReason);
        connected = false;
    }

    public void onWebSocketMessage(String message) {
        logger.debug("收到ROS Bridge消息: {}", message);
    }

    public void onWebSocketError(Session session, Throwable error) {
        logger.error("ROS Bridge连接错误", error);
        connected = false;
    }

    @Override
    public boolean sendNavigationGoal(NavigationGoalDTO navigationGoalDTO) {
        if (!connected || clientEndpoint == null || !clientEndpoint.isOpen()) {
            logger.error("ROS Bridge未连接，无法发送导航目标");
            return false;
        }

        try {
            // 生成唯一的目标ID
            currentGoalId = "goal_" + System.currentTimeMillis();

            // 调用欧拉角转四元数方法，如果提供了欧拉角则更新四元数
            navigationGoalDTO.eulerToQuaternion();
            
            logger.info("欧拉角转四元数结果: orientationX={}, orientationY={}, orientationZ={}, orientationW={}", 
                navigationGoalDTO.getOrientationX(), 
                navigationGoalDTO.getOrientationY(), 
                navigationGoalDTO.getOrientationZ(), 
                navigationGoalDTO.getOrientationW());

            // 坐标转换：根据实际需求调整坐标系转换逻辑
            // 这里假设需要交换X和Y坐标，并调整方向
            double x = navigationGoalDTO.getX();
            double y = navigationGoalDTO.getY();
            double z = navigationGoalDTO.getZ();
            
            // 示例转换：根据实际情况调整转换逻辑
            // 1. 保持原坐标不变
            double convertedX = x;
            double convertedY = y;
            // 2. 或者交换X和Y坐标
            // double convertedX = y;
            // double convertedY = x;
            // 3. 或者调整方向
            // double convertedX = -x;
            // double convertedY = -y;
            // 4. 或者缩放坐标
            // double convertedX = x * 0.1;
            // double convertedY = y * 0.1;
            logger.info("坐标转换后: X={}, Y={}, Z={}", convertedX, convertedY, z);

            // 构建导航目标消息，使用正确的send_action_goal格式
            // 确保JSON格式正确，使用双引号，字段名使用goal而不是args
            String message = String.format(
                    "{\"op\":\"send_action_goal\",\"action\":\"/move_to_coordinate\",\"action_type\":\"interfaces/action/MoveToCoordinate\",\"goal_id\":\"%s\",\"goal\":{\"x\":%f,\"y\":%f,\"z\":%f,\"heading\":\"'x'\"}}",
                    currentGoalId,
                    convertedX,
                    convertedY,
                    z
            );

            logger.info("发送导航目标: {}", message);
            clientEndpoint.sendMessage(message);
            return true;
        } catch (IOException e) {
            logger.error("发送导航目标失败", e);
            return false;
        }
    }

    @Override
    public boolean cancelNavigationGoal() {
        if (!connected || clientEndpoint == null || !clientEndpoint.isOpen()) {
            logger.error("ROS Bridge未连接，无法取消导航目标");
            return false;
        }

        try {
            // 构建取消导航目标消息，使用新的action名称
            String message = String.format(
                "{\"op\":\"cancel_action_goal\",\"action\":\"/move_to_coordinate\",\"goal_id\":\"%s\"}",
                currentGoalId != null ? currentGoalId : ""
            );

            logger.info("取消导航目标: {}", message);
            clientEndpoint.sendMessage(message);
            return true;
        } catch (IOException e) {
            logger.error("取消导航目标失败", e);
            return false;
        }
    }

    @Override
    public boolean connectToRosBridge(String rosBridgeUrl) {
        if (connected) {
            logger.info("ROS Bridge已连接");
            return true;
        }

        try {
            // 确保URL格式正确
            String finalRosBridgeUrl = rosBridgeUrl;
            if (!rosBridgeUrl.startsWith("ws://") && !rosBridgeUrl.startsWith("wss://")) {
                finalRosBridgeUrl = "ws://" + rosBridgeUrl;
            }

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            CountDownLatch connectionLatch = new CountDownLatch(1);

            // 创建并连接客户端端点
            clientEndpoint = new RosBridgeClientEndpoint(this, connectionLatch);
            container.connectToServer(clientEndpoint, URI.create(finalRosBridgeUrl));

            // 等待连接完成，最多等待5秒
            boolean result = connectionLatch.await(5, TimeUnit.SECONDS);
            if (result) {
                logger.info("ROS Bridge连接状态: {}", connected);
            } else {
                logger.error("ROS Bridge连接超时");
            }

            return result && connected;
        } catch (Exception e) {
            logger.error("连接ROS Bridge失败", e);
            return false;
        }
    }

    @Override
    public void disconnectFromRosBridge() {
        if (clientEndpoint != null) {
            try {
                clientEndpoint.close();
                logger.info("ROS Bridge连接已断开");
            } catch (IOException e) {
                logger.error("断开ROS Bridge连接失败", e);
            } finally {
                clientEndpoint = null;
                connected = false;
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connected && clientEndpoint != null && clientEndpoint.isOpen();
    }

    @Override
    public boolean sendTextToSpeech(TextToSpeechDTO textToSpeechDTO) {
        if (!connected || clientEndpoint == null || !clientEndpoint.isOpen()) {
            logger.error("ROS Bridge未连接，无法发送文本转语音请求");
            return false;
        }

        try {
            // 构建文本转语音服务调用消息，使用rosbridge服务调用格式
            String message = String.format(
                "{\"op\":\"call_service\",\"service\":\"/text_to_speech\",\"service_type\":\"interfaces/srv/TextToSpeech\",\"args\":{\"text\":\"%s\",\"language\":\"%s\",\"speed\":%d,\"pitch\":%d,\"volume\":%d}}",
                textToSpeechDTO.getText(),
                textToSpeechDTO.getLanguage(),
                textToSpeechDTO.getSpeed(),
                textToSpeechDTO.getPitch(),
                textToSpeechDTO.getVolume()
            );

            logger.info("发送文本转语音请求: {}", message);
            clientEndpoint.sendMessage(message);
            return true;
        } catch (IOException e) {
            logger.error("发送文本转语音请求失败", e);
            return false;
        }
    }
}
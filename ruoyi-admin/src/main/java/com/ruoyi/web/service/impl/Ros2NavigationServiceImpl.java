package com.ruoyi.web.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.web.model.dto.NavigationGoalDTO;
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
        logger.info("ROS Bridge连接成功");
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

            // 构建导航目标消息，使用rosbridge action goal格式
            String message = String.format(
                "{\"op\":\"send_action_goal\",\"action\":\"/navigate_to_pose\",\"action_type\":\"nav2_msgs/action/NavigateToPose\",\"goal_id\":\"%s\",\"goal\":{\"pose\":{\"header\":{\"stamp\":{\"sec\":0,\"nanosec\":0},\"frame_id\":\"map\"},\"pose\":{\"position\":{\"x\":%f,\"y\":%f,\"z\":%f},\"orientation\":{\"x\":%f,\"y\":%f,\"z\":%f,\"w\":%f}},\"behavior_tree\":\"%s\"}}}",
                currentGoalId,
                navigationGoalDTO.getX(),
                navigationGoalDTO.getY(),
                navigationGoalDTO.getZ(),
                navigationGoalDTO.getOrientationX(),
                navigationGoalDTO.getOrientationY(),
                navigationGoalDTO.getOrientationZ(),
                navigationGoalDTO.getOrientationW(),
                navigationGoalDTO.getBehaviorTree()
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
            // 构建取消导航目标消息
            String message = String.format(
                "{\"op\":\"cancel_action_goal\",\"action\":\"/navigate_to_pose\",\"goal_id\":\"%s\"}",
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
}
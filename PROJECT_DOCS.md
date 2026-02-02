# OpenTCS-RCS 项目文档

## 1. 项目简介

**OpenTCS-RCS** 是一个基于 **RuoYi** 框架开发的机器人调度控制系统 (Robot Control System)。该项目集成了 **openTCS** (开放式运输控制系统) 的核心调度能力，并结合 **MQTT** 和 **ROS2** 技术，实现了对自动导引车 (AGV) 和移动机器人的统一管理、调度与控制。

本系统旨在提供一个现代化的 Web 界面来监控车辆状态、下发运输订单、管理地图以及进行直接的远程控制。

## 2. 系统架构

项目采用前后端分离架构：

- **后端**: Spring Boot (RuoYi-Vue-Admin)
  - 集成 openTCS Kernel Service Portal (用于高层调度)
  - 集成 MQTT Client (用于低延迟指令控制)
  - 集成 Rosbridge Client (WebSocket) (用于 ROS2 导航与交互)
- **前端**: Vue3 + Element Plus (RuoYi-UI)
  - 提供可视化监控大屏
  - 订单与车辆管理界面
  - 地图展示与编辑
- **中间件**:
  - **MySQL**: 持久化存储用户、权限、历史订单数据。
  - **Redis**: 缓存与会话管理。
  - **MQTT Broker (Mosquitto)**: 消息代理，用于车辆实时通信。
  - **Rosbridge Server**: 连接 Web/Java 端与 ROS2 环境。

## 3. 核心功能模块

### 3.1 车辆管理 (Vehicle Management)
- **实时监控**: 查看车辆位置、状态 (空闲/忙碌/充电)、电池电量等。
- **远程控制**: 
  - **初始化**: 设置车辆初始位置。
  - **暂停/恢复**: 紧急暂停或恢复车辆运行。
  - **直接控制**: 通过 MQTT 发送 前进、后退、转向、停止、重置 等指令。
- **接口**: `VehicleController`, `VehiclesService`

### 3.2 订单调度 (Order Scheduling)
- **订单创建**: 指定源点和终点，创建运输任务。
- **自动分配**: 基于 openTCS 算法将订单分配给最合适的车辆。
- **订单管理**: 查询订单状态、撤回正在执行的订单。
- **接口**: `OrderController`, `OrderService`

### 3.3 地图与导航 (Map & Navigation)
- **地图同步**: 从 openTCS 获取 Plant Model (点位 Point, 路径 Path, 位置 Location)。
- **ROS2 导航**:
  - 发送导航目标点 (Goal) 到 ROS2 Navigation Stack。
  - 取消当前导航目标。
- **导航点管理**: 维护常用的导航目标点位。
- **接口**: `MapController`, `Nav2PointController`, `Ros2NavigationController`

### 3.4 语音交互 (Text to Speech)
- 支持通过 ROS2 接口发送 TTS 指令，让机器人进行语音播报。
- **接口**: `Ros2TtsController`

## 4. 接口与通信协议

### 4.1 HTTP API
主要业务接口位于 `com.ruoyi.web.controller.openTCS` 包下：
- `/vehicle/*`: 车辆管理
- `/order/*`: 订单管理
- `/map/*`: 地图数据
- `/ros2/navigation/*`: ROS2 导航控制
- `/mqtt/control`: 发送 MQTT 控制指令

### 4.2 MQTT 通信
系统使用 MQTT 进行低级指令传输。
- **Topic**: `vehicle/control`
- **Payload 格式**: JSON
  ```json
  {
    "vehicleId": "Vehicle-001",
    "command": "forward"  // 可选值: forward, backward, left, right, stop, reset
  }
  ```

### 4.3 ROS2 交互
通过 WebSocket 连接 Rosbridge。
- **服务调用**: `/text_to_speech` (语音合成)
- **导航**: 标准 ROS2 Navigation Action/Topic 接口。

## 5. 目录结构说明

```
opentcs-rcs/
├── bin/                 # 启动脚本
├── mosquitto/           # MQTT Broker 相关文件
├── ruoyi-admin/         # [核心] 后端服务模块
│   └── src/main/java/com/ruoyi/web/controller/openTCS  # 业务控制器
│   └── src/main/java/com/ruoyi/web/service             # 业务逻辑实现
├── ruoyi-common/        # 通用工具模块
├── ruoyi-framework/     # 框架核心配置
├── ruoyi-system/        # 系统实体与持久层
├── ruoyi-ui/            # [核心] 前端 Vue3 项目
│   ├── src/api/ros2     # ROS2 相关前端 API
│   ├── src/api/scheduler# 调度相关前端 API
│   └── src/views/robot  # 机器人监控页面
└── pom.xml              # Maven 父工程配置
```

## 6. 快速开始

### 环境准备
1. **JDK 1.8+**
2. **MySQL 5.7+** (导入 RuoYi 数据库脚本)
3. **Redis**
4. **Node.js 14+** (前端构建)
5. **Mosquitto** (如需测试 MQTT 功能)
6. **openTCS Kernel** (如需完整调度功能，需运行 openTCS 内核)

### 后端启动
1. 修改 `ruoyi-admin/src/main/resources/application-druid.yml` 配置数据库连接。
2. 修改 `application.yml` 配置 Redis 和 MQTT 连接。
3. 运行 `RuoYiApplication` 启动类。

### 前端启动
```bash
cd ruoyi-ui
npm install
npm run dev
```
访问 `http://localhost:80` (默认) 即可进入系统。

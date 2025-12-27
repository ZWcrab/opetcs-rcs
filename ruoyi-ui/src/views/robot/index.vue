<template>
    <div class="robot-simulation">
      <div id="container"></div>
      <div class="controls" :class="{ collapsed: !panelExpanded }">
        <div class="panel-header">
          <h3>AGV机器人仿真</h3>
          <button class="toggle-button" @click="togglePanel">
            <span v-if="panelExpanded">▼</span>
            <span v-else>▶</span>
          </button>
        </div>

        <div class="panel-content" v-if="panelExpanded">
          <div class="control-group">
            <div class="mqtt-status" :class="{ connected: mqttConnected }">
              <span class="status-indicator"></span>
              <span>MQTT {{ mqttConnected ? '已连接' : '未连接' }}</span>
            </div>
          </div>
          <div class="control-group">
            <label>速度：</label>
            <input type="range" min="0" max="1" step="0.01" v-model.number="speed" />
            <span>{{ Number(speed).toFixed(2) }}</span>
          </div>
          <div class="control-group">
            <div class="button-group">
              <button @click="moveForward">前进</button>
              <button @click="moveBackward">后退</button>
              <button @click="turnLeft">左转</button>
              <button @click="turnRight">右转</button>
              <button @click="resetPosition">重置</button>
              <button @click="loadPoints" :disabled="loadingMap" style="background-color: #4CAF50;">
                {{ loadingMap ? '加载中...' : '加载地图' }}
              </button>
            </div>
          </div>
          <div class="control-group">
            <h4>MQTT测试</h4>
            <div class="button-group">
              <button @click="testMqttForward">前进</button>
              <button @click="testMqttBackward">后退</button>
              <button @click="testMqttLeft">左转</button>
              <button @click="testMqttRight">右转</button>
              <button @click="testMqttStop">停止</button>
              <button @click="testMqttReset">重置</button>
            </div>
            <button @click="clearMqttMessages" style="background-color: #666;">清空日志</button>
          </div>
          <div class="control-group mqtt-logs">
            <h4>MQTT指令日志</h4>
            <div class="logs-container">
              <div v-for="(msg, index) in mqttMessages" :key="index" class="log-item">
                <div class="log-time">{{ msg.time }}</div>
                <div class="log-content">{{ msg.content }}</div>
              </div>
              <div v-if="mqttMessages.length === 0" class="log-empty">暂无指令</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </template>

<script>
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { AnimationMixer } from 'three';
import mqtt from 'mqtt';
import { getPoints, getLocations } from "@/api/scheduler/map";
import { listVehicles } from "@/api/scheduler/vehicles";

export default {
  name: 'RobotSimulation',
  data() {
    return {
      scene: null,
      camera: null,
      renderer: null,
      controls: null,
      mixer: null,
      clock: null,
      animationId: null,
      speed: 1,
      agvModel: null,
      // 移动状态管理
      isMoving: false,
      moveDirection: null, // forward, backward, left, right
      turnAngle: 0,
      targetRotation: 0,
      isTurning: false,
      turnFinishDirection: null, // 转向完成后要移动的方向
      // 目标位置管理
      targetPosition: null,
      isMovingToTarget: false,
      // 边界设置
      boundary: 32, // 边界大小，AGV不能超出±32的范围
      // MQTT相关
      mqttClient: null,
      mqttConnected: false,
      mqttTopic: 'vehicle/control',
      // MQTT指令日志
      mqttMessages: [],
      // 面板状态
      panelExpanded: true,
      // 加载状态
      loadingMap: false,
      // 点位数据
      points: [],
      pointObjects: [],
      // 车辆数据
      vehicles: [],
      vehicleObjects: []
    };
  },
  mounted() {
    this.init();
    this.initMqtt();
    this.loadPoints();
    this.loadAGVModel();
  },
  beforeDestroy() {
    this.dispose();
    this.disconnectMqtt();
  },
  methods: {
    init() {
      this.clock = new THREE.Clock();
      this.initScene();
      this.initCamera();
      this.initRenderer();
      this.initControls();
      this.initLights();
      this.initFloor();
      this.animate();

      window.addEventListener('resize', this.onWindowResize);

      // 初始化时将车辆重置到对应的point点位
      this.$nextTick(async () => {
        await this.resetPosition();
      });
    },

    // 加载点位数据
    async loadPoints() {
      try {
        this.loadingMap = true;
        const response = await getPoints();
        this.points = response.data || response.rows || [];
        this.createPoints();
        // 重新加载车辆，确保车辆在新的点位上
        this.createVehicles();
      } catch (error) {
        console.error('加载点位数据失败:', error);
      } finally {
        this.loadingMap = false;
      }
    },

    // 加载车辆数据
    async loadVehicles() {
      try {
        const response = await listVehicles();
        this.vehicles = response.rows || [];
        this.createVehicles();
      } catch (error) {
        console.error('加载车辆数据失败:', error);
      }
    },

    // 创建车辆
    createVehicles() {
      // 清除现有车辆
      this.vehicleObjects.forEach(obj => {
        if (this.scene) {
          this.scene.remove(obj);
        }
      });
      this.vehicleObjects = [];

      if (this.vehicles.length === 0 || this.points.length === 0) {
        return;
      }

      // 计算点位数据的范围，用于归一化车辆位置
      let minX = Infinity, maxX = -Infinity;
      let minY = Infinity, maxY = -Infinity;

      this.points.forEach(point => {
        const x = point.pose && point.pose.position ? point.pose.position.x : point.x || 0;
        const y = point.pose && point.pose.position ? point.pose.position.y : point.y || 0;

        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
      });

      // 归一化车辆位置到80x80的地板范围内
      const floorSize = 80;
      const padding = 2;
      const availableSize = floorSize - padding * 2;

      const scaleX = availableSize / (maxX - minX || 1);
      const scaleY = availableSize / (maxY - minY || 1);
      const scaleFactor = Math.min(scaleX, scaleY);

      const offsetX = -((minX + maxX) / 2) * scaleFactor;
      const offsetY = -((minY + maxY) / 2) * scaleFactor;

      // 创建车辆
      this.vehicles.forEach(vehicle => {
        // 找到车辆对应的point
        const currentPoint = this.points.find(point => point.name === vehicle.currentPosition);
        if (currentPoint) {
          const rawX = (currentPoint.pose && currentPoint.pose.position ? currentPoint.pose.position.x : currentPoint.x || 0);
          const rawZ = (currentPoint.pose && currentPoint.pose.position ? currentPoint.pose.position.y : currentPoint.y || 0);

          // 归一化车辆位置
          const x = rawX * scaleFactor + offsetX;
          const z = rawZ * scaleFactor + offsetY;

          // 创建车辆模型
          const vehicleGroup = this.createVehicleModel();
          vehicleGroup.position.set(x, 0.5, z);

          // 设置车辆模型的渲染顺序，确保它显示在前面
          vehicleGroup.traverse((obj) => {
            if (obj.isMesh) {
              obj.renderOrder = 1;
            }
          });

          // 添加车辆标签
          const label = this.createLabel(vehicle.name, x, 2, z);

          this.scene.add(vehicleGroup);
          this.scene.add(label);

          this.vehicleObjects.push(vehicleGroup);
          this.vehicleObjects.push(label);
        }
      });
    },

    // 创建车辆模型
    createVehicleModel() {
      // 创建AGV主体
      const agvGroup = new THREE.Group();

      // AGV底盘
      const chassisGeometry = new THREE.BoxGeometry(1.5, 0.3, 2.5);
      const chassisMaterial = new THREE.MeshStandardMaterial({ color: 0x0066cc });
      const chassis = new THREE.Mesh(chassisGeometry, chassisMaterial);
      chassis.castShadow = true;
      chassis.receiveShadow = true;
      agvGroup.add(chassis);

      // AGV顶部
      const topGeometry = new THREE.BoxGeometry(1.3, 0.2, 2.3);
      const topMaterial = new THREE.MeshStandardMaterial({ color: 0x0088ff });
      const top = new THREE.Mesh(topGeometry, topMaterial);
      top.position.y = 0.25;
      top.castShadow = true;
      top.receiveShadow = true;
      agvGroup.add(top);

      // 车轮 - 前左
      const wheelGeometry = new THREE.CylinderGeometry(0.2, 0.2, 0.15, 16);
      const wheelMaterial = new THREE.MeshStandardMaterial({ color: 0x333333 });

      const frontLeftWheel = new THREE.Mesh(wheelGeometry, wheelMaterial);
      frontLeftWheel.rotation.z = Math.PI / 2;
      frontLeftWheel.position.set(-0.8, 0.15, 1.0);
      frontLeftWheel.castShadow = true;
      agvGroup.add(frontLeftWheel);

      // 车轮 - 前右
      const frontRightWheel = new THREE.Mesh(wheelGeometry, wheelMaterial);
      frontRightWheel.rotation.z = Math.PI / 2;
      frontRightWheel.position.set(0.8, 0.15, 1.0);
      frontRightWheel.castShadow = true;
      agvGroup.add(frontRightWheel);

      // 车轮 - 后左
      const rearLeftWheel = new THREE.Mesh(wheelGeometry, wheelMaterial);
      rearLeftWheel.rotation.z = Math.PI / 2;
      rearLeftWheel.position.set(-0.8, 0.15, -1.0);
      rearLeftWheel.castShadow = true;
      agvGroup.add(rearLeftWheel);

      // 车轮 - 后右
      const rearRightWheel = new THREE.Mesh(wheelGeometry, wheelMaterial);
      rearRightWheel.rotation.z = Math.PI / 2;
      rearRightWheel.position.set(0.8, 0.15, -1.0);
      rearRightWheel.castShadow = true;
      agvGroup.add(rearRightWheel);

      // 顶部天线
      const antennaGeometry = new THREE.CylinderGeometry(0.05, 0.05, 0.5, 8);
      const antennaMaterial = new THREE.MeshStandardMaterial({ color: 0xff0000 });
      const antenna = new THREE.Mesh(antennaGeometry, antennaMaterial);
      antenna.position.set(0, 0.5, 0);
      antenna.castShadow = true;
      agvGroup.add(antenna);

      // 添加车头标记 - 前部红色箭头
      const arrowGeometry = new THREE.ConeGeometry(0.15, 0.3, 8);
      const arrowMaterial = new THREE.MeshStandardMaterial({ color: 0xff0000 });
      const arrow = new THREE.Mesh(arrowGeometry, arrowMaterial);
      arrow.position.set(0, 0.4, 1.3); // 位于AGV顶部前端
      arrow.rotation.x = Math.PI / 2; // 指向正前方
      arrow.castShadow = true;
      agvGroup.add(arrow);

      return agvGroup;
    },

    // 移动到指定点位
    moveToPoint(pointName) {
      if (!this.agvModel || this.points.length === 0) {
        return;
      }

      // 找到目标点位
      const targetPoint = this.points.find(point => point.name === pointName);
      if (!targetPoint) {
        console.error('Target point not found:', pointName);
        return;
      }

      // 计算点位数据的范围，用于归一化目标位置
      let minX = Infinity, maxX = -Infinity;
      let minY = Infinity, maxY = -Infinity;

      this.points.forEach(point => {
        const x = point.pose && point.pose.position ? point.pose.position.x : point.x || 0;
        const y = point.pose && point.pose.position ? point.pose.position.y : point.y || 0;

        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
      });

      // 归一化目标位置到80x80的地板范围内
      const floorSize = 80;
      const padding = 2;
      const availableSize = floorSize - padding * 2;

      const scaleX = availableSize / (maxX - minX || 1);
      const scaleY = availableSize / (maxY - minY || 1);
      const scaleFactor = Math.min(scaleX, scaleY);

      const offsetX = -((minX + maxX) / 2) * scaleFactor;
      const offsetY = -((minY + maxY) / 2) * scaleFactor;

      // 计算目标位置
      const rawX = (targetPoint.pose && targetPoint.pose.position ? targetPoint.pose.position.x : targetPoint.x || 0);
      const rawZ = (targetPoint.pose && targetPoint.pose.position ? targetPoint.pose.position.y : targetPoint.y || 0);

      const targetX = rawX * scaleFactor + offsetX;
      const targetZ = rawZ * scaleFactor + offsetY;

      // 设置目标位置
      this.targetPosition = { x: targetX, z: targetZ };
      this.isMovingToTarget = true;
      this.isMoving = true;
    },

    // 创建点位
    createPoints() {
      // 清除现有点位
      this.pointObjects.forEach(obj => {
        if (this.scene) {
          this.scene.remove(obj);
        }
      });
      this.pointObjects = [];

      if (this.points.length === 0) {
        return;
      }

      // 计算点位数据的范围
      let minX = Infinity, maxX = -Infinity;
      let minY = Infinity, maxY = -Infinity;

      this.points.forEach(point => {
        const x = point.pose && point.pose.position ? point.pose.position.x : point.x || 0;
        const y = point.pose && point.pose.position ? point.pose.position.y : point.y || 0;

        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
      });

      // 归一化点位数据到80x80的地板范围内
      const floorSize = 80;
      const padding = 2; // 留出边距
      const availableSize = floorSize - padding * 2;

      // 计算缩放比例和偏移量
      const scaleX = availableSize / (maxX - minX || 1);
      const scaleY = availableSize / (maxY - minY || 1);
      const scaleFactor = Math.min(scaleX, scaleY); // 使用较小的缩放比例，确保所有点位都能放下

      const offsetX = -((minX + maxX) / 2) * scaleFactor;
      const offsetY = -((minY + maxY) / 2) * scaleFactor;

      // 创建新点位
      this.points.forEach(point => {
        const rawX = (point.pose && point.pose.position ? point.pose.position.x : point.x || 0);
        const rawZ = (point.pose && point.pose.position ? point.pose.position.y : point.y || 0);

        // 归一化并缩放点位坐标
        const x = rawX * scaleFactor + offsetX;
        const y = 0.5;
        const z = rawZ * scaleFactor + offsetY;

        // 点的几何体
        const geometry = new THREE.SphereGeometry(0.2, 16, 16);
        let material = new THREE.MeshStandardMaterial({
          color: 0x64748b,
          roughness: 0.5,
          metalness: 0.5
        });

        // 根据点类型设置不同颜色
        if (point.name && point.name.includes('Point-')) {
          material = new THREE.MeshStandardMaterial({
            color: 0x3b82f6,
            roughness: 0.3,
            metalness: 0.7
          });
        }

        const pointMesh = new THREE.Mesh(geometry, material);
        pointMesh.position.set(x, y, z);
        pointMesh.castShadow = true;
        pointMesh.receiveShadow = true;
        // 设置点位模型的渲染顺序，确保它显示在车辆后面
        pointMesh.renderOrder = 0;

        // 添加标签
        const label = this.createLabel(point.name || point.id || 'Point', x, y + 0.8, z);

        this.scene.add(pointMesh);
        this.scene.add(label);

        this.pointObjects.push(pointMesh);
        this.pointObjects.push(label);
      });
    },

    // 创建标签
    createLabel(text, x, y, z) {
      // 创建更大的文字标签，提高可见性
      const canvas = document.createElement('canvas');
      const context = canvas.getContext('2d');
      canvas.width = 256; // 画布尺寸
      canvas.height = 64;

      // 背景
      context.fillStyle = 'rgba(15, 23, 42, 0.95)';
      context.fillRect(0, 0, canvas.width, canvas.height);

      // 边框
      context.strokeStyle = '#3b82f6';
      context.lineWidth = 2;
      context.strokeRect(0, 0, canvas.width, canvas.height);

      // 文字
      context.fillStyle = '#ffffff';
      context.font = 'bold 24px Arial';
      context.textAlign = 'center';
      context.textBaseline = 'middle';
      context.fillText(text, canvas.width / 2, canvas.height / 2);

      const texture = new THREE.CanvasTexture(canvas);
      const spriteMaterial = new THREE.SpriteMaterial({
        map: texture,
        transparent: true
      });
      const sprite = new THREE.Sprite(spriteMaterial);
      sprite.position.set(x, y, z);
      sprite.scale.set(5, 1.25, 1);

      // 始终面向相机
      sprite.lookAt(this.camera.position);

      return sprite;
    },

    // MQTT初始化
    initMqtt() {
      try {
        // 连接到本地MQTT broker
        console.log('Attempting to connect to local MQTT broker at ws://localhost:8083/mqtt');
        this.mqttClient = mqtt.connect({
          host: 'localhost',
          port: 8083,
          protocol: 'ws',
          path: '/mqtt',
          clientId: 'robot-simulation-' + Math.random().toString(16).substr(2, 8),
          keepalive: 60,
          reconnectPeriod: 1000,
          connectTimeout: 5000,
          clean: true
        });

        // 监听连接事件
        this.mqttClient.on('connect', (connack) => {
          console.log('MQTT connected successfully:', connack);
          this.mqttConnected = true;
          // 订阅主题
          this.mqttClient.subscribe(this.mqttTopic, (err, granted) => {
            if (err) {
              console.error('Failed to subscribe to MQTT topic:', err);
            } else {
              console.log('Subscribed to MQTT topic:', this.mqttTopic, 'granted:', granted);
            }
          });
        });

        // 监听消息事件
        this.mqttClient.on('message', (topic, message, packet) => {
          console.log('Received MQTT message:', {
            topic,
            message: message.toString(),
            packet
          });
          this.handleMqttMessage(message.toString());
        });

        // 监听错误事件
        this.mqttClient.on('error', (err) => {
          console.error('MQTT error:', err);
          console.error('Error stack:', err.stack);
          this.mqttConnected = false;
        });

        // 监听断开连接事件
        this.mqttClient.on('close', () => {
          console.log('MQTT connection closed');
          this.mqttConnected = false;
        });

        // 监听重连事件
        this.mqttClient.on('reconnect', () => {
          console.log('MQTT reconnecting...');
        });

        // 监听离线事件
        this.mqttClient.on('offline', () => {
          console.log('MQTT offline');
          this.mqttConnected = false;
        });

        // 监听结束事件
        this.mqttClient.on('end', () => {
          console.log('MQTT connection ended');
          this.mqttConnected = false;
        });

      } catch (error) {
        console.error('Failed to initialize MQTT:', error);
        console.error('Error stack:', error.stack);
      }
    },

    // 处理MQTT消息
    handleMqttMessage(message) {
      console.log('=== MQTT MESSAGE RECEIVED ===');
      console.log('Raw message:', message);

      // 添加到指令日志
      const now = new Date();
      const timeStr = now.toLocaleTimeString();
      this.addMqttMessage(`[接收] ${timeStr}: ${message}`);

      try {
        // 解析JSON消息
        const command = JSON.parse(message);
        console.log('Parsed command:', command);

        // 检查消息格式
        if (!command || !command.command) {
          console.error('Invalid MQTT message format:', message);
          this.addMqttMessage(`[错误] ${timeStr}: 无效消息格式`);
          return;
        }

        console.log('Executing command:', command.command);
        this.addMqttMessage(`[执行] ${timeStr}: ${command.command}`);

        // 根据指令类型执行相应的操作
        switch (command.command) {
          case 'forward':
            console.log('Executing forward command');
            this.moveForward();
            break;
          case 'backward':
            console.log('Executing backward command');
            this.moveBackward();
            break;
          case 'left':
            console.log('Executing left command');
            this.turnLeft();
            break;
          case 'right':
            console.log('Executing right command');
            this.turnRight();
            break;
          case 'stop':
            console.log('Executing stop command');
            this.stopMovement();
            break;
          case 'reset':
            console.log('Executing reset command');
            this.resetPosition();
            break;
          case 'move':
            console.log('Executing move command');
            if (command.destination) {
              this.moveToPoint(command.destination);
            } else {
              console.error('Missing destination in move command');
              this.addMqttMessage(`[错误] ${timeStr}: 移动指令缺少目标点位`);
            }
            break;
          default:
            console.warn('Unknown command:', command.command);
            this.addMqttMessage(`[警告] ${timeStr}: 未知指令 ${command.command}`);
        }

        console.log('=== MQTT MESSAGE PROCESSED ===');

      } catch (error) {
        console.error('Failed to parse MQTT message:', error);
        console.error('Error parsing message:', message);
        this.addMqttMessage(`[错误] ${timeStr}: 解析失败 - ${error.message}`);
      }
    },

    // 添加MQTT消息到日志
    addMqttMessage(content) {
      this.mqttMessages.push({
        time: new Date().toLocaleTimeString(),
        content: content
      });

      // 保持日志数量在合理范围（最多20条）
      if (this.mqttMessages.length > 20) {
        this.mqttMessages.shift();
      }

      // 自动滚动到底部
      this.$nextTick(() => {
        const logsContainer = document.querySelector('.logs-container');
        if (logsContainer) {
          logsContainer.scrollTop = logsContainer.scrollHeight;
        }
      });
    },

    // 清空MQTT日志
    clearMqttMessages() {
      this.mqttMessages = [];
    },

    // 切换面板展开/折叠状态
    togglePanel() {
      this.panelExpanded = !this.panelExpanded;
    },

    // 断开MQTT连接
    disconnectMqtt() {
      if (this.mqttClient) {
        this.mqttClient.end();
        this.mqttClient = null;
        this.mqttConnected = false;
        console.log('MQTT disconnected');
      }
    },

    // 停止所有移动
    stopMovement() {
      this.isMoving = false;
      this.isTurning = false;
      this.moveDirection = null;
      this.turnFinishDirection = null;
    },

    initScene() {
      this.scene = new THREE.Scene();
      this.scene.background = new THREE.Color(0xf0f0f0);
    },

    initCamera() {
      const container = document.getElementById('container');
      this.camera = new THREE.PerspectiveCamera(
        45,
        container.clientWidth / container.clientHeight,
        0.1,
        1000
      );
      // 调整摄像头位置，使其能够看到整个80x80的地板
      this.camera.position.set(60, 60, 60);
      this.camera.lookAt(0, 0, 0);
    },

    initRenderer() {
      const container = document.getElementById('container');
      this.renderer = new THREE.WebGLRenderer({ antialias: true });
      this.renderer.setSize(container.clientWidth, container.clientHeight);
      this.renderer.setPixelRatio(window.devicePixelRatio);
      this.renderer.shadowMap.enabled = true;
      container.appendChild(this.renderer.domElement);
    },

    initControls() {
      this.controls = new OrbitControls(this.camera, this.renderer.domElement);
      this.controls.enableDamping = true;
      this.controls.dampingFactor = 0.05;
      this.controls.minDistance = 0.1;
      this.controls.maxDistance = Infinity;
    },

    initLights() {
      // 环境光
      const ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
      this.scene.add(ambientLight);

      // 方向光
      const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
      directionalLight.position.set(5, 10, 5);
      directionalLight.castShadow = true;
      directionalLight.shadow.mapSize.width = 2048;
      directionalLight.shadow.mapSize.height = 2048;
      this.scene.add(directionalLight);

      // 点光源
      const pointLight = new THREE.PointLight(0xffffff, 0.5);
      pointLight.position.set(-5, 5, -5);
      this.scene.add(pointLight);
    },

    initFloor() {
      // 将地板扩大至当前的两倍（80x80）
      const floorGeometry = new THREE.PlaneGeometry(80, 80);
      const floorMaterial = new THREE.MeshStandardMaterial({
        color: 0x808080,
        roughness: 0.8,
        metalness: 0.2
      });
      const floor = new THREE.Mesh(floorGeometry, floorMaterial);
      floor.rotation.x = -Math.PI / 2;
      floor.receiveShadow = true;
      this.scene.add(floor);

      // 添加网格辅助线，同样扩大至80x80
      const gridHelper = new THREE.GridHelper(80, 80, 0x444444, 0x888888);
      this.scene.add(gridHelper);
    },

    loadAGVModel() {
        this.createDefaultAGVModel();
    },

    createDefaultAGVModel() {
      // 创建AGV主体
      const agvGroup = new THREE.Group();

      // AGV底盘
      const chassisGeometry = new THREE.BoxGeometry(1.5, 0.3, 2.5);
      const chassisMaterial = new THREE.MeshStandardMaterial({ color: 0x0066cc });
      const chassis = new THREE.Mesh(chassisGeometry, chassisMaterial);
      chassis.castShadow = true;
      chassis.receiveShadow = true;
      agvGroup.add(chassis);

      // AGV顶部
      const topGeometry = new THREE.BoxGeometry(1.3, 0.2, 2.3);
      const topMaterial = new THREE.MeshStandardMaterial({ color: 0x0088ff });
      const top = new THREE.Mesh(topGeometry, topMaterial);
      top.position.y = 0.25;
      top.castShadow = true;
      top.receiveShadow = true;
      agvGroup.add(top);

      // 车轮 - 前左
      const wheelGeometry = new THREE.CylinderGeometry(0.2, 0.2, 0.15, 16);
      const wheelMaterial = new THREE.MeshStandardMaterial({ color: 0x333333 });

      const frontLeftWheel = new THREE.Mesh(wheelGeometry, wheelMaterial);
      frontLeftWheel.rotation.z = Math.PI / 2;
      frontLeftWheel.position.set(-0.8, 0.15, 1.0);
      frontLeftWheel.castShadow = true;
      agvGroup.add(frontLeftWheel);

      // 车轮 - 前右
      const frontRightWheel = new THREE.Mesh(wheelGeometry, wheelMaterial);
      frontRightWheel.rotation.z = Math.PI / 2;
      frontRightWheel.position.set(0.8, 0.15, 1.0);
      frontRightWheel.castShadow = true;
      agvGroup.add(frontRightWheel);

      // 车轮 - 后左
      const rearLeftWheel = new THREE.Mesh(wheelGeometry, wheelMaterial);
      rearLeftWheel.rotation.z = Math.PI / 2;
      rearLeftWheel.position.set(-0.8, 0.15, -1.0);
      rearLeftWheel.castShadow = true;
      agvGroup.add(rearLeftWheel);

      // 车轮 - 后右
      const rearRightWheel = new THREE.Mesh(wheelGeometry, wheelMaterial);
      rearRightWheel.rotation.z = Math.PI / 2;
      rearRightWheel.position.set(0.8, 0.15, -1.0);
      rearRightWheel.castShadow = true;
      agvGroup.add(rearRightWheel);

      // 顶部天线
      const antennaGeometry = new THREE.CylinderGeometry(0.05, 0.05, 0.5, 8);
      const antennaMaterial = new THREE.MeshStandardMaterial({ color: 0xff0000 });
      const antenna = new THREE.Mesh(antennaGeometry, antennaMaterial);
      antenna.position.set(0, 0.5, 0);
      antenna.castShadow = true;
      agvGroup.add(antenna);

      // 添加车头标记 - 前部红色箭头
      const arrowGeometry = new THREE.ConeGeometry(0.15, 0.3, 8);
      const arrowMaterial = new THREE.MeshStandardMaterial({ color: 0xff0000 });
      const arrow = new THREE.Mesh(arrowGeometry, arrowMaterial);
      arrow.position.set(0, 0.4, 1.3); // 位于AGV顶部前端
      arrow.rotation.x = Math.PI / 2; // 指向正前方
      arrow.castShadow = true;
      agvGroup.add(arrow);

      // 添加车头灯
      const headlightGeometry = new THREE.SphereGeometry(0.1, 16, 16);
      const headlightMaterial = new THREE.MeshStandardMaterial({
        color: 0xffff00,
        emissive: 0xffff00,
        emissiveIntensity: 0.5
      });

      // 左前灯
      const leftHeadlight = new THREE.Mesh(headlightGeometry, headlightMaterial);
      leftHeadlight.position.set(-0.4, 0.3, 1.2);
      leftHeadlight.castShadow = true;
      agvGroup.add(leftHeadlight);

      // 右前灯
      const rightHeadlight = new THREE.Mesh(headlightGeometry, headlightMaterial);
      rightHeadlight.position.set(0.4, 0.3, 1.2);
      rightHeadlight.castShadow = true;
      agvGroup.add(rightHeadlight);

      // 更改顶部颜色，前端部分颜色不同以区分车头
      const topFrontGeometry = new THREE.BoxGeometry(1.3, 0.15, 0.5);
      const topFrontMaterial = new THREE.MeshStandardMaterial({ color: 0xff4444 });
      const topFront = new THREE.Mesh(topFrontGeometry, topFrontMaterial);
      topFront.position.y = 0.325;
      topFront.position.z = 0.9;
      topFront.castShadow = true;
      agvGroup.add(topFront);

      this.agvModel = agvGroup;
      this.agvModel.position.set(0, 0, 0);
      // 设置车辆模型的渲染顺序，确保它始终显示在前面
      this.agvModel.traverse((obj) => {
        if (obj.isMesh) {
          obj.renderOrder = 1;
        }
      });
      this.scene.add(this.agvModel);

      // 为默认模型创建简单的旋转动画
      this.mixer = new AnimationMixer(this.agvModel);
    },

    animate() {
      this.animationId = requestAnimationFrame(this.animate);

      const delta = this.clock ? this.clock.getDelta() : 0.016;
      const speedValue = Number(this.speed);

      // 处理转向逻辑
      if (this.isTurning && this.agvModel) {
        const rotationSpeed = 6.0 * speedValue;
        let angleDiff = this.targetRotation - this.agvModel.rotation.y;

        // 规范化角度差到[-π, π]范围，确保车辆选择最短路径
        angleDiff = ((angleDiff + Math.PI) % (2 * Math.PI)) - Math.PI;

        if (Math.abs(angleDiff) < 0.01) {
          // 转向完成
          console.log('Turn finished...');
          this.agvModel.rotation.y = this.targetRotation;
          this.isTurning = false;

          if (this.turnFinishDirection) {
            // 如果有转向完成后的移动方向，继续移动
            console.log('Turn finished, moving in direction:', this.turnFinishDirection);
            if (this.turnFinishDirection === 'moveToTarget') {
              // 恢复移动到目标位置的状态
              this.isMoving = true;
            } else {
              // 正常移动
              this.isMoving = true;
              this.moveDirection = this.turnFinishDirection;
            }
            this.turnFinishDirection = null;
          } else {
            // 否则停止移动
            console.log('Turn finished, stopping all movement...');
            this.isMoving = false;
            this.moveDirection = null;
          }

          console.log('Final state after turn:', {
            isMoving: this.isMoving,
            isTurning: this.isTurning,
            moveDirection: this.moveDirection,
            turnFinishDirection: this.turnFinishDirection,
            isMovingToTarget: this.isMovingToTarget
          });
        } else {
          // 继续转向
          const rotationAmount = Math.sign(angleDiff) * rotationSpeed * delta;
          this.agvModel.rotation.y += rotationAmount;
        }
      }

      // 处理移动到目标位置的逻辑
      if (this.isMovingToTarget && this.agvModel && this.targetPosition) {
        const moveSpeed = 6.0 * speedValue;

        // 计算当前位置到目标位置的方向
        const direction = new THREE.Vector2(
          this.targetPosition.x - this.agvModel.position.x,
          this.targetPosition.z - this.agvModel.position.z
        );

        const distance = direction.length();

        // 检查是否到达目标位置
        if (distance < 0.1) {
          // 到达目标位置，停止移动
          this.agvModel.position.set(this.targetPosition.x, this.agvModel.position.y, this.targetPosition.z);
          this.isMovingToTarget = false;
          this.isMoving = false;
          this.targetPosition = null;
          console.log('Reached target position');
        } else {
          // 计算目标朝向
          direction.normalize();
          const targetRotation = Math.atan2(direction.x, direction.y);

          // 计算角度差并规范化到[-π, π]范围，确保车辆选择最短路径
          let angleDiff = targetRotation - this.agvModel.rotation.y;
          // 规范化角度差到[-π, π]范围
          angleDiff = ((angleDiff + Math.PI) % (2 * Math.PI)) - Math.PI;

          if (Math.abs(angleDiff) > 0.01) {
            // 如果需要转向，先停止移动，原地转向
            if (!this.isTurning) {
              console.log('Need to turn, stopping movement and starting in-place turn...');
              this.isMoving = false;
              this.isTurning = true;
              this.turnFinishDirection = 'moveToTarget';
              this.targetRotation = targetRotation;
            }
          } else {
            // 方向正确，继续移动
            if (!this.isMoving && !this.isTurning) {
              this.isMoving = true;
            }

            if (this.isMoving) {
              // 向前移动
              this.agvModel.position.x += Math.sin(this.agvModel.rotation.y) * moveSpeed * delta;
              this.agvModel.position.z += Math.cos(this.agvModel.rotation.y) * moveSpeed * delta;

              // 边界检测
              // if (Math.abs(this.agvModel.position.x) > this.boundary || Math.abs(this.agvModel.position.z) > this.boundary) {
              //   // 超出边界，停止移动
              //   this.isMovingToTarget = false;
              //   this.isMoving = false;
              //   this.targetPosition = null;
              //   // 将AGV拉回边界内
              //   this.agvModel.position.x = Math.max(-this.boundary, Math.min(this.boundary, this.agvModel.position.x));
              //   this.agvModel.position.z = Math.max(-this.boundary, Math.min(this.boundary, this.agvModel.position.z));
              // }
            }
          }
        }
      }
      // 处理手动移动逻辑
      else if (this.isMoving && this.agvModel && !this.isTurning) {
        console.log('AGV is moving in direction:', this.moveDirection);
        const moveSpeed = 6.0 * speedValue;
        let moved = false;

        switch (this.moveDirection) {
          case 'forward':
            // 沿当前朝向的Z轴正方向移动
            this.agvModel.position.x += Math.sin(this.agvModel.rotation.y) * moveSpeed * delta;
            this.agvModel.position.z += Math.cos(this.agvModel.rotation.y) * moveSpeed * delta;
            moved = true;
            break;
          case 'backward':
            // 沿当前朝向的Z轴负方向移动
            this.agvModel.position.x -= Math.sin(this.agvModel.rotation.y) * moveSpeed * delta;
            this.agvModel.position.z -= Math.cos(this.agvModel.rotation.y) * moveSpeed * delta;
            moved = true;
            break;
          default:
            // 无效的移动方向，停止移动
            console.log('Invalid move direction:', this.moveDirection, 'stopping...');
            this.isMoving = false;
            this.moveDirection = null;
            moved = false;
            break;
        }

        // if (moved) {
        //   // 边界检测
        //   if (Math.abs(this.agvModel.position.x) > this.boundary || Math.abs(this.agvModel.position.z) > this.boundary) {
        //     // 超出边界，停止移动
        //     this.isMoving = false;
        //     this.moveDirection = null;
        //     // 将AGV拉回边界内
        //     this.agvModel.position.x = Math.max(-this.boundary, Math.min(this.boundary, this.agvModel.position.x));
        //     this.agvModel.position.z = Math.max(-this.boundary, Math.min(this.boundary, this.agvModel.position.z));
        //   }
        // }
      }

      if (this.controls) {
        this.controls.update();
      }

      if (this.renderer && this.scene && this.camera) {
        this.renderer.render(this.scene, this.camera);
      }
    },

    // 方向控制方法
    moveForward() {
      this.isMoving = true;
      this.moveDirection = 'forward';
      this.isTurning = false;
    },

    moveBackward() {
      this.isMoving = true;
      this.moveDirection = 'backward';
      this.isTurning = false;
    },

    turnLeft() {
      if (this.agvModel) {
        console.log('Turn left clicked, stopping all movement and starting in-place turn...');
        // 立即停止所有移动
        this.isMoving = false;
        this.isMovingToTarget = false;

        // 开始原地转向
        this.isTurning = true;
        // 向左转90度
        this.targetRotation = this.agvModel.rotation.y + Math.PI / 2;
        this.turnFinishDirection = null;
        console.log('Starting in-place left turn...');
      }
    },

    turnRight() {
      if (this.agvModel) {
        console.log('Turn right clicked, stopping all movement and starting in-place turn...');
        // 立即停止所有移动
        this.isMoving = false;
        this.isMovingToTarget = false;

        // 开始原地转向
        this.isTurning = true;
        // 向右转90度
        this.targetRotation = this.agvModel.rotation.y - Math.PI / 2;
        this.turnFinishDirection = null;
        console.log('Starting in-place right turn...');
      }
    },

    // 重置车辆位置
    async resetPosition() {
      if (!this.agvModel) {
        return;
      }

      // 停止所有移动
      this.isMoving = false;
      this.isTurning = false;
      this.moveDirection = null;
      this.turnFinishDirection = null;
      this.isMovingToTarget = false;
      this.targetPosition = null;

      try {
        // 获取车辆位置信息
        const response = await listVehicles();
        this.vehicles = response.rows || [];

        if (this.vehicles.length > 0 && this.points.length > 0) {
          // 获取第一个车辆
          const vehicle = this.vehicles[0];
          // 找到对应的point
          const currentPoint = this.points.find(point => point.name === vehicle.currentPosition);

          if (currentPoint) {
            // 计算点位数据的范围，用于归一化位置
            let minX = Infinity, maxX = -Infinity;
            let minY = Infinity, maxY = -Infinity;

            this.points.forEach(point => {
              const x = point.pose && point.pose.position ? point.pose.position.x : point.x || 0;
              const y = point.pose && point.pose.position ? point.pose.position.y : point.y || 0;

              minX = Math.min(minX, x);
              maxX = Math.max(maxX, x);
              minY = Math.min(minY, y);
              maxY = Math.max(maxY, y);
            });

            // 归一化位置到80x80的地板范围内
            const floorSize = 80;
            const padding = 2;
            const availableSize = floorSize - padding * 2;

            const scaleX = availableSize / (maxX - minX || 1);
            const scaleY = availableSize / (maxY - minY || 1);
            const scaleFactor = Math.min(scaleX, scaleY);

            const offsetX = -((minX + maxX) / 2) * scaleFactor;
            const offsetY = -((minY + maxY) / 2) * scaleFactor;

            // 计算车辆位置
            const rawX = (currentPoint.pose && currentPoint.pose.position ? currentPoint.pose.position.x : currentPoint.x || 0);
            const rawZ = (currentPoint.pose && currentPoint.pose.position ? currentPoint.pose.position.y : currentPoint.y || 0);

            const x = rawX * scaleFactor + offsetX;
            const z = rawZ * scaleFactor + offsetY;

            // 将车辆重置到对应的point位置
            this.agvModel.position.set(x, this.agvModel.position.y, z);
            this.agvModel.rotation.set(0, 0, 0);
            console.log('Vehicle reset to position:', { x, z });
          } else {
            // 如果找不到对应的point，重置到原点
            this.agvModel.position.set(0, 0, 0);
            this.agvModel.rotation.set(0, 0, 0);
            console.log('Vehicle reset to origin, no current point found');
          }
        } else {
          // 如果没有车辆数据或点位数据，重置到原点
          this.agvModel.position.set(0, 0, 0);
          this.agvModel.rotation.set(0, 0, 0);
          console.log('Vehicle reset to origin, no vehicles or points data');
        }
      } catch (error) {
        console.error('Failed to reset vehicle position:', error);
        // 重置失败时，重置到原点
        this.agvModel.position.set(0, 0, 0);
        this.agvModel.rotation.set(0, 0, 0);
      }
    },

    // MQTT测试方法
    testMqttForward() {
      if (this.mqttClient && this.mqttConnected) {
        const message = JSON.stringify({ vehicleId: 'test-vehicle', command: 'forward' });
        console.log('Sending test MQTT message:', message);
        // 添加到指令日志
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[发送] ${timeStr}: ${message}`);
        this.mqttClient.publish(this.mqttTopic, message, { qos: 0, retain: false });
      } else {
        console.error('MQTT not connected');
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[错误] ${timeStr}: MQTT未连接，无法发送消息`);
      }
    },

    testMqttBackward() {
      if (this.mqttClient && this.mqttConnected) {
        const message = JSON.stringify({ vehicleId: 'test-vehicle', command: 'backward' });
        console.log('Sending test MQTT message:', message);
        // 添加到指令日志
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[发送] ${timeStr}: ${message}`);
        this.mqttClient.publish(this.mqttTopic, message, { qos: 0, retain: false });
      } else {
        console.error('MQTT not connected');
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[错误] ${timeStr}: MQTT未连接，无法发送消息`);
      }
    },

    testMqttLeft() {
      if (this.mqttClient && this.mqttConnected) {
        const message = JSON.stringify({ vehicleId: 'test-vehicle', command: 'left' });
        console.log('Sending test MQTT message:', message);
        // 添加到指令日志
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[发送] ${timeStr}: ${message}`);
        this.mqttClient.publish(this.mqttTopic, message, { qos: 0, retain: false });
      } else {
        console.error('MQTT not connected');
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[错误] ${timeStr}: MQTT未连接，无法发送消息`);
      }
    },

    testMqttRight() {
      if (this.mqttClient && this.mqttConnected) {
        const message = JSON.stringify({ vehicleId: 'test-vehicle', command: 'right' });
        console.log('Sending test MQTT message:', message);
        // 添加到指令日志
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[发送] ${timeStr}: ${message}`);
        this.mqttClient.publish(this.mqttTopic, message, { qos: 0, retain: false });
      } else {
        console.error('MQTT not connected');
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[错误] ${timeStr}: MQTT未连接，无法发送消息`);
      }
    },

    testMqttStop() {
      if (this.mqttClient && this.mqttConnected) {
        const message = JSON.stringify({ vehicleId: 'test-vehicle', command: 'stop' });
        console.log('Sending test MQTT message:', message);
        // 添加到指令日志
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[发送] ${timeStr}: ${message}`);
        this.mqttClient.publish(this.mqttTopic, message, { qos: 0, retain: false });
      } else {
        console.error('MQTT not connected');
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[错误] ${timeStr}: MQTT未连接，无法发送消息`);
      }
    },

    testMqttReset() {
      if (this.mqttClient && this.mqttConnected) {
        const message = JSON.stringify({ vehicleId: 'test-vehicle', command: 'reset' });
        console.log('Sending test MQTT message:', message);
        // 添加到指令日志
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[发送] ${timeStr}: ${message}`);
        this.mqttClient.publish(this.mqttTopic, message, { qos: 0, retain: false });
      } else {
        console.error('MQTT not connected');
        const now = new Date();
        const timeStr = now.toLocaleTimeString();
        this.addMqttMessage(`[错误] ${timeStr}: MQTT未连接，无法发送消息`);
      }
    },

    onWindowResize() {
      const container = document.getElementById('container');
      if (this.camera && this.renderer) {
        this.camera.aspect = container.clientWidth / container.clientHeight;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(container.clientWidth, container.clientHeight);
      }
    },

    dispose() {
      if (this.animationId) {
        cancelAnimationFrame(this.animationId);
      }

      if (this.controls) {
        this.controls.dispose();
      }

      if (this.renderer) {
        this.renderer.dispose();
      }

      if (this.mixer) {
        this.mixer.stopAllActions();
      }

      window.removeEventListener('resize', this.onWindowResize);
    }
  }
};
</script>

<style scoped>
.robot-simulation {
  position: relative;
  width: 100%;
  height: 100vh;
  background-color: #f0f0f0;
  overflow: hidden;
}

#container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.controls {
  position: absolute;
  top: 20px;
  left: 20px;
  background-color: rgba(255, 255, 255, 0.9);
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 100;
  min-width: 360px;
  max-width: 440px;
  transition: all 0.3s ease;
  overflow: hidden;
}

/* 折叠状态 */
.controls.collapsed {
  min-width: auto;
  max-width: auto;
  padding: 10px 15px;
}

/* 面板头部 */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.panel-header h3 {
  margin: 0;
  color: #333;
  font-size: 16px;
  flex: 1;
  text-align: center;
}

/* 切换按钮 */
.toggle-button {
  background: none;
  border: none;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  padding: 4px;
  border-radius: 3px;
  transition: all 0.2s ease;
  min-width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toggle-button:hover {
  background-color: rgba(0, 0, 0, 0.05);
  color: #333;
}

.toggle-button:active {
  transform: scale(0.95);
}

/* 面板内容 */
.panel-content {
  transition: all 0.3s ease;
}

/* 折叠状态下的面板内容 */
.controls.collapsed .panel-content {
  display: none;
}

.control-group {
  margin-bottom: 12px;
}

.control-group label {
  display: block;
  width: 100%;
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}

.control-group input[type="range"] {
  width: calc(100% - 60px);
  margin: 0;
  display: inline-block;
  vertical-align: middle;
}

.control-group span {
  display: inline-block;
  font-size: 13px;
  color: #333;
  width: 40px;
  text-align: right;
  vertical-align: middle;
  margin-left: 10px;
}

.control-group button {
  margin: 0 4px 6px 0;
  padding: 5px 10px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
  min-width: 60px;
  display: inline-block;
}

.control-group button:hover {
  background-color: #66b1ff;
}

.control-group button:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}

/* 按钮容器 */
.control-group .button-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}

/* 测试按钮样式 */
.control-group button[style*="background-color: #666"] {
  width: 100%;
  margin: 8px 0 0 0;
  min-width: auto;
}

/* MQTT状态样式 */
.mqtt-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #666;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #ff4d4f;
  transition: all 0.3s ease;
  box-shadow: 0 0 4px rgba(255, 77, 79, 0.5);
}

.mqtt-status.connected .status-indicator {
  background-color: #52c41a;
  box-shadow: 0 0 4px rgba(82, 196, 26, 0.5);
}

.mqtt-status.connected {
  color: #52c41a;
}

/* MQTT日志样式 */
.mqtt-logs {
  margin-top: 15px;
  background-color: rgba(0, 0, 0, 0.03);
  border-radius: 6px;
  padding: 10px;
}

.mqtt-logs h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.logs-container {
  max-height: 200px;
  overflow-y: auto;
  background-color: white;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 10px;
  font-family: monospace;
  font-size: 12px;
  line-height: 1.4;
}

.log-item {
  margin-bottom: 6px;
  padding: 4px 0;
  border-bottom: 1px solid #f0f0f0;
}

.log-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.log-time {
  color: #999;
  margin-right: 8px;
  font-size: 11px;
}

.log-content {
  color: #333;
  word-break: break-all;
}

.log-empty {
  color: #999;
  text-align: center;
  padding: 20px;
  font-style: italic;
}

/* 滚动条样式 */
.logs-container::-webkit-scrollbar {
  width: 6px;
}

.logs-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.logs-container::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}

.logs-container::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* 日志内容颜色 */
.log-content:contains('[接收]') {
  color: #1890ff;
}

.log-content:contains('[发送]') {
  color: #52c41a;
}

.log-content:contains('[执行]') {
  color: #faad14;
}

.log-content:contains('[错误]') {
  color: #ff4d4f;
}

.log-content:contains('[警告]') {
  color: #fa8c16;
}
</style>

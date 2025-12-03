<template>
  <div class="robot-simulation">
    <div id="container"></div>
    <div class="controls">
      <h3>AGV机器人仿真</h3>
      <div class="control-group">
        <label>速度：</label>
        <input type="range" min="0" max="1" step="0.01" v-model.number="speed" />
        <span>{{ Number(speed).toFixed(2) }}</span>
      </div>
      <div class="control-group">
        <button @click="moveForward">前进</button>
        <button @click="moveBackward">后退</button>
        <button @click="turnLeft">左转</button>
        <button @click="turnRight">右转</button>
        <button @click="resetPosition">重置位置</button>
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
      speed: 0.5,
      agvModel: null,
      // 移动状态管理
      isMoving: false,
      moveDirection: null, // forward, backward, left, right
      turnAngle: 0,
      targetRotation: 0,
      isTurning: false,
      turnFinishDirection: null, // 转向完成后要移动的方向
      // 边界设置
      boundary: 8, // 边界大小，AGV不能超出±8的范围
      // MQTT相关
      mqttClient: null,
      mqttConnected: false,
      mqttTopic: 'vehicle/control'
    };
  },
  mounted() {
    this.init();
    this.initMqtt();
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
      this.loadAGVModel();
      this.animate();
      
      window.addEventListener('resize', this.onWindowResize);
    },
    
    // MQTT初始化
    initMqtt() {
      try {
        // 连接到MQTT broker
        this.mqttClient = mqtt.connect('ws://localhost:8083/mqtt');
        
        // 监听连接事件
        this.mqttClient.on('connect', () => {
          console.log('MQTT connected');
          this.mqttConnected = true;
          // 订阅主题
          this.mqttClient.subscribe(this.mqttTopic, (err) => {
            if (err) {
              console.error('Failed to subscribe to MQTT topic:', err);
            } else {
              console.log('Subscribed to MQTT topic:', this.mqttTopic);
            }
          });
        });
        
        // 监听消息事件
        this.mqttClient.on('message', (topic, message) => {
          console.log('Received MQTT message:', {
            topic,
            message: message.toString()
          });
          this.handleMqttMessage(message.toString());
        });
        
        // 监听错误事件
        this.mqttClient.on('error', (err) => {
          console.error('MQTT error:', err);
          this.mqttConnected = false;
        });
        
        // 监听断开连接事件
        this.mqttClient.on('close', () => {
          console.log('MQTT connection closed');
          this.mqttConnected = false;
        });
        
      } catch (error) {
        console.error('Failed to initialize MQTT:', error);
      }
    },
    
    // 处理MQTT消息
    handleMqttMessage(message) {
      try {
        // 解析JSON消息
        const command = JSON.parse(message);
        
        // 检查消息格式
        if (!command || !command.command) {
          console.error('Invalid MQTT message format:', message);
          return;
        }
        
        // 根据指令类型执行相应的操作
        switch (command.command) {
          case 'forward':
            this.moveForward();
            break;
          case 'backward':
            this.moveBackward();
            break;
          case 'left':
            this.turnLeft();
            break;
          case 'right':
            this.turnRight();
            break;
          case 'stop':
            this.stopMovement();
            break;
          case 'reset':
            this.resetPosition();
            break;
          default:
            console.warn('Unknown command:', command.command);
        }
        
      } catch (error) {
        console.error('Failed to parse MQTT message:', error);
      }
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
      this.camera.position.set(5, 5, 5);
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
      this.controls.minDistance = 2;
      this.controls.maxDistance = 20;
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
      const floorGeometry = new THREE.PlaneGeometry(20, 20);
      const floorMaterial = new THREE.MeshStandardMaterial({
        color: 0x808080,
        roughness: 0.8,
        metalness: 0.2
      });
      const floor = new THREE.Mesh(floorGeometry, floorMaterial);
      floor.rotation.x = -Math.PI / 2;
      floor.receiveShadow = true;
      this.scene.add(floor);
      
      // 添加网格辅助线
      const gridHelper = new THREE.GridHelper(20, 20, 0x444444, 0x888888);
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
        const rotationSpeed = 1.0 * speedValue;
        const angleDiff = this.targetRotation - this.agvModel.rotation.y;
        
        if (Math.abs(angleDiff) < 0.01) {
          // 转向完成，保持静止
          console.log('Turn finished, stopping all movement...');
          this.agvModel.rotation.y = this.targetRotation;
          this.isTurning = false;
          this.isMoving = false;
          this.moveDirection = null;
          this.turnFinishDirection = null;
          console.log('Final state after turn:', {
            isMoving: this.isMoving,
            isTurning: this.isTurning,
            moveDirection: this.moveDirection,
            turnFinishDirection: this.turnFinishDirection
          });
        } else {
          // 继续转向
          const rotationAmount = Math.sign(angleDiff) * rotationSpeed * delta;
          this.agvModel.rotation.y += rotationAmount;
        }
      }
      
      // 处理移动逻辑
      if (this.isMoving && this.agvModel && !this.isTurning) {
        console.log('AGV is moving in direction:', this.moveDirection);
        const moveSpeed = 2.0 * speedValue;
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
          case 'left':
            // 沿当前朝向的X轴负方向移动
            this.agvModel.position.x -= Math.cos(this.agvModel.rotation.y) * moveSpeed * delta;
            this.agvModel.position.z += Math.sin(this.agvModel.rotation.y) * moveSpeed * delta;
            moved = true;
            break;
          case 'right':
            // 沿当前朝向的X轴正方向移动
            this.agvModel.position.x += Math.cos(this.agvModel.rotation.y) * moveSpeed * delta;
            this.agvModel.position.z -= Math.sin(this.agvModel.rotation.y) * moveSpeed * delta;
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
        
        if (moved) {
          // 边界检测
          if (Math.abs(this.agvModel.position.x) > this.boundary || Math.abs(this.agvModel.position.z) > this.boundary) {
            // 超出边界，停止移动
            this.isMoving = false;
            this.moveDirection = null;
            // 将AGV拉回边界内
            this.agvModel.position.x = Math.max(-this.boundary, Math.min(this.boundary, this.agvModel.position.x));
            this.agvModel.position.z = Math.max(-this.boundary, Math.min(this.boundary, this.agvModel.position.z));
          }
        }
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
        console.log('Turn left clicked, stopping all movement...');
        // 立即停止所有移动，重置所有相关状态
        this.isMoving = false;
        this.isTurning = false;
        this.moveDirection = null;
        this.turnFinishDirection = null;
        
        // 延迟一下再开始转向，确保状态重置完成
        setTimeout(() => {
          console.log('Starting left turn...');
          // 开始转向
          this.isTurning = true;
          // 向左转90度
          this.targetRotation = this.agvModel.rotation.y + Math.PI / 2;
        }, 50);
      }
    },
    
    turnRight() {
      if (this.agvModel) {
        console.log('Turn right clicked, stopping all movement...');
        // 立即停止所有移动，重置所有相关状态
        this.isMoving = false;
        this.isTurning = false;
        this.moveDirection = null;
        this.turnFinishDirection = null;
        
        // 延迟一下再开始转向，确保状态重置完成
        setTimeout(() => {
          console.log('Starting right turn...');
          // 开始转向
          this.isTurning = true;
          // 向右转90度
          this.targetRotation = this.agvModel.rotation.y - Math.PI / 2;
        }, 50);
      }
    },
    
    resetPosition() {
      if (this.agvModel) {
        this.isMoving = false;
        this.isTurning = false;
        this.moveDirection = null;
        this.turnFinishDirection = null;
        this.agvModel.position.set(0, 0, 0);
        this.agvModel.rotation.set(0, 0, 0);
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
  background-color: rgba(255, 255, 255, 0.8);
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 100;
  min-width: 250px;
}

.controls h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 18px;
}

.control-group {
  margin-bottom: 15px;
}

.control-group label {
  display: inline-block;
  width: 60px;
  font-size: 14px;
  color: #666;
}

.control-group input[type="range"] {
  width: 120px;
  margin: 0 10px;
}

.control-group span {
  display: inline-block;
  width: 40px;
  text-align: right;
  font-size: 14px;
  color: #333;
}

.control-group button {
  margin-right: 10px;
  padding: 6px 12px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.control-group button:hover {
  background-color: #66b1ff;
}

.control-group button:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}
</style>
<!-- AdvancedMapViewer.vue -->
<template>
  <div class="advanced-map-viewer">
    <!-- 侧边控制面板 -->
    <div class="sidebar">
      <h3>ROS2 地图控制</h3>

      <div class="connection-panel">
        <h4>连接设置</h4>
        <input v-model="config.rosBridgeUrl" placeholder="WebSocket URL" />
        <button @click="handleConnection">
          {{ rosConnected ? '断开' : '连接' }}
        </button>
        <div class="connection-status" :class="rosStatusClass">
          {{ rosStatusText }}
        </div>
      </div>

      <div class="map-controls">
        <h4>地图控制</h4>
        <label>
          <input type="checkbox" v-model="showGrid" />
          显示网格
        </label>
        <label>
          <input type="checkbox" v-model="showRobot" />
          显示机器人
        </label>
        <label>
          <input type="checkbox" v-model="autoCenter" />
          自动居中
        </label>

        <div class="zoom-control">
          <button @click="zoomIn">+</button>
          <span>缩放: {{ zoomLevel.toFixed(1) }}x</span>
          <button @click="zoomOut">-</button>
        </div>
      </div>

      <div class="navigation-controls">
        <h4>导航控制</h4>
        <div class="sidebar-buttons">
          <button @click="setTool('pan')" :class="{ active: currentTool === 'pan' }">
            🖐️ 平移
          </button>
          <button @click="toggleInitializationMode" :class="{ active: isInitializationMode }">
            📍 初始化位置
          </button>
          <button @click="toggleNavigationMode" :class="{ active: isNavigationMode }">
            🚩 设置目标
          </button>
          <button @click="cancelNavigation">
            ⏹️ 取消导航
          </button>
          <button @click="openSavePositionDialog">
            💾 保存当前位置
          </button>
        </div>
      </div>

      <div class="sidebar-section">
        <h3>控制</h3>
        <div class="control-group">
          <button
            @click="toggleKeyboardControl"
            class="keyboard-toggle-btn"
            :class="{ active: enableKeyboardControl }"
          >
            <span class="btn-icon">{{ enableKeyboardControl ? '🎮' : '⌨️' }}</span>
            <span class="btn-text">{{ enableKeyboardControl ? '关闭键盘控制' : '打开键盘控制' }}</span>
          </button>
        </div>
      </div>

      <div class="robot-info" v-if="robotPose">
        <h4>机器人信息</h4>
        <p>位置: ({{ robotPose.x.toFixed(2) }}, {{ robotPose.y.toFixed(2) }})</p>
        <p>朝向: {{ (robotPose.yaw * 180 / Math.PI).toFixed(1) }}°</p>
      </div>

      <div class="map-info" v-if="currentMap">
        <h4>地图信息</h4>
        <p>尺寸: {{ currentMap.width }} × {{ currentMap.height }}</p>
        <p>分辨率: {{ currentMap.resolution }} m/px</p>
        <p>占用率: {{ occupancyRate }}%</p>
      </div>
    </div>

    <!-- 主地图区域 -->
    <div class="main-content">
      <div ref="mapContainer" class="map-area">
        <!-- 相机图像面板 - 小窗模式 -->
        <div class="camera-panel-small" ref="cameraPanel">
          <div class="camera-header-small">
            <h5>相机图像</h5>
            <div class="camera-controls-small">
              <label>
                <input type="checkbox" v-model="cameraEnabled" @change="toggleCamera" class="camera-checkbox" />
                {{ cameraEnabled ? '禁用' : '启用' }}
              </label>
            </div>
          </div>
          <div class="camera-content-small" v-if="cameraEnabled">
            <iframe
              src="http://10.188.232.82:8080/stream_viewer?topic=/image_raw"
              frameborder="0"
              class="camera-iframe-small"
              title="ROS2相机图像"
              ref="cameraIframe"
            ></iframe>
          </div>
          <!-- 调整大小手柄 -->
          <div class="resize-handle" @mousedown="startResize" v-if="cameraEnabled"></div>
        </div>
        <!-- 语音合成控制面板 -->
        <div class="tts-control-panel">
          <!-- 切换按钮 -->
          <button class="tts-toggle-btn" @click="toggleTTSPanel">
            {{ showTTSPanel ? '收起语音' : '语音合成' }}
          </button>

          <!-- 语音合成面板 -->
          <div v-if="showTTSPanel" class="tts-panel">
            <div class="tts-header">
              <h4>语音合成</h4>
            </div>
            <div class="tts-content">
              <div class="tts-form-item">
                <label>语音内容:</label>
                <textarea
                  v-model="ttsRequest.text"
                  placeholder="请输入要合成的文本"
                  rows="3"
                  class="tts-textarea"
                ></textarea>
              </div>

              <div class="tts-form-row">
                <div class="tts-form-item">
                  <label>语言:</label>
                  <select v-model="ttsRequest.language" class="tts-select">
                    <option value="zh">中文</option>
                    <option value="en">英文</option>
                  </select>
                </div>
              </div>

              <div class="tts-form-row">
                <div class="tts-form-item slider-item">
                  <label>语速: {{ ttsRequest.speed }}</label>
                  <input
                    type="range"
                    v-model="ttsRequest.speed"
                    min="0"
                    max="100"
                    class="tts-slider"
                  >
                </div>
              </div>

              <div class="tts-form-row">
                <div class="tts-form-item slider-item">
                  <label>音调: {{ ttsRequest.pitch }}</label>
                  <input
                    type="range"
                    v-model="ttsRequest.pitch"
                    min="0"
                    max="100"
                    class="tts-slider"
                  >
                </div>
              </div>

              <div class="tts-form-row">
                <div class="tts-form-item slider-item">
                  <label>音量: {{ ttsRequest.volume }}</label>
                  <input
                    type="range"
                    v-model="ttsRequest.volume"
                    min="0"
                    max="100"
                    class="tts-slider"
                  >
                </div>
              </div>

              <div class="tts-actions">
                <button
                  @click="sendTextToSpeech"
                  class="tts-send-btn"
                  :disabled="!rosConnected"
                >
                  发送语音
                </button>
              </div>

              <!-- 语音指令部分 -->
              <div class="tts-header" style="margin-top: 20px; border-top: 1px solid #eee; padding-top: 10px;">
                <h4>语音指令</h4>
              </div>
              <div class="tts-content">
                <div class="tts-form-item">
                  <label>选择指令:</label>
                  <select v-model="selectedVoiceWord" class="tts-select">
                    <option v-for="opt in voiceWordOptions" :key="opt" :value="opt">{{ opt }}</option>
                  </select>
                </div>
                <div class="tts-actions">
                  <button
                    @click="publishVoiceWord"
                    class="tts-send-btn"
                    :disabled="!rosConnected"
                  >
                    播放语音
                  </button>
                </div>
              </div>

            </div>
          </div>
        </div>

        <!-- three.js 渲染容器 -->
        <div ref="threeContainer" class="three-container"></div>

        <!-- 实时位置信息覆盖层 -->
        <div v-if="robotPose" class="robot-info-overlay">
          <div class="info-item">
            <strong>位置:</strong> ({{ robotPose.x.toFixed(2) }}, {{ robotPose.y.toFixed(2) }})
          </div>
          <div class="info-item">
            <strong>朝向:</strong> {{ (robotPose.yaw * 180 / Math.PI).toFixed(1) }}°
          </div>
        </div>

        <!-- 保存位置列表 -->
        <div v-if="savedPositions.length > 0" class="saved-positions-list">
          <div class="list-header">
            <strong>已保存位置</strong>
          </div>
          <div
            v-for="(position, index) in savedPositions"
            :key="position.id || index"
            class="saved-position-item"
          >
            <div class="position-name">{{ position.name }}</div>
            <div class="position-coords">
              ({{ position.x.toFixed(2) }}, {{ position.y.toFixed(2) }})
            </div>
            <div class="position-actions">
              <button @click="goToPosition(position)" class="action-btn goto-btn">
                🚀
              </button>
              <button @click="deleteSavedPosition(index)" class="action-btn delete-btn">
                🗑️
              </button>
            </div>
          </div>
        </div>

        <!-- 交互元素 -->
        <div
          v-if="selectedPoint"
          class="selected-point"
          :style="{
            left: `${selectedPoint.screenX}px`,
            top: `${selectedPoint.screenY}px`
          }"
        >
          <div class="point-label">
            ({{ selectedPoint.worldX.toFixed(2) }}, {{ selectedPoint.worldY.toFixed(2) }})
          </div>
        </div>

        <!-- 初始化提示 -->
        <div
          v-if="initializationPrompt"
          class="initialization-prompt"
        >
          {{ initializationPrompt }}
        </div>

        <!-- 导航提示 -->
        <div
          v-if="navigationPrompt"
          class="navigation-prompt"
        >
          {{ navigationPrompt }}
        </div>
      </div>
    </div>

    <!-- 保存位置弹窗 -->
    <div v-if="showSaveDialog" class="dialog-overlay">
      <div class="dialog">
        <h3>保存当前位置</h3>
        <div class="dialog-content">
          <div class="position-info">
            <p><strong>当前位置:</strong> ({{ currentSavePosition.x.toFixed(2) }}, {{ currentSavePosition.y.toFixed(2) }})</p>
            <p><strong>当前朝向:</strong> {{ (currentSavePosition.yaw * 180 / Math.PI).toFixed(1) }}°</p>
          </div>
          <div class="form-group">
            <label for="positionName">位置名称:</label>
            <input
              id="positionName"
              type="text"
              v-model="savePositionName"
              placeholder="请输入位置名称"
              @keyup.enter="savePosition"
            >
          </div>
        </div>
        <div class="dialog-actions">
          <button @click="closeSavePositionDialog">取消</button>
          <button @click="savePosition" class="primary">保存</button>
        </div>
      </div>
    </div>
    <!-- 键盘控制小窗 -->
    <div v-if="showKeyboardPanel" class="keyboard-control-panel">
      <div class="keyboard-header">
        <h5>键盘控制</h5>
        <button class="close-btn" @click="toggleKeyboardControl">×</button>
      </div>
      <div class="keyboard-content">
        <div class="arrow-row">
          <div class="arrow-key up" :class="{ active: keysPressed['w'] || keysPressed['arrowup'] }">↑</div>
        </div>
        <div class="arrow-row">
          <div class="arrow-key left" :class="{ active: keysPressed['a'] || keysPressed['arrowleft'] }">←</div>
          <div class="arrow-key down" :class="{ active: keysPressed['s'] || keysPressed['arrowdown'] }">↓</div>
          <div class="arrow-key right" :class="{ active: keysPressed['d'] || keysPressed['arrowright'] }">→</div>
        </div>
        <div class="keyboard-tip">
          <p>WASD 或 方向键控制</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import { sendTextToSpeech } from '@/api/ros2/tts'
export default {
  name: 'AdvancedMapViewer',
  data() {
    return {
      // 配置
      config: {
        rosBridgeUrl: 'ws://10.188.232.82:9090',
        topics: {
          map: '/map',
          pose: '/amcl_pose',
          goal: '/goal_pose',
          path: '/plan',
          cmd_vel: '/cmd_vel'
        }
      },

      // 状态
      rosConnected: false,
      rosStatusText: '未连接',
      rosStatusClass: 'disconnected',

      // ROS 对象
      ROSLIB: null,
      ros: null,
      mapSubscriber: null,
      poseSubscriber: null, // 位姿订阅
      pathSubscriber: null,
      pathMesh: null, // 3D路径对象

      // 地图相关
      currentMap: null,
      occupancyRate: 0,

      // 视图参数
      zoomLevel: 1.0,
      offsetX: 0,
      offsetY: 0,
      showGrid: true,
      showRobot: true,
      autoCenter: false,

      // 机器人位姿
      robotPose: null,

      // 工具选择
      currentTool: 'pan',
      selectedPoint: null,

      // 鼠标状态
      isDragging: false,
      lastMouseX: 0,
      lastMouseY: 0,

      // 初始化模式
      isInitializationMode: false,
      initializationPrompt: '',

      // 导航模式
      isNavigationMode: false,
      navigationPrompt: '',
      isSettingOrientation: false,
      targetPoint: null,
      orientationStartPoint: null,
      orientationEndPoint: null,
      navigationArrow: null,

      // 保存位置功能
      showSaveDialog: false,
      currentSavePosition: { x: 0, y: 0, yaw: 0 },
      savePositionName: '',
      savedPositions: [],
      savedPositionMeshes: [], // 存储3D场景中的位置标记,

      // three.js 相关
      scene: null,
      camera: null,
      renderer: null,
      mapMesh: null,
      robotMesh: null,
      gridHelper: null,
      animationId: null,
      raycaster: null,

      // 语音合成相关
      showTTSPanel: false,
      ttsRequest: {
        text: '你好，这是一个文本转语音测试',
        language: 'zh',
        speed: 50,
        pitch: 50,
        volume: 50
      },

      // 语音指令相关
      voiceCommandTopic: null,
      selectedVoiceWord: 'voice1',
      voiceWordOptions: ['voice1', 'voice2', 'voice3'],

      // 相机相关
      cameraEnabled: false,
      cameraImage: null,
      cameraTopic: '/camera/image_raw',
      cameraSubscriber: null,
      imageTransport: null,

      // 调整大小相关
      isResizing: false,
      startX: 0,
      startY: 0,
      startWidth: 0,
      startHeight: 0,

      // 键盘控制相关
      enableKeyboardControl: false,
      showKeyboardPanel: false, // 键盘控制面板显示状态
      cmdVelTopic: null,
      keysPressed: {},
      velocityLoop: null,
      linearSpeed: 0.2,
      angularSpeed: 0.5,
      lastCmdTime: 0
    }
  },
  mounted() {
    this.initialize()
    // 添加调整大小的事件监听器
    document.addEventListener('mousemove', this.handleResize)
    document.addEventListener('mouseup', this.stopResize)

    // 添加键盘事件监听
    window.addEventListener('keydown', this.handleKeyDown)
    window.addEventListener('keyup', this.handleKeyUp)
  },
  beforeDestroy() {
    this.disconnectROS()
    // 移除调整大小的事件监听器
    document.removeEventListener('mousemove', this.handleResize)
    document.removeEventListener('mouseup', this.stopResize)

    // 移除键盘事件监听
    window.removeEventListener('keydown', this.handleKeyDown)
    window.removeEventListener('keyup', this.handleKeyUp)

    // 清理速度循环
    if (this.velocityLoop) {
      clearInterval(this.velocityLoop)
      this.velocityLoop = null
    }
  },
  methods: {
    // 初始化
    initialize() {
      this.setupThreeJS()
      this.setupEventListeners()
      this.loadSavedPositions() // 加载保存的位置
    },

    // 从后端加载保存的位置
    loadSavedPositions() {
      request({
        url: '/ros2/point/list',
        method: 'get'
      }).then(response => {
        // 因为request已经处理了response.data，所以直接使用response
        if (response.code === 200) {
          // 将后端返回的数据转换为前端需要的格式
          this.savedPositions = response.data.map(point => ({
            id: point.id,
            name: point.name,
            x: point.xPos,
            y: point.yPos,
            yaw: point.yaw,
            timestamp: new Date().toISOString()
          }))
          // 渲染保存的位置
          this.renderSavedPositions()
        }
      }).catch(error => {
        console.error('加载保存位置失败:', error)
      })
    },

    // 设置 three.js
    setupThreeJS() {
      const container = this.$refs.threeContainer
      if (!container) return

      // 导入 three.js 和轨道控制器
      import('three').then(THREE => {
        // 创建场景
        this.scene = new THREE.Scene()
        this.scene.background = new THREE.Color(0xf0f0f0)

        // 创建相机
        const width = container.clientWidth
        const height = container.clientHeight
        this.camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 1000)
        this.camera.position.set(0, 10, 10) // 初始位置：从上方俯视
        this.camera.lookAt(0, 0, 0)

        // 创建渲染器
        this.renderer = new THREE.WebGLRenderer({ antialias: true })
        this.renderer.setSize(width, height)
        this.renderer.setPixelRatio(window.devicePixelRatio)
        container.appendChild(this.renderer.domElement)

        // 添加灯光
        // 增强环境光，使整体更明亮
        const ambientLight = new THREE.AmbientLight(0xffffff, 3)
        this.scene.add(ambientLight)

        // 增强平行光强度并调整位置
        const directionalLight = new THREE.DirectionalLight(0xffffff, 2.5)
        directionalLight.position.set(10, 20, 10)
        this.scene.add(directionalLight)

        // 添加第二盏平行光，减少阴影
        const directionalLight2 = new THREE.DirectionalLight(0xffffff, 1.5)
        directionalLight2.position.set(-10, 15, -10)
        this.scene.add(directionalLight2)

        // 创建网格辅助线
        this.gridHelper = new THREE.GridHelper(20, 20, 0x888888, 0x444444)
        this.scene.add(this.gridHelper)

        // 创建 raycaster 用于点击检测
        this.raycaster = new THREE.Raycaster()

        // 导入并创建轨道控制器
        import('three/examples/jsm/controls/OrbitControls').then(({ OrbitControls }) => {
          this.controls = new OrbitControls(this.camera, this.renderer.domElement)
          this.controls.enableDamping = true // 启用阻尼效果，使控制更平滑
          this.controls.dampingFactor = 0.05 // 阻尼系数
          this.controls.screenSpacePanning = false // 禁用屏幕空间平移
          this.controls.maxPolarAngle = Math.PI / 2 // 最大极角，限制不能从下方看
          this.controls.minDistance = 1 // 最小距离
          this.controls.maxDistance = 50 // 最大距离

          // 开始渲染循环
          this.animate()
        }).catch(error => {
          console.error('Failed to load OrbitControls:', error)
          this.animate() // 加载失败时仍启动渲染循环
        })
      }).catch(error => {
        console.error('Failed to load THREE:', error)
      })
    },

    // 渲染循环
    animate() {
      this.animationId = requestAnimationFrame(this.animate.bind(this))

      // 更新轨道控制器
      if (this.controls) {
        this.controls.update()
      }

      this.renderer.render(this.scene, this.camera)
    },

    // 事件监听
    setupEventListeners() {
      // 注意：轨道控制器会自动处理鼠标事件，所以不需要手动添加
      // 只保留 wheel 事件用于自定义缩放逻辑
      const container = this.$refs.mapContainer
      if (!container) return

      // 移除原有鼠标事件监听器，避免与轨道控制器冲突
      // container.addEventListener('wheel', this.handleWheel)
      // container.addEventListener('mousedown', this.handleMouseDown)
      // container.addEventListener('mousemove', this.handleMouseMove)
      // container.addEventListener('mouseup', this.handleMouseUp)

      // 添加点击事件监听，用于初始化位置
      container.addEventListener('click', this.handleMapClick)
    },

    // 处理地图点击事件
    handleMapClick(event) {
      if (!this.renderer || !this.raycaster || !this.camera || !this.mapMesh) {
        return
      }

      // 获取 canvas 尺寸
      const canvas = this.renderer.domElement
      const rect = canvas.getBoundingClientRect()

      // 计算鼠标在 canvas 中的归一化设备坐标
      const mouse = {
        x: ((event.clientX - rect.left) / canvas.clientWidth) * 2 - 1,
        y: -((event.clientY - rect.top) / canvas.clientHeight) * 2 + 1
      }

      // 设置 raycaster
      this.raycaster.setFromCamera(mouse, this.camera)

      // 检测与地图网格的交点
      const intersects = this.raycaster.intersectObject(this.mapMesh)

      if (intersects.length > 0) {
        const intersection = intersects[0]

        if (this.isInitializationMode) {
          this.onMapClick(intersection.point)
        } else if (this.isNavigationMode) {
          this.onNavigationClick(event, intersection.point)
        }
      }
    },

    // 处理导航点击
    onNavigationClick(event, point) {
      if (!this.isSettingOrientation) {
        // 第一次点击：设置目标点
        this.targetPoint = point.clone()
        this.orientationStartPoint = point.clone()
        this.orientationEndPoint = point.clone()
        this.isSettingOrientation = true

        // 更新提示
        this.navigationPrompt = '请拖动设置机器人朝向'

        // 添加鼠标移动和释放事件监听
        const canvas = this.renderer.domElement
        canvas.addEventListener('mousemove', this.handleOrientationDrag)
        canvas.addEventListener('mouseup', this.handleOrientationRelease)

        // 创建初始导航箭头
        this.createNavigationArrow(this.targetPoint, this.orientationEndPoint)
      }
    },

    // 连接处理
    handleConnection() {
      if (this.rosConnected) {
        this.disconnectROS()
      } else {
        this.connectROS()
      }
    },

    // 连接 ROS2
    connectROS() {
      try {
        // 动态导入 ROSLIB
        import('roslib').then(ROSLIB => {
          this.ROSLIB = ROSLIB
          this.ros = new ROSLIB.Ros({ url: this.config.rosBridgeUrl })

          this.ros.on('connection', () => {
            this.rosConnected = true
            this.updateStatus('已连接', 'connected')
            this.setupSubscribers()
          })

          this.ros.on('error', (error) => {
            console.error('ROS错误:', error)
            this.updateStatus('连接错误', 'error')
          })

          this.ros.on('close', () => {
            this.rosConnected = false
            this.updateStatus('已断开', 'disconnected')
            this.cleanupCamera() // 清理相机资源
          })
        }).catch(error => {
          console.error('Failed to load ROSLIB:', error)
          this.updateStatus('依赖错误', 'error')
        })
      } catch (error) {
        console.error('连接失败:', error)
        this.updateStatus('连接失败', 'error')
      }
    },

    // 更新状态
    updateStatus(text, className) {
      this.rosStatusText = text
      this.rosStatusClass = className
    },

    // 设置订阅者
    setupSubscribers() {
      if (!this.ros || !this.ROSLIB) return

      // 地图订阅
      this.mapSubscriber = new this.ROSLIB.Topic({
        ros: this.ros,
        name: this.config.topics.map,
        messageType: 'nav_msgs/OccupancyGrid'
      })

      this.mapSubscriber.subscribe(this.handleMapMessage.bind(this))

      // 位姿订阅
      this.poseSubscriber = new this.ROSLIB.Topic({
        ros: this.ros,
        name: this.config.topics.pose,
        messageType: 'geometry_msgs/PoseWithCovarianceStamped'
      })

      this.poseSubscriber.subscribe(this.handlePoseMessage.bind(this))

      // 路径订阅（可选）
      this.pathSubscriber = new this.ROSLIB.Topic({
        ros: this.ros,
        name: this.config.topics.path,
        messageType: 'nav_msgs/Path'
      })

      this.pathSubscriber.subscribe(this.handlePathMessage.bind(this))

      // 初始化速度控制发布者
      this.cmdVelTopic = new this.ROSLIB.Topic({
        ros: this.ros,
        name: this.config.topics.cmd_vel,
        messageType: 'geometry_msgs/Twist'
      })

      // 初始化语音指令发布者
      this.voiceCommandTopic = new this.ROSLIB.Topic({
        ros: this.ros,
        name: '/voice_words',
        messageType: 'std_msgs/String'
      })
    },

    // 处理地图消息
    handleMapMessage(message) {
      const mapData = {
        width: message.info.width,
        height: message.info.height,
        resolution: message.info.resolution,
        origin: {
          x: message.info.origin.position.x,
          y: message.info.origin.position.y
        },
        data: message.data,
        timestamp: Date.now()
      }

      this.currentMap = mapData
      this.calculateOccupancyRate(message.data)
      this.renderMap()

      if (this.autoCenter) {
        this.centerMap()
      }
    },

    // 计算占用率
    calculateOccupancyRate(data) {
      let occupied = 0
      let total = 0

      data.forEach(value => {
        if (value === 100) occupied++
        if (value !== -1) total++
      })

      this.occupancyRate = total > 0 ? ((occupied / total) * 100).toFixed(1) : 0
    },

    // 处理位姿消息
    handlePoseMessage(message) {
      const pose = message.pose.pose
      const orientation = pose.orientation

      // 计算偏航角
      const yaw = Math.atan2(
        2.0 * (orientation.w * orientation.z + orientation.x * orientation.y),
        1.0 - 2.0 * (orientation.y * orientation.y + orientation.z * orientation.z)
      )

      this.robotPose = {
        x: pose.position.x,
        y: pose.position.y,
        yaw: yaw + Math.PI / 2 // 调整朝向，从朝右改为朝上
      }

      this.updateRobotPose()
    },

    // 处理路径消息
    handlePathMessage(message) {
      if (!this.currentMap || !this.scene) return

      // 提取路径点
      const poses = message.poses || []
      if (poses.length === 0) {
        // 如果路径为空，清除现有路径
        this.clearPath()
        return
      }

      this.renderPath(poses)
    },

    // 渲染路径
    renderPath(poses) {
      import('three').then(THREE => {
        // 清除旧路径
        this.clearPath()

        if (!this.currentMap) return

        const map = this.currentMap
        const mapWidth = map.width * map.resolution
        const mapHeight = map.height * map.resolution
        const mapOriginX = map.origin.x
        const mapOriginY = map.origin.y

        // 创建路径点数组
        const points = []

        poses.forEach(poseMsg => {
          const pose = poseMsg.pose.position

          // 坐标转换
          const robotMapX = pose.x - mapOriginX
          const robotMapY = pose.y - mapOriginY
          const threeX = robotMapX - mapWidth / 2
          const threeZ = (mapHeight - robotMapY) - mapHeight / 2

          // 路径稍微抬高一点，避免与地图重叠
          points.push(new THREE.Vector3(threeX, 0.05, threeZ))
        })

        // 创建路径几何体
        // 使用 CatmullRomCurve3 创建平滑曲线
        const curve = new THREE.CatmullRomCurve3(points)

        // 增加分段数使曲线更圆滑
        const pointsCount = Math.max(points.length * 5, 50)
        const smoothPoints = curve.getPoints(pointsCount)

        const geometry = new THREE.BufferGeometry().setFromPoints(smoothPoints)

        // 创建材质
        const material = new THREE.LineBasicMaterial({
          color: 0x00ff00, // 绿色路径
          linewidth: 3, // WebGL限制，可能在某些浏览器无效
          transparent: true,
          opacity: 0.8
        })

        // 创建线条网格
        this.pathMesh = new THREE.Line(geometry, material)

        // 添加到场景
        this.scene.add(this.pathMesh)

      }).catch(error => {
        console.error('Failed to render path:', error)
      })
    },

    // 清除路径
    clearPath() {
      if (this.pathMesh && this.scene) {
        this.scene.remove(this.pathMesh)
        if (this.pathMesh.geometry) this.pathMesh.geometry.dispose()
        if (this.pathMesh.material) this.pathMesh.material.dispose()
        this.pathMesh = null
      }
    },

    // 渲染地图
    renderMap() {
      if (!this.currentMap || !this.scene) return

      // 导入 three.js
      import('three').then(THREE => {
        // 如果已有地图网格，先移除
        if (this.mapMesh) {
          this.scene.remove(this.mapMesh)
          this.mapMesh.geometry.dispose()
          this.mapMesh.material.dispose()
          this.mapMesh = null
        }

        const map = this.currentMap
        const width = map.width
        const height = map.height
        const resolution = map.resolution

        // 创建几何体
        const geometry = new THREE.PlaneGeometry(
          width * resolution,
          height * resolution,
          width - 1,
          height - 1
        )

        // 创建顶点颜色数组
        const colors = []
        const vertices = geometry.attributes.position.array

        // 遍历地图数据，设置每个顶点的颜色（翻转y轴）
        for (let y = height - 1; y >= 0; y--) {
          for (let x = 0; x < width; x++) {
            const index = y * width + x
            const value = map.data[index]

            let color
            if (value === -1) {
              color = new THREE.Color(0x808080) // 未知
            } else if (value === 0) {
              color = new THREE.Color(0xFFFFFF) // 空闲
            } else if (value === 100) {
              color = new THREE.Color(0x000000) // 占用
            } else {
              const intensity = value / 100
              color = new THREE.Color(1 - intensity, 1 - intensity, 1 - intensity) // 渐变占用
            }

            // 为每个顶点设置颜色
            colors.push(color.r, color.g, color.b)
          }
        }

        // 设置几何体的颜色属性
        geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3))

        // 创建材质
        const material = new THREE.MeshStandardMaterial({
          vertexColors: true,
          side: THREE.DoubleSide,
          roughness: 0.8,
          metalness: 0.2
        })

        // 创建网格
        this.mapMesh = new THREE.Mesh(geometry, material)

        // 设置地图位置和旋转
        this.mapMesh.rotation.x = -Math.PI / 2 // 平面旋转为水平
        this.mapMesh.position.set(0, 0, 0) // 地图中心位于原点

        // 添加到场景
        this.scene.add(this.mapMesh)

        // 更新机器人位置
        if (this.robotPose) {
          this.updateRobotPose()
        }

        // 渲染保存的位置
        this.renderSavedPositions()
      }).catch(error => {
        console.error('Failed to render 3D map:', error)
      })
    },

    // 更新机器人位姿
    updateRobotPose() {
      if (!this.robotPose || !this.scene || !this.currentMap) return

      // 导入 three.js
      import('three').then(THREE => {
        // 如果已有机器人模型，先移除并释放资源
        if (this.robotMesh) {
          this.scene.remove(this.robotMesh)

          // 释放组内所有子对象的资源
          this.robotMesh.traverse((child) => {
            if (child.geometry) {
              child.geometry.dispose()
            }
            if (child.material) {
              if (Array.isArray(child.material)) {
                child.material.forEach(material => material.dispose())
              } else {
                child.material.dispose()
              }
            }
          })

          this.robotMesh = null
        }

        const pose = this.robotPose
        const map = this.currentMap

        // 获取地图信息
        const mapWidth = map.width * map.resolution
        const mapHeight = map.height * map.resolution
        const mapOriginX = map.origin.x
        const mapOriginY = map.origin.y

        // 1. 计算机器人在地图坐标系中的相对位置（相对于地图左下角）
        const robotMapX = pose.x - mapOriginX
        const robotMapY = pose.y - mapOriginY

        // 2. 转换到three.js坐标系
        // three.js地图中心在(0, 0, 0)
        // 地图左下角在(-mapWidth/2, 0, -mapHeight/2)
        // 需要考虑地图翻转（因为渲染时y轴是倒序遍历的）
        const threeX = robotMapX - mapWidth / 2
        const threeZ = (mapHeight - robotMapY) - mapHeight / 2

        // 创建AGV机器人组
        this.robotMesh = new THREE.Group()

        // 1. AGV主体（长方体）- 缩小到1/3
        const bodyGeometry = new THREE.BoxGeometry(0.2, 0.07, 0.27) // 长x宽x高
        const bodyMaterial = new THREE.MeshStandardMaterial({
          color: 0xFF3333, // 红色主体
          metalness: 0.3,
          roughness: 0.4
        })
        const body = new THREE.Mesh(bodyGeometry, bodyMaterial)
        body.position.y = 0.035 // 主体高度
        this.robotMesh.add(body)

        // 2. AGV顶部平台 - 缩小到1/3
        const topGeometry = new THREE.BoxGeometry(0.17, 0.02, 0.23)
        const topMaterial = new THREE.MeshStandardMaterial({
          color: 0x333333,
          metalness: 0.5,
          roughness: 0.3
        })
        const top = new THREE.Mesh(topGeometry, topMaterial)
        top.position.y = 0.08 // 主体顶部上方
        this.robotMesh.add(top)

        // 3. 轮子 - 缩小到1/3
        const wheelGeometry = new THREE.CylinderGeometry(0.027, 0.027, 0.05, 16)
        const wheelMaterial = new THREE.MeshStandardMaterial({
          color: 0x222222,
          metalness: 0.7,
          roughness: 0.2
        })

        // 前轮
        const frontLeftWheel = new THREE.Mesh(wheelGeometry, wheelMaterial)
        frontLeftWheel.position.set(-0.1, 0.025, 0.115)
        frontLeftWheel.rotation.z = Math.PI / 2
        this.robotMesh.add(frontLeftWheel)

        const frontRightWheel = new THREE.Mesh(wheelGeometry, wheelMaterial)
        frontRightWheel.position.set(0.1, 0.025, 0.115)
        frontRightWheel.rotation.z = Math.PI / 2
        this.robotMesh.add(frontRightWheel)

        // 后轮
        const backLeftWheel = new THREE.Mesh(wheelGeometry, wheelMaterial)
        backLeftWheel.position.set(-0.1, 0.025, -0.115)
        backLeftWheel.rotation.z = Math.PI / 2
        this.robotMesh.add(backLeftWheel)

        const backRightWheel = new THREE.Mesh(wheelGeometry, wheelMaterial)
        backRightWheel.position.set(0.1, 0.025, -0.115)
        backRightWheel.rotation.z = Math.PI / 2
        this.robotMesh.add(backRightWheel)

        // 4. 顶部指示灯 - 缩小到1/3
        const lightGeometry = new THREE.CylinderGeometry(0.017, 0.017, 0.033, 8)
        const lightMaterial = new THREE.MeshStandardMaterial({
          color: 0x00FFFF,
          emissive: 0x00FFFF,
          emissiveIntensity: 0.5
        })
        const frontLight = new THREE.Mesh(lightGeometry, lightMaterial)
        frontLight.position.set(0, 0.095, 0.135)
        frontLight.rotation.x = Math.PI / 2
        this.robotMesh.add(frontLight)

        // 5. 朝向指示器 - 缩小到1/3
        const arrowGeometry = new THREE.ConeGeometry(0.05, 0.2, 8)
        const arrowMaterial = new THREE.MeshStandardMaterial({
          color: 0x00FF00,
          emissive: 0x00FF00,
          emissiveIntensity: 0.3
        })
        const arrow = new THREE.Mesh(arrowGeometry, arrowMaterial)
        arrow.position.set(0, 0.135, 0)
        arrow.rotation.x = Math.PI / 2
        this.robotMesh.add(arrow)

        // 设置机器人位置和朝向
        this.robotMesh.position.set(threeX, 0, threeZ) // y轴为地面高度
        this.robotMesh.rotation.y = pose.yaw

        // 添加到场景
        this.scene.add(this.robotMesh)
      }).catch(error => {
        console.error('Failed to update robot pose:', error)
      })
    },

    // 渲染保存的位置标记
    renderSavedPositions() {
      if (!this.currentMap || !this.scene) return

      import('three').then(THREE => {
        // 清除旧标记
        if (this.savedPositionMeshes && this.savedPositionMeshes.length > 0) {
          this.savedPositionMeshes.forEach(mesh => {
            this.scene.remove(mesh)
            // 递归释放资源
            mesh.traverse((child) => {
              if (child.geometry) child.geometry.dispose()
              if (child.material) {
                if (Array.isArray(child.material)) {
                  child.material.forEach(m => {
                    if (m.map) m.map.dispose()
                    m.dispose()
                  })
                } else {
                  if (child.material.map) child.material.map.dispose()
                  child.material.dispose()
                }
              }
            })
          })
        }
        this.savedPositionMeshes = []

        const map = this.currentMap
        const mapWidth = map.width * map.resolution
        const mapHeight = map.height * map.resolution
        const mapOriginX = map.origin.x
        const mapOriginY = map.origin.y

        this.savedPositions.forEach(pos => {
          // 坐标转换
          const robotMapX = pos.x - mapOriginX
          const robotMapY = pos.y - mapOriginY
          const threeX = robotMapX - mapWidth / 2
          const threeZ = (mapHeight - robotMapY) - mapHeight / 2

          // 创建标记组
          const group = new THREE.Group()

          // 1. 杆子
          const poleGeometry = new THREE.CylinderGeometry(0.02, 0.02, 0.5, 8)
          const poleMaterial = new THREE.MeshStandardMaterial({ color: 0xFFFFFF })
          const pole = new THREE.Mesh(poleGeometry, poleMaterial)
          pole.position.y = 0.25
          group.add(pole)

          // 2. 顶部标志（菱形/八面体）
          const headGeometry = new THREE.OctahedronGeometry(0.15)
          const headMaterial = new THREE.MeshStandardMaterial({
            color: 0xe67e22, // 橙色
            metalness: 0.3,
            roughness: 0.4
          })
          const head = new THREE.Mesh(headGeometry, headMaterial)
          head.position.y = 0.5
          group.add(head)

          // 3. 底部底座（小圆盘）
          const baseGeometry = new THREE.CircleGeometry(0.1, 16)
          const baseMaterial = new THREE.MeshBasicMaterial({
            color: 0xe67e22,
            transparent: true,
            opacity: 0.5,
            side: THREE.DoubleSide
          })
          const base = new THREE.Mesh(baseGeometry, baseMaterial)
          base.rotation.x = -Math.PI / 2
          base.position.y = 0.01 // 略微高于地面
          group.add(base)

          // 4. 文字标签
          if (pos.name) {
            const canvas = document.createElement('canvas')
            const context = canvas.getContext('2d')
            const fontSize = 24
            const font = `bold ${fontSize}px Arial`
            context.font = font
            const textMetrics = context.measureText(pos.name)
            const textWidth = textMetrics.width

            // 设置画布大小（添加内边距）
            canvas.width = textWidth + 20
            canvas.height = fontSize + 16

            // 绘制背景
            context.fillStyle = 'rgba(0, 0, 0, 0.6)'
            // 圆角矩形背景
            context.beginPath()
            context.roundRect(0, 0, canvas.width, canvas.height, 6)
            context.fill()

            // 绘制文字
            context.font = font
            context.fillStyle = '#ffffff'
            context.textAlign = 'center'
            context.textBaseline = 'middle'
            context.fillText(pos.name, canvas.width / 2, canvas.height / 2)

            // 创建纹理和精灵
            const texture = new THREE.CanvasTexture(canvas)
            const spriteMaterial = new THREE.SpriteMaterial({
              map: texture,
              transparent: true
            })
            const sprite = new THREE.Sprite(spriteMaterial)

            // 调整精灵大小和位置
            const scale = 0.007 // 缩小字体大小 (0.02 -> 0.007)
            sprite.scale.set(canvas.width * scale, canvas.height * scale, 1)
            sprite.position.y = 0.75 // 稍微降低高度，使其更贴近标记
            group.add(sprite)
          }

          // 设置位置
          group.position.set(threeX, 0, threeZ)

          // 添加到场景
          this.scene.add(group)
          this.savedPositionMeshes.push(group)
        })
      }).catch(error => {
        console.error('Failed to render saved positions:', error)
      })
    },

    // 工具方法
    zoomIn() {
      if (!this.camera) return
      // 相机向前移动（放大）
      this.camera.position.y *= 0.9
      this.camera.position.z *= 0.9
      this.zoomLevel *= 1.2
    },

    zoomOut() {
      if (!this.camera) return
      // 相机向后移动（缩小）
      this.camera.position.y *= 1.1
      this.camera.position.z *= 1.1
      this.zoomLevel /= 1.2
    },

    centerOnRobot() {
      if (!this.robotPose || !this.camera) return

      const pose = this.robotPose
      // 相机看向机器人位置
      this.camera.lookAt(pose.x, 0, pose.y)
    },

    centerMap() {
      if (!this.camera) return
      // 相机看向地图中心
      this.camera.lookAt(0, 0, 0)
    },

    setTool(tool) {
      this.currentTool = tool
    },

    exportMap() {
      if (!this.renderer) return

      const link = document.createElement('a')
      link.download = `map_${Date.now()}.png`
      link.href = this.renderer.domElement.toDataURL('image/png')
      link.click()
    },

    // 初始化位置模式切换
    toggleInitializationMode() {
      this.isInitializationMode = !this.isInitializationMode
      this.isNavigationMode = false // 确保不同时处于两种模式

      if (this.isInitializationMode) {
        this.initializationPrompt = '请点击地图上的位置进行初始化'
        this.navigationPrompt = ''
        // 初始化 raycaster
        if (!this.raycaster) {
          import('three').then(THREE => {
            this.raycaster = new THREE.Raycaster()
          })
        }
      } else {
        this.initializationPrompt = ''
      }
    },

    // 导航目标模式切换
    toggleNavigationMode() {
      this.isNavigationMode = !this.isNavigationMode
      this.isInitializationMode = false // 确保不同时处于两种模式

      if (this.isNavigationMode) {
        this.navigationPrompt = '请点击地图上的位置，然后拖动设置朝向'
        this.initializationPrompt = ''
        this.isSettingOrientation = false
        this.targetPoint = null
        this.orientationStartPoint = null
        this.orientationEndPoint = null
        // 移除之前的导航箭头
        this.removeNavigationArrow()
      } else {
        this.navigationPrompt = ''
        this.isSettingOrientation = false
        this.removeNavigationArrow()
      }
    },

    // 处理方向拖动
    handleOrientationDrag(event) {
      if (!this.isNavigationMode || !this.isSettingOrientation || !this.targetPoint || !this.renderer || !this.raycaster || !this.camera || !this.mapMesh) {
        return
      }

      event.preventDefault()

      // 获取 canvas 尺寸
      const canvas = this.renderer.domElement
      const rect = canvas.getBoundingClientRect()

      // 计算鼠标在 canvas 中的归一化设备坐标
      const mouse = {
        x: ((event.clientX - rect.left) / canvas.clientWidth) * 2 - 1,
        y: -((event.clientY - rect.top) / canvas.clientHeight) * 2 + 1
      }

      // 设置 raycaster
      this.raycaster.setFromCamera(mouse, this.camera)

      // 检测与地图网格的交点
      const intersects = this.raycaster.intersectObject(this.mapMesh)

      if (intersects.length > 0) {
        // 更新结束点
        this.orientationEndPoint = intersects[0].point.clone()

        // 更新导航箭头
        this.createNavigationArrow(this.targetPoint, this.orientationEndPoint)
      }
    },

    // 处理方向释放
    handleOrientationRelease(event) {
      if (!this.isNavigationMode || !this.isSettingOrientation) {
        return
      }

      // 移除事件监听
      const canvas = this.renderer.domElement
      canvas.removeEventListener('mousemove', this.handleOrientationDrag)
      canvas.removeEventListener('mouseup', this.handleOrientationRelease)

      // 计算最终朝向
      const deltaX = this.orientationEndPoint.x - this.orientationStartPoint.x
      const deltaZ = this.orientationEndPoint.z - this.orientationStartPoint.z
      const yaw = Math.atan2(deltaZ, deltaX)

      // 转换为 ROS 坐标并发布目标位姿
      this.publishNavigationGoal(this.targetPoint, yaw)

      // 退出设置朝向模式和导航模式
      this.isSettingOrientation = false
      this.isNavigationMode = false
      this.navigationPrompt = ''

      // 保持导航箭头显示
      // 注意：导航箭头会在下次进入导航模式时移除
    },

    // 鼠标事件处理
    handleWheel(event) {
      event.preventDefault()
      const delta = event.deltaY > 0 ? 1.1 : 0.9

      if (this.camera) {
        // 相机沿视线方向移动（缩放）
        this.camera.position.y *= delta
        this.camera.position.z *= delta
        this.zoomLevel *= delta > 1 ? 0.9 : 1.1
      }
    },

    handleMouseDown(event) {
      if (this.currentTool === 'pan') {
        this.isDragging = true
        this.lastMouseX = event.clientX
        this.lastMouseY = event.clientY
      }
    },

    handleMouseMove(event) {
      if (this.isDragging && this.currentTool === 'pan' && this.camera) {
        const dx = event.clientX - this.lastMouseX
        const dy = event.clientY - this.lastMouseY

        // 计算旋转量
        const rotationSpeed = 0.01

        // 保存当前相机位置
        const cameraPosition = this.camera.position.clone()
        const cameraTarget = new THREE.Vector3(0, 0, 0)

        // 计算相机到目标点的距离
        const distance = cameraPosition.distanceTo(cameraTarget)

        // 创建旋转矩阵
        const rotationMatrix = new THREE.Matrix4()
        rotationMatrix.makeRotationY(-dx * rotationSpeed)

        // 旋转相机位置
        const offset = cameraPosition.clone().sub(cameraTarget)
        offset.applyMatrix4(rotationMatrix)
        this.camera.position.copy(cameraTarget.clone().add(offset))

        // 相机看向目标点
        this.camera.lookAt(cameraTarget)

        this.lastMouseX = event.clientX
        this.lastMouseY = event.clientY
      }
    },

    handleMouseUp() {
      this.isDragging = false
    },

    // 创建导航箭头
    createNavigationArrow(startPoint, endPoint) {
      import('three').then(THREE => {
        // 移除旧箭头
        this.removeNavigationArrow()

        // 计算方向向量
        const direction = new THREE.Vector3()
        direction.subVectors(endPoint, startPoint)
        const length = direction.length()

        // 创建箭头几何体
        const arrowGeometry = new THREE.ArrowHelper(
          direction.normalize(),
          startPoint,
          length,
          0x00ff00, // 绿色
          0.1, // 箭头头部长度
          0.05  // 箭头头部宽度
        )

        // 设置箭头位置和旋转
        this.navigationArrow = arrowGeometry

        // 添加到场景
        this.scene.add(this.navigationArrow)
      }).catch(error => {
        console.error('Failed to create navigation arrow:', error)
      })
    },

    // 处理地图点击
    onMapClick(point) {
      if (!this.currentMap || !this.ROSLIB || !this.ros) {
        return
      }

      const map = this.currentMap
      const mapWidth = map.width * map.resolution
      const mapHeight = map.height * map.resolution

      // 1. 将 three.js 坐标转换为地图局部坐标
      // three.js: map origin at center (0,0,0), x向右, z向前
      // map局部: origin at bottom-left, x向右, y向上
      const mapLocalX = point.x + mapWidth / 2
      const mapLocalY = mapHeight - (point.z + mapHeight / 2)

      // 2. 转换为 ROS 地图坐标（考虑地图原点偏移）
      const rosX = map.origin.x + mapLocalX
      const rosY = map.origin.y + mapLocalY

      // 3. 调用 Nav2 初始化位置服务
      this.callInitialPoseService(rosX, rosY)

      // 4. 退出初始化模式
      this.isInitializationMode = false
      this.initializationPrompt = ''
    },

    // 移除导航箭头
    removeNavigationArrow() {
      if (this.navigationArrow && this.scene) {
        this.scene.remove(this.navigationArrow)
        this.navigationArrow.geometry.dispose()
        this.navigationArrow.material.dispose()
        this.navigationArrow = null
      }
    },

    // 调用 Nav2 初始位置服务
    callInitialPoseService(x, y) {
      if (!this.ROSLIB || !this.ros) {
        console.error('ROS not connected')
        return
      }

      // 创建初始位姿消息
      const initialPose = {
        header: {
          frame_id: 'map',
          stamp: {
            secs: Math.floor(Date.now() / 1000),
            nsecs: (Date.now() % 1000) * 1000000
          }
        },
        pose: {
          pose: {
            position: {
              x: x,
              y: y,
              z: 0.0
            },
            orientation: {
              x: 0.0,
              y: 0.0,
              z: 0.0,
              w: 1.0
            }
          },
          covariance: [0.25, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.25, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.06853892326654787]
        }
      }

      // 发布到 /initialpose 话题
      const initialPoseTopic = new this.ROSLIB.Topic({
        ros: this.ros,
        name: '/initialpose',
        messageType: 'geometry_msgs/PoseWithCovarianceStamped'
      })

      initialPoseTopic.publish(initialPose)
      console.log('初始位置已发布:', { x, y })

      // 关闭话题连接
      initialPoseTopic.unadvertise()
    },

    // 发布导航目标
    publishNavigationGoal(threePoint, yaw) {
      if (!this.currentMap || !this.ROSLIB || !this.ros) {
        console.error('Map not loaded or ROS not connected')
        return
      }

      const map = this.currentMap
      const mapWidth = map.width * map.resolution
      const mapHeight = map.height * map.resolution

      // 1. 将 three.js 坐标转换为地图局部坐标
      // three.js: map origin at center (0,0,0), x向右, z向前
      // map局部: origin at bottom-left, x向右, y向上
      const mapLocalX = threePoint.x + mapWidth / 2
      const mapLocalY = mapHeight - (threePoint.z + mapHeight / 2)

      // 2. 转换为 ROS 地图坐标（考虑地图原点偏移）
      const rosX = map.origin.x + mapLocalX
      const rosY = map.origin.y + mapLocalY

      // 3. 将 yaw 转换为四元数
      // 注意：Nav2 使用的是 ROS 坐标系统，需要调整方向
      const qx = 0.0
      const qy = 0.0
      const qz = Math.sin(yaw / 2)
      const qw = Math.cos(yaw / 2)

      // 4. 创建目标位姿消息
      const goalPose = {
        header: {
          frame_id: 'map',
          stamp: {
            secs: Math.floor(Date.now() / 1000),
            nsecs: (Date.now() % 1000) * 1000000
          }
        },
        pose: {
          position: {
            x: rosX,
            y: rosY,
            z: 0.0
          },
          orientation: {
            x: qx,
            y: qy,
            z: qz,
            w: qw
          }
        }
      }

      // 5. 发布到 /goal_pose 话题
      const goalPoseTopic = new this.ROSLIB.Topic({
        ros: this.ros,
        name: this.config.topics.goal,
        messageType: 'geometry_msgs/PoseStamped'
      })

      goalPoseTopic.publish(goalPose)
      console.log('导航目标已发布:', { x: rosX, y: rosY, yaw: yaw * 180 / Math.PI })

      // 6. 关闭话题连接
      goalPoseTopic.unadvertise()

      // 7. 保持导航箭头显示
    },

    // 取消导航
    cancelNavigation() {
      if (!this.ROSLIB || !this.ros) {
        console.error('ROS not connected')
        return
      }

      // 创建取消目标消息
      const cancelMsg = {
        stamp: {
          secs: 0,
          nsecs: 0
        },
        id: '' // 空ID表示取消所有目标
      }

      // 发布到 /move_base/cancel 话题
      const cancelTopic = new this.ROSLIB.Topic({
        ros: this.ros,
        name: '/move_base/cancel',
        messageType: 'actionlib_msgs/GoalID'
      })

      cancelTopic.publish(cancelMsg)
      console.log('导航已取消')

      // 关闭话题连接
      cancelTopic.unadvertise()
    },

    // 打开保存位置弹窗
    openSavePositionDialog() {
      if (!this.robotPose) {
        console.error('No robot pose available')
        return
      }

      // 保存当前位置和朝向
      this.currentSavePosition = {
        x: this.robotPose.x,
        y: this.robotPose.y,
        yaw: this.robotPose.yaw
      }

      // 重置位置名称
      this.savePositionName = ''

      // 打开弹窗
      this.showSaveDialog = true
    },

    // 关闭保存位置弹窗
    closeSavePositionDialog() {
      this.showSaveDialog = false
      this.savePositionName = ''
    },

    // 保存当前位置
    savePosition() {
      if (!this.savePositionName.trim()) {
        alert('请输入位置名称')
        return
      }

      // 创建保存的位置对象
      const positionToSave = {
        id: Date.now(),
        name: this.savePositionName.trim(),
        x: this.currentSavePosition.x,
        y: this.currentSavePosition.y,
        yaw: this.currentSavePosition.yaw,
        timestamp: new Date().toISOString()
      }

      // 添加到保存位置列表
      this.savedPositions.push(positionToSave)

      // 立即更新3D显示
      this.renderSavedPositions()

      // 调用后端接口保存位置（这里需要替换为实际的API调用）
      this.callSavePositionAPI(positionToSave)

      // 关闭弹窗
      this.closeSavePositionDialog()
    },

    // 跳转到保存位置
    goToPosition(position) {
      // 使用已有的发布导航目标功能
      import('three').then(THREE => {
        const threePoint = new THREE.Vector3(
          position.x - (this.currentMap.origin.x + this.currentMap.width * this.currentMap.resolution / 2),
          0,
          (this.currentMap.origin.y + this.currentMap.height * this.currentMap.resolution) - position.y - this.currentMap.height * this.currentMap.resolution / 2
        )
        this.publishNavigationGoal(threePoint, position.yaw)
      })
    },

    // 删除保存位置
    deleteSavedPosition(index) {
      if (confirm('确定要删除这个位置吗？')) {
        // 从列表中删除
        const deletedPosition = this.savedPositions[index]
        this.savedPositions.splice(index, 1)

        // 立即更新3D显示
        this.renderSavedPositions()

        // 调用后端接口删除位置（这里需要替换为实际的API调用）
        this.callDeletePositionAPI(deletedPosition.id)
      }
    },

    // 调用后端保存位置接口
    callSavePositionAPI(position) {
      // 调用实际的后端API保存位置
      const nav2Point = {
        name: position.name,
        xPos: position.x,
        yPos: position.y,
        yaw: position.yaw
      }

      request({
        url: '/ros2/point/save',
        method: 'post',
        data: nav2Point
      }).then(response => {
        console.log('位置保存成功:', response)
        // 保存成功后，显示提示信息
        this.$message({
          message: '位置保存成功',
          type: 'success'
        })
        // 刷新保存的位置列表
        this.loadSavedPositions()
      }).catch(error => {
        console.error('位置保存失败:', error)
        // 保存失败后，显示错误信息
        this.$message({
          message: '位置保存失败',
          type: 'error'
        })
      })
    },

    // 调用后端删除位置接口
    callDeletePositionAPI(positionId) {
      // 调用实际的后端API删除位置
      request({
        url: `/ros2/point/delete/${positionId}`,
        method: 'delete'
      }).then(response => {
        console.log('位置删除成功:', response)
        // 删除成功后，显示提示信息
        this.$message({
          message: '位置删除成功',
          type: 'success'
        })
        // 刷新保存的位置列表，确保数据一致性
        this.loadSavedPositions()
      }).catch(error => {
        console.error('位置删除失败:', error)
        // 删除失败后，显示错误信息
        this.$message({
          message: '位置删除失败',
          type: 'error'
        })
        // 重新加载列表，恢复可能误删的项
        this.loadSavedPositions()
      })
    },

    // 断开连接
    disconnectROS() {
      if (this.mapSubscriber) {
        this.mapSubscriber.unsubscribe()
        this.mapSubscriber = null
      }

      if (this.poseSubscriber) {
        this.poseSubscriber.unsubscribe()
        this.poseSubscriber = null
      }

      if (this.pathSubscriber) {
        this.pathSubscriber.unsubscribe()
        this.pathSubscriber = null
      }

      this.clearPath() // 清除3D路径对象

      if (this.ros) {
        this.ros.close()
        this.ros = null
      }

      this.rosConnected = false
      this.updateStatus('已断开', 'disconnected')
    },

    // 切换语音合成面板显示/隐藏
    toggleTTSPanel() {
      this.showTTSPanel = !this.showTTSPanel
    },

    // 发送语音合成请求
    async sendTextToSpeech() {
      if (!this.rosConnected) {
        this.$message.warning('ROS未连接，请先连接ROS')
        return
      }

      if (!this.ttsRequest.text.trim()) {
        this.$message.warning('请输入要合成的文本')
        return
      }

      try {
        console.log('正在发送语音合成请求...')

        const response = await sendTextToSpeech(this.ttsRequest)

        if (response.code === 200) {
          this.$message.success('语音合成请求发送成功')
        } else {
          this.$message.error(response.msg || '语音合成请求发送失败')
        }
      } catch (error) {
        console.error('发送语音合成请求失败:', error)
        this.$message.error('发送语音合成请求失败，请检查ROS配置')
      }
    },

    // 发布语音指令
    publishVoiceWord() {
      if (!this.rosConnected || !this.voiceCommandTopic) {
        this.$message.warning('ROS未连接')
        return
      }

      const message = new this.ROSLIB.Message({
        data: this.selectedVoiceWord
      })

      this.voiceCommandTopic.publish(message)
      this.$message.success(`已发送语音指令: ${this.selectedVoiceWord}`)
    },

    // 初始化相机订阅
    setupCameraSubscriber() {
      if (!this.ros || !this.ROSLIB) {
        console.error('ROS connection or ROSLIB not available')
        return
      }

      try {
        console.log(`Setting up camera subscriber for topic: ${this.cameraTopic}`)

        // 直接使用已导入的 ROSLIB
        if (this.ROSLIB) {
          // 创建图像订阅者
          this.cameraSubscriber = new this.ROSLIB.Topic({
            ros: this.ros,
            name: this.cameraTopic,
            messageType: 'sensor_msgs/Image'
          })

          // 订阅相机图像
          this.cameraSubscriber.subscribe(this.handleCameraImage.bind(this))
          console.log(`✅ 已订阅相机话题: ${this.cameraTopic}`)
          console.log('订阅者信息:', {
            topic: this.cameraTopic,
            messageType: 'sensor_msgs/Image',
            subscriber: this.cameraSubscriber
          })
        } else {
          console.error('ROSLIB is not available')
        }
      } catch (error) {
        console.error('Failed to setup camera subscriber:', error)
        if (error.stack) {
          console.error(error.stack)
        }
      }
    },

    // 处理相机图像数据 - 优化延迟
    handleCameraImage(message) {
      if (!this.cameraEnabled) {
        return
      }

      try {
        // 检查消息数据是否存在
        if (!message.data) {
          return
        }

        // 直接使用简化的 Canvas 绘制方法，避免不必要的日志
        this.handleImageWithCanvas(message)

      } catch (error) {
        // 只在开发环境输出错误
        if (process.env.NODE_ENV === 'development') {
          console.error('Error processing camera image:', error)
        }
        // 显示错误信息
        this.showErrorImage(message, error)
      }
    },

    // 修复后的 Canvas 绘制，正确处理 ROS2 相机消息
    drawImageWithCanvas(message, binaryData) {
      try {
        const canvas = document.createElement('canvas')
        canvas.width = message.width || 640
        canvas.height = message.height || 480
        const ctx = canvas.getContext('2d')

        // 检查是否有有效数据
        let hasValidData = false
        if (binaryData && binaryData.length > 0) {
          // 快速检查：只检查前100个数据
          for (let i = 0; i < Math.min(100, binaryData.length); i++) {
            if (binaryData[i] !== 0) {
              hasValidData = true
              break
            }
          }
        }

        if (hasValidData) {
          // 尝试绘制像素数据
          try {
            const imageData = ctx.createImageData(canvas.width, canvas.height)
            const data = imageData.data

            // 使用 TypedArray 提高性能
            const uint8Data = new Uint8Array(binaryData)

            // 获取 ROS2 相机消息中的 step 字段（每一行的字节数）
            const step = message.step || (message.width * 3) // 默认每像素3字节
            const width = message.width || 640
            const height = message.height || 480

            console.log(`图像参数: width=${width}, height=${height}, step=${step}, encoding=${message.encoding}`)
            console.log(`数据长度: ${uint8Data.length}, 期望长度: ${height * step}`)

            // 按行处理图像数据，解决多幅画面问题
            for (let y = 0; y < height; y++) {
              for (let x = 0; x < width; x++) {
                const pixelIndex = (y * width + x) * 4 // Canvas 像素索引
                const dataOffset = y * step + x * 3 // 原始数据偏移量

                // 确保数据偏移量在有效范围内
                if (dataOffset + 2 < uint8Data.length && pixelIndex + 3 < data.length) {
                  let r, g, b

                  // 根据编码格式获取正确的颜色通道
                  if (message.encoding === 'rgb8') {
                    // RGB8 格式：每个像素3字节 (R, G, B)
                    r = uint8Data[dataOffset]
                    g = uint8Data[dataOffset + 1]
                    b = uint8Data[dataOffset + 2]
                  } else if (message.encoding === 'bgr8') {
                    // BGR8 格式：每个像素3字节 (B, G, R)
                    b = uint8Data[dataOffset]
                    g = uint8Data[dataOffset + 1]
                    r = uint8Data[dataOffset + 2]
                  } else if (message.encoding === 'mono8' || message.encoding === '8UC1') {
                    // 灰度格式：每个像素1字节
                    const gray = uint8Data[y * step + x]
                    r = g = b = gray
                  } else {
                    // 默认处理为 RGB8
                    r = uint8Data[dataOffset]
                    g = uint8Data[dataOffset + 1]
                    b = uint8Data[dataOffset + 2]
                  }

                  // 适度增强亮度和对比度
                  const brightness = 1.3
                  const contrast = 1.2

                  // 对比度调整公式: (pixel - 128) * contrast + 128
                  r = Math.max(0, Math.min(255, (r - 128) * contrast + 128) * brightness)
                  g = Math.max(0, Math.min(255, (g - 128) * contrast + 128) * brightness)
                  b = Math.max(0, Math.min(255, (b - 128) * contrast + 128) * brightness)

                  // 设置像素值
                  data[pixelIndex] = r     // R
                  data[pixelIndex + 1] = g // G
                  data[pixelIndex + 2] = b // B
                  data[pixelIndex + 3] = 255 // A
                }
              }
            }

            ctx.putImageData(imageData, 0, 0)
          } catch (drawError) {
            console.error('像素绘制失败，显示测试图案:', drawError)
            // 绘制测试图案
            this.drawTestPattern(ctx, canvas.width, canvas.height)
          }
        } else {
          // 绘制测试图案
          this.drawTestPattern(ctx, canvas.width, canvas.height)
        }

        // 使用 PNG 格式以获得更好的图像质量
        this.cameraImage = canvas.toDataURL('image/png')

      } catch (error) {
        console.error('Canvas 绘制失败:', error)
        this.showErrorImage(message, error)
      }
    },

    // 绘制测试图案
    drawTestPattern(ctx, width, height) {
      // 绘制棋盘格
      const size = 20
      for (let y = 0; y < height; y += size) {
        for (let x = 0; x < width; x += size) {
          ctx.fillStyle = (x / size + y / size) % 2 === 0 ? '#3498db' : '#2ecc71'
          ctx.fillRect(x, y, size, size)
        }
      }

      // 绘制中心文本
      ctx.fillStyle = '#ffffff'
      ctx.font = '16px Arial'
      ctx.textAlign = 'center'
      ctx.fillText('测试图案', width / 2, height / 2)
      ctx.fillText('相机数据可能为空', width / 2, height / 2 + 20)
    },

    // 显示错误图像
    showErrorImage(message, error) {
      try {
        const canvas = document.createElement('canvas')
        canvas.width = message.width || 640
        canvas.height = message.height || 480
        const ctx = canvas.getContext('2d')

        // 绘制背景
        ctx.fillStyle = '#e74c3c'
        ctx.fillRect(0, 0, canvas.width, canvas.height)

        // 绘制错误信息
        ctx.fillStyle = '#ffffff'
        ctx.font = '16px Arial'
        ctx.textAlign = 'center'
        ctx.fillText('相机图像绘制失败', canvas.width / 2, canvas.height / 2 - 10)
        ctx.fillText(error.message.substring(0, 50), canvas.width / 2, canvas.height / 2 + 10)

        this.cameraImage = canvas.toDataURL('image/png')
        console.log('显示错误图像')
      } catch (fallbackError) {
        console.error('显示错误图像失败:', fallbackError)
      }
    },

    // 使用 Canvas 处理图像数据 - 优化性能
    handleImageWithCanvas(message) {
      try {
        // 处理不同类型的数据，优化转换速度
        let binaryData

        if (Array.isArray(message.data)) {
          binaryData = message.data
        } else if (message.data instanceof Uint8Array) {
          // 直接使用，避免 Array.from 转换
          binaryData = message.data
        } else if (typeof message.data === 'string') {
          // 创建 Uint8Array 直接转换，比 push 更快
          const strLen = message.data.length
          binaryData = new Uint8Array(strLen)
          for (let i = 0; i < strLen; i++) {
            binaryData[i] = message.data.charCodeAt(i)
          }
        } else {
          // 尝试直接使用或快速转换
          try {
            if (typeof message.data === 'object' && message.data.buffer) {
              // 如果有 buffer，直接使用
              binaryData = new Uint8Array(message.data.buffer)
            } else {
              binaryData = []
            }
          } catch {
            binaryData = []
          }
        }

        // 调用绘制方法
        this.drawImageWithCanvas(message, binaryData)
      } catch (error) {
        // 只在开发环境输出错误
        if (process.env.NODE_ENV === 'development') {
          console.error('Canvas 处理失败:', error)
        }
        this.showErrorImage(message, error)
      }
    },

    // 根据编码格式获取 MIME 类型
    getMimeTypeFromEncoding(encoding) {
      const mimeMap = {
        'rgb8': 'image/png',
        'bgr8': 'image/png',
        'mono8': 'image/png',
        'rgba8': 'image/png',
        'bgra8': 'image/png',
        'rgb16': 'image/png',
        'bgr16': 'image/png',
        'mono16': 'image/png',
        'jpeg': 'image/jpeg',
        'jpg': 'image/jpeg',
        'png': 'image/png'
      }

      // 移除可能的分号
      const cleanEncoding = encoding.replace(/;.*$/, '')

      return mimeMap[cleanEncoding] || 'image/png'
    },

    // 检查字符串是否为 base64 编码
    isBase64(str) {
      if (typeof str !== 'string') {
        return false
      }

      console.log('检查 base64，长度:', str.length)

      // 简单的 base64 检查，主要用于小字符串
      // 相机图像数据（1MB+）不太可能是 base64
      if (str.length > 100000) {
        console.log('大尺寸数据，跳过 base64 检查')
        return false
      }

      // 基本的 base64 格式检查
      const base64Regex = /^[A-Za-z0-9+/]*={0,2}$/
      const isBase64Valid = base64Regex.test(str)

      console.log('base64 检查结果:', isBase64Valid)

      return isBase64Valid
    },

    // 移除旧的 fallbackImageProcessing 方法，使用新的 handleImageWithCanvas 方法


    // 启用/禁用相机
    toggleCamera() {
      console.log('相机状态:', this.cameraEnabled ? '启用' : '禁用')
    },

    // 更新相机话题 - 保留方法以避免错误
    updateCameraTopic() {
      console.log('相机话题更新:', this.cameraTopic)
    },

    // 清理相机资源 - 保留方法以避免错误
    cleanupCamera() {
      console.log('清理相机资源')
    },

    // 开始调整大小
    startResize(event) {
      event.preventDefault()
      this.isResizing = true

      // 获取初始位置和尺寸
      const panel = this.$refs.cameraPanel
      if (panel) {
        const rect = panel.getBoundingClientRect()
        this.startX = event.clientX
        this.startY = event.clientY
        this.startWidth = rect.width
        this.startHeight = rect.height

        // 添加样式以指示正在调整大小
        panel.style.cursor = 'nwse-resize'
      }
    },

    // 处理调整大小
    handleResize(event) {
      if (!this.isResizing) return

      const panel = this.$refs.cameraPanel
      const iframe = this.$refs.cameraIframe
      if (!panel) return

      // 计算新尺寸
      const deltaX = event.clientX - this.startX
      const deltaY = event.clientY - this.startY

      // 设置最小尺寸限制
      const minWidth = 200
      const minHeight = 150

      const newWidth = Math.max(minWidth, this.startWidth + deltaX)
      const newHeight = Math.max(minHeight, this.startHeight + deltaY)

      // 更新面板尺寸
      panel.style.width = `${newWidth}px`
      panel.style.height = `${newHeight}px`

      // 更新iframe高度（面板高度减去头部和内边距）
      if (iframe) {
        const headerHeight = 36 // 头部高度
        const padding = 16 // 上下内边距总和
        const newIframeHeight = Math.max(100, newHeight - headerHeight - padding)
        iframe.style.height = `${newIframeHeight}px`
      }
    },

    // 停止调整大小
    stopResize() {
      if (this.isResizing) {
        this.isResizing = false
        const panel = this.$refs.cameraPanel
        if (panel) {
          panel.style.cursor = ''
        }
      }
    },

    // 键盘控制逻辑
    handleKeyDown(event) {
      // 如果没有开启键盘控制，或者正在输入文字，不触发控制
      if (!this.enableKeyboardControl || event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') return

      const key = event.key.toLowerCase()
      // 允许的方向键：ArrowUp, ArrowDown, ArrowLeft, ArrowRight, w, a, s, d
      const allowedKeys = ['arrowup', 'arrowdown', 'arrowleft', 'arrowright', 'w', 'a', 's', 'd']

      if (allowedKeys.includes(key)) {
        // 防止滚动页面
        event.preventDefault()

        // 使用 $set 确保响应式更新，以便 UI 能即时反应
        this.$set(this.keysPressed, key, true)
        this.startVelocityLoop()
      }
    },

    handleKeyUp(event) {
      const key = event.key.toLowerCase()
      if (this.keysPressed[key]) {
        // 使用 $delete 确保响应式更新
        this.$delete(this.keysPressed, key)

        // 检查是否还有按键按下，如果没有则发送停止指令
        if (Object.keys(this.keysPressed).length === 0) {
          this.stopRobot()
          this.stopVelocityLoop()
        }
      }
    },

    toggleKeyboardControl() {
      this.enableKeyboardControl = !this.enableKeyboardControl
      this.showKeyboardPanel = this.enableKeyboardControl

      if (this.enableKeyboardControl) {
        this.$message.success('键盘控制已开启')
      } else {
        this.$message.info('键盘控制已关闭')
        this.stopRobot()
        this.stopVelocityLoop()
        this.keysPressed = {}
      }
    },

    startVelocityLoop() {
      if (this.velocityLoop) return

      // 以 10Hz 频率发送速度指令
      this.velocityLoop = setInterval(() => {
        this.calculateAndPublishVelocity()
      }, 100)
    },

    stopVelocityLoop() {
      if (this.velocityLoop) {
        clearInterval(this.velocityLoop)
        this.velocityLoop = null
      }
    },

    calculateAndPublishVelocity() {
      if (!this.rosConnected || !this.cmdVelTopic) return

      let linear = 0
      let angular = 0

      // 前后移动
      if (this.keysPressed['w'] || this.keysPressed['arrowup']) {
        linear += this.linearSpeed
      }
      if (this.keysPressed['s'] || this.keysPressed['arrowdown']) {
        linear -= this.linearSpeed
      }

      // 左右旋转
      if (this.keysPressed['a'] || this.keysPressed['arrowleft']) {
        angular += this.angularSpeed
      }
      if (this.keysPressed['d'] || this.keysPressed['arrowright']) {
        angular -= this.angularSpeed
      }

      // 构建 Twist 消息
      const twist = new this.ROSLIB.Message({
        linear: {
          x: linear,
          y: 0,
          z: 0
        },
        angular: {
          x: 0,
          y: 0,
          z: angular
        }
      })

      this.cmdVelTopic.publish(twist)
    },

    stopRobot() {
      if (!this.rosConnected || !this.cmdVelTopic) return

      // 发送零速度指令
      const twist = new this.ROSLIB.Message({
        linear: { x: 0, y: 0, z: 0 },
        angular: { x: 0, y: 0, z: 0 }
      })
      this.cmdVelTopic.publish(twist)
    }
  }
}
</script>

<style scoped>
/* Reset default margins and padding */
html, body {
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.advanced-map-viewer {
  display: flex;
  height: 100vh;
  font-family: 'Segoe UI', Arial, sans-serif;
  overflow: hidden;
}

.sidebar {
  width: 260px; /* 减小侧边栏宽度 */
  background: #2c3e50;
  color: white;
  padding: 15px; /* 减小内边距 */
  overflow-y: auto;
  min-width: 260px; /* 确保最小宽度 */
}

/* 语音合成控制面板样式 */
.tts-control-panel {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.tts-toggle-btn {
  background: #3498db;
  color: white;
  border: none;
  padding: 10px 15px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.tts-toggle-btn:hover {
  background: #2980b9;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.tts-panel {
  margin-top: 10px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  width: 350px;
  max-width: calc(100vw - 40px);
  overflow: hidden;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.tts-header {
  background: #3498db;
  color: white;
  padding: 12px 15px;
  border-bottom: 1px solid #e4e7ed;
}

.tts-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.tts-content {
  padding: 15px;
}

.tts-form-row {
  display: flex;
  margin-bottom: 15px;
  gap: 15px;
  flex-wrap: wrap;
}

.tts-form-item {
  margin-bottom: 15px;
  width: 100%;
}

.tts-form-item label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.tts-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  resize: vertical;
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.tts-textarea:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.tts-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;
  background: white;
  box-sizing: border-box;
}

.tts-select:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.tts-slider {
  width: 100%;
  margin: 8px 0;
}

.tts-form-item.slider-item {
  flex: 1;
  min-width: 150px;
}

.tts-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  gap: 10px;
}

.tts-send-btn {
  background: #3498db;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.tts-send-btn:hover:not(:disabled) {
  background: #2980b9;
}

.tts-send-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
  opacity: 0.6;
}

/* 适配小屏幕 */
@media (max-width: 768px) {
  .tts-panel {
    width: calc(100vw - 40px);
  }

  .tts-form-row {
    flex-direction: column;
  }

  .tts-form-item.slider-item {
    width: 100%;
  }
}

/* 键盘控制按钮美化 */
.keyboard-toggle-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.keyboard-toggle-btn:hover {
  background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
  border-color: #c6e2ff;
  color: #409eff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.15);
}

.keyboard-toggle-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.1);
}

.keyboard-toggle-btn.active {
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border-color: #3a8ee6;
  color: white;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.keyboard-toggle-btn.active:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}

.btn-icon {
  font-size: 16px;
}

/* 键盘控制小窗样式 */
.keyboard-control-panel {
  position: absolute;
  right: 20px;
  bottom: 20px;
  width: 200px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  backdrop-filter: blur(10px);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.keyboard-control-panel:hover {
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
  transform: translateY(-2px);
}

.keyboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.keyboard-header h5 {
  margin: 0;
  color: #303133;
  font-size: 14px;
}

.close-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 18px;
  color: #909399;
  padding: 0 4px;
  line-height: 1;
}

.close-btn:hover {
  color: #f56c6c;
}

.keyboard-content {
  padding: 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.arrow-row {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.arrow-key {
  width: 40px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #606266;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.1s ease;
  user-select: none;
}

.arrow-key.active {
  background: #3498db;
  color: white;
  border-color: #3498db;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1) inset;
  transform: translateY(1px);
}

.keyboard-tip {
  margin-top: 5px;
  text-align: center;
}

.keyboard-tip p {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

/* 相机图像面板样式 */
.camera-panel {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin: 10px;
  margin-bottom: 15px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.camera-header {
  background: #3498db;
  color: white;
  padding: 12px 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
}

.camera-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.camera-controls {
  display: flex;
  gap: 15px;
  align-items: center;
}

.camera-controls label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  cursor: pointer;
  user-select: none;
}

.camera-select {
  padding: 4px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  transition: border-color 0.2s;
}

.camera-select:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.camera-content {
  padding: 15px;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;
}

.camera-image-container {
  width: 100%;
  max-height: 400px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.camera-image {
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
}

.camera-iframe {
  width: 100%;
  height: 400px;
  border: none;
  border-radius: 4px;
}

/* 小窗相机面板样式 */
.camera-panel-small {
  position: absolute;
  top: 20px;
  left: 380px;
  width: 672px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 100;
  overflow: hidden;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

/* 当面板包含内容时（相机启用），固定高度 */
.camera-panel-small:has(.camera-content-small) {
  height: 566px;
}

/* 当面板没有内容时（相机禁用），自适应高度 */
.camera-panel-small:not(:has(.camera-content-small)) {
  height: auto;
  min-height: 36px;
}

.camera-header-small {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #409eff;
  color: white;
  font-size: 14px;
  height: 36px;
  box-sizing: border-box;
}

.camera-header-small h5 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

.camera-controls-small {
  display: flex;
  align-items: center;
}

.camera-controls-small label {
  display: flex;
  align-items: center;
  font-size: 12px;
  margin: 0;
  color: white;
  cursor: pointer;
}

.camera-checkbox {
  margin-right: 4px;
}

.camera-content-small {
  padding: 8px;
  box-sizing: border-box;
  height: calc(100% - 36px);
}

.camera-iframe-small {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 4px;
  transition: height 0.3s ease;
}

/* 调整大小手柄样式 */
.resize-handle {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 16px;
  height: 16px;
  background: #409eff;
  cursor: nwse-resize;
  border-top-left-radius: 4px;
  box-shadow: -2px -2px 4px rgba(0, 0, 0, 0.1);
  z-index: 101;
  transition: all 0.2s ease;
}

.resize-handle:hover {
  background: #66b1ff;
  width: 20px;
  height: 20px;
}

.camera-disabled,
.camera-loading {
  color: #909399;
  font-size: 16px;
  text-align: center;
  padding: 50px 20px;
}

/* 调整主地图区域 */
.main-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.map-area {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.sidebar h3, .sidebar h4 {
  margin-top: 0;
  color: #ecf0f1;
}

.connection-panel input {
  width: 100%;
  padding: 8px;
  margin-bottom: 10px;
  border: 1px solid #34495e;
  border-radius: 4px;
  background: #34495e;
  color: white;
}

.connection-panel button {
  width: 100%;
  padding: 10px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 10px;
}

.connection-panel button:hover {
  background: #2980b9;
}

.connection-status {
  padding: 8px;
  border-radius: 4px;
  text-align: center;
  font-weight: bold;
}

.connection-status.connected {
  background: #27ae60;
}

.connection-status.disconnected {
  background: #e74c3c;
}

.connection-status.error {
  background: #f39c12;
}

.map-controls label {
  display: block;
  margin: 8px 0;
  cursor: pointer;
}

.zoom-control {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.zoom-control button {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: #3498db;
  color: white;
  cursor: pointer;
  font-size: 16px;
}

.robot-info, .map-info {
  margin-top: 20px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.navigation-controls {
  margin-top: 20px;
}

.sidebar-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 10px;
}

.sidebar-buttons button {
  padding: 10px 15px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  transition: background-color 0.2s;
}

.sidebar-buttons button:hover {
  background: #2980b9;
}

.sidebar-buttons button.active {
  background: #e74c3c;
}

.sidebar-buttons button:active {
  transform: translateY(1px);
}

/* 实时位置信息覆盖层 */
.robot-info-overlay {
  position: absolute;
  top: 15px;
  left: 15px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 12px 16px;
  border-radius: 6px;
  font-size: 14px;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  pointer-events: none;
  min-width: 200px;
}

.robot-info-overlay .info-item {
  margin-bottom: 5px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.robot-info-overlay .info-item:last-child {
  margin-bottom: 0;
}

/* 保存位置弹窗样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.dialog {
  background: #2c3e50;
  color: white;
  border-radius: 8px;
  padding: 20px;
  width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.dialog h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #ecf0f1;
  text-align: center;
}

.dialog-content {
  margin-bottom: 20px;
}

.position-info {
  background: rgba(255, 255, 255, 0.1);
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 20px;
}

.position-info p {
  margin: 5px 0;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
}

.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #34495e;
  border-radius: 4px;
  background: #34495e;
  color: white;
  font-size: 14px;
}

.form-group input:focus {
  outline: none;
  border-color: #3498db;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.dialog-actions button {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.dialog-actions button:first-child {
  background: #7f8c8d;
  color: white;
}

.dialog-actions button:first-child:hover {
  background: #95a5a6;
}

.dialog-actions button.primary {
  background: #3498db;
  color: white;
}

.dialog-actions button.primary:hover {
  background: #2980b9;
}

/* 已保存的位置列表样式 */
.saved-positions-list {
  position: absolute;
  top: 100px;
  left: 15px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border-radius: 6px;
  padding: 12px;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  max-height: 300px;
  overflow-y: auto;
  min-width: 250px;
  max-width: 350px;
}

.list-header {
  margin-bottom: 10px;
  padding-bottom: 5px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 14px;
  font-weight: bold;
}

.saved-position-item {
  display: flex;
  align-items: center;
  padding: 8px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  transition: background-color 0.2s;
}

.saved-position-item:hover {
  background: rgba(255, 255, 255, 0.15);
}

.position-name {
  flex: 1;
  font-size: 13px;
  font-weight: bold;
  margin-right: 10px;
}

.position-coords {
  font-size: 12px;
  color: #bdc3c7;
  margin-right: 15px;
  white-space: nowrap;
}

.position-actions {
  display: flex;
  gap: 5px;
}

.action-btn {
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  padding: 5px;
  border-radius: 3px;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.goto-btn:hover {
  background: rgba(52, 152, 219, 0.4);
}

.delete-btn:hover {
  background: rgba(231, 76, 60, 0.4);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #1a1a1a;
  overflow: hidden;
}

.map-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  height: 100%; /* Full height since toolbar is removed */
}

.three-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.selected-point {
  position: absolute;
  width: 20px;
  height: 20px;
  background: #e74c3c;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  z-index: 20;
  pointer-events: none;
}

.point-label {
  position: absolute;
  top: -30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.initialization-prompt {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 0, 0.9);
  color: #000;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: bold;
  z-index: 100;
  pointer-events: none;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.navigation-prompt {
  position: absolute;
  top: 60px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 255, 0, 0.9);
  color: #000;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: bold;
  z-index: 100;
  pointer-events: none;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.toolbar {
  padding: 8px;
  background: #34495e;
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
  overflow-x: auto;
  /* 隐藏滚动条但保持功能 */
  -ms-overflow-style: none; /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}

/* 隐藏滚动条 */
.toolbar::-webkit-scrollbar {
  display: none;
}

.toolbar button {
  padding: 6px 12px; /* 减小按钮内边距 */
  background: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  font-size: 14px; /* 减小字体大小 */
}

.toolbar button:hover {
  background: #2980b9;
}

.toolbar button.active {
  background: #e74c3c;
}
</style>

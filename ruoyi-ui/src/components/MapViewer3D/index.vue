<template>
  <div class="map-viewer-3d">
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
</template>

<script>
export default {
  name: 'MapViewer3D',
  props: {
    ros: {
      type: Object,
      default: null
    },
    rosConnected: {
      type: Boolean,
      default: false
    },
    config: {
      type: Object,
      default: () => ({
        topics: {
          map: '/map',
          pose: '/amcl_pose',
          goal: '/goal_pose',
          path: '/plan'
        }
      })
    }
  },
  data() {
    return {
      // ROS 对象
      ROSLIB: null,
      mapSubscriber: null,
      poseSubscriber: null,
      pathSubscriber: null,
      pathMesh: null,
      
      // 地图相关
      currentMap: null,
      
      // 机器人位姿
      robotPose: null,
      
      // three.js 相关
      scene: null,
      camera: null,
      renderer: null,
      controls: null,
      mapMesh: null,
      robotMesh: null,
      gridHelper: null,
      animationId: null,
      raycaster: null,
      
      // 交互模式
      currentTool: 'pan',
      isInitializationMode: false,
      initializationPrompt: '',
      isNavigationMode: false,
      navigationPrompt: '',
      isSettingOrientation: false,
      targetPoint: null,
      orientationStartPoint: null,
      orientationEndPoint: null,
      navigationArrow: null,
      
      // 保存位置
      savedPositionMeshes: [],
      savedPositions: []
    }
  },
  watch: {
    rosConnected(val) {
      if (val && this.ros) {
        this.setupSubscribers()
      } else {
        this.disconnectROS()
      }
    },
    ros(val) {
      if (val && this.rosConnected) {
        this.setupSubscribers()
      }
    }
  },
  mounted() {
    this.initialize()
  },
  beforeDestroy() {
    this.disconnectROS()
    if (this.animationId) {
      cancelAnimationFrame(this.animationId)
    }
    // 清理 three.js 资源
    if (this.renderer) {
      this.renderer.dispose()
    }
    // 移除事件监听
    const container = this.$refs.threeContainer
    if (container) {
      container.removeEventListener('click', this.handleMapClick)
      container.removeEventListener('mousemove', this.handleOrientationDrag)
      container.removeEventListener('mouseup', this.handleOrientationRelease)
    }
  },
  methods: {
    initialize() {
      this.setupThreeJS()
      this.setupEventListeners()
      
      // 动态导入 ROSLIB，确保可用
      import('roslib').then(ROSLIB => {
        this.ROSLIB = ROSLIB
        // 如果已经连接，立即设置订阅
        if (this.rosConnected && this.ros) {
          this.setupSubscribers()
        }
      })
    },
    
    // 设置 three.js
    setupThreeJS() {
      const container = this.$refs.threeContainer
      if (!container) return
      
      import('three').then(THREE => {
        // 创建场景
        this.scene = new THREE.Scene()
        this.scene.background = new THREE.Color(0xf0f2f5) // 浅灰色背景
        // 移除雾效，保持明亮
        
        // 创建相机
        const width = container.clientWidth || 800
        const height = container.clientHeight || 600
        this.camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 1000)
        this.camera.position.set(0, 15, 15)
        this.camera.lookAt(0, 0, 0)
        
        // 创建渲染器
        this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
        this.renderer.setSize(width, height)
        this.renderer.setPixelRatio(window.devicePixelRatio)
        this.renderer.shadowMap.enabled = true
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap
        container.appendChild(this.renderer.domElement)
        
        // 优化灯光 (适应明亮场景)
        const ambientLight = new THREE.HemisphereLight(0xffffff, 0xffffff, 0.8)
        this.scene.add(ambientLight)
        
        const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
        directionalLight.position.set(10, 20, 10)
        directionalLight.castShadow = true
        directionalLight.shadow.mapSize.width = 2048
        directionalLight.shadow.mapSize.height = 2048
        this.scene.add(directionalLight)
        
        // 创建网格 (深灰色以在浅背景可见)
        const gridHelper = new THREE.GridHelper(60, 60, 0x999999, 0xdddddd)
        gridHelper.position.y = -0.01 // 略微下沉避免z-fighting
        this.scene.add(gridHelper)
        
        // 添加坐标轴辅助 (缩小)
        const axesHelper = new THREE.AxesHelper(2)
        axesHelper.position.y = 0.01
        this.scene.add(axesHelper)
        
        // 创建 raycaster
        this.raycaster = new THREE.Raycaster()
        
        // 监听容器大小变化
        this.resizeObserver = new ResizeObserver(() => {
          this.handleResize()
        })
        this.resizeObserver.observe(container)
        
        // 导入并创建轨道控制器
        import('three/examples/jsm/controls/OrbitControls').then(({ OrbitControls }) => {
          this.controls = new OrbitControls(this.camera, this.renderer.domElement)
          this.controls.enableDamping = true
          this.controls.dampingFactor = 0.05
          this.controls.screenSpacePanning = false
          this.controls.maxPolarAngle = Math.PI / 2 - 0.1
          this.controls.minDistance = 1
          this.controls.maxDistance = 100
          
          this.animate()
        })
      })
    },

    handleResize() {
      const container = this.$refs.threeContainer
      if (!container || !this.camera || !this.renderer) return
      
      const width = container.clientWidth
      const height = container.clientHeight
      
      this.camera.aspect = width / height
      this.camera.updateProjectionMatrix()
      this.renderer.setSize(width, height)
    },
    
    // 渲染循环
    animate() {
      this.animationId = requestAnimationFrame(this.animate.bind(this))
      if (this.controls) {
        this.controls.update()
      }
      if (this.renderer && this.scene && this.camera) {
        this.renderer.render(this.scene, this.camera)
      }
    },
    
    setupEventListeners() {
      const container = this.$refs.threeContainer
      if (!container) return
      container.addEventListener('click', this.handleMapClick)
    },
    
    // 断开连接清理
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
      this.clearPath()
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
      
      // 路径订阅
      this.pathSubscriber = new this.ROSLIB.Topic({
        ros: this.ros,
        name: this.config.topics.path,
        messageType: 'nav_msgs/Path'
      })
      this.pathSubscriber.subscribe(this.handlePathMessage.bind(this))
    },
    
    handleMapMessage(message) {
      const mapData = {
        width: message.info.width,
        height: message.info.height,
        resolution: message.info.resolution,
        origin: {
          x: message.info.origin.position.x,
          y: message.info.origin.position.y
        },
        data: message.data
      }
      this.currentMap = mapData
      this.renderMap()
    },
    
    handlePoseMessage(message) {
      const pose = message.pose.pose
      const orientation = pose.orientation
      
      const yaw = Math.atan2(
        2.0 * (orientation.w * orientation.z + orientation.x * orientation.y),
        1.0 - 2.0 * (orientation.y * orientation.y + orientation.z * orientation.z)
      )
      
      this.robotPose = {
        x: pose.position.x,
        y: pose.position.y,
        yaw: yaw + Math.PI / 2
      }
      
      this.updateRobotPose()
    },
    
    handlePathMessage(message) {
      if (!this.currentMap || !this.scene) return
      
      const poses = message.poses || []
      if (poses.length === 0) {
        this.clearPath()
        return
      }
      this.renderPath(poses)
    },
    
    renderMap() {
      if (!this.currentMap || !this.scene) return
      
      import('three').then(THREE => {
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
        
        const geometry = new THREE.PlaneGeometry(
          width * resolution,
          height * resolution,
          width - 1,
          height - 1
        )
        
        const colors = []
        const vertices = geometry.attributes.position.array
        
        for (let y = height - 1; y >= 0; y--) {
          for (let x = 0; x < width; x++) {
            const index = y * width + x
            const value = map.data[index]
            
            let color
            if (value === -1) {
              color = new THREE.Color(0x808080)
            } else if (value === 0) {
              color = new THREE.Color(0xFFFFFF)
            } else if (value === 100) {
              color = new THREE.Color(0x000000)
            } else {
              const intensity = value / 100
              color = new THREE.Color(1 - intensity, 1 - intensity, 1 - intensity)
            }
            colors.push(color.r, color.g, color.b)
          }
        }
        
        geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3))
        
        const material = new THREE.MeshStandardMaterial({
          vertexColors: true,
          side: THREE.DoubleSide,
          roughness: 0.8,
          metalness: 0.2
        })
        
        this.mapMesh = new THREE.Mesh(geometry, material)
        this.mapMesh.rotation.x = -Math.PI / 2
        this.mapMesh.position.set(0, 0, 0)
        this.scene.add(this.mapMesh)
        
        if (this.robotPose) {
          this.updateRobotPose()
        }
        this.renderSavedPositions()
      })
    },
    
    updateRobotPose() {
      if (!this.robotPose || !this.scene || !this.currentMap) return
      
      import('three').then(THREE => {
        if (this.robotMesh) {
          this.scene.remove(this.robotMesh)
          this.robotMesh.traverse((child) => {
            if (child.geometry) child.geometry.dispose()
            if (child.material) {
              if (Array.isArray(child.material)) {
                child.material.forEach(m => m.dispose())
              } else {
                child.material.dispose()
              }
            }
          })
          this.robotMesh = null
        }
        
        const pose = this.robotPose
        const map = this.currentMap
        const mapWidth = map.width * map.resolution
        const mapHeight = map.height * map.resolution
        const mapOriginX = map.origin.x
        const mapOriginY = map.origin.y
        
        const robotMapX = pose.x - mapOriginX
        const robotMapY = pose.y - mapOriginY
        const threeX = robotMapX - mapWidth / 2
        const threeZ = (mapHeight - robotMapY) - mapHeight / 2
        
        this.robotMesh = new THREE.Group()
        
        // AGV主体
        const bodyGeometry = new THREE.BoxGeometry(0.2, 0.07, 0.27)
        // 使用更鲜艳的颜色以在亮色背景下突出
        const bodyMaterial = new THREE.MeshStandardMaterial({ 
          color: 0xe74c3c, // 鲜艳红
          metalness: 0.3, 
          roughness: 0.4
        })
        const body = new THREE.Mesh(bodyGeometry, bodyMaterial)
        body.position.y = 0.035
        body.castShadow = true
        this.robotMesh.add(body)
        
        // 装饰条
        const stripGeometry = new THREE.BoxGeometry(0.22, 0.01, 0.02)
        const stripMaterial = new THREE.MeshBasicMaterial({ 
          color: 0x3498db // 亮蓝
        })
        const stripFront = new THREE.Mesh(stripGeometry, stripMaterial)
        stripFront.position.set(0, 0.04, -0.1)
        this.robotMesh.add(stripFront)

        const stripBack = new THREE.Mesh(stripGeometry, stripMaterial)
        stripBack.position.set(0, 0.04, 0.1)
        this.robotMesh.add(stripBack)
        
        // 朝向指示器
        const arrowGeometry = new THREE.ConeGeometry(0.05, 0.2, 8)
        const arrowMaterial = new THREE.MeshBasicMaterial({ 
          color: 0x2ecc71 // 鲜绿
        })
        const arrow = new THREE.Mesh(arrowGeometry, arrowMaterial)
        arrow.position.set(0, 0.135, 0)
        arrow.rotation.x = Math.PI / 2
        this.robotMesh.add(arrow)
        
        this.robotMesh.position.set(threeX, 0, threeZ)
        this.robotMesh.rotation.y = pose.yaw
        this.scene.add(this.robotMesh)
      })
    },
    
    renderPath(poses) {
      import('three').then(THREE => {
        this.clearPath()
        if (!this.currentMap) return
        
        const map = this.currentMap
        const mapWidth = map.width * map.resolution
        const mapHeight = map.height * map.resolution
        const mapOriginX = map.origin.x
        const mapOriginY = map.origin.y
        
        const points = []
        poses.forEach(poseMsg => {
          const pose = poseMsg.pose.position
          const robotMapX = pose.x - mapOriginX
          const robotMapY = pose.y - mapOriginY
          const threeX = robotMapX - mapWidth / 2
          const threeZ = (mapHeight - robotMapY) - mapHeight / 2
          points.push(new THREE.Vector3(threeX, 0.05, threeZ))
        })
        
        const curve = new THREE.CatmullRomCurve3(points)
        const pointsCount = Math.max(points.length * 5, 50)
        const smoothPoints = curve.getPoints(pointsCount)
        const geometry = new THREE.BufferGeometry().setFromPoints(smoothPoints)
        
        const material = new THREE.LineBasicMaterial({
          color: 0x00ff00, linewidth: 3, transparent: true, opacity: 0.8
        })
        
        this.pathMesh = new THREE.Line(geometry, material)
        this.scene.add(this.pathMesh)
      })
    },
    
    clearPath() {
      if (this.pathMesh && this.scene) {
        this.scene.remove(this.pathMesh)
        if (this.pathMesh.geometry) this.pathMesh.geometry.dispose()
        if (this.pathMesh.material) this.pathMesh.material.dispose()
        this.pathMesh = null
      }
    },
    
    // 渲染保存的位置标记 (外部调用)
    updateSavedPositions(positions) {
      this.savedPositions = positions
      this.renderSavedPositions()
    },
    
    renderSavedPositions() {
      if (!this.currentMap || !this.scene) return
      
      import('three').then(THREE => {
        // 清除旧标记
        if (this.savedPositionMeshes && this.savedPositionMeshes.length > 0) {
          this.savedPositionMeshes.forEach(mesh => {
            this.scene.remove(mesh)
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
          const robotMapX = pos.x - mapOriginX
          const robotMapY = pos.y - mapOriginY
          const threeX = robotMapX - mapWidth / 2
          const threeZ = (mapHeight - robotMapY) - mapHeight / 2

          const group = new THREE.Group()
          
          // 杆子
          const poleGeometry = new THREE.CylinderGeometry(0.02, 0.02, 0.5, 8)
          const poleMaterial = new THREE.MeshStandardMaterial({ color: 0xFFFFFF })
          const pole = new THREE.Mesh(poleGeometry, poleMaterial)
          pole.position.y = 0.25
          group.add(pole)
          
          // 顶部标志
          const headGeometry = new THREE.OctahedronGeometry(0.15)
          const headMaterial = new THREE.MeshStandardMaterial({ 
            color: 0xe67e22, metalness: 0.3, roughness: 0.4
          })
          const head = new THREE.Mesh(headGeometry, headMaterial)
          head.position.y = 0.5
          group.add(head)
          
          // 文字标签
          if (pos.name) {
            const canvas = document.createElement('canvas')
            const context = canvas.getContext('2d')
            const fontSize = 24
            const font = `bold ${fontSize}px Arial`
            context.font = font
            const textMetrics = context.measureText(pos.name)
            const textWidth = textMetrics.width
            
            canvas.width = textWidth + 20
            canvas.height = fontSize + 16
            
            context.fillStyle = 'rgba(0, 0, 0, 0.6)'
            context.beginPath()
            context.roundRect(0, 0, canvas.width, canvas.height, 6)
            context.fill()
            
            context.font = font
            context.fillStyle = '#ffffff'
            context.textAlign = 'center'
            context.textBaseline = 'middle'
            context.fillText(pos.name, canvas.width / 2, canvas.height / 2)
            
            const texture = new THREE.CanvasTexture(canvas)
            const spriteMaterial = new THREE.SpriteMaterial({ map: texture, transparent: true })
            const sprite = new THREE.Sprite(spriteMaterial)
            
            const scale = 0.007
            sprite.scale.set(canvas.width * scale, canvas.height * scale, 1)
            sprite.position.y = 0.75
            group.add(sprite)
          }

          group.position.set(threeX, 0, threeZ)
          this.scene.add(group)
          this.savedPositionMeshes.push(group)
        })
      })
    },
    
    // 处理地图点击
    handleMapClick(event) {
      // 仅当需要交互时实现，例如设置导航点
      // 这里可以发出事件给父组件
    },
    
    // 方向拖动逻辑 (可按需实现)
    handleOrientationDrag(event) { },
    handleOrientationRelease(event) { }
  }
}
</script>

<style scoped>
.map-viewer-3d {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #f0f0f0;
}

.three-container {
  width: 100%;
  height: 100%;
}

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
}

.info-item {
  margin-bottom: 5px;
}

.initialization-prompt, .navigation-prompt {
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
}
</style>
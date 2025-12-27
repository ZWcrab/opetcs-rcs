<template>
  <div class="navigation-planning">
    <div class="container">
      <h2>导航规划</h2>
      
      <!-- ROSBridge连接状态 -->
      <div class="status-section">
        <div class="ros-status" :class="{ connected: rosConnected }">
          <span class="status-indicator"></span>
          <span>ROSBridge {{ rosConnected ? '已连接' : '未连接' }}</span>
        </div>
        <div class="connection-form">
          <el-input 
            v-model="rosBridgeUrl" 
            placeholder="ROSBridge地址，如 ws://localhost:9090" 
            style="width: 300px; margin-right: 10px;"
          />
          <el-button type="primary" @click="connectRos" :disabled="rosConnected">连接</el-button>
          <el-button @click="disconnectRos" :disabled="!rosConnected">断开连接</el-button>
        </div>
      </div>
      
      <!-- 地图显示区域 -->
      <div class="map-section">
        <h3>地图显示</h3>
        <div class="map-container">
          <canvas ref="mapCanvas" class="map-canvas"></canvas>
          
          <!-- 地图控制 -->
          <div class="map-controls">
            <el-button type="primary" @click="loadMap">加载地图</el-button>
            <el-button @click="clearMarkers">清除标注</el-button>
            <div class="zoom-controls">
              <el-button @click="zoomIn" size="small">+</el-button>
              <el-button @click="zoomOut" size="small">-</el-button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 点位列表 -->
      <div class="markers-section">
        <h3>标注点位</h3>
        <div class="markers-list">
          <el-table :data="markers" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80"></el-table-column>
            <el-table-column prop="name" label="名称"></el-table-column>
            <el-table-column prop="x" label="X坐标"></el-table-column>
            <el-table-column prop="y" label="Y坐标"></el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button @click="deleteMarker(scope.row.id)" type="danger" size="small">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <!-- 新增点位 -->
        <div class="add-marker">
          <h4>新增点位</h4>
          <el-form :model="newMarker" inline>
            <el-form-item label="名称">
              <el-input v-model="newMarker.name" placeholder="点位名称"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="addMarker">添加</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
      
      <!-- 导航路径 -->
      <div class="path-section">
        <h3>导航路径</h3>
        <div class="path-controls">
          <el-button type="success" @click="planPath">规划路径</el-button>
          <el-button @click="clearPath">清除路径</el-button>
        </div>
        <div class="path-info" v-if="path.length > 0">
          <p>路径点数量: {{ path.length }}</p>
          <p>路径预览: {{ path.map(p => `(${p.x},${p.y})`).join(' → ') }}</p>
        </div>
      </div>
      
      <!-- ROS导航控制 -->
      <div class="ros-navigation-section">
        <h3>ROS导航控制</h3>
        <div class="navigation-controls">
          <el-button 
            type="primary" 
            @click="sendNavigationGoal" 
            :disabled="!rosConnected || markers.length === 0"
          >
            发送导航目标(使用标记点)
          </el-button>
          <el-button 
            type="warning" 
            @click="cancelNavigationGoal" 
            :disabled="!rosConnected || !currentGoal"
          >
            取消导航目标
          </el-button>
        </div>
        <div class="navigation-status" v-if="currentGoal">
          <h4>当前导航状态</h4>
          <p>目标位置: ({{ currentGoal.x }}, {{ currentGoal.y }})</p>
          <p>导航状态: {{ navigationStatus }}</p>
          <p>反馈信息: {{ navigationFeedback }}</p>
        </div>
      </div>
      
      <!-- 手动发送指令面板 -->
      <div class="manual-navigation-section">
        <h3>手动发送指令</h3>
        <el-form :model="manualGoal" label-width="100px" inline>
          <el-form-item label="X坐标">
            <el-input 
              v-model.number="manualGoal.x" 
              placeholder="输入X坐标" 
              type="number" 
              style="width: 150px;"
            ></el-input>
          </el-form-item>
          <el-form-item label="Y坐标">
            <el-input 
              v-model.number="manualGoal.y" 
              placeholder="输入Y坐标" 
              type="number" 
              style="width: 150px;"
            ></el-input>
          </el-form-item>
          <el-form-item label="Z坐标">
            <el-input 
              v-model.number="manualGoal.z" 
              placeholder="输入Z坐标" 
              type="number" 
              style="width: 150px;"
              :disabled="true"
              value="0"
            ></el-input>
          </el-form-item>
          <el-form-item>
            <el-button 
              type="success" 
              @click="sendManualNavigationGoal" 
              :disabled="!rosConnected"
            >
              手动发送目标
            </el-button>
          </el-form-item>
        </el-form>
        <div class="manual-navigation-tips">
          <h4>使用说明</h4>
          <ul>
            <li>直接输入坐标值，点击"手动发送目标"按钮</li>
            <li>Z坐标默认为0，一般无需修改</li>
            <li>消息格式与ROS2命令完全匹配</li>
            <li>发送后可在日志中查看详细信息</li>
          </ul>
        </div>
      </div>
      
      <!-- 日志显示区域 -->
      <div class="log-section">
        <h3>ROS日志</h3>
        <div class="log-container">
          <div v-for="(log, index) in logs" :key="index" class="log-item">
            <span class="log-time">{{ log.time }}</span>
            <span class="log-content">{{ log.content }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NavigationPlanning',
  data() {
    return {
      mapImage: null,
      mapCanvas: null,
      ctx: null,
      mapLoaded: false,
      zoom: 1,
      markers: [],
      markerId: 1,
      newMarker: {
        name: ''
      },
      path: [],
      mapData: {
        yaml: null,
        pgm: null
      },
      canvasOffset: {
        x: 0,
        y: 0
      },
      // ROS相关数据
      rosBridgeUrl: 'ws://192.168.31.177:9090',
      ros: null,
      rosConnected: false,
      logs: [],
      roslibLoaded: false,
      roslibLoading: false,
      // 导航Action相关数据
      navigateToPoseAction: null,
      navigateToPoseTopic: null, // 保留Topic属性，兼容之前的代码
      currentGoal: null,
      currentGoalId: null,
      navigationStatus: '空闲',
      navigationFeedback: '',
      navigationGoal: null,
      // 手动导航目标数据
      manualGoal: {
        x: 1.0,
        y: 2.0,
        z: 0.0
      }
    };
  },
  mounted() {
    this.initCanvas();
    this.loadMapFiles();
    // 确保DOM加载完成后再检查roslib
    this.$nextTick(() => {
      this.checkRoslibLoaded();
    });
  },
  beforeUnmount() {
    // 页面卸载时断开ROSBridge连接
    this.disconnectRos();
  },
  methods: {
    // 初始化画布
    initCanvas() {
      this.mapCanvas = this.$refs.mapCanvas;
      this.ctx = this.mapCanvas.getContext('2d');
      
      // 设置画布大小
      this.mapCanvas.width = 800;
      this.mapCanvas.height = 600;
      
      // 添加点击事件监听
      this.mapCanvas.addEventListener('click', this.handleCanvasClick);
    },
    
    // 检查roslib是否已加载，添加重试机制
    checkRoslibLoaded() {
      if (typeof ROSLIB !== 'undefined') {
        this.roslibLoaded = true;
        this.addLog('roslib.js已成功加载');
        // 自动连接
        this.connectRos();
      } else {
        if (!this.roslibLoading) {
          this.roslibLoading = true;
          this.addLog('尝试加载roslib.js...');
          // 尝试动态加载roslib.js
          this.loadRoslibDynamically();
        }
      }
    },
    
    // 动态加载roslib.js
    loadRoslibDynamically() {
      const script = document.createElement('script');
      script.src = 'https://cdn.jsdelivr.net/npm/roslib@1.3.0/build/roslib.min.js';
      script.onload = () => {
        this.roslibLoaded = true;
        this.roslibLoading = false;
        this.addLog('roslib.js动态加载成功');
        // 加载成功后自动连接
        this.connectRos();
      };
      script.onerror = () => {
        this.roslibLoading = false;
        this.addLog('roslib.js加载失败，将在3秒后重试...');
        // 3秒后重试
        setTimeout(() => {
          this.checkRoslibLoaded();
        }, 3000);
      };
      document.head.appendChild(script);
    },
    
    // 连接ROSBridge
    connectRos() {
      if (!this.rosBridgeUrl) {
        this.$message.error('请输入ROSBridge地址');
        return;
      }
      
      // 验证URL格式
      try {
        new URL(this.rosBridgeUrl);
      } catch (e) {
        this.$message.error('ROSBridge地址格式不正确，请检查');
        this.addLog(`连接失败: 地址格式不正确 - ${this.rosBridgeUrl}`);
        return;
      }
      
      // 检查URL是否为ws或wss协议
      if (!this.rosBridgeUrl.startsWith('ws://') && !this.rosBridgeUrl.startsWith('wss://')) {
        this.$message.error('ROSBridge地址必须使用ws或wss协议');
        this.addLog(`连接失败: 协议不正确，必须使用ws或wss - ${this.rosBridgeUrl}`);
        return;
      }
      
      try {
        // 再次检查roslib是否已加载
        if (typeof ROSLIB === 'undefined') {
          this.addLog('roslib.js未加载，正在重试...');
          this.checkRoslibLoaded();
          return;
        }
        
        this.addLog(`正在连接ROSBridge: ${this.rosBridgeUrl}...`);
        
        this.ros = new ROSLIB.Ros({
          url: this.rosBridgeUrl
        });
        
        this.ros.on('connection', () => {
          this.rosConnected = true;
          this.addLog('✅ ROSBridge连接成功');
          this.initTopics();
        });
        
        this.ros.on('error', (error) => {
          this.rosConnected = false;
          // 进一步改进错误处理，提供更详细的错误信息
          let errorMessage = '连接错误';
          if (typeof error === 'string') {
            errorMessage = error;
          } else if (error && error.message) {
            errorMessage = error.message;
          } else if (error && error.type) {
            errorMessage = error.type;
          } else if (error && error.code) {
            errorMessage = `错误代码: ${error.code}`;
            if (error.code === 1006) {
              errorMessage += ' (连接被意外关闭)';
            } else if (error.code === 1003) {
              errorMessage += ' (不支持的协议)';
            } else if (error.code === 1002) {
              errorMessage += ' (协议错误)';
            } else if (error.code === 1001) {
              errorMessage += ' (服务器关闭连接)';
            }
          } else {
            errorMessage = JSON.stringify(error, null, 2);
          }
          this.addLog(`❌ ROSBridge连接错误: ${errorMessage}`);
          this.addLog(`💡 连接提示: 请检查ROSBridge服务器是否运行，URL是否正确，网络是否通畅`);
        });
        
        this.ros.on('close', (event) => {
          this.rosConnected = false;
          let closeMessage = '连接已关闭';
          if (event && event.code) {
            closeMessage += `, 代码: ${event.code}`;
            if (event.code === 1000) {
              closeMessage += ' (正常关闭)';
            } else if (event.code === 1006) {
              closeMessage += ' (意外关闭)';
            }
          }
          this.addLog(`⚠️ ROSBridge${closeMessage}`);
        });
      } catch (error) {
        this.rosConnected = false;
        this.addLog(`❌ 连接失败: ${error.message}`);
        this.addLog(`💡 错误提示: ${error.stack}`);
      }
    },
    
    // 断开ROSBridge连接
    disconnectRos() {
      if (this.ros) {
        // 取消当前导航目标
        if (this.navigationGoal && this.currentGoalId) {
          this.cancelNavigationGoal();
        }
        
        this.ros.close();
        this.ros = null;
        this.rosConnected = false;
        this.navigateToPoseAction = null;
        this.navigateToPoseTopic = null;
      }
    },
    
    // 初始化ROS话题
    initTopics() {
      // 初始化导航Action
      this.initNavigateToPoseAction();
      this.addLog('已初始化ROS话题和导航Action');
    },
    
    // 初始化导航Action客户端
    initNavigateToPoseAction() {
      if (!this.ros) return;
      
      try {
        // 初始化ActionClient来处理导航请求
        this.navigateToPoseAction = new ROSLIB.ActionClient({
          ros: this.ros,
          actionName: '/navigate_to_pose',
          actionType: 'nav2_msgs/action/NavigateToPose'
        });
        
        this.addLog('已成功初始化NavigateToPose Action客户端');
      } catch (error) {
        this.addLog(`❌ 初始化ActionClient失败: ${error.message}`);
        this.addLog(`💡 将回退到直接Topic发布方式`);
      }
    },
    
    // 添加日志
    addLog(content) {
      const time = new Date().toLocaleTimeString();
      this.logs.unshift({ time, content });
      // 只保留最近100条日志
      if (this.logs.length > 100) {
        this.logs.pop();
      }
    },
    
    // 加载地图文件
    async loadMapFiles() {
      try {
        // 加载YAML文件
        const yamlResponse = await fetch('/map/yahboomcar.yaml');
        const yamlText = await yamlResponse.text();
        this.mapData.yaml = this.parseYaml(yamlText);
        
        // 使用默认地图或占位图，因为浏览器不直接支持PGM格式
        this.$message.info('PGM格式需要特殊处理，当前使用占位图展示');
        this.mapLoaded = true;
        this.drawMapWithPlaceholder();
      } catch (error) {
        console.error('加载地图文件失败:', error);
        this.$message.error('加载地图文件失败');
      }
    },
    
    // 使用占位图绘制地图
    drawMapWithPlaceholder() {
      if (!this.ctx) return;
      
      // 清空画布
      this.ctx.clearRect(0, 0, this.mapCanvas.width, this.mapCanvas.height);
      
      // 绘制占位地图背景
      const centerX = this.mapCanvas.width / 2;
      const centerY = this.mapCanvas.height / 2;
      const imageWidth = 600 * this.zoom;
      const imageHeight = 400 * this.zoom;
      
      this.canvasOffset.x = centerX - imageWidth / 2;
      this.canvasOffset.y = centerY - imageHeight / 2;
      
      // 绘制灰色背景
      this.ctx.fillStyle = '#e0e0e0';
      this.ctx.fillRect(
        this.canvasOffset.x,
        this.canvasOffset.y,
        imageWidth,
        imageHeight
      );
      
      // 绘制网格线
      this.ctx.strokeStyle = '#c0c0c0';
      this.ctx.lineWidth = 1;
      
      // 垂直线
      for (let x = this.canvasOffset.x; x < this.canvasOffset.x + imageWidth; x += 50 * this.zoom) {
        this.ctx.beginPath();
        this.ctx.moveTo(x, this.canvasOffset.y);
        this.ctx.lineTo(x, this.canvasOffset.y + imageHeight);
        this.ctx.stroke();
      }
      
      // 水平线
      for (let y = this.canvasOffset.y; y < this.canvasOffset.y + imageHeight; y += 50 * this.zoom) {
        this.ctx.beginPath();
        this.ctx.moveTo(this.canvasOffset.x, y);
        this.ctx.lineTo(this.canvasOffset.x + imageWidth, y);
        this.ctx.stroke();
      }
      
      // 绘制地图标题
      this.ctx.fillStyle = '#606266';
      this.ctx.font = `${16 * this.zoom}px Arial`;
      this.ctx.textAlign = 'center';
      this.ctx.fillText(
        'YahboomCar Map (PGM格式占位图)',
        centerX,
        this.canvasOffset.y + 30 * this.zoom
      );
      
      // 绘制地图信息
      this.ctx.fillStyle = '#909399';
      this.ctx.font = `${12 * this.zoom}px Arial`;
      this.ctx.fillText(
        '地图尺寸: 600x400px | 分辨率: 0.05m/像素',
        centerX,
        this.canvasOffset.y + imageHeight - 20 * this.zoom
      );
      
      // 绘制标注点
      this.drawMarkers();
      
      // 绘制路径
      this.drawPath();
    },
    
    // 解析YAML文件
    parseYaml(yamlText) {
      const yaml = {};
      const lines = yamlText.split('\n');
      
      lines.forEach(line => {
        line = line.trim();
        if (line && !line.startsWith('#')) {
          const [key, value] = line.split(':').map(item => item.trim());
          if (key && value) {
            yaml[key] = isNaN(value) ? value : parseFloat(value);
          }
        }
      });
      
      return yaml;
    },
    
    // 绘制地图
    drawMap() {
      if (!this.mapLoaded || !this.ctx) return;
      
      // 使用占位图绘制地图
      this.drawMapWithPlaceholder();
    },
    
    // 绘制标注点
    drawMarkers() {
      this.markers.forEach(marker => {
        const canvasX = this.canvasOffset.x + marker.x * this.zoom;
        const canvasY = this.canvasOffset.y + marker.y * this.zoom;
        
        // 绘制圆圈
        this.ctx.beginPath();
        this.ctx.arc(canvasX, canvasY, 8, 0, Math.PI * 2);
        this.ctx.fillStyle = '#409eff';
        this.ctx.fill();
        this.ctx.strokeStyle = '#ffffff';
        this.ctx.lineWidth = 2;
        this.ctx.stroke();
        
        // 绘制文字
        this.ctx.fillStyle = '#303133';
        this.ctx.font = '12px Arial';
        this.ctx.textAlign = 'center';
        this.ctx.fillText(marker.name, canvasX, canvasY + 20);
      });
    },
    
    // 绘制路径
    drawPath() {
      if (this.path.length < 2) return;
      
      this.ctx.beginPath();
      this.ctx.moveTo(
        this.canvasOffset.x + this.path[0].x * this.zoom,
        this.canvasOffset.y + this.path[0].y * this.zoom
      );
      
      for (let i = 1; i < this.path.length; i++) {
        this.ctx.lineTo(
          this.canvasOffset.x + this.path[i].x * this.zoom,
          this.canvasOffset.y + this.path[i].y * this.zoom
        );
      }
      
      this.ctx.strokeStyle = '#67c23a';
      this.ctx.lineWidth = 3;
      this.ctx.stroke();
    },
    
    // 处理画布点击事件
    handleCanvasClick(event) {
      const rect = this.mapCanvas.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      
      // 转换为地图坐标
      const mapX = (x - this.canvasOffset.x) / this.zoom;
      const mapY = (y - this.canvasOffset.y) / this.zoom;
      
      // 如果有新点位名称，添加点位
      if (this.newMarker.name) {
        const markerName = this.newMarker.name;
        const markerX = Math.round(mapX);
        const markerY = Math.round(mapY);
        
        this.markers.push({
          id: this.markerId++,
          name: markerName,
          x: markerX,
          y: markerY
        });
        
        this.addLog(`✅ 添加点位: ${markerName} (${markerX}, ${markerY})`);
        this.newMarker.name = '';
        this.drawMap();
      }
    },
    
    // 添加点位
    addMarker() {
      if (!this.newMarker.name) {
        this.$message.warning('请输入点位名称');
        return;
      }
      
      this.addLog(`💡 提示: 请在地图上点击选择点位位置`);
      // 这里可以添加默认位置或提示用户在地图上点击
      this.$message.info('请在地图上点击选择点位位置');
    },
    
    // 删除点位
    deleteMarker(id) {
      const marker = this.markers.find(m => m.id === id);
      if (marker) {
        this.addLog(`❌ 删除点位: ${marker.name} (${marker.x}, ${marker.y})`);
      }
      
      this.markers = this.markers.filter(marker => marker.id !== id);
      this.drawMap();
    },
    
    // 清除所有标注
    clearMarkers() {
      this.addLog(`🗑️  清除所有标注点位`);
      this.markers = [];
      this.markerId = 1;
      this.drawMap();
    },
    
    // 放大
    zoomIn() {
      this.zoom = Math.min(this.zoom + 0.5, 3);
      this.addLog(`🔍 放大地图，当前缩放级别: ${this.zoom}`);
      this.drawMap();
    },
    
    // 缩小
    zoomOut() {
      this.zoom = Math.max(this.zoom - 0.5, 0.5);
      this.addLog(`🔍 缩小地图，当前缩放级别: ${this.zoom}`);
      this.drawMap();
    },
    
    // 加载地图
    loadMap() {
      this.addLog(`🗺️  开始加载地图...`);
      this.loadMapFiles();
      this.$message.success('地图加载成功');
    },
    
    // 规划路径
    planPath() {
      if (this.markers.length < 2) {
        this.$message.warning('至少需要2个点位才能规划路径');
        return;
      }
      
      // 简单的直线路径规划，实际项目中可以替换为更复杂的算法
      this.path = [...this.markers.map(m => ({ x: m.x, y: m.y }))];
      
      this.addLog(`📏 规划路径成功，路径点数量: ${this.path.length}`);
      this.addLog(`📋 路径详情: ${this.path.map(p => `(${p.x},${p.y})`).join(' → ')}`);
      
      this.drawMap();
      this.$message.success('路径规划成功');
    },
    
    // 清除路径
    clearPath() {
      this.addLog(`🗑️  清除导航路径`);
      this.path = [];
      this.drawMap();
    },
    
    // 发送导航目标
    sendNavigationGoal() {
      if (!this.rosConnected || this.markers.length === 0) {
        this.$message.error('ROS未连接或没有标注点位');
        return;
      }
      
      // 使用最后一个标记点作为导航目标
      const targetMarker = this.markers[this.markers.length - 1];
      this.currentGoal = {
        x: targetMarker.x,
        y: targetMarker.y
      };
      
      // 创建导航目标消息，只包含goal字段（ActionClient会自动处理goal_id）
      const goalMessage = {
        pose: {
          header: {
            frame_id: 'map',
            stamp: {
              sec: 0,
              nanosec: 0
            }
          },
          pose: {
            position: {
              x: targetMarker.x,
              y: targetMarker.y,
              z: 0.0
            },
            orientation: {
              x: 0.0,
              y: 0.0,
              z: 0.0,
              w: 1.0
            }
          }
        },
        behavior_tree: ''
      };
      
      // 详细日志记录
      this.addLog(`📤 准备发送导航目标`);
      this.addLog(`📋 目标位置: (${targetMarker.x}, ${targetMarker.y})`);
      this.addLog(`📋 完整消息格式: ${JSON.stringify(goalMessage)}`);
      
      try {
        // 优先使用ActionClient
        if (this.navigateToPoseAction) {
          this.addLog(`📤 使用ActionClient发送导航目标`);
          
          // 创建Goal对象
          this.navigationGoal = new ROSLIB.Goal({
            actionClient: this.navigateToPoseAction,
            goalMessage: goalMessage
          });
          
          // 记录目标ID
          this.currentGoalId = this.navigationGoal.id;
          
          // 监听状态变化
          this.navigationGoal.on('status', (status) => {
            this.navigationStatus = this.getNavigationStatusText(status);
            this.addLog(`导航状态更新: ${this.navigationStatus}`);
          });
          
          // 监听反馈
          this.navigationGoal.on('feedback', (feedback) => {
            this.navigationFeedback = JSON.stringify(feedback);
            this.addLog(`导航反馈: ${JSON.stringify(feedback.navigation_time)}`);
          });
          
          // 监听结果
          this.navigationGoal.on('result', (result) => {
            this.addLog(`导航完成: ${JSON.stringify(result)}`);
            this.navigationStatus = '导航完成';
            this.currentGoal = null;
            this.currentGoalId = null;
          });
          
          // 监听被拒绝
          this.navigationGoal.on('rejected', () => {
            this.addLog('导航目标被拒绝');
            this.navigationStatus = '目标被拒绝';
            this.currentGoal = null;
            this.currentGoalId = null;
          });
          
          // 发送目标
          this.navigationGoal.send();
          this.navigationStatus = '发送目标中';
          this.navigationFeedback = '';
          
          this.addLog(`📤 已发送导航Action目标，目标ID: ${this.currentGoalId}`);
          this.$message.success('导航目标已发送到/navigate_to_pose Action');
        } else {
          // 回退到Topic发布方式
          this.addLog(`📤 ActionClient不可用，使用Topic方式发送`);
          this.tryTopicPublish(goalMessage);
        }
      } catch (error) {
        this.addLog(`❌ ActionClient发送失败: ${error.message}`);
        this.addLog(`💡 错误详情: ${error.stack}`);
        this.$message.error('发送导航目标失败，尝试回退到Topic方式');
        
        // 回退到Topic发布方式
        this.tryTopicPublish(goalMessage);
      }
    },
    
    // 取消导航目标
    cancelNavigationGoal() {
      if (!this.currentGoal) {
        this.$message.warning('没有正在执行的导航目标');
        return;
      }
      
      // 重置导航状态
      this.addLog(`📤 取消导航目标`);
      this.navigationStatus = '已取消';
      this.currentGoal = null;
      this.navigationFeedback = '';
      this.$message.success('导航目标已取消');
    },
    
    // 获取导航状态文本
    getNavigationStatusText(status) {
      const statusMap = {
        0: '等待中',
        1: '执行中',
        2: '已取消',
        3: '成功',
        4: '失败'
      };
      return statusMap[status] || `未知状态(${status})`;
    },
    
    // 尝试使用Topic方式发布导航目标（作为ActionClient的备选）
    tryTopicPublish(goalMsg) {
      try {
        this.addLog('💡 尝试使用Topic方式发送...');
        
        // 创建完整的Action Goal消息，包含goal_id和goal字段
        const completeGoalMsg = {
          goal_id: {
            stamp: {
              sec: Math.floor(Date.now() / 1000),
              nanosec: (Date.now() % 1000) * 1000000
            },
            id: `goal_${Date.now()}`
          },
          goal: goalMsg
        };
        
        // 不指定消息类型，让rosbridge自动处理
        const goalTopic = new ROSLIB.Topic({
          ros: this.ros,
          name: '/navigate_to_pose/goal'
        });
        
        goalTopic.publish(completeGoalMsg);
        this.navigationStatus = '目标已发送';
        this.navigationFeedback = '';
        this.currentGoalId = completeGoalMsg.goal_id.id;
        
        this.addLog(`📤 已使用Topic方式发送导航目标，目标ID: ${this.currentGoalId}`);
        this.$message.success('已使用Topic方式发送导航目标');
      } catch (altError) {
        this.addLog(`❌ Topic方式也失败: ${altError.message}`);
        this.addLog(`💡 错误详情: ${altError.stack}`);
        this.$message.error('发送导航目标失败，请检查ROS2配置');
      }
    },
    
    // 手动发送导航目标
    sendManualNavigationGoal() {
      if (!this.rosConnected) {
        this.$message.error('ROS未连接');
        return;
      }
      
      // 使用手动输入的坐标作为导航目标
      this.currentGoal = {
        x: this.manualGoal.x,
        y: this.manualGoal.y
      };
      
      // 创建导航目标消息，只包含goal字段（ActionClient会自动处理goal_id）
      const goalMessage = {
        pose: {
          header: {
            frame_id: 'map',
            stamp: {
              sec: 0,
              nanosec: 0
            }
          },
          pose: {
            position: {
              x: this.manualGoal.x,
              y: this.manualGoal.y,
              z: this.manualGoal.z
            },
            orientation: {
              x: 0.0,
              y: 0.0,
              z: 0.0,
              w: 1.0
            }
          }
        },
        behavior_tree: ''
      };
      
      // 详细日志记录
      this.addLog(`📤 准备发送手动导航目标`);
      this.addLog(`📋 手动输入位置: (${this.manualGoal.x}, ${this.manualGoal.y}, ${this.manualGoal.z})`);
      this.addLog(`📋 完整消息格式: ${JSON.stringify(goalMessage)}`);
      
      try {
        // 优先使用ActionClient
        if (this.navigateToPoseAction) {
          this.addLog(`📤 使用ActionClient发送手动导航目标`);
          
          // 创建Goal对象
          this.navigationGoal = new ROSLIB.Goal({
            actionClient: this.navigateToPoseAction,
            goalMessage: goalMessage
          });
          
          // 记录目标ID
          this.currentGoalId = this.navigationGoal.id;
          
          // 监听状态变化
          this.navigationGoal.on('status', (status) => {
            this.navigationStatus = this.getNavigationStatusText(status);
            this.addLog(`导航状态更新: ${this.navigationStatus}`);
          });
          
          // 监听反馈
          this.navigationGoal.on('feedback', (feedback) => {
            this.navigationFeedback = JSON.stringify(feedback);
            this.addLog(`导航反馈: ${JSON.stringify(feedback.navigation_time)}`);
          });
          
          // 监听结果
          this.navigationGoal.on('result', (result) => {
            this.addLog(`导航完成: ${JSON.stringify(result)}`);
            this.navigationStatus = '导航完成';
            this.currentGoal = null;
            this.currentGoalId = null;
          });
          
          // 监听被拒绝
          this.navigationGoal.on('rejected', () => {
            this.addLog('导航目标被拒绝');
            this.navigationStatus = '目标被拒绝';
            this.currentGoal = null;
            this.currentGoalId = null;
          });
          
          // 发送目标
          this.navigationGoal.send();
          this.navigationStatus = '发送目标中';
          this.navigationFeedback = '';
          
          this.addLog(`📤 已发送手动导航Action目标，目标ID: ${this.currentGoalId}`);
          this.$message.success('手动导航目标已发送到/navigate_to_pose Action');
        } else {
          // 回退到Topic发布方式
          this.addLog(`📤 ActionClient不可用，使用Topic方式发送手动导航目标`);
          this.tryTopicPublish(goalMessage);
        }
      } catch (error) {
        this.addLog(`❌ ActionClient发送失败: ${error.message}`);
        this.addLog(`💡 错误详情: ${error.stack}`);
        this.$message.error('发送手动导航目标失败，尝试回退到Topic方式');
        
        // 回退到Topic发布方式
        this.tryTopicPublish(goalMessage);
      }
    }
  }
};
</script>

<style scoped>
.navigation-planning {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  background-color: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

h2 {
  color: #303133;
  margin-bottom: 20px;
  text-align: center;
}

h3 {
  color: #606266;
  margin: 20px 0 15px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

h4 {
  color: #606266;
  margin: 15px 0;
}

/* 地图区域样式 */
.map-section {
  margin-bottom: 30px;
}

.map-container {
  position: relative;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  background-color: #fafafa;
}

.map-canvas {
  width: 100%;
  height: 500px;
  cursor: crosshair;
}

.map-controls {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 10px;
  z-index: 10;
}

.zoom-controls {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

/* 标注区域样式 */
.markers-section {
  margin-bottom: 30px;
}

.markers-list {
  margin-bottom: 20px;
}

.add-marker {
  padding: 15px;
  background-color: #f0f2f5;
  border-radius: 4px;
}

/* 路径区域样式 */
.path-section {
  margin-bottom: 30px;
}

.path-controls {
  margin-bottom: 15px;
  display: flex;
  gap: 10px;
}

.path-info {
  padding: 15px;
  background-color: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 4px;
  color: #67c23a;
}

.path-info p {
  margin: 5px 0;
}

/* ROS状态区域样式 */
.status-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 15px;
  background-color: #f0f2f5;
  border-radius: 4px;
}

.ros-status {
  display: flex;
  align-items: center;
  padding: 8px 15px;
  border-radius: 20px;
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.ros-status.connected {
  background-color: #f0f9eb;
  color: #67c23a;
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #409eff;
  margin-right: 8px;
  animation: pulse 1.5s infinite;
}

.ros-status.connected .status-indicator {
  background-color: #67c23a;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.7;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.connection-form {
  display: flex;
  align-items: center;
}

/* ROS导航控制区域样式 */
.ros-navigation-section {
  margin-bottom: 30px;
  padding: 15px;
  background-color: #f0f2f5;
  border-radius: 4px;
}

.navigation-controls {
  margin-bottom: 15px;
  display: flex;
  gap: 10px;
}

.navigation-status {
  margin-top: 15px;
  padding: 15px;
  background-color: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 4px;
}

.navigation-status h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #409eff;
}

.navigation-status p {
  margin: 5px 0;
  font-size: 14px;
  color: #606266;
}

.navigation-status p:last-child {
  word-break: break-all;
  white-space: pre-wrap;
}

/* 手动导航区域样式 */
.manual-navigation-section {
  margin-bottom: 30px;
  padding: 15px;
  background-color: #f0f2f5;
  border-radius: 4px;
}

.manual-navigation-section .el-form {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.manual-navigation-section .el-form-item {
  margin-right: 20px;
  margin-bottom: 15px;
}

.manual-navigation-tips {
  padding: 15px;
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
}

.manual-navigation-tips h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #52c41a;
}

.manual-navigation-tips ul {
  margin: 0;
  padding-left: 20px;
  color: #606266;
}

.manual-navigation-tips li {
  margin: 5px 0;
  font-size: 14px;
  line-height: 1.5;
}

/* 日志区域样式 */
.log-section {
  margin-top: 30px;
}

.log-container {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
  background-color: #fafafa;
}

.log-item {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.log-time {
  color: #909399;
  margin-right: 15px;
  min-width: 100px;
}

.log-content {
  color: #303133;
  flex: 1;
}
</style>
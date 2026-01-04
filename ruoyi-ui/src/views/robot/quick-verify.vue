<template>
  <div class="quick-verify">
    <div class="container">
      <div class="layout">
        <!-- 左侧点位列表 -->
        <div class="sidebar">
          <h3>保存的点位</h3>
          <div class="points-container">
            <div v-if="savedPoints.length === 0" class="empty-points">
              暂无保存的点位
            </div>
            <div 
              v-for="point in savedPoints" 
              :key="point.id" 
              class="point-item"
            >
              <div class="point-info">
                <div class="point-name">{{ point.name }}</div>
                <div class="point-coords">
                  X: {{ point.xPos.toFixed(2) }}, Y: {{ point.yPos.toFixed(2) }}
                </div>
              </div>
              <el-button 
                type="primary" 
                size="small" 
                @click="sendSavedPoint(point)"
                :disabled="!rosConnected"
              >
                发送
              </el-button>
            </div>
          </div>
          <el-button 
            type="default" 
            size="small" 
            class="refresh-btn"
            @click="loadSavedPoints"
          >
            刷新列表
          </el-button>
        </div>
        
        <!-- 右侧主要内容 -->
        <div class="main-content">
          <h2>快速验证导航功能</h2>
          
          <!-- ROS连接配置 -->
          <div class="connection-section">
            <h3>ROS连接配置</h3>
            <el-form :model="rosConfig" label-width="120px">
              <el-form-item label="ROSBridge地址">
                <el-input 
                  v-model="rosConfig.url" 
                  placeholder="ws://192.168.x.x:9090" 
                  style="width: 300px;"
                ></el-input>
              </el-form-item>
              <el-form-item>
                <el-button 
                  type="primary" 
                  @click="connectRos" 
                  :disabled="rosConnected"
                >
                  连接ROS
                </el-button>
                <el-button 
                  @click="disconnectRos" 
                  :disabled="!rosConnected"
                >
                  断开连接
                </el-button>
              </el-form-item>
            </el-form>
            <div class="connection-status" :class="{ connected: rosConnected }">
              <span class="status-indicator"></span>
              <span>ROSBridge {{ rosConnected ? '已连接' : '未连接' }}</span>
            </div>
          </div>
          
          <!-- 导航目标配置 -->
          <div class="goal-section">
            <h3>导航目标配置</h3>
            <el-form :model="navGoal" label-width="120px" inline>
              <el-form-item label="点位名称">
                <el-input 
                  v-model="navGoal.name" 
                  placeholder="请输入点位名称" 
                  style="width: 150px;"
                ></el-input>
              </el-form-item>
              <el-form-item label="X坐标">
                <el-input 
                  v-model="navGoal.x" 
                  type="text" 
                  placeholder="2.0" 
                  style="width: 100px;"
                ></el-input>
              </el-form-item>
              <el-form-item label="Y坐标">
                <el-input 
                  v-model="navGoal.y" 
                  type="text" 
                  placeholder="1.5" 
                  style="width: 100px;"
                ></el-input>
              </el-form-item>
              <el-form-item label="Z坐标">
                <el-input 
                  v-model="navGoal.z" 
                  type="text"
                  placeholder="0.0" 
                  style="width: 100px;"
                  :disabled="true"
                ></el-input>
              </el-form-item>
              <el-form-item>
                <el-button 
                  type="success" 
                  @click="sendGoal" 
                  :disabled="!rosConnected"
                >
                  发送导航目标
                </el-button>
                <el-button 
                  type="danger" 
                  @click="cancelGoal" 
                  :disabled="!rosConnected"
                >
                  中断导航
                </el-button>
                <el-button 
                  type="primary" 
                  @click="saveNavPoint"
                  :disabled="!navGoal.name || !navGoal.x || !navGoal.y"
                >
                  保存点位
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          
          <!-- 语音合成功能 -->
          <div class="tts-section">
            <h3>语音合成</h3>
            <el-form :model="ttsRequest" label-width="120px">
              <el-form-item label="语音内容">
                <el-input 
                  v-model="ttsRequest.text" 
                  type="textarea" 
                  placeholder="请输入要合成的文本" 
                  rows="3"
                  style="width: 500px;"
                ></el-input>
              </el-form-item>
              <el-form-item label="语言">
                <el-select v-model="ttsRequest.language" style="width: 150px;">
                  <el-option label="中文" value="zh"></el-option>
                  <el-option label="英文" value="en"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="语速(0-100)">
                <el-slider 
                  v-model="ttsRequest.speed" 
                  :min="0" 
                  :max="100" 
                  style="width: 200px;"
                ></el-slider>
                <span class="slider-value">{{ ttsRequest.speed }}</span>
              </el-form-item>
              <el-form-item label="音调(0-100)">
                <el-slider 
                  v-model="ttsRequest.pitch" 
                  :min="0" 
                  :max="100" 
                  style="width: 200px;"
                ></el-slider>
                <span class="slider-value">{{ ttsRequest.pitch }}</span>
              </el-form-item>
              <el-form-item label="音量(0-100)">
                <el-slider 
                  v-model="ttsRequest.volume" 
                  :min="0" 
                  :max="100" 
                  style="width: 200px;"
                ></el-slider>
                <span class="slider-value">{{ ttsRequest.volume }}</span>
              </el-form-item>
              <el-form-item>
                <el-button 
                  type="warning" 
                  @click="sendTextToSpeech" 
                  :disabled="!rosConnected || !ttsRequest.text.trim()"
                >
                  发送语音
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          
          <!-- 日志输出 -->
          <div class="logs-section">
            <h3>日志输出</h3>
            <div class="logs-container">
              <div v-for="(log, index) in logs" :key="index" class="log-item">
                <span class="log-time">{{ log.time }}</span>
                <span class="log-content">{{ log.content }}</span>
              </div>
            </div>
          </div>
          
          <!-- 结果显示 -->
          <div class="result-section" v-if="result">
            <h3>导航结果</h3>
            <pre class="result-content">{{ JSON.stringify(result, null, 2) }}</pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { connectRos2, disconnectRos2, getRos2Status, sendNavigationGoal, cancelNavigationGoal } from '@/api/ros2/navigation'
import { saveNav2Point, getAllNav2Points } from '@/api/ros2/point'
import { sendTextToSpeech } from '@/api/ros2/tts'

export default {
  name: 'QuickVerify',
  data() {
    return {
      // ROS配置
      rosConfig: {
        url: 'ws://192.168.31.177:9090'
      },
      rosConnected: false,
      // 导航目标
      navGoal: {
        name: '',
        x: 2.0,
        y: 1.5,
        z: 0.0
      },
      // 保存的点位列表
      savedPoints: [],
      // 日志
      logs: [],
      // 结果
      result: null,
      // 语音合成
      ttsRequest: {
        text: '你好，这是一个文本转语音测试',
        language: 'zh',
        speed: 50,
        pitch: 50,
        volume: 50
      }
    };
  },
  mounted() {
    // 页面加载时检查ROS2连接状态
    this.checkRos2Status();
    // 加载保存的点位列表
    this.loadSavedPoints();
  },
  beforeUnmount() {
    // 页面卸载时断开ROS2连接
    this.disconnectRos();
  },
  methods: {
    // 检查ROS2连接状态
    async checkRos2Status() {
      try {
        const response = await getRos2Status();
        this.rosConnected = response.data.connected;
        this.addLog(`ROS2连接状态: ${this.rosConnected ? '已连接' : '未连接'}`);
      } catch (error) {
        this.addLog(`检查ROS2状态失败: ${error.message}`);
      }
    },

    // 连接ROS2
    async connectRos() {
      try {
        this.addLog(`正在连接ROS2: ${this.rosConfig.url}`);
        const response = await connectRos2(this.rosConfig.url);
        
        if (response.code === 200) {
          this.rosConnected = true;
          this.addLog('ROS2连接成功');
          this.$message.success('ROS2连接成功');
        } else {
          this.rosConnected = false;
          this.addLog(`ROS2连接失败: ${response.msg}`);
          this.$message.error(response.msg || 'ROS2连接失败');
        }
      } catch (error) {
        this.rosConnected = false;
        this.addLog(`连接ROS2失败: ${error.message}`);
        this.$message.error('连接ROS2失败，请检查网络和地址');
      }
    },

    // 断开ROS2连接
    async disconnectRos() {
      if (!this.rosConnected) {
        return;
      }

      try {
        const response = await disconnectRos2();
        if (response.code === 200) {
          this.rosConnected = false;
          this.addLog('ROS2连接已断开');
          this.$message.success('ROS2连接已断开');
        } else {
          this.addLog(`断开ROS2连接失败: ${response.msg}`);
        }
      } catch (error) {
        this.addLog(`断开ROS2连接失败: ${error.message}`);
      }
    },

    // 发送导航目标
    async sendGoal() {
      if (!this.rosConnected) {
        this.addLog('ROS2未连接，请先连接ROS2');
        this.$message.warning('ROS2未连接，请先连接ROS2');
        return;
      }

      try {
        this.addLog('正在发送导航目标...');
        this.addLog(`目标位置: (${this.navGoal.x}, ${this.navGoal.y}, ${this.navGoal.z})`);

        const navigationGoal = {
          x: this.navGoal.x,
          y: this.navGoal.y,
          z: this.navGoal.z,
          orientationX: 0.0,
          orientationY: 0.0,
          orientationZ: 0.0,
          orientationW: 1.0,
          behaviorTree: ''
        };

        const response = await sendNavigationGoal(navigationGoal);
        
        if (response.code === 200) {
          this.addLog('导航目标发送成功');
          this.$message.success('导航目标发送成功');
        } else {
          this.addLog(`导航目标发送失败: ${response.msg}`);
          this.$message.error(response.msg || '导航目标发送失败');
        }
      } catch (error) {
        this.addLog(`发送导航目标失败: ${error.message}`);
        this.$message.error('发送导航目标失败，请检查ROS2配置');
      }
    },

    // 中断导航
    async cancelGoal() {
      if (!this.rosConnected) {
        this.addLog('ROS2未连接，请先连接ROS2');
        this.$message.warning('ROS2未连接，请先连接ROS2');
        return;
      }

      try {
        this.addLog('正在中断导航...');

        const response = await cancelNavigationGoal();
        
        if (response.code === 200) {
          this.addLog('导航中断成功');
          this.$message.success('导航中断成功');
        } else {
          this.addLog(`导航中断失败: ${response.msg}`);
          this.$message.error(response.msg || '导航中断失败');
        }
      } catch (error) {
        this.addLog(`中断导航失败: ${error.message}`);
        this.$message.error('中断导航失败，请检查ROS2配置');
      }
    },

    // 加载保存的点位列表
    async loadSavedPoints() {
      try {
        this.addLog('正在加载保存的点位列表...');
        const response = await getAllNav2Points();
        
        if (response.code === 200) {
          this.savedPoints = response.data;
          this.addLog(`✅ 已加载 ${this.savedPoints.length} 个保存的点位`);
        } else {
          this.addLog(`❌ 加载点位列表失败: ${response.msg}`);
          this.$message.error('加载点位列表失败');
        }
      } catch (error) {
        this.addLog(`❌ 加载点位列表时发生异常: ${error.message}`);
        this.$message.error('加载过程中发生异常');
      }
    },

    // 保存导航点位
    async saveNavPoint() {
      if (!this.navGoal.name || !this.navGoal.x || !this.navGoal.y) {
        this.$message.warning('请填写完整的点位信息');
        return;
      }

      try {
        this.addLog(`正在保存导航点位: ${this.navGoal.name} (${this.navGoal.x}, ${this.navGoal.y})`);
        
        const nav2Point = {
          name: this.navGoal.name,
          xPos: this.navGoal.x,
          yPos: this.navGoal.y
        };
        
        const response = await saveNav2Point(nav2Point);
        
        if (response.code === 200) {
          this.addLog(`✅ 导航点位保存成功: ${this.navGoal.name}`);
          this.$message.success('导航点位保存成功');
          // 重新加载点位列表
          this.loadSavedPoints();
        } else {
          this.addLog(`❌ 导航点位保存失败: ${response.msg}`);
          this.$message.error(response.msg || '保存失败');
        }
      } catch (error) {
        this.addLog(`❌ 保存导航点位时发生异常: ${error.message}`);
        this.$message.error('保存过程中发生异常');
      }
    },

    // 发送保存的点位
    async sendSavedPoint(point) {
      try {
        this.addLog(`正在发送保存的点位: ${point.name} (${point.xPos}, ${point.yPos})`);
        
        // 更新当前导航目标
        this.navGoal.name = point.name;
        this.navGoal.x = point.xPos;
        this.navGoal.y = point.yPos;
        
        // 使用现有的发送导航目标方法
        await this.sendGoal();
        
        this.addLog(`✅ 已发送保存的点位: ${point.name}`);
      } catch (error) {
        this.addLog(`❌ 发送保存的点位时发生异常: ${error.message}`);
        this.$message.error('发送过程中发生异常');
      }
    },

    // 发送语音合成请求
    async sendTextToSpeech() {
      if (!this.rosConnected) {
        this.addLog('ROS2未连接，请先连接ROS2');
        this.$message.warning('ROS2未连接，请先连接ROS2');
        return;
      }

      if (!this.ttsRequest.text.trim()) {
        this.$message.warning('请输入要合成的文本');
        return;
      }

      try {
        this.addLog('正在发送语音合成请求...');
        this.addLog(`语音内容: ${this.ttsRequest.text}`);
        this.addLog(`参数: 语言=${this.ttsRequest.language}, 语速=${this.ttsRequest.speed}, 音调=${this.ttsRequest.pitch}, 音量=${this.ttsRequest.volume}`);

        const response = await sendTextToSpeech(this.ttsRequest);
        
        if (response.code === 200) {
          this.addLog('语音合成请求发送成功');
          this.$message.success('语音合成请求发送成功');
        } else {
          this.addLog(`语音合成请求发送失败: ${response.msg}`);
          this.$message.error(response.msg || '语音合成请求发送失败');
        }
      } catch (error) {
        this.addLog(`发送语音合成请求失败: ${error.message}`);
        this.$message.error('发送语音合成请求失败，请检查ROS2配置');
      }
    },

    // 添加日志
    addLog(content) {
      const time = new Date().toLocaleTimeString();
      this.logs.unshift({ time, content });
      
      // 限制日志数量
      if (this.logs.length > 100) {
        this.logs.pop();
      }
    }
  }
};
</script>

<style scoped>
.quick-verify {
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

.layout {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}

/* 左侧边栏样式 */
.sidebar {
  width: 300px;
  background-color: #f8f9fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  height: fit-content;
  min-height: 500px;
}

.sidebar h3 {
  margin: 0 0 20px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 16px;
  color: #303133;
}

.points-container {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 20px;
}

.empty-points {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
  font-size: 14px;
}

.point-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  margin-bottom: 12px;
  background-color: white;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.point-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.point-info {
  flex: 1;
  min-width: 0;
}

.point-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 5px;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.point-coords {
  font-size: 13px;
  color: #606266;
}

.refresh-btn {
  width: 100%;
  margin-top: 10px;
}

/* 右侧主要内容样式 */
.main-content {
  flex: 1;
  min-width: 0;
}

h2 {
  color: #303133;
  margin-bottom: 20px;
  text-align: center;
}

h3 {
  color: #606266;
  margin: 25px 0 15px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

/* 连接区域样式 */
.connection-section {
  margin-bottom: 25px;
}

.connection-status {
  margin-top: 15px;
  display: inline-flex;
  align-items: center;
  padding: 8px 15px;
  border-radius: 20px;
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.connection-status.connected {
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

.connection-status.connected .status-indicator {
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

/* 目标区域样式 */
.goal-section {
  margin-bottom: 25px;
}

/* 语音合成区域样式 */
.tts-section {
  margin-bottom: 25px;
}

.slider-value {
  margin-left: 10px;
  font-size: 14px;
  color: #606266;
  min-width: 30px;
  display: inline-block;
}

/* 日志区域样式 */
.logs-section {
  margin-bottom: 25px;
}

.logs-container {
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
  word-break: break-all;
}

/* 结果区域样式 */
.result-section {
  margin-bottom: 25px;
}

.result-content {
  padding: 15px;
  background-color: #f0f2f5;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
  font-family: monospace;
  font-size: 14px;
}
</style>
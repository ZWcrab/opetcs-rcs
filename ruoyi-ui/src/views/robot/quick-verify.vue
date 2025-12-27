<template>
  <div class="quick-verify">
    <div class="container">
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
          <el-form-item label="X坐标">
            <el-input 
              v-model.number="navGoal.x" 
              type="number" 
              placeholder="2.0" 
              style="width: 100px;"
            ></el-input>
          </el-form-item>
          <el-form-item label="Y坐标">
            <el-input 
              v-model.number="navGoal.y" 
              type="number" 
              placeholder="1.5" 
              style="width: 100px;"
            ></el-input>
          </el-form-item>
          <el-form-item label="Z坐标">
            <el-input 
              v-model.number="navGoal.z" 
              type="number" 
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
</template>

<script>
import { connectRos2, disconnectRos2, getRos2Status, sendNavigationGoal, cancelNavigationGoal } from '@/api/ros2/navigation'

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
        x: 2.0,
        y: 1.5,
        z: 0.0
      },
      // 日志
      logs: [],
      // 结果
      result: null
    };
  },
  mounted() {
    // 页面加载时检查ROS2连接状态
    this.checkRos2Status();
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
  max-width: 800px;
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
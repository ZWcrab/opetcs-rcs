<template>
  <div class="yahboom-robot">
    <div class="container">
      <h2>Yahboom机器人控制</h2>
      
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
      
      <!-- 控制按钮区域 -->
      <div class="control-section">
        <h3>机器人控制</h3>
        <div class="button-grid">
          <div class="button-row">
            <el-button type="primary" @click="moveForward" :disabled="!rosConnected">前进</el-button>
          </div>
          <div class="button-row">
            <el-button type="primary" @click="turnLeft" :disabled="!rosConnected">左转90°</el-button>
            <el-button type="danger" @click="stop" :disabled="!rosConnected" style="margin: 0 10px;">停止</el-button>
            <el-button type="primary" @click="turnRight" :disabled="!rosConnected">右转90°</el-button>
          </div>
          <div class="button-row">
            <el-button type="primary" @click="moveBackward" :disabled="!rosConnected">后退</el-button>
          </div>
        </div>
      </div>
      
      <!-- 发送日志区域 -->
      <div class="log-section">
        <h3>发送日志</h3>
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
  name: 'YahboomRobot',
  data() {
    return {
      rosBridgeUrl: 'ws://192.168.31.177:9090',
      ros: null,
      rosConnected: false,
      cmdVelTopic: null,
      logs: [],
      linearSpeed: 0.5,
      angularSpeed: 0.8,
      roslibLoaded: false,
      roslibLoading: false
    };
  },
  mounted() {
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
        this.ros.close();
        this.ros = null;
        this.rosConnected = false;
        this.cmdVelTopic = null;
      }
    },
    
    // 初始化ROS话题
    initTopics() {
      // 初始化速度控制话题，使用VOLATILE策略（队列大小1，不缓存）
      this.cmdVelTopic = new ROSLIB.Topic({
        ros: this.ros,
        name: '/cmd_vel',
        messageType: 'geometry_msgs/Twist',
        queue_size: 1, // 队列大小1，只保留最新消息
        latch: false, // 不缓存消息
        throttle_rate: 0 // 不限制发布频率
      });
      this.addLog('已初始化/cmd_vel话题（VOLATILE策略）');
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
    
    // 发送速度指令
    sendVelCommand(linearX, angularZ) {
      if (!this.rosConnected || !this.cmdVelTopic) {
        this.$message.error('ROSBridge未连接或话题未初始化');
        return;
      }
      
      const twist = new ROSLIB.Message({
        linear: {
          x: linearX,
          y: 0,
          z: 0
        },
        angular: {
          x: 0,
          y: 0,
          z: angularZ
        }
      });
      
      this.cmdVelTopic.publish(twist);
      this.addLog(`发送指令: linear.x=${linearX}, angular.z=${angularZ}`);
    },
    
    // 前进
    moveForward() {
      this.sendVelCommand(this.linearSpeed, 0);
    },
    
    // 后退
    moveBackward() {
      this.sendVelCommand(-this.linearSpeed, 0);
    },
    
    // 左转90度
    turnLeft() {
      // 计算旋转90度所需的时间（根据角速度计算）
      // 90度 = π/2 弧度
      const turnTime = Math.PI / 2 / Math.abs(this.angularSpeed);
      this.sendVelCommand(0, this.angularSpeed);
      
      // 旋转完成后停止
      setTimeout(() => {
        this.stop();
      }, turnTime * 1000);
    },
    
    // 右转90度
    turnRight() {
      // 计算旋转90度所需的时间（根据角速度计算）
      // 90度 = π/2 弧度
      const turnTime = Math.PI / 2 / Math.abs(this.angularSpeed);
      this.sendVelCommand(0, -this.angularSpeed);
      
      // 旋转完成后停止
      setTimeout(() => {
        this.stop();
      }, turnTime * 1000);
    },
    
    // 停止
    stop() {
      this.sendVelCommand(0, 0);
    }
  }
};
</script>

<style scoped>
.yahboom-robot {
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
  margin: 20px 0 15px 0;
}

/* 状态区域样式 */
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

/* 控制区域样式 */
.control-section {
  margin-bottom: 30px;
}

.button-grid {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.button-row {
  display: flex;
  gap: 10px;
}

.button-row button {
  width: 120px;
  height: 40px;
  font-size: 16px;
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
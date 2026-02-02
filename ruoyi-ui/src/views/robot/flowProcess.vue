<template>
  <div class="app-container flow-process-container">
    <div class="header-actions">
      <div class="connection-status">
        <span class="status-dot" :class="rosConnected ? 'connected' : 'disconnected'"></span>
        <el-input 
          v-model="rosBridgeUrl" 
          placeholder="WebSocket URL" 
          size="small" 
          style="width: 250px; margin: 0 10px"
          :disabled="rosConnected"
        ></el-input>
        <el-button type="text" @click="handleConnection" size="small">
          {{ rosConnected ? '断开' : '连接' }}
        </el-button>
      </div>
      <div class="execution-controls">
        <el-button 
          type="primary" 
          icon="el-icon-video-play" 
          @click="startExecution" 
          :disabled="!rosConnected || processList.length === 0 || isExecuting"
        >
          开始执行
        </el-button>
        <el-button 
          type="danger" 
          icon="el-icon-video-pause" 
          @click="stopExecution" 
          :disabled="!isExecuting"
        >
          停止
        </el-button>
        <el-button 
          icon="el-icon-delete" 
          @click="clearProcess" 
          :disabled="isExecuting || processList.length === 0"
        >
          清空流程
        </el-button>
      </div>
    </div>

    <div class="content-wrapper">
      <!-- 左侧：流程编排 (50%) -->
      <div class="left-column">
        <div class="left-panel-wrapper">
          <!-- 语音列表 -->
          <el-card class="box-card voice-panel">
            <div slot="header" class="clearfix">
              <span>语音列表</span>
            </div>
            <div class="list-container">
              <draggable
                v-model="voiceList"
                :group="{ name: 'points', pull: 'clone', put: false }"
                :clone="cloneVoice"
                @start="drag=true"
                @end="drag=false"
                class="draggable-list"
              >
                <div v-for="element in voiceList" :key="element.id" class="list-item source-item voice-item">
                  <div style="flex: 1">
                    <i class="el-icon-mic"></i>
                    {{ element.name }}
                  </div>
                  <div class="voice-time-input" @mousedown.stop>
                    <el-input-number 
                      v-model="element.waitTime" 
                      size="mini" 
                      :min="1" 
                      :max="60"
                      controls-position="right"
                      style="width: 80px"
                    ></el-input-number>
                    <span class="unit">秒</span>
                  </div>
                </div>
              </draggable>
            </div>
          </el-card>

          <!-- 位置列表 -->
          <el-card class="box-card position-panel">
            <div slot="header" class="clearfix">
              <span>位置列表</span>
              <el-button style="float: right; padding: 3px 0" type="text" @click="loadSavedPositions">刷新</el-button>
            </div>
            <div class="list-container">
              <draggable
                v-model="savedPositions"
                :group="{ name: 'points', pull: 'clone', put: false }"
                :clone="clonePoint"
                @start="drag=true"
                @end="drag=false"
                class="draggable-list"
              >
                <div v-for="element in savedPositions" :key="element.id" class="list-item source-item">
                  <i class="el-icon-location"></i>
                  {{ element.name }}
                </div>
              </draggable>
            </div>
          </el-card>
        </div>

        <!-- 流程面板 -->
        <el-card class="box-card process-panel">
          <div slot="header" class="clearfix">
            <span>执行流程</span>
            <span v-if="isExecuting" class="executing-status">
              (正在执行第 {{ currentStepIndex + 1 }} / {{ processList.length }} 步)
            </span>
          </div>
          <div class="list-container process-container">
            <draggable
              v-model="processList"
              group="points"
              @start="drag=true"
              @end="drag=false"
              class="draggable-list process-list"
            >
              <div 
                v-for="(element, index) in processList" 
                :key="index" 
                class="list-item process-item"
                :class="{ 'active': isExecuting && currentStepIndex === index, 'completed': isExecuting && currentStepIndex > index }"
              >
                <div class="step-index">{{ index + 1 }}</div>
                <div class="step-content">
                  <div class="step-name">
                    <i :class="element.type === 'voice' ? 'el-icon-mic' : 'el-icon-location'"></i>
                    {{ element.name }}
                  </div>
                  <div class="step-coords" v-if="element.type === 'position'">
                    x: {{ element.x.toFixed(2) }}, y: {{ element.y.toFixed(2) }}
                  </div>
                  <div class="step-coords" v-else>
                  <div style="display: flex; align-items: center; gap: 5px;">
                    <span>指令: {{ element.command }}</span>
                    <span style="color: #dcdfe6">|</span>
                    <span>等待:</span>
                    <el-input-number 
                      v-if="!isExecuting"
                      v-model="element.waitTime" 
                      size="mini" 
                      :min="1" 
                      :max="60"
                      controls-position="right"
                      style="width: 100px"
                    ></el-input-number>
                    <span v-else style="font-weight: bold; margin: 0 5px;">{{ element.waitTime }}</span>
                    <span>秒</span>
                  </div>
                </div>
                </div>
                <div class="step-actions">
                  <i class="el-icon-close" @click="removeStep(index)" v-if="!isExecuting"></i>
                  <i class="el-icon-loading" v-if="isExecuting && currentStepIndex === index"></i>
                  <i class="el-icon-check" v-if="isExecuting && currentStepIndex > index"></i>
                </div>
              </div>
              <div class="empty-tip" v-if="processList.length === 0">
                请从左侧拖拽位置到此处
              </div>
            </draggable>
          </div>
        </el-card>
      </div>

      <!-- 右侧：3D地图 (50%) -->
      <div class="right-column">
        <el-card class="box-card map-panel">
          <div slot="header" class="clearfix">
            <span>实时监控</span>
          </div>
          <div class="map-wrapper">
            <MapViewer3D 
              ref="mapViewer"
              :ros="ros"
              :rosConnected="rosConnected"
            />
          </div>
        </el-card>
      </div>
    </div>

    <!-- 实时状态监控 (底部) -->
    <div class="robot-status-bar" v-if="robotPose">
      当前位置: x={{ robotPose.x.toFixed(2) }}, y={{ robotPose.y.toFixed(2) }} | 
      距离目标: {{ currentDistance ? currentDistance.toFixed(2) + 'm' : '--' }}
    </div>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import ROSLIB from 'roslib'
import request from '@/utils/request'
import MapViewer3D from '@/components/MapViewer3D'

export default {
  name: 'FlowProcess',
  components: {
    draggable,
    MapViewer3D
  },
  data() {
    return {
      // ROS相关
      ros: null,
      rosConnected: false,
      rosBridgeUrl: 'ws://10.188.232.82:9090', // 默认连接地址
      poseSubscriber: null,
      goalTopic: null,
      voiceCommandTopic: null,
      voiceStatusSubscriber: null,
      
      // 数据相关
      savedPositions: [],
      voiceList: [
        { id: 'v1', name: '语音1', command: 'voice1', type: 'voice', waitTime: 10 },
        { id: 'v2', name: '语音2', command: 'voice2', type: 'voice', waitTime: 10 },
        { id: 'v3', name: '语音3', command: 'voice3', type: 'voice', waitTime: 10 }
      ],
      processList: [],
      robotPose: null,
      
      // 执行状态
      isExecuting: false,
      currentStepIndex: -1,
      currentDistance: null,
      checkInterval: null,
      voiceTimeoutTimer: null,
      
      // 阈值配置
      arrivalThreshold: 0.5,
      voiceTimeout: 10000,
    }
  },
  watch: {
    savedPositions: {
      handler(val) {
        if (this.$refs.mapViewer) {
          this.$refs.mapViewer.updateSavedPositions(val)
        }
      },
      deep: true
    }
  },
  mounted() {
    this.initialize()
  },
  beforeDestroy() {
    this.stopExecution()
    this.disconnectROS()
  },
  methods: {
    initialize() {
      this.loadSavedPositions()
      this.connectROS()
    },

    // 加载位置列表
    loadSavedPositions() {
      request({
        url: '/ros2/point/list',
        method: 'get'
      }).then(response => {
        if (response.code === 200) {
          this.savedPositions = response.data.map(point => ({
            id: point.id,
            name: point.name,
            x: point.xPos,
            y: point.yPos,
            yaw: point.yaw,
            type: 'position'
          }))
          // 更新地图组件中的位置标记
          this.$nextTick(() => {
            if (this.$refs.mapViewer) {
              this.$refs.mapViewer.updateSavedPositions(this.savedPositions)
            }
          })
        }
      })
    },

    // 拖拽克隆逻辑
    clonePoint(original) {
      return {
        ...original,
        tempId: Date.now() + Math.random()
      }
    },

    cloneVoice(original) {
      return {
        ...original,
        tempId: Date.now() + Math.random()
      }
    },

    removeStep(index) {
      this.processList.splice(index, 1)
    },

    clearProcess() {
      this.processList = []
    },

    // ROS 连接逻辑
    connectROS() {
      try {
        // 先关闭旧连接
        if (this.ros) {
          this.ros.close()
        }

        this.ros = new ROSLIB.Ros({
          url: this.rosBridgeUrl
        })

        this.ros.on('connection', () => {
          console.log('Connected to websocket server.')
          this.rosConnected = true
          this.setupSubscribers()
          this.$message.success('ROS已连接')
        })

        this.ros.on('error', (error) => {
          console.log('Error connecting to websocket server: ', error)
          this.rosConnected = false
        })

        this.ros.on('close', () => {
          console.log('Connection to websocket server closed.')
          this.rosConnected = false
        })
      } catch (error) {
        console.error('ROS连接异常:', error)
      }
    },

    handleConnection() {
      if (this.rosConnected) {
        this.disconnectROS()
      } else {
        this.connectROS()
      }
    },

    disconnectROS() {
      if (this.poseSubscriber) {
        this.poseSubscriber.unsubscribe()
        this.poseSubscriber = null
      }
      if (this.voiceStatusSubscriber) {
        this.voiceStatusSubscriber.unsubscribe()
        this.voiceStatusSubscriber = null
      }
      if (this.ros) {
        this.ros.close()
        this.ros = null
      }
      this.rosConnected = false
    },

    setupSubscribers() {
      // 订阅机器人位置
      this.poseSubscriber = new ROSLIB.Topic({
        ros: this.ros,
        name: '/amcl_pose',
        messageType: 'geometry_msgs/PoseWithCovarianceStamped'
      })
      this.poseSubscriber.subscribe(this.handlePoseMessage)

      // 初始化目标点发布者
      this.goalTopic = new ROSLIB.Topic({
        ros: this.ros,
        name: '/goal_pose',
        messageType: 'geometry_msgs/PoseStamped'
      })

      // 初始化语音指令发布者
      this.voiceCommandTopic = new ROSLIB.Topic({
        ros: this.ros,
        name: '/voice_words',
        messageType: 'std_msgs/String'
      })
      
      // 订阅语音状态
      this.voiceStatusSubscriber = new ROSLIB.Topic({
        ros: this.ros,
        name: '/voice_status',
        messageType: 'std_msgs/String'
      })
      this.voiceStatusSubscriber.subscribe(this.handleVoiceStatus)
    },

    handlePoseMessage(message) {
      this.robotPose = {
        x: message.pose.pose.position.x,
        y: message.pose.pose.position.y,
      }
    },

    handleVoiceStatus(message) {
      if (!this.isExecuting || this.currentStepIndex === -1) return
      
      const currentStep = this.processList[this.currentStepIndex]
      if (currentStep.type !== 'voice') return

      if (message.data === 'completed' || message.data === 'done' || message.data === 'finish') {
        this.finishVoiceStep()
      }
    },
    
    finishVoiceStep() {
      if (this.voiceTimeoutTimer) {
        clearTimeout(this.voiceTimeoutTimer)
        this.voiceTimeoutTimer = null
      }
      
      this.$message.success('语音播放完成')
      
      setTimeout(() => {
        this.currentStepIndex++
        this.executeStep()
      }, 500)
    },

    // 流程执行逻辑
    async startExecution() {
      if (this.processList.length === 0) return
      
      this.isExecuting = true
      this.currentStepIndex = 0
      
      await this.executeStep()
    },

    stopExecution() {
      this.isExecuting = false
      this.currentStepIndex = -1
      this.currentDistance = null
      if (this.checkInterval) {
        clearInterval(this.checkInterval)
        this.checkInterval = null
      }
      if (this.voiceTimeoutTimer) {
        clearTimeout(this.voiceTimeoutTimer)
        this.voiceTimeoutTimer = null
      }
    },

    executeStep() {
      if (!this.isExecuting) return
      
      if (this.currentStepIndex >= this.processList.length) {
        this.$message.success('流程执行完成')
        this.stopExecution()
        return
      }

      const target = this.processList[this.currentStepIndex]
      
      if (target.type === 'voice') {
        this.executeVoiceStep(target)
      } else {
        this.executePositionStep(target)
      }
    },

    executeVoiceStep(target) {
      this.publishVoice(target.command)
      this.$message.success(`播放语音: ${target.name}`)
      
      const timeout = (target.waitTime || 10) * 1000

      this.voiceTimeoutTimer = setTimeout(() => {
        console.warn('语音播放超时，自动进入下一步')
        this.finishVoiceStep()
      }, timeout)
    },

    executePositionStep(target) {
      this.publishGoal(target)
      
      this.checkInterval = setInterval(() => {
        if (!this.robotPose) return
        
        const dist = Math.sqrt(
          Math.pow(this.robotPose.x - target.x, 2) + 
          Math.pow(this.robotPose.y - target.y, 2)
        )
        this.currentDistance = dist
        
        if (dist < this.arrivalThreshold) {
          clearInterval(this.checkInterval)
          this.checkInterval = null
          
          this.$message.success(`已到达: ${target.name}`)
          
          setTimeout(() => {
            this.currentStepIndex++
            this.executeStep()
          }, 1000)
        }
      }, 500)
    },

    publishGoal(target) {
      if (!this.rosConnected || !this.goalTopic) return

      const pose = new ROSLIB.Message({
        header: {
          frame_id: 'map',
          stamp: { secs: 0, nsecs: 0 }
        },
        pose: {
          position: {
            x: target.x,
            y: target.y,
            z: 0.0
          },
          orientation: {
            z: Math.sin(target.yaw / 2),
            w: Math.cos(target.yaw / 2)
          }
        }
      })

      this.goalTopic.publish(pose)
      console.log(`正在前往: ${target.name}`)
    },

    publishVoice(command) {
      if (!this.rosConnected || !this.voiceCommandTopic) return

      const message = new ROSLIB.Message({
        data: command
      })

      this.voiceCommandTopic.publish(message)
      console.log(`发送语音指令: ${command}`)
    }
  }
}
</script>

<style scoped>
.flow-process-container {
  height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: #fff;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}

.connection-status {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #606266;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f56c6c;
  margin-right: 8px;
  display: inline-block;
}

.status-dot.connected {
  background: #67c23a;
}

.content-wrapper {
  display: flex;
  gap: 20px;
  flex: 1;
  overflow: hidden;
}

.left-column {
  flex: 1;
  display: flex;
  flex-direction: row;
  gap: 20px;
  min-width: 0;
}

.right-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.left-panel-wrapper {
  width: 250px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex-shrink: 0;
}

.voice-panel {
  flex: 1;
  min-height: 200px;
}

.position-panel {
  flex: 2;
}

.process-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.map-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.map-wrapper {
  flex: 1;
  position: relative;
  background: #000;
  min-height: 400px;
}

.box-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.list-container {
  height: 100%;
  overflow-y: auto;
  padding: 10px;
}

.draggable-list {
  min-height: 100%;
}

.list-item {
  padding: 12px;
  margin-bottom: 10px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: grab;
  transition: all 0.3s;
}

.list-item:hover {
  background: #ecf5ff;
  border-color: #c6e2ff;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.voice-item {
  border-left: 3px solid #e6a23c;
  justify-content: space-between;
}

.voice-time-input {
  display: flex;
  align-items: center;
  gap: 4px;
}

.unit {
  font-size: 12px;
  color: #909399;
}

.process-container {
  background: #f0f2f5;
  border-radius: 4px;
}

.process-item {
  display: flex;
  align-items: center;
  background: #fff;
  cursor: default;
}

.process-item.active {
  border-left: 4px solid #409eff;
  background: #ecf5ff;
}

.process-item.completed {
  border-left: 4px solid #67c23a;
  opacity: 0.7;
}

.step-index {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #909399;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-weight: bold;
}

.process-item.active .step-index {
  background: #409eff;
}

.process-item.completed .step-index {
  background: #67c23a;
}

.step-content {
  flex: 1;
}

.step-name {
  font-weight: bold;
  font-size: 15px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-coords {
  font-size: 12px;
  color: #909399;
}

.step-actions {
  margin-left: 15px;
  font-size: 18px;
  color: #909399;
  cursor: pointer;
}

.step-actions:hover {
  color: #f56c6c;
}

.empty-tip {
  text-align: center;
  color: #909399;
  padding: 40px;
  border: 2px dashed #dcdfe6;
  border-radius: 4px;
}

.robot-status-bar {
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 10px 20px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 2000;
  text-align: center;
  font-family: monospace;
}

/* 适配 element-ui card body */
::v-deep .el-card__body {
  height: calc(100% - 55px);
  overflow: hidden;
  padding: 0;
}

/* 适配地图组件 */
::v-deep .map-viewer-3d {
  width: 100%;
  height: 100%;
}
</style>
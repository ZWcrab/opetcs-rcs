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
          v-if="isWaitingForContinue"
          type="warning" 
          icon="el-icon-video-play" 
          @click="continueExecution"
          class="blink-btn"
        >
          继续执行
        </el-button>
        <el-input 
          v-model="flowName" 
          placeholder="流程名称" 
          size="small" 
          style="width: 200px; margin-right: 10px"
        ></el-input>
        <el-button 
          type="success" 
          icon="el-icon-check" 
          @click="saveFlow" 
          :disabled="isExecuting || processList.length === 0"
        >
          保存流程
        </el-button>
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
              <el-button
                style="float: right; padding: 3px 0"
                type="text"
                @click="openAddVoiceDialog"
              >
                添加语音
              </el-button>
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
                  <div style="flex: 1; display: flex; align-items: center; gap: 8px">
                    <i class="el-icon-mic"></i>
                    <span style="font-weight: bold">{{ element.name }}</span>
                  </div>
                  <div class="voice-actions" @mousedown.stop>
                    <el-button 
                      type="text" 
                      icon="el-icon-video-play" 
                      size="mini" 
                      title="播放/发送到ROS"
                      @click="playVoiceItem(element)"
                    ></el-button>
                    <el-button 
                      type="text" 
                      icon="el-icon-delete" 
                      size="mini" 
                      title="删除"
                      style="color: #f56c6c"
                      @click="handleDeleteVoice(element)"
                    ></el-button>
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
          <div slot="header" class="clearfix" style="display: flex; align-items: center; justify-content: space-between;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <span>执行流程</span>
              <el-select 
                v-model="flowId" 
                placeholder="选择流程" 
                size="mini" 
                style="width: 160px" 
                @change="handleFlowChange"
                filterable
              >
                <el-option 
                  v-for="item in flowOptions" 
                  :key="item.flowId" 
                  :label="item.flowName" 
                  :value="item.flowId" 
                />
              </el-select>
              <el-button type="text" icon="el-icon-plus" @click="createNewFlow" size="mini">新建</el-button>
            </div>
            <div style="display: flex; align-items: center; gap: 15px;">
              <el-switch
                v-model="isLoopInspect"
                active-text="巡检模式"
                inactive-text="单次执行"
                size="small"
              />
              <span v-if="isExecuting" class="executing-status">
                (正在执行第 {{ currentStepIndex + 1 }} / {{ processList.length }} 步)
              </span>
            </div>
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
                  <el-switch
                    v-if="!isExecuting"
                    v-model="element.isStop"
                    active-text="停留"
                    inactive-text=""
                    style="margin-right: 10px"
                  >
                  </el-switch>
                  <span v-else-if="element.isStop" style="font-size: 12px; color: #e6a23c; margin-right: 10px">
                    (需确认)
                  </span>
                  <i class="el-icon-close" @click="removeStep(index)" v-if="!isExecuting"></i>
                  <i class="el-icon-loading" v-if="isExecuting && currentStepIndex === index && !isWaitingForContinue"></i>
                  <i class="el-icon-video-pause" v-if="isExecuting && currentStepIndex === index && isWaitingForContinue" style="color: #e6a23c"></i>
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
              :key="mapViewerKey"
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

    <!-- 添加语音弹框 -->
    <el-dialog
      title="添加语音"
      :visible.sync="addVoiceDialogVisible"
      width="500px"
    >
      <el-form
        ref="addVoiceForm"
        :model="newVoiceForm"
        :rules="newVoiceRules"
        label-width="80px"
      >
        <el-form-item label="文本" prop="ttsText">
          <el-input
            v-model="newVoiceForm.ttsText"
            type="textarea"
            :rows="3"
            placeholder="请输入要合成/播放的语音文本"
          />
        </el-form-item>
        <el-form-item label="文件名" prop="fileName">
          <el-input
            v-model="newVoiceForm.fileName"
            placeholder="例如：voice3、voice4"
          />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="addVoiceDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="addVoiceLoading" @click="handleSaveNewVoice">
          保 存
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import ROSLIB from 'roslib'
import request from '@/utils/request'
import MapViewer3D from '@/components/MapViewer3D'
import { listFlow, getFlow, addFlow, updateFlow } from '@/api/ros2/flow'
import { delVoice, listVoice, addVoice } from '@/api/ros2/tts'

export default {
  name: 'FlowProcess',
  components: {
    draggable,
    MapViewer3D
  },
  data() {
    return {
      // 流程信息
      flowId: null,
      flowName: '新建流程_' + new Date().getTime(),
      flowOptions: [], // 流程列表选项
      
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
      voiceList: [],
      processList: [],
      robotPose: null,
      
      // 执行状态
      isExecuting: false,
      isWaitingForContinue: false, // 是否等待用户点击继续
      currentStepIndex: -1,
      currentDistance: null,
      checkInterval: null,
      voiceTimeoutTimer: null,
      // 是否巡检（循环执行流程）
      isLoopInspect: false,
      // 控制 MapViewer3D 重新渲染的 key（用于激活时强制重建组件）
      mapViewerKey: 0,
      
      // 阈值配置
      arrivalThreshold: 0.5,
      voiceTimeout: 10000,

      // 添加语音弹框
      addVoiceDialogVisible: false,
      addVoiceLoading: false,
      newVoiceForm: {
        ttsText: '',
        fileName: ''
      },
      newVoiceRules: {
        ttsText: [{ required: true, message: '请输入语音内容', trigger: 'blur' }],
        fileName: [{ required: true, message: '请输入文件名', trigger: 'blur' }]
      },
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
  // 被 <keep-alive> 缓存的情况下，从其他标签页切回时会触发 activated
  activated() {
    // 切回流程编排标签时，强制重新挂载 MapViewer3D，避免 WebGL 画面黑屏
    this.mapViewerKey++
  },
  beforeDestroy() {
    this.stopExecution()
    this.disconnectROS()
  },
  methods: {
    initialize() {
      this.loadSavedPositions()
      this.loadFlowList()
      this.connectROS()
      this.loadVoiceList()
    },

    // 加载语音列表
    loadVoiceList() {
      listVoice().then(response => {
        if (response.code === 200 && response.data) {
          // 将后端语音配置转换为前端可拖拽的数据结构
          this.voiceList = response.data.map(item => ({
            id: item.id,
            name: item.fileName || item.ttsText,
            command: item.fileName, // 作为后续流程执行时的语音指令标识
            type: 'voice',
            waitTime: 10
          }))
        }
      })
    },

    // 加载流程列表
    loadFlowList() {
      listFlow().then(response => {
        if (response.rows) {
          this.flowOptions = response.rows
          // 如果当前没有选中流程且列表不为空，默认选中第一个
          if (!this.flowId && this.flowOptions.length > 0) {
            this.handleFlowChange(this.flowOptions[0].flowId)
          }
        }
      })
    },

    // 切换流程
    handleFlowChange(flowId) {
      if (!flowId) return
      
      getFlow(flowId).then(response => {
        if (response.code === 200) {
          const flow = response.data
          this.flowId = flow.flowId
          this.flowName = flow.flowName
          
          if (flow.stepList) {
            this.processList = flow.stepList.map(step => ({
              id: 'loaded_' + step.stepId,
              name: step.pointName,
              type: step.stepType,
              x: step.xPos,
              y: step.yPos,
              yaw: step.yaw,
              command: step.command,
              waitTime: step.waitTime,
              isStop: step.isStop, // 加载停留状态
              tempId: Date.now() + Math.random()
            }))
          } else {
            this.processList = []
          }
        }
      })
    },

    createNewFlow() {
      this.flowId = null
      this.flowName = '新建流程_' + new Date().getTime()
      this.processList = []
      this.isWaitingForContinue = false
      this.$message.info('已切换到新建模式')
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
        isStop: false, // 默认不停留
        tempId: Date.now() + Math.random()
      }
    },

    cloneVoice(original) {
      return {
        ...original,
        isStop: false, // 默认不停留
        tempId: Date.now() + Math.random()
      }
    },

    // 打开添加语音弹框
    openAddVoiceDialog() {
      this.addVoiceDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.addVoiceForm) {
          this.$refs.addVoiceForm.resetFields()
        }
        this.newVoiceForm.ttsText = ''
        this.newVoiceForm.fileName = ''
      })
    },

    // 保存新语音：保存到 MySQL 并在后端立刻发送到 ROS2
    handleSaveNewVoice() {
      this.$refs.addVoiceForm.validate(async (valid) => {
        if (!valid) return
        this.addVoiceLoading = true
        try {
          const payload = {
            ttsText: this.newVoiceForm.ttsText,
            fileName: this.newVoiceForm.fileName
          }
          const res = await addVoice(payload)
          if (res.code === 200) {
            // 保存成功后，发布语音命令到 ROS2
            if (this.rosConnected) {
              const voiceData = {
                tts_text: this.newVoiceForm.ttsText,
                fileName: this.newVoiceForm.fileName
              }
              this.publishVoice(JSON.stringify(voiceData))
            }
            this.$message.success(res.msg || '保存成功，已发送到ROS2')
            this.addVoiceDialogVisible = false
            this.loadVoiceList()
          } else {
            this.$message.error(res.msg || '保存失败')
          }
        } catch (e) {
          this.$message.error('保存语音失败：' + (e.message || '未知错误'))
        } finally {
          this.addVoiceLoading = false
        }
      })
    },

    removeStep(index) {
      this.processList.splice(index, 1)
    },

    clearProcess() {
      this.processList = []
      this.flowId = null
      this.flowName = '新建流程_' + new Date().getTime()
    },

    saveFlow() {
      if (this.processList.length === 0) return

      const data = {
        flowId: this.flowId,
        flowName: this.flowName,
        stepList: this.processList.map((item, index) => ({
          stepOrder: index + 1,
          stepType: item.type,
          pointName: item.name,
          xPos: item.x,
          yPos: item.y,
          yaw: item.yaw,
          command: item.command,
          waitTime: item.waitTime,
          isStop: item.isStop
        }))
      }

      if (this.flowId) {
        updateFlow(data).then(response => {
          if (response.code === 200) {
            this.$message.success('流程更新成功')
            this.loadFlowList() // 刷新列表以更新名称
          } else {
            this.$message.error('更新失败')
          }
        })
      } else {
        addFlow(data).then(response => {
          if (response.code === 200) {
            this.$message.success('新流程保存成功')
            this.flowId = response.data // 假设后端返回新ID
            this.loadFlowList() // 刷新列表
            // 选中新流程
            this.handleFlowChange(this.flowId)
          } else {
            this.$message.error('保存失败')
          }
        })
      }
    },

    // 删除语音配置
    handleDeleteVoice(voice) {
      this.$confirm('确认删除该语音配置吗？', '提示', {
        type: 'warning'
      }).then(() => {
        delVoice(voice.id).then(response => {
          if (response.code === 200) {
            this.$message.success('删除成功')
            this.loadVoiceList()
          }
        })
      })
    },

    // 列表直接播放语音
    playVoiceItem(voice) {
      if (!this.rosConnected) {
        this.$message.warning('请先连接 ROS')
        return
      }
      this.publishVoice(voice.command)
      this.$message.success(`已发送语音指令: ${voice.command}`)
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
    
    // 流程执行逻辑
    async startExecution() {
      if (this.processList.length === 0) return
      
      this.isExecuting = true
      this.isWaitingForContinue = false
      this.currentStepIndex = 0
      
      await this.executeStep()
    },

    stopExecution() {
      this.isExecuting = false
      this.isWaitingForContinue = false
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

    continueExecution() {
      if (!this.isWaitingForContinue) return
      this.isWaitingForContinue = false
      this.currentStepIndex++
      this.executeStep()
    },

    executeStep() {
      if (!this.isExecuting) return
      
      if (this.currentStepIndex >= this.processList.length) {
        // 已到最后一步，判断是否巡检
        if (this.isLoopInspect) {
          this.$message.success('本轮流程执行完成，开始下一轮巡检')
          this.currentStepIndex = 0
          this.isWaitingForContinue = false
          this.executeStep()
        } else {
          this.$message.success('流程执行完成')
          this.stopExecution()
        }
        return
      }

      const target = this.processList[this.currentStepIndex]
      
      if (target.type === 'voice') {
        this.executeVoiceStep(target)
      } else {
        this.executePositionStep(target)
      }
    },
    
    // 步骤完成后的统一处理
    handleStepComplete() {
      const currentStep = this.processList[this.currentStepIndex]
      
      if (currentStep.isStop) {
        this.isWaitingForContinue = true
        this.$message.warning(`流程已在步骤 ${this.currentStepIndex + 1} 暂停，等待继续执行`)
      } else {
        setTimeout(() => {
          this.currentStepIndex++
          this.executeStep()
        }, 1000)
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
    
    finishVoiceStep() {
      if (this.voiceTimeoutTimer) {
        clearTimeout(this.voiceTimeoutTimer)
        this.voiceTimeoutTimer = null
      }
      
      this.$message.success('语音播放完成')
      this.handleStepComplete()
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
          this.handleStepComplete()
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

<!-- 添加语音弹框样式可复用全局 Element UI 默认样式，此处无需额外样式 -->

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
  flex: 2;
  min-height: 250px;
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
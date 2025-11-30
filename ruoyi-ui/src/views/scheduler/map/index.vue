<template>
  <div>
    <el-card class="box-card">
      <el-button icon="el-icon-refresh" size="mini" @click="startPolling"
          >加载地图</el-button
        >
      <div class="canvas-container">
        <canvas ref="graphCanvas"></canvas>
      </div>
    </el-card>
  </div>
</template>

  <script>
import { getPoints, getLocations } from "@/api/scheduler/map";
import { listVehicles } from "@/api/scheduler/vehicles";
export default {
  name: "map",
  data() {
    return {
      points: [],
      locations: [],
      vehicles: [],
      // 视图变换参数
      viewOffset: { x: 0, y: 0 },
      isDragging: false,
      lastMousePos: { x: 0, y: 0 },
      scale: 1.0,
      // 地图是否需要居中标志
      needsCentering: true
    };
  },
  created() {
    this.loadAllData();
  },
  mounted() {
    // 监听窗口大小变化
    window.addEventListener('resize', this.handleResize);
    this.resizeCanvas();
    this.setupMouseEvents();
  },
  beforeDestroy() {
    this.stopPolling();
    // 移除事件监听
    window.removeEventListener('resize', this.handleResize);
    this.removeMouseEvents();
  },
  methods: {
    loadAllData() {
      Promise.all([
        this.getVehicles(),
        this.getLocations(),
        this.getPoints()
      ]).then(() => {
        this.$nextTick(() => {
          // 数据重新加载时，设置需要居中标志
          this.needsCentering = true;
          this.renderGraph();
        });
      }).catch(error => {
        console.error('数据加载失败:', error);
        this.$message.error('地图数据加载失败');
      });
    },
    getVehicles() {
      return listVehicles().then((response) => {
        this.vehicles = response.rows;
      }).catch(error => {
        console.error('获取车辆数据失败:', error);
        this.$message.error('车辆数据加载失败');
      });
    },
    getLocations() {
      return getLocations().then((response) => {
        this.locations = response.data;
      }).catch(error => {
        console.error('获取位置数据失败:', error);
        this.$message.error('位置数据加载失败');
      });
    },
    getPoints() {
      return getPoints().then((response) => {
        this.points = response.data || response.rows || [];
      }).catch(error => {
        console.error('获取地图数据失败:', error);
        this.$message.error('地图数据加载失败');
      });
    },
    resizeCanvas() {
      const canvas = this.$refs.graphCanvas;
      const container = canvas.parentElement;

      // 获取容器尺寸
      const containerWidth = container.clientWidth;
      const containerHeight = container.clientHeight;

      // 设置Canvas尺寸
      canvas.width = containerWidth;
      canvas.height = containerHeight;
    },
    handleResize() {
      // 防抖处理，避免频繁重绘
      clearTimeout(this.resizeTimer);
      this.resizeTimer = setTimeout(() => {
        this.resizeCanvas();
        this.renderGraph();
      }, 250);
    },
    setupMouseEvents() {
      const canvas = this.$refs.graphCanvas;
      if (!canvas) return;

      canvas.addEventListener('mousedown', this.handleMouseDown);
      canvas.addEventListener('mousemove', this.handleMouseMove);
      canvas.addEventListener('mouseup', this.handleMouseUp);
      canvas.addEventListener('mouseleave', this.handleMouseUp);
      canvas.addEventListener('wheel', this.handleWheel);

      // 设置光标样式
      canvas.style.cursor = 'grab';
    },
    removeMouseEvents() {
      const canvas = this.$refs.graphCanvas;
      if (!canvas) return;

      canvas.removeEventListener('mousedown', this.handleMouseDown);
      canvas.removeEventListener('mousemove', this.handleMouseMove);
      canvas.removeEventListener('mouseup', this.handleMouseUp);
      canvas.removeEventListener('mouseleave', this.handleMouseUp);
      canvas.removeEventListener('wheel', this.handleWheel);
    },
    handleMouseDown(event) {
      if (event.button === 0) { // 左键
        this.isDragging = true;
        this.lastMousePos = { x: event.clientX, y: event.clientY };
        const canvas = this.$refs.graphCanvas;
        canvas.style.cursor = 'grabbing';
      }
    },
    handleMouseMove(event) {
      if (this.isDragging) {
        const deltaX = event.clientX - this.lastMousePos.x;
        const deltaY = event.clientY - this.lastMousePos.y;

        this.viewOffset.x += deltaX;
        this.viewOffset.y += deltaY;

        this.lastMousePos = { x: event.clientX, y: event.clientY };
        this.renderGraph();
      }
    },
    handleMouseUp() {
      this.isDragging = false;
      const canvas = this.$refs.graphCanvas;
      canvas.style.cursor = 'grab';
    },
    handleWheel(event) {
      event.preventDefault();

      const zoomIntensity = 0.1;
      const wheel = event.deltaY < 0 ? 1 : -1;
      const zoom = Math.exp(wheel * zoomIntensity);

      // 获取鼠标位置
      const rect = event.target.getBoundingClientRect();
      const mouseX = event.clientX - rect.left;
      const mouseY = event.clientY - rect.top;

      // 计算缩放前后的坐标变换
      const worldX = (mouseX - this.viewOffset.x) / this.scale;
      const worldY = (mouseY - this.viewOffset.y) / this.scale;

      this.scale *= zoom;
      this.scale = Math.max(0.1, Math.min(5, this.scale)); // 限制缩放范围

      // 调整偏移量以保持鼠标位置不变
      this.viewOffset.x = mouseX - worldX * this.scale;
      this.viewOffset.y = mouseY - worldY * this.scale;

      this.renderGraph();
    },
    renderGraph() {
      this.resizeCanvas();
      const canvas = this.$refs.graphCanvas;
      const ctx = canvas.getContext("2d");

      // 保存当前画布状态
      ctx.save();

      // 计算地图边界框并自动居中（仅在初始加载时执行）
      if (this.needsCentering) {
        this.centerMap();
        this.needsCentering = false;
      }

      // 应用视图变换：先平移后缩放
      ctx.translate(this.viewOffset.x, this.viewOffset.y);
      ctx.scale(this.scale, this.scale);

      ctx.strokeStyle = "#000";
      ctx.lineWidth = 1.5 / this.scale; // 根据缩放调整线宽

      ctx.clearRect(-this.viewOffset.x / this.scale, -this.viewOffset.y / this.scale,
                   canvas.width / this.scale, canvas.height / this.scale);

      ctx.font = `${12 / this.scale}px Arial`; // 根据缩放调整字体大小

      // 绘制网格背景
      const gridSize = 50 / this.scale;
      const canvasWidth = canvas.width / this.scale;
      const canvasHeight = canvas.height / this.scale;
      
      ctx.strokeStyle = "#e0e0e0";
      ctx.lineWidth = 0.5 / this.scale;
      
      // 绘制垂直线
      for (let x = 0; x < canvasWidth; x += gridSize) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, canvasHeight);
        ctx.stroke();
      }
      
      // 绘制水平线
      for (let y = 0; y < canvasHeight; y += gridSize) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(canvasWidth, y);
        ctx.stroke();
      }
      
      // 首先绘制地图点和路径
      this.points.forEach((point) => {
        // 根据实际数据结构获取位置信息
        const x = (point.pose && point.pose.position ? point.pose.position.x : point.x || 0) / 50;
        const y = (point.pose && point.pose.position ? point.pose.position.y : point.y || 0) / -50;

        // 绘制地图点
        let pointColor = "#808080"; // 默认灰色
        let pointSize = 3 / this.scale;
        
        // 根据点的名称或其他属性设置不同的颜色
        if (point.name && point.name.includes('Point-')) {
          // 蓝色点
          pointColor = "#0000FF";
          pointSize = 4 / this.scale;
        }
        
        ctx.fillStyle = pointColor;
        ctx.beginPath();
        ctx.arc(x, y, pointSize, 0, 2 * Math.PI); // 根据缩放调整点大小
        ctx.fill();
        
        // 绘制点边框
        ctx.strokeStyle = "#000000";
        ctx.lineWidth = 1 / this.scale;
        ctx.stroke();
        
        // 显示点名称
        ctx.fillStyle = "#000000";
        ctx.fillText(point.name || point.id || 'Point', x, y + 20 / this.scale);

        // 绘制路径连接（如果有路径信息）
        if (point.outgoingPaths) {
          point.outgoingPaths.forEach((path) => {
            const targetPoint = this.points.find(
              (p) => p.name === path.name.split(" --- ")[1]
            );
            if (targetPoint) {
              const targetX = (targetPoint.pose && targetPoint.pose.position ? targetPoint.pose.position.x : targetPoint.x || 0) / 50;
              const targetY = (targetPoint.pose && targetPoint.pose.position ? targetPoint.pose.position.y : targetPoint.y || 0) / -50;

              // 根据路径属性设置不同的颜色
              let pathColor = "#000000"; // 默认黑色
              let lineWidth = 1.5 / this.scale;
              
              // 示例：根据路径名称或索引设置不同颜色
              const pathIndex = point.outgoingPaths.indexOf(path);
              const colors = ["#0000FF", "#008000", "#FF0000", "#00FFFF", "#FF00FF"];
              pathColor = colors[pathIndex % colors.length];

              ctx.strokeStyle = pathColor;
              ctx.lineWidth = lineWidth;
              
              ctx.beginPath();
              ctx.moveTo(x, y);
              ctx.lineTo(targetX, targetY);
              ctx.stroke();

              // 绘制箭头
              drawArrow(ctx, x, y, targetX, targetY, this.scale);
            }
          });
        }
      });

      // 绘制位置（如果有位置数据）
      this.locations.forEach((location) => {
        const x = (location.position ? location.position.x : location.x || 0) / 50;
        const y = (location.position ? location.position.y : location.y || 0) / -50;
        
        // 根据位置类型设置不同的样式
        let locationColor = "#FFA500"; // 默认橙色
        let locationSize = 20 / this.scale;
        let symbol = "";
        
        // 充电站
        if (location.name && (location.name.includes('Charge') || location.name.includes('charge'))) {
          locationColor = "#FF0000"; // 红色
          symbol = "C";
        }
        // 工作站
        else if (location.name && (location.name.includes('Work') || location.name.includes('work'))) {
          locationColor = "#008000"; // 绿色
          symbol = "T";
        }
        // 货物进出口
        else if (location.name && (location.name.includes('Goods') || location.name.includes('goods'))) {
          locationColor = "#0000FF"; // 蓝色
        }
        // 存储区
        else if (location.name && (location.name.includes('Storage') || location.name.includes('storage'))) {
          locationColor = "#800080"; // 紫色
        }
        
        // 绘制位置矩形
        ctx.strokeStyle = locationColor;
        ctx.lineWidth = 2 / this.scale;
        ctx.strokeRect(x - locationSize / 2, y - locationSize / 2,
                      locationSize, locationSize);
        
        // 填充颜色（半透明）
        ctx.fillStyle = locationColor + "40"; // 添加透明度
        ctx.fillRect(x - locationSize / 2, y - locationSize / 2,
                    locationSize, locationSize);

        // 显示位置名称
        ctx.fillStyle = "#000000";
        ctx.fillText(location.name || location.id || 'Location',
                    x + locationSize / 2, y - locationSize / 2);
        
        // 绘制符号（如充电站的C，工作站的T）
        if (symbol) {
          ctx.fillStyle = locationColor;
          ctx.font = `bold ${16 / this.scale}px Arial`;
          ctx.textAlign = "center";
          ctx.fillText(symbol, x, y + 5 / this.scale);
          ctx.textAlign = "left";
        }

        // 绘制位置与关联点之间的虚线连接
        if (location.attachedLinks && location.attachedLinks.length > 0) {
          location.attachedLinks.forEach((link) => {
            const targetPoint = this.points.find(point => point.name === link.point.name);
            if (targetPoint) {
              const targetX = (targetPoint.pose && targetPoint.pose.position ? targetPoint.pose.position.x : targetPoint.x || 0) / 50;
              const targetY = (targetPoint.pose && targetPoint.pose.position ? targetPoint.pose.position.y : targetPoint.y || 0) / -50;

              // 设置虚线样式
              ctx.setLineDash([5, 5]);
              ctx.strokeStyle = "#888888"; // 灰色虚线
              ctx.lineWidth = 1 / this.scale;

              ctx.beginPath();
              ctx.moveTo(x, y);
              ctx.lineTo(targetX, targetY);
              ctx.stroke();

              // 恢复实线样式
              ctx.setLineDash([]);
            }
          });
        }
      });

      // 绘制小车
      this.vehicles.forEach((vehicle) => {
        // 根据车辆当前位置找到对应的点
        const currentPoint = this.points.find(point => point.name === vehicle.currentPosition);
        if (currentPoint) {
          const x = (currentPoint.pose && currentPoint.pose.position ? currentPoint.pose.position.x : currentPoint.x || 0) / 50;
          const y = (currentPoint.pose && currentPoint.pose.position ? currentPoint.pose.position.y : currentPoint.y || 0) / -50;

          // 绘制小车
          drawVehicle(ctx, x, y, vehicle.name, this.scale);
        }
      });

      // 恢复画布状态
      ctx.restore();
    },
    startPolling() {
      this.pollingTimer = setInterval(() => {
        this.getLocations();
        this.getPoints();
        this.getVehicles();
        this.renderGraph();
      }, 10000);
    },
    stopPolling() {
      clearInterval(this.pollingTimer);
    },
    centerMap() {
      if (this.points.length === 0) return;

      const canvas = this.$refs.graphCanvas;
      const canvasCenterX = canvas.width / 2;
      const canvasCenterY = canvas.height / 2;

      // 计算地图所有元素的边界框
      let minX = Infinity;
      let maxX = -Infinity;
      let minY = Infinity;
      let maxY = -Infinity;

      // 遍历所有点计算边界
      this.points.forEach(point => {
        const x = (point.position ? point.position.x : point.x || 0) / 50;
        const y = (point.position ? point.position.y : point.y || 0) / -50;
        
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
      });

      // 遍历所有位置计算边界
      this.locations.forEach(location => {
        const x = (location.position ? location.position.x : location.x || 0) / 50;
        const y = (location.position ? location.position.y : location.y || 0) / -50;
        
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
      });

      // 计算地图中心点
      const mapCenterX = (minX + maxX) / 2;
      const mapCenterY = (minY + maxY) / 2;

      // 调整viewOffset使地图居中
      this.viewOffset.x = canvasCenterX - mapCenterX * this.scale;
      this.viewOffset.y = canvasCenterY - mapCenterY * this.scale;
    },
  },
};

function drawArrow(ctx, fromX, fromY, toX, toY, scale) {
  const headlen = 10 / scale;
  const dx = toX - fromX;
  const dy = toY - fromY;
  const angle = Math.atan2(dy, dx);

  ctx.beginPath();
  ctx.moveTo(toX, toY);
  ctx.lineTo(toX - headlen * Math.cos(angle - Math.PI / 6), toY - headlen * Math.sin(angle - Math.PI / 6));
  ctx.moveTo(toX, toY);
  ctx.lineTo(toX - headlen * Math.cos(angle + Math.PI / 6), toY - headlen * Math.sin(angle + Math.PI / 6));
  ctx.stroke();
}

function drawVehicle(ctx, x, y, vehicleName, scale) {
  // 绘制小车主体（矩形）
  ctx.fillStyle = "#FFFF00"; // 黄色
  const size = 16 / scale;
  const halfSize = size / 2;
  ctx.fillRect(x - halfSize, y - halfSize, size, size);

  // 绘制小车边框
  ctx.strokeStyle = "#000000";
  ctx.lineWidth = 1 / scale;
  ctx.strokeRect(x - halfSize, y - halfSize, size, size);

  // 显示小车名称（黑色）
  ctx.fillStyle = "#000000"; // 黑色
  ctx.font = `bold ${14 / scale}px Arial`; // 放大并加粗
  ctx.textAlign = "center";
  ctx.fillText(vehicleName, x, y + 4 / scale);
  ctx.textAlign = "left";
}
</script>

  <style scoped>
.canvas-container {
  width: 100%;
  height: 100%;
  min-height: 400px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  flex: 1;
}

canvas {
  background-color: #f1f1f1;
  display: block;
  width: 100%;
  height: 100%;
}

.box-card {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.box-card >>> .el-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}
</style>

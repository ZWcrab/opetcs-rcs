package com.ruoyi.web.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

@ApiModel("导航目标DTO")
public class NavigationGoalDTO {

    @ApiModelProperty(value = "X坐标", required = true)
    @NotNull(message = "X坐标不能为空")
    private Double x;

    @ApiModelProperty(value = "Y坐标", required = true)
    @NotNull(message = "Y坐标不能为空")
    private Double y;

    @ApiModelProperty(value = "Z坐标")
    private Double z = 0.0;

    @ApiModelProperty(value = "欧拉角Roll (绕X轴旋转，单位：弧度)")
    private Double roll = 0.0;

    @ApiModelProperty(value = "欧拉角Pitch (绕Y轴旋转，单位：弧度)")
    private Double pitch = 0.0;

    @ApiModelProperty(value = "欧拉角Yaw (绕Z轴旋转，单位：弧度)")
    private Double yaw = 0.0;

    @ApiModelProperty(value = "方向X (四元数)")
    private Double orientationX = 0.0;

    @ApiModelProperty(value = "方向Y (四元数)")
    private Double orientationY = 0.0;

    @ApiModelProperty(value = "方向Z (四元数)")
    private Double orientationZ = 0.0;

    @ApiModelProperty(value = "方向W (四元数)")
    private Double orientationW = 1.0;

    @ApiModelProperty(value = "行为树")
    private String behaviorTree = "";

    // 欧拉角转四元数的转换方法
    public void eulerToQuaternion() {
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);

        orientationW = cr * cp * cy + sr * sp * sy;
        orientationX = sr * cp * cy - cr * sp * sy;
        orientationY = cr * sp * cy + sr * cp * sy;
        orientationZ = cr * cp * sy - sr * sp * cy;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }

    public Double getRoll() {
        return roll;
    }

    public void setRoll(Double roll) {
        this.roll = roll;
    }

    public Double getPitch() {
        return pitch;
    }

    public void setPitch(Double pitch) {
        this.pitch = pitch;
    }

    public Double getYaw() {
        return yaw;
    }

    public void setYaw(Double yaw) {
        this.yaw = yaw;
    }

    public Double getOrientationX() {
        return orientationX;
    }

    public void setOrientationX(Double orientationX) {
        this.orientationX = orientationX;
    }

    public Double getOrientationY() {
        return orientationY;
    }

    public void setOrientationY(Double orientationY) {
        this.orientationY = orientationY;
    }

    public Double getOrientationZ() {
        return orientationZ;
    }

    public void setOrientationZ(Double orientationZ) {
        this.orientationZ = orientationZ;
    }

    public Double getOrientationW() {
        return orientationW;
    }

    public void setOrientationW(Double orientationW) {
        this.orientationW = orientationW;
    }

    public String getBehaviorTree() {
        return behaviorTree;
    }

    public void setBehaviorTree(String behaviorTree) {
        this.behaviorTree = behaviorTree;
    }

    @Override
    public String toString() {
        return "NavigationGoalDTO{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", roll=" + roll +
                ", pitch=" + pitch +
                ", yaw=" + yaw +
                ", orientationX=" + orientationX +
                ", orientationY=" + orientationY +
                ", orientationZ=" + orientationZ +
                ", orientationW=" + orientationW +
                ", behaviorTree='" + behaviorTree + '\'' +
                '}';
    }
}
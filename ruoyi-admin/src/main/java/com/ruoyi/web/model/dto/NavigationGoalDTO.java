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

    @ApiModelProperty(value = "方向X")
    private Double orientationX = 0.0;

    @ApiModelProperty(value = "方向Y")
    private Double orientationY = 0.0;

    @ApiModelProperty(value = "方向Z")
    private Double orientationZ = 0.0;

    @ApiModelProperty(value = "方向W")
    private Double orientationW = 1.0;

    @ApiModelProperty(value = "行为树")
    private String behaviorTree = "";

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
                ", orientationX=" + orientationX +
                ", orientationY=" + orientationY +
                ", orientationZ=" + orientationZ +
                ", orientationW=" + orientationW +
                ", behaviorTree='" + behaviorTree + '\'' +
                '}';
    }
}
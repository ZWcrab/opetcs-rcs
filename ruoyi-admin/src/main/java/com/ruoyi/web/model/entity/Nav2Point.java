package com.ruoyi.web.model.entity;


/**
 * 导航点位实体类
 *
 * @author ruoyi
 * @date 2025-12-27
 */
public class Nav2Point {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * X坐标
     */
    private Double xPos;

    /**
     * Y坐标
     */
    private Double yPos;

    /**
     * 朝向
     */
    private Double yaw;

    /**
     * 点位名称
     */
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getxPos() {
        return xPos;
    }

    public void setxPos(Double xPos) {
        this.xPos = xPos;
    }

    public Double getyPos() {
        return yPos;
    }

    public void setyPos(Double yPos) {
        this.yPos = yPos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getYaw() {
        return yaw;
    }

    public void setYaw(Double yaw) {
        this.yaw = yaw;
    }
}
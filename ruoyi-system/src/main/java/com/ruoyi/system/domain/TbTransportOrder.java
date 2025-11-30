package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单表对象 tb_transport_order
 *
 * @author Ying
 * @date 2024-04-11
 */

public class TbTransportOrder extends BaseEntity
{

    /** id */
    private Long id;

    /** 订单编号 */
    @Excel(name = "订单编号")
    private String name;

    /** 能否撤回 */
    @Excel(name = "能否撤回")
    private Boolean dispensable;

    /** 订单类型 */
    @Excel(name = "订单类型")
    private String type;

    /** 订单状态 */
    @Excel(name = "订单状态")
    private String state;

    /** 预定执行车辆 */
    @Excel(name = "预定执行车辆")
    private String intendedVehicle;

    /** 实际执行车辆 */
    @Excel(name = "实际执行车辆")
    private String processingVehicle;

    /** 目的地 */
    @Excel(name = "目的地")
    private String destinations;

    /** 创建时间 */

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private LocalDateTime creationTime;

    /** 截止时间 */

    @Excel(name = "截止时间", width = 30, dateFormat = "yyyy-MM-dd")
    private LocalDateTime deadline;

    /** 完成时间 */

    @Excel(name = "完成时间", width = 30, dateFormat = "yyyy-MM-dd")
    private LocalDateTime finishedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDispensable() {
        return dispensable;
    }

    public void setDispensable(Boolean dispensable) {
        this.dispensable = dispensable;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getIntendedVehicle() {
        return intendedVehicle;
    }

    public void setIntendedVehicle(String intendedVehicle) {
        this.intendedVehicle = intendedVehicle;
    }

    public String getProcessingVehicle() {
        return processingVehicle;
    }

    public void setProcessingVehicle(String processingVehicle) {
        this.processingVehicle = processingVehicle;
    }

    public String getDestinations() {
        return destinations;
    }

    public void setDestinations(String destinations) {
        this.destinations = destinations;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(LocalDateTime finishedTime) {
        this.finishedTime = finishedTime;
    }
}

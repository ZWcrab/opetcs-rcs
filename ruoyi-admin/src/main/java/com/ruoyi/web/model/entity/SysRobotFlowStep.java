package com.ruoyi.web.model.entity;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 机器人流程步骤对象 sys_robot_flow_step
 * 
 * @author ruoyi
 * @date 2026-02-02
 */
public class SysRobotFlowStep extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 步骤ID */
    private Long stepId;

    /** 流程ID */
    private Long flowId;

    /** 步骤顺序 */
    private Integer stepOrder;

    /** 步骤类型(position/voice) */
    private String stepType;

    /** 点位名称 */
    private String pointName;

    /** X坐标 */
    private Double xPos;

    /** Y坐标 */
    private Double yPos;

    /** 朝向 */
    private Double yaw;

    /** 语音指令 */
    private String command;

    /** 等待时间(秒) */
    private Integer waitTime;

    /** 是否停留 */
    private Boolean isStop;

    public void setStepId(Long stepId) 
    {
        this.stepId = stepId;
    }

    public Long getStepId() 
    {
        return stepId;
    }
    public void setFlowId(Long flowId) 
    {
        this.flowId = flowId;
    }

    public Long getFlowId() 
    {
        return flowId;
    }
    public void setStepOrder(Integer stepOrder) 
    {
        this.stepOrder = stepOrder;
    }

    public Integer getStepOrder() 
    {
        return stepOrder;
    }
    public void setStepType(String stepType) 
    {
        this.stepType = stepType;
    }

    public String getStepType() 
    {
        return stepType;
    }
    public void setPointName(String pointName) 
    {
        this.pointName = pointName;
    }

    public String getPointName() 
    {
        return pointName;
    }
    public void setxPos(Double xPos) 
    {
        this.xPos = xPos;
    }

    public Double getxPos() 
    {
        return xPos;
    }
    public void setyPos(Double yPos) 
    {
        this.yPos = yPos;
    }

    public Double getyPos() 
    {
        return yPos;
    }
    public void setYaw(Double yaw) 
    {
        this.yaw = yaw;
    }

    public Double getYaw() 
    {
        return yaw;
    }
    public void setCommand(String command) 
    {
        this.command = command;
    }

    public String getCommand() 
    {
        return command;
    }
    public void setWaitTime(Integer waitTime) 
    {
        this.waitTime = waitTime;
    }

    public Integer getWaitTime() 
    {
        return waitTime;
    }
    public void setIsStop(Boolean isStop) 
    {
        this.isStop = isStop;
    }

    public Boolean getIsStop() 
    {
        return isStop;
    }

    @Override
    public String toString() {
        return "SysRobotFlowStep{" +
                "stepId=" + stepId +
                ", flowId=" + flowId +
                ", stepOrder=" + stepOrder +
                ", stepType='" + stepType + '\'' +
                ", pointName='" + pointName + '\'' +
                ", xPos=" + xPos +
                ", yPos=" + yPos +
                ", yaw=" + yaw +
                ", command='" + command + '\'' +
                ", waitTime=" + waitTime +
                ", isStop=" + isStop +
                '}';
    }
}

package com.ruoyi.web.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.List;

/**
 * 机器人流程对象 sys_robot_flow
 * 
 * @author ruoyi
 * @date 2026-02-02
 */
public class SysRobotFlow extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 流程ID */
    private Long flowId;

    /** 流程名称 */
    private String flowName;

    /** 流程步骤列表 */
    private List<SysRobotFlowStep> stepList;

    public void setFlowId(Long flowId) 
    {
        this.flowId = flowId;
    }

    public Long getFlowId() 
    {
        return flowId;
    }
    public void setFlowName(String flowName) 
    {
        this.flowName = flowName;
    }

    public String getFlowName() 
    {
        return flowName;
    }

    public List<SysRobotFlowStep> getStepList() {
        return stepList;
    }

    public void setStepList(List<SysRobotFlowStep> stepList) {
        this.stepList = stepList;
    }

    @Override
    public String toString() {
        return "SysRobotFlow{" +
                "flowId=" + flowId +
                ", flowName='" + flowName + '\'' +
                ", stepList=" + stepList +
                '}';
    }
}

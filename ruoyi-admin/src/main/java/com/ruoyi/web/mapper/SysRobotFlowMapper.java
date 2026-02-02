package com.ruoyi.web.mapper;

import java.util.List;
import com.ruoyi.web.model.entity.SysRobotFlow;
import com.ruoyi.web.model.entity.SysRobotFlowStep;

/**
 * 机器人流程Mapper接口
 * 
 * @author ruoyi
 * @date 2026-02-02
 */
public interface SysRobotFlowMapper 
{
    /**
     * 查询机器人流程
     * 
     * @param flowId 机器人流程ID
     * @return 机器人流程
     */
    public SysRobotFlow selectSysRobotFlowById(Long flowId);

    /**
     * 查询机器人流程列表
     * 
     * @param sysRobotFlow 机器人流程
     * @return 机器人流程集合
     */
    public List<SysRobotFlow> selectSysRobotFlowList(SysRobotFlow sysRobotFlow);

    /**
     * 新增机器人流程
     * 
     * @param sysRobotFlow 机器人流程
     * @return 结果
     */
    public int insertSysRobotFlow(SysRobotFlow sysRobotFlow);

    /**
     * 修改机器人流程
     * 
     * @param sysRobotFlow 机器人流程
     * @return 结果
     */
    public int updateSysRobotFlow(SysRobotFlow sysRobotFlow);

    /**
     * 删除机器人流程
     * 
     * @param flowId 机器人流程ID
     * @return 结果
     */
    public int deleteSysRobotFlowById(Long flowId);

    /**
     * 批量删除机器人流程
     * 
     * @param flowIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteSysRobotFlowByIds(Long[] flowIds);

    /**
     * 批量删除机器人流程步骤
     * 
     * @param flowIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteSysRobotFlowStepByFlowIds(Long[] flowIds);
    
    /**
     * 批量新增机器人流程步骤
     * 
     * @param sysRobotFlowStepList 机器人流程步骤列表
     * @return 结果
     */
    public int batchSysRobotFlowStep(List<SysRobotFlowStep> sysRobotFlowStepList);
    

    /**
     * 通过机器人流程ID删除机器人流程步骤
     * 
     * @param flowId 机器人流程ID
     * @return 结果
     */
    public int deleteSysRobotFlowStepByFlowId(Long flowId);
    
    /**
     * 通过机器人流程ID查询步骤
     * @param flowId
     * @return
     */
    public List<SysRobotFlowStep> selectSysRobotFlowStepByFlowId(Long flowId);

    /**
     * 清空所有流程
     * @return
     */
    public int deleteAllSysRobotFlow();
    
    /**
     * 清空所有流程步骤
     * @return
     */
    public int deleteAllSysRobotFlowStep();
}

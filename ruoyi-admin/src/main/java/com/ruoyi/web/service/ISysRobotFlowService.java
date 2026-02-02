package com.ruoyi.web.service;

import java.util.List;
import com.ruoyi.web.model.entity.SysRobotFlow;

/**
 * 机器人流程Service接口
 * 
 * @author ruoyi
 * @date 2026-02-02
 */
public interface ISysRobotFlowService 
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
     * 批量删除机器人流程
     * 
     * @param flowIds 需要删除的机器人流程ID
     * @return 结果
     */
    public int deleteSysRobotFlowByIds(Long[] flowIds);

    /**
     * 删除机器人流程信息
     * 
     * @param flowId 机器人流程ID
     * @return 结果
     */
    public int deleteSysRobotFlowById(Long flowId);

    /**
     * 保存单条全局流程（先删后增）
     * 
     * @param sysRobotFlow 机器人流程
     * @return 结果
     */
    public int saveSingletonFlow(SysRobotFlow sysRobotFlow);
}

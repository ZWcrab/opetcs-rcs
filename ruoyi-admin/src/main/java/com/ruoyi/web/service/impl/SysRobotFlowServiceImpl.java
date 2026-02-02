package com.ruoyi.web.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.web.model.entity.SysRobotFlowStep;
import com.ruoyi.web.mapper.SysRobotFlowMapper;
import com.ruoyi.web.model.entity.SysRobotFlow;
import com.ruoyi.web.service.ISysRobotFlowService;

/**
 * 机器人流程Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-02-02
 */
@Service
public class SysRobotFlowServiceImpl implements ISysRobotFlowService 
{
    @Autowired
    private SysRobotFlowMapper sysRobotFlowMapper;

    /**
     * 查询机器人流程
     * 
     * @param flowId 机器人流程ID
     * @return 机器人流程
     */
    @Override
    public SysRobotFlow selectSysRobotFlowById(Long flowId)
    {
        return sysRobotFlowMapper.selectSysRobotFlowById(flowId);
    }

    /**
     * 查询机器人流程列表
     * 
     * @param sysRobotFlow 机器人流程
     * @return 机器人流程
     */
    @Override
    public List<SysRobotFlow> selectSysRobotFlowList(SysRobotFlow sysRobotFlow)
    {
        return sysRobotFlowMapper.selectSysRobotFlowList(sysRobotFlow);
    }

    /**
     * 新增机器人流程
     * 
     * @param sysRobotFlow 机器人流程
     * @return 结果
     */
    @Transactional
    @Override
    public int insertSysRobotFlow(SysRobotFlow sysRobotFlow)
    {
        sysRobotFlow.setCreateTime(DateUtils.getNowDate());
        int rows = sysRobotFlowMapper.insertSysRobotFlow(sysRobotFlow);
        insertSysRobotFlowStep(sysRobotFlow);
        return rows;
    }

    /**
     * 修改机器人流程
     * 
     * @param sysRobotFlow 机器人流程
     * @return 结果
     */
    @Transactional
    @Override
    public int updateSysRobotFlow(SysRobotFlow sysRobotFlow)
    {
        sysRobotFlow.setUpdateTime(DateUtils.getNowDate());
        sysRobotFlowMapper.deleteSysRobotFlowStepByFlowId(sysRobotFlow.getFlowId());
        insertSysRobotFlowStep(sysRobotFlow);
        return sysRobotFlowMapper.updateSysRobotFlow(sysRobotFlow);
    }

    /**
     * 批量删除机器人流程
     * 
     * @param flowIds 需要删除的机器人流程ID
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteSysRobotFlowByIds(Long[] flowIds)
    {
        sysRobotFlowMapper.deleteSysRobotFlowStepByFlowIds(flowIds);
        return sysRobotFlowMapper.deleteSysRobotFlowByIds(flowIds);
    }

    /**
     * 删除机器人流程信息
     * 
     * @param flowId 机器人流程ID
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteSysRobotFlowById(Long flowId)
    {
        sysRobotFlowMapper.deleteSysRobotFlowStepByFlowId(flowId);
        return sysRobotFlowMapper.deleteSysRobotFlowById(flowId);
    }

    /**
     * 保存单条全局流程（先删后增）
     * 
     * @param sysRobotFlow 机器人流程
     * @return 结果
     */
    @Transactional
    @Override
    public int saveSingletonFlow(SysRobotFlow sysRobotFlow)
    {
        // 1. 清空所有旧数据
        sysRobotFlowMapper.deleteAllSysRobotFlowStep();
        sysRobotFlowMapper.deleteAllSysRobotFlow();
        
        // 2. 插入新数据
        sysRobotFlow.setCreateTime(DateUtils.getNowDate());
        int rows = sysRobotFlowMapper.insertSysRobotFlow(sysRobotFlow);
        insertSysRobotFlowStep(sysRobotFlow);
        return rows;
    }

    /**
     * 新增机器人流程步骤信息
     * 
     * @param sysRobotFlow 机器人流程对象
     */
    public void insertSysRobotFlowStep(SysRobotFlow sysRobotFlow)
    {
        List<SysRobotFlowStep> sysRobotFlowStepList = sysRobotFlow.getStepList();
        Long flowId = sysRobotFlow.getFlowId();
        if (StringUtils.isNotNull(sysRobotFlowStepList))
        {
            List<SysRobotFlowStep> list = new ArrayList<SysRobotFlowStep>();
            for (SysRobotFlowStep sysRobotFlowStep : sysRobotFlowStepList)
            {
                sysRobotFlowStep.setFlowId(flowId);
                list.add(sysRobotFlowStep);
            }
            if (list.size() > 0)
            {
                sysRobotFlowMapper.batchSysRobotFlowStep(list);
            }
        }
    }
}

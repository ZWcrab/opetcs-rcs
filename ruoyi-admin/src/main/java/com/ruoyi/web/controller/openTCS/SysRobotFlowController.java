package com.ruoyi.web.controller.openTCS;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.web.model.entity.SysRobotFlow;
import com.ruoyi.web.service.ISysRobotFlowService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 机器人流程Controller
 * 
 * @author ruoyi
 * @date 2026-02-02
 */
@RestController
@RequestMapping("/ros2/flow")
public class SysRobotFlowController extends BaseController
{
    @Autowired
    private ISysRobotFlowService sysRobotFlowService;

    /**
     * 查询机器人流程列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysRobotFlow sysRobotFlow)
    {
        startPage();
        List<SysRobotFlow> list = sysRobotFlowService.selectSysRobotFlowList(sysRobotFlow);
        return getDataTable(list);
    }

    /**
     * 导出机器人流程列表
     */
    @Log(title = "机器人流程", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysRobotFlow sysRobotFlow)
    {
        List<SysRobotFlow> list = sysRobotFlowService.selectSysRobotFlowList(sysRobotFlow);
        ExcelUtil<SysRobotFlow> util = new ExcelUtil<SysRobotFlow>(SysRobotFlow.class);
        util.exportExcel(response, list, "机器人流程数据");
    }

    /**
     * 获取机器人流程详细信息
     */
    @GetMapping(value = "/{flowId}")
    public AjaxResult getInfo(@PathVariable("flowId") Long flowId)
    {
        return AjaxResult.success(sysRobotFlowService.selectSysRobotFlowById(flowId));
    }

    /**
     * 新增机器人流程
     */
    @Log(title = "机器人流程", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysRobotFlow sysRobotFlow)
    {
        return toAjax(sysRobotFlowService.insertSysRobotFlow(sysRobotFlow));
    }

    /**
     * 修改机器人流程
     */
    @Log(title = "机器人流程", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysRobotFlow sysRobotFlow)
    {
        return toAjax(sysRobotFlowService.updateSysRobotFlow(sysRobotFlow));
    }

    /**
     * 删除机器人流程
     */
    @Log(title = "机器人流程", businessType = BusinessType.DELETE)
	@DeleteMapping("/{flowIds}")
    public AjaxResult remove(@PathVariable Long[] flowIds)
    {
        return toAjax(sysRobotFlowService.deleteSysRobotFlowByIds(flowIds));
    }

    /**
     * 保存单条全局流程
     */
    @Log(title = "机器人流程", businessType = BusinessType.INSERT)
    @PostMapping("/save")
    public AjaxResult saveSingletonFlow(@RequestBody SysRobotFlow sysRobotFlow)
    {
        return toAjax(sysRobotFlowService.saveSingletonFlow(sysRobotFlow));
    }
}

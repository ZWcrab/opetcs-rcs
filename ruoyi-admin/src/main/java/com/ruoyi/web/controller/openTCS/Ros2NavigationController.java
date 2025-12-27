package com.ruoyi.web.controller.openTCS;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.web.model.dto.NavigationGoalDTO;
import com.ruoyi.web.service.Ros2NavigationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@Api(value = "ROS2导航相关接口", tags = "ROS2导航相关接口")
@RequestMapping("/ros2/navigation")
public class Ros2NavigationController extends BaseController {

    @Resource
    private Ros2NavigationService ros2NavigationService;

    @ApiOperation("发送导航目标")
    @PostMapping("/sendGoal")
    public AjaxResult sendNavigationGoal(@RequestBody NavigationGoalDTO navigationGoalDTO) {
        try {
            boolean result = ros2NavigationService.sendNavigationGoal(navigationGoalDTO);
            if (result) {
                return AjaxResult.success("导航目标发送成功");
            } else {
                return AjaxResult.error("导航目标发送失败，请检查ROS2连接");
            }
        } catch (Exception e) {
            return AjaxResult.error("发送导航目标时发生异常: " + e.getMessage());
        }
    }

    @ApiOperation("取消导航目标")
    @PostMapping("/cancelGoal")
    public AjaxResult cancelNavigationGoal() {
        try {
            boolean result = ros2NavigationService.cancelNavigationGoal();
            if (result) {
                return AjaxResult.success("导航目标取消成功");
            } else {
                return AjaxResult.error("导航目标取消失败，请检查ROS2连接");
            }
        } catch (Exception e) {
            return AjaxResult.error("取消导航目标时发生异常: " + e.getMessage());
        }
    }

    @ApiOperation("连接ROS2")
    @PostMapping("/connect")
    public AjaxResult connectRos2(@RequestParam String rosBridgeUrl) {
        try {
            boolean result = ros2NavigationService.connectToRosBridge(rosBridgeUrl);
            if (result) {
                return AjaxResult.success("ROS2连接成功");
            } else {
                return AjaxResult.error("ROS2连接失败，请检查地址和网络");
            }
        } catch (Exception e) {
            return AjaxResult.error("连接ROS2时发生异常: " + e.getMessage());
        }
    }

    @ApiOperation("断开ROS2连接")
    @PostMapping("/disconnect")
    public AjaxResult disconnectRos2() {
        try {
            ros2NavigationService.disconnectFromRosBridge();
            return AjaxResult.success("ROS2连接已断开");
        } catch (Exception e) {
            return AjaxResult.error("断开ROS2连接时发生异常: " + e.getMessage());
        }
    }

    @ApiOperation("获取ROS2连接状态")
    @GetMapping("/status")
    public AjaxResult getRos2Status() {
        try {
            boolean connected = ros2NavigationService.isConnected();
            return AjaxResult.success().put("connected", connected);
        } catch (Exception e) {
            return AjaxResult.error("获取ROS2状态时发生异常: " + e.getMessage());
        }
    }
}
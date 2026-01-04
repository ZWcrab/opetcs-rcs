package com.ruoyi.web.controller.openTCS;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.web.model.entity.Nav2Point;
import com.ruoyi.web.service.Nav2PointService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 导航点位控制器
 *
 * @author ruoyi
 * @date 2025-12-27
 */
@RestController
@Api(value = "导航点位相关接口", tags = "导航点位相关接口")
@RequestMapping("/ros2/point")
public class Nav2PointController {

    @Autowired
    private Nav2PointService nav2PointService;

    @ApiOperation("保存导航点位")
    @PostMapping("/save")
    public AjaxResult saveNav2Point(@RequestBody Nav2Point nav2Point) {
        try {
            int result = nav2PointService.saveNav2Point(nav2Point);
            if (result > 0) {
                return AjaxResult.success("保存成功");
            } else {
                return AjaxResult.error("保存失败");
            }
        } catch (Exception e) {
            return AjaxResult.error("保存过程中发生异常: " + e.getMessage());
        }
    }

    @ApiOperation("查询所有导航点位")
    @GetMapping("/list")
    public AjaxResult getAllNav2Points() {
        try {
            List<Nav2Point> nav2Points = nav2PointService.getAllNav2Points();
            return AjaxResult.success(nav2Points);
        } catch (Exception e) {
            return AjaxResult.error("查询过程中发生异常: " + e.getMessage());
        }
    }
    
    @ApiOperation("删除导航点位")
    @DeleteMapping("/delete/{id}")
    public AjaxResult deleteNav2Point(@PathVariable Long id) {
        try {
            int result = nav2PointService.deleteNav2Point(id);
            if (result > 0) {
                return AjaxResult.success("删除成功");
            } else {
                return AjaxResult.error("删除失败");
            }
        } catch (Exception e) {
            return AjaxResult.error("删除过程中发生异常: " + e.getMessage());
        }
    }
}
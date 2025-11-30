package com.ruoyi.web.controller.openTCS;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.web.model.dto.VehicleInitDTO;
import com.ruoyi.web.model.dto.VehiclePausedDTO;
import com.ruoyi.web.model.dto.VehiclesQueryDTO;
import com.ruoyi.web.service.VehiclesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@Api(value = "车辆管理相关接口", tags = "车辆管理相关接口")
@RequestMapping("/vehicle")
public class VehicleController
    extends BaseController {
@Resource
private VehiclesService vehiclesService;


  @ApiOperation("分页获取车辆列表")
  @GetMapping("/page")
  public TableDataInfo page(VehiclesQueryDTO vehiclesQueryDTO) {
    startPage();
    return getDataTable(vehiclesService.page(vehiclesQueryDTO));
  }

  @ApiOperation("开启适配器使能和初始化位置")
  @PostMapping("/init")
  public AjaxResult initVehicle(@RequestBody VehicleInitDTO vehiclesQueryDTO) {
    vehiclesService.initVehicle(vehiclesQueryDTO);
    return AjaxResult.success();
  }


  @ApiOperation("恢复或暂停车辆")
  @PostMapping("/paused")
  public AjaxResult paused(@RequestBody VehiclePausedDTO vehiclePausedDTO) {
    vehiclesService.paused(vehiclePausedDTO);
    return AjaxResult.success();
  }
}

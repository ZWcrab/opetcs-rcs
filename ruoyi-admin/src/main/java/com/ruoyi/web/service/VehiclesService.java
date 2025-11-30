package com.ruoyi.web.service;


import com.ruoyi.web.model.dto.VehicleInitDTO;
import com.ruoyi.web.model.dto.VehiclePausedDTO;
import com.ruoyi.web.model.dto.VehiclesQueryDTO;
import com.ruoyi.web.model.vo.MyVehiclesVO;

import java.util.List;

public interface VehiclesService {

  List<MyVehiclesVO> page(VehiclesQueryDTO vehiclesQueryDTO);

  void initVehicle(VehicleInitDTO vehiclesQueryDTO);

  void paused(VehiclePausedDTO vehiclePausedDTO);
}

package com.ruoyi.web.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.web.service.MapService;
import org.opentcs.access.KernelServicePortal;
import org.opentcs.components.kernel.services.PlantModelService;
import org.opentcs.data.model.Location;
import org.opentcs.data.model.PlantModel;
import org.opentcs.data.model.Point;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Set;

@Service
public class MapServiceImpl implements MapService {

    /**
     * 通过 Spring 注入 KernelServicePortal，避免在项目启动时就触发连接。
     */
    @Resource
    private KernelServicePortal kernelServicePortal;

    private PlantModelService getPlantModelService() {
        try {
            return kernelServicePortal.getPlantModelService();
        } catch (Exception e) {
            throw new ServiceException("openTCS 内核未连接或异常，无法获取地图信息").setDetailMessage(e.getMessage());
        }
    }

    @Override
    public Set<Point> getPoints() {
        return getPlantModelService().getPlantModel().getPoints();
    }

    @Override
    public Set<Location> getLocations() {
        return getPlantModelService().getPlantModel().getLocations();
    }

    @Override
    public PlantModel getMap() {
        return getPlantModelService().getPlantModel();
    }
}

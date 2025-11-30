package com.ruoyi.web.service.impl;

import com.ruoyi.web.kernel.KernelServiceConfig;
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

    private static final KernelServicePortal kernelServicePortal = KernelServiceConfig.getKernelServicePortal();


    @Override
    public Set<Point> getPoints() {
        PlantModelService plantModelService = kernelServicePortal.getPlantModelService();
        return plantModelService.getPlantModel().getPoints();
    }
    @Override
    public Set<Location> getLocations() {
        PlantModelService plantModelService = kernelServicePortal.getPlantModelService();
        return plantModelService.getPlantModel().getLocations();
    }

    @Override
    public PlantModel getMap() {
        PlantModelService plantModelService = kernelServicePortal.getPlantModelService();
        return plantModelService.getPlantModel();
    }
}

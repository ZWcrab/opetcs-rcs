package com.ruoyi.web.service;

import org.opentcs.data.model.Location;
import org.opentcs.data.model.PlantModel;
import org.opentcs.data.model.Point;

import java.util.Set;

public interface MapService {

  Set<Point> getPoints();

  Set<Location> getLocations();

  PlantModel getMap();
}

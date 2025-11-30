package com.ruoyi.web.model.vo;

import org.opentcs.data.model.Vehicle;
import org.opentcs.kernel.extensions.servicewebapi.v1.binding.GetVehicleResponseTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyVehiclesVO {


  private String name;

  private Map<String, String> properties = new HashMap<>();

  private int length;

  private int energyLevelGood;

  private int energyLevelCritical;

  private int energyLevel;

  private Vehicle.IntegrationLevel integrationLevel = Vehicle.IntegrationLevel.TO_BE_RESPECTED;

  private boolean paused;

  private Vehicle.ProcState procState = Vehicle.ProcState.IDLE;

  private Integer procStatus;

  private String transportOrder;

  private String currentPosition;

  private GetVehicleResponseTO.PrecisePosition precisePosition;

  private Vehicle.State state = Vehicle.State.UNKNOWN;

  private Integer status;

  private List<RoutePath> allocatedResources = new ArrayList<>();

  private List<RoutePath> claimedResources = new ArrayList<>();

  private List<String> allowedOrderTypes = new ArrayList<>();

  private String envelopeKey;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getEnergyLevelGood() {
        return energyLevelGood;
    }

    public void setEnergyLevelGood(int energyLevelGood) {
        this.energyLevelGood = energyLevelGood;
    }

    public int getEnergyLevelCritical() {
        return energyLevelCritical;
    }

    public void setEnergyLevelCritical(int energyLevelCritical) {
        this.energyLevelCritical = energyLevelCritical;
    }

    public int getEnergyLevel() {
        return energyLevel;
    }

    public void setEnergyLevel(int energyLevel) {
        this.energyLevel = energyLevel;
    }

    public Vehicle.IntegrationLevel getIntegrationLevel() {
        return integrationLevel;
    }

    public void setIntegrationLevel(Vehicle.IntegrationLevel integrationLevel) {
        this.integrationLevel = integrationLevel;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public Vehicle.ProcState getProcState() {
        return procState;
    }

    public void setProcState(Vehicle.ProcState procState) {
        this.procState = procState;
    }

    public Integer getProcStatus() {
        return procStatus;
    }

    public void setProcStatus(Integer procStatus) {
        this.procStatus = procStatus;
    }

    public String getTransportOrder() {
        return transportOrder;
    }

    public void setTransportOrder(String transportOrder) {
        this.transportOrder = transportOrder;
    }

    public String getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(String currentPosition) {
        this.currentPosition = currentPosition;
    }

    public GetVehicleResponseTO.PrecisePosition getPrecisePosition() {
        return precisePosition;
    }

    public void setPrecisePosition(GetVehicleResponseTO.PrecisePosition precisePosition) {
        this.precisePosition = precisePosition;
    }

    public Vehicle.State getState() {
        return state;
    }

    public void setState(Vehicle.State state) {
        this.state = state;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<RoutePath> getAllocatedResources() {
        return allocatedResources;
    }

    public void setAllocatedResources(List<RoutePath> allocatedResources) {
        this.allocatedResources = allocatedResources;
    }

    public List<RoutePath> getClaimedResources() {
        return claimedResources;
    }

    public void setClaimedResources(List<RoutePath> claimedResources) {
        this.claimedResources = claimedResources;
    }

    public List<String> getAllowedOrderTypes() {
        return allowedOrderTypes;
    }

    public void setAllowedOrderTypes(List<String> allowedOrderTypes) {
        this.allowedOrderTypes = allowedOrderTypes;
    }

    public String getEnvelopeKey() {
        return envelopeKey;
    }

    public void setEnvelopeKey(String envelopeKey) {
        this.envelopeKey = envelopeKey;
    }
}

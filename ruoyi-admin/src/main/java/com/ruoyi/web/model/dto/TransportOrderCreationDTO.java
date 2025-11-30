package com.ruoyi.web.model.dto;


import java.util.List;

public class TransportOrderCreationDTO {

    private Boolean dispensable;

    private String vehicleName;

    private List<DestinationCreationDTO> destinations;

    public Boolean getDispensable() {
        return dispensable;
    }

    public void setDispensable(Boolean dispensable) {
        this.dispensable = dispensable;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public List<DestinationCreationDTO> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<DestinationCreationDTO> destinations) {
        this.destinations = destinations;
    }
}

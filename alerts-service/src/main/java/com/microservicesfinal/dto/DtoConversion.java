package com.microservicesfinal.dto;

import com.microservicesfinal.dto.requests.CreateAlertRequest;
import com.microservicesfinal.dto.responses.AlertResponse;
import com.microservicesfinal.model.Alert;

public class DtoConversion {

    //Alert <-> DTO
    public static AlertResponse mapAlertToDTO (Alert alert) {
        return AlertResponse.builder()
                .alertId(alert.getId())
                .vehicleId(alert.getVehicleId())
                .vehicleMileage(alert.getVehicleMileage())
                .maintenanceType(alert.getMaintenanceType())
                .status(alert.getStatus())
                .createdOn(alert.getCreatedOn())
                .closedOn(alert.getClosedOn())
                .build();
    }

    public static Alert mapDTOToAlert (CreateAlertRequest createAlertRequest) {
        return Alert.builder()
                .vehicleId(createAlertRequest.getVehicleId())
                .vehicleMileage(createAlertRequest.getVehicleMileage())
                .maintenanceType(createAlertRequest.getMaintenanceType())
                .status(createAlertRequest.getStatus())
                .build();

                //NOTE: set created on within save alert service method
                //NOTE: set closed on within patch alert service method when status is set to closed.
    }
}

package com.microservicesfinal.dto;

import com.microservicesfinal.dto.requests.CreateMaintenanceHistoryRequest;
import com.microservicesfinal.dto.responses.MaintenanceHistoryResponse;
import com.microservicesfinal.dto.responses.MaintenanceIntervalResponse;
import com.microservicesfinal.model.MaintenanceHistory;
import com.microservicesfinal.model.MaintenanceInterval;

public class DtoConversion {
    //MaintenanceInterval -> DTO  --- ONLY NEED ONE-WAY MAP SINCE THERE ARE NO CREATE/UPDATE REQUESTS
    public static MaintenanceIntervalResponse mapMaintenanceIntervalToDto(MaintenanceInterval maintenanceInterval) {
        return MaintenanceIntervalResponse.builder()
                .maintenanceIntervalId(maintenanceInterval.getId())
                .templateName(maintenanceInterval.getTemplateName())
                .make(maintenanceInterval.getMake())
                .model(maintenanceInterval.getModel())
                .oilChangeIntervalMiles(maintenanceInterval.getOilChangeIntervalMiles())
                .tireRotationIntervalMonths(maintenanceInterval.getTireRotationIntervalMonths())
                .cabinFilterChangeIntervalMiles(maintenanceInterval.getCabinFilterChangeIntervalMiles())
                .sparkPlugChangeIntervalMiles(maintenanceInterval.getSparkPlugChangeIntervalMiles())
                .build();
    }

    //History <-> DTO
    public static MaintenanceHistoryResponse mapMaintenanceHistoryToDto(MaintenanceHistory maintenanceHistory) {
        return MaintenanceHistoryResponse.builder()
                .maintenanceHistoryId(maintenanceHistory.getId())
                .vehicleId(maintenanceHistory.getVehicleId())
                .maintenanceType(maintenanceHistory.getMaintenanceType())
                .completedMileage(maintenanceHistory.getCompletedMileage())
                .completedDate(maintenanceHistory.getCompletedDate())
                .build();
    }

    public static MaintenanceHistory mapDtoToMaintenanceHistory(CreateMaintenanceHistoryRequest createMaintenanceHistoryRequest){
        return MaintenanceHistory.builder()
                .vehicleId(createMaintenanceHistoryRequest.getVehicleId())
                .maintenanceType(createMaintenanceHistoryRequest.getMaintenanceType())
                .completedMileage(createMaintenanceHistoryRequest.getCompletedMileage())
                .build();
    }
}

package com.microservicesfinal.dto.requests;

import com.microservicesfinal.enums.AlertStatus;
import com.microservicesfinal.enums.MaintenanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateAlertRequest {

    private Integer vehicleId;
    private Integer vehicleMileage;
    private MaintenanceType maintenanceType;
    private AlertStatus status;
}

//Maintenance type enum
/*
    OIL_CHANGE,
    TIRE_ROTATION,
    CABIN_FILTER,
    SPARK_PLUGS

 */
//Status type enum
/*
    ACTIVE,
    COMPLETED
 */
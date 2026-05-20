package com.microservicesfinal.dto.requests;

import com.microservicesfinal.model.enums.MaintenanceType;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateMaintenanceHistoryRequest {

    private Integer vehicleId;
    private MaintenanceType maintenanceType;
    private Integer completedMileage;
}

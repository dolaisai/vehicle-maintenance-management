package com.microservicesfinal.dto.responses;

import com.microservicesfinal.enums.MaintenanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MaintenanceHistoryResponse {

    private Integer maintenanceHistoryId;
    private Integer vehicleId;
    private MaintenanceType maintenanceType;
    private Integer completedMileage;
    private LocalDateTime completedDate;
}

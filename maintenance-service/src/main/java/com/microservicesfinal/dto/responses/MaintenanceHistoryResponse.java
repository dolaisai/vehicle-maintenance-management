package com.microservicesfinal.dto.responses;

import com.microservicesfinal.model.enums.MaintenanceType;
import lombok.*;

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

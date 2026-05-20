package com.microservicesfinal.dto.responses;

import com.microservicesfinal.model.enums.AlertStatus;
import com.microservicesfinal.model.enums.MaintenanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlertResponse {
    private Integer alertId;
    private Integer vehicleId;
    private Integer vehicleMileage;
    private MaintenanceType maintenanceType;
    private AlertStatus status;
    private LocalDateTime createdOn;
    private LocalDateTime closedOn;
}

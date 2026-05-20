package com.microservicesfinal.dto.responses;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MaintenanceIntervalResponse {
    private Integer maintenanceIntervalId;
    private String templateName;
    private String make;
    private String model;
    private Integer oilChangeIntervalMiles;
    private Integer cabinFilterChangeIntervalMiles;
    private Integer sparkPlugChangeIntervalMiles;
    private Integer tireRotationIntervalMonths;
}

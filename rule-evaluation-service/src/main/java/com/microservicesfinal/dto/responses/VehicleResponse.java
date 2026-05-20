package com.microservicesfinal.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class VehicleResponse {
    private Integer vehicleId;
    private String make;
    private String model;
    private int mileage;
    private int maintenanceTemplateId;
    private Integer userId;
    private LocalDateTime createdAt;
}

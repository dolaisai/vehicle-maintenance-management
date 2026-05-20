package com.microservicesfinal.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class CreateVehicleRequest {
    private String make;
    private String model;
    private int mileage;
    private int maintenanceTemplateId;
    private Integer userId;
}

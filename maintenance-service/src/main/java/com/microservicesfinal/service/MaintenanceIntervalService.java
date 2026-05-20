package com.microservicesfinal.service;

import com.microservicesfinal.dto.responses.MaintenanceIntervalResponse;

import java.util.List;

public interface MaintenanceIntervalService {
    //GET
    MaintenanceIntervalResponse getMaintenanceInterval(Integer maintenanceIntervalId);
    MaintenanceIntervalResponse getMaintenanceIntervalByMakeAndModel(String make, String model);
    List<MaintenanceIntervalResponse> getAllMaintenanceIntervals();

    //NOTE: This service layer will be READ ONLY.
    //      Maintenance Interval Templates will be populated into postgres through a data.sql file!
}

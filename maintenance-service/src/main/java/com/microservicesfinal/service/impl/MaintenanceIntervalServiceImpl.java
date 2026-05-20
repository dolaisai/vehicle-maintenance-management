package com.microservicesfinal.service.impl;

import com.microservicesfinal.dto.DtoConversion;
import com.microservicesfinal.dto.responses.MaintenanceIntervalResponse;
import com.microservicesfinal.exceptions.ResourceNotFoundException;
import com.microservicesfinal.model.MaintenanceInterval;
import com.microservicesfinal.repository.MaintenanceIntervalRepo;
import com.microservicesfinal.service.MaintenanceIntervalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MaintenanceIntervalServiceImpl implements MaintenanceIntervalService {

    private final MaintenanceIntervalRepo maintenanceIntervalRepo;

    @Override
    public MaintenanceIntervalResponse getMaintenanceInterval(Integer maintenanceIntervalId) {
        log.info("fetching Maintenance Interval with id: {}", maintenanceIntervalId);
        MaintenanceInterval maintenanceInterval = maintenanceIntervalRepo.findById(maintenanceIntervalId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance Interval with id: " + maintenanceIntervalId));
        return DtoConversion.mapMaintenanceIntervalToDto(maintenanceInterval);
    }

    @Override
    public MaintenanceIntervalResponse getMaintenanceIntervalByMakeAndModel(String make, String model) {
        log.info("fetching Maintenance Interval with Make name: {} and Model name: {}", make, model);
        MaintenanceInterval maintenanceInterval = maintenanceIntervalRepo.findByMakeIgnoreCaseAndModelIgnoreCase(make, model)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance Interval with make: " + make + " and model: " + model));
        return DtoConversion.mapMaintenanceIntervalToDto(maintenanceInterval);
    }

    @Override
    public List<MaintenanceIntervalResponse> getAllMaintenanceIntervals() {
        log.info("fetching ALL Maintenance Intervals");
        List<MaintenanceInterval> intervals = maintenanceIntervalRepo.findAll();
        return intervals.stream().map(DtoConversion::mapMaintenanceIntervalToDto).toList();
    }

}

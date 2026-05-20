package com.microservicesfinal.controller;

import com.microservicesfinal.dto.responses.MaintenanceIntervalResponse;
import com.microservicesfinal.service.MaintenanceIntervalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-intervals")
@RequiredArgsConstructor
public class MaintenanceIntervalController {

    private final MaintenanceIntervalService maintenanceIntervalService;

    @GetMapping
    public ResponseEntity<List<MaintenanceIntervalResponse>> getAllMaintenanceIntervals() {
        return ResponseEntity.ok(maintenanceIntervalService.getAllMaintenanceIntervals());
    }

    @GetMapping("/{maintenanceIntervalId}")
    public ResponseEntity<MaintenanceIntervalResponse> getMaintenanceIntervalById(@PathVariable Integer maintenanceIntervalId) {
        return ResponseEntity.ok(maintenanceIntervalService.getMaintenanceInterval(maintenanceIntervalId));
    }
    @GetMapping(params = {"make", "model"})
    public ResponseEntity<MaintenanceIntervalResponse> getMaintenanceIntervalByMakeAndModel(
            @RequestParam String make,
            @RequestParam String model
    ) {
        return ResponseEntity.ok(maintenanceIntervalService.getMaintenanceIntervalByMakeAndModel(make, model));
    }

}

package com.microservicesfinal.controller;

import com.microservicesfinal.dto.requests.CreateMaintenanceHistoryRequest;
import com.microservicesfinal.dto.responses.MaintenanceHistoryResponse;
import com.microservicesfinal.service.MaintenanceHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-histories")
@RequiredArgsConstructor
public class MaintenanceHistoryController {

    private final MaintenanceHistoryService maintenanceHistoryService;

    //history ID specific
    @GetMapping("/{historyId}")
    public ResponseEntity<MaintenanceHistoryResponse> getMaintenanceHistory(@PathVariable("historyId") Integer historyId) {
        return ResponseEntity.ok(maintenanceHistoryService.getMaintenanceHistory(historyId));
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceHistoryResponse>> getAllMaintenanceHistory() {
        return ResponseEntity.ok(maintenanceHistoryService.getAllMaintenanceHistory());
    }

    //vehicle specific
    @GetMapping("/vehicle/{vehicleId}/recent")
    public ResponseEntity<List<MaintenanceHistoryResponse>> getAllRecentMaintenanceHistoryForVehicleWithId(@PathVariable("vehicleId") Integer vehicleId) {
        return ResponseEntity.ok(maintenanceHistoryService.getAllRecentMaintenanceHistoryForVehicleWithId(vehicleId));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceHistoryResponse>> getAllMaintenanceHistoryForVehicleWithId(@PathVariable("vehicleId") Integer vehicleId) {
        return ResponseEntity.ok(maintenanceHistoryService.getAllMaintenanceHistoryByVehicleId(vehicleId));
    }



    @PostMapping
    public ResponseEntity<MaintenanceHistoryResponse> postMaintenanceHistory(@RequestBody CreateMaintenanceHistoryRequest request) {
        return new ResponseEntity<>(
                maintenanceHistoryService.saveMaintenanceHistory(request),
                HttpStatus.CREATED
        );
    }
}

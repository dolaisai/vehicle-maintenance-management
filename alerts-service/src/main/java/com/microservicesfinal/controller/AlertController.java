package com.microservicesfinal.controller;

import com.microservicesfinal.dto.requests.CreateAlertRequest;
import com.microservicesfinal.dto.requests.PatchAlertRequest;
import com.microservicesfinal.dto.responses.AlertResponse;
import com.microservicesfinal.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(
        origins = "http://localhost:3000",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertResponse>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/all-active")
    public ResponseEntity<List<AlertResponse>> getAllActiveAlerts(){
        return ResponseEntity.ok(alertService.getAllActiveAlerts());
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable Integer alertId) {
        return ResponseEntity.ok(alertService.getAlert(alertId));
    }

    @PostMapping
    public ResponseEntity<AlertResponse> postAlert(@RequestBody CreateAlertRequest createAlertRequest) {
        return new ResponseEntity<>(
                alertService.saveAlert(createAlertRequest),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/{alertId}")
    public ResponseEntity<AlertResponse> patchAlert(
            @RequestBody PatchAlertRequest patchAlertRequest,
            @PathVariable Integer alertId
    ) {
        return ResponseEntity.ok(alertService.patchAlert(patchAlertRequest, alertId));
    }

    @DeleteMapping("/{alertId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteVehicleById(@PathVariable Integer alertId){
        alertService.deleteAlert(alertId);
    }
}

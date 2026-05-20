package com.microservicesfinal.controller;
import com.microservicesfinal.dto.requests.CreateVehicleRequest;
import com.microservicesfinal.dto.requests.PatchVehicleRequest;
import com.microservicesfinal.dto.responses.VehicleResponse;
import com.microservicesfinal.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @GetMapping("/{vehicleId}")
    @SneakyThrows
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Integer vehicleId) {
        return ResponseEntity.ok(vehicleService.getVehicle(vehicleId));
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> postVehicle(@RequestBody CreateVehicleRequest createVehicleRequest) {
        return new ResponseEntity<>(
                vehicleService.saveVehicle(createVehicleRequest),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> patchVehicle(
            @RequestBody PatchVehicleRequest patchVehicleRequest,
            @PathVariable Integer vehicleId
    ) {
        return ResponseEntity.ok(vehicleService.patchVehicle(patchVehicleRequest, vehicleId));
    }

    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteVehicleById(@PathVariable Integer vehicleId){
        vehicleService.deleteVehicle(vehicleId);
    }
}

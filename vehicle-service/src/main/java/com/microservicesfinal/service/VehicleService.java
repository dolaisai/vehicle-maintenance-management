package com.microservicesfinal.service;

import com.microservicesfinal.dto.requests.CreateVehicleRequest;
import com.microservicesfinal.dto.requests.PatchVehicleRequest;
import com.microservicesfinal.dto.responses.VehicleResponse;

import java.util.List;

public interface VehicleService {

    //GET
    VehicleResponse getVehicle(Integer vehicleId);
    List<VehicleResponse> getAllVehicles();

    //POST
    VehicleResponse saveVehicle(CreateVehicleRequest createVehicleRequest);

    //PUT-PATCH
    VehicleResponse patchVehicle(PatchVehicleRequest patchVehicleRequest, Integer vehicleId);

    //DELETE
    void deleteVehicle(Integer vehicleId);
}

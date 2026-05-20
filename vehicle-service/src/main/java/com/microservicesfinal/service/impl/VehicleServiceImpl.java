package com.microservicesfinal.service.impl;

import com.microservicesfinal.dto.DtoConversion;
import com.microservicesfinal.dto.requests.CreateVehicleRequest;
import com.microservicesfinal.dto.requests.PatchVehicleRequest;
import com.microservicesfinal.dto.responses.VehicleResponse;
import com.microservicesfinal.exceptions.ResourceNotFoundException;
import com.microservicesfinal.model.User;
import com.microservicesfinal.model.Vehicle;
import com.microservicesfinal.repository.UserRepo;
import com.microservicesfinal.repository.VehicleRepo;
import com.microservicesfinal.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepo vehicleRepo;
    private final UserRepo userRepo;
    private final WebClient.Builder webClientBuilder;

    @Override
    public VehicleResponse getVehicle(Integer vehicleId) {
        log.info("fetching Vehicle with id: {}", vehicleId);
        Vehicle fetchedVehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle with id: " + vehicleId));

        return DtoConversion.mapVehicletoDTO(fetchedVehicle);
    }

    @Override
    public List<VehicleResponse> getAllVehicles() {
        log.info("fetching ALL Vehicles");
        List<Vehicle> vehicles = vehicleRepo.findAll();
        return vehicles.stream().map(DtoConversion::mapVehicletoDTO).toList();
    }

    @Override
    public VehicleResponse saveVehicle(CreateVehicleRequest createVehicleRequest) {
        log.info("Creating new Vehicle under user id: {}", createVehicleRequest.getUserId());

        //check if user exists. or else throw exception
        User fetchedUser = userRepo.findById(createVehicleRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + createVehicleRequest.getUserId()));

        Vehicle vehicle = DtoConversion.mapDTOtoVehicle(createVehicleRequest);
        vehicle.setUser(fetchedUser);

        return DtoConversion.mapVehicletoDTO(vehicleRepo.save(vehicle));
    }

    @Override
    public VehicleResponse patchVehicle(PatchVehicleRequest patchVehicleRequest, Integer vehicleId) {
        log.info("Check if vehicle with id: {} exists for potential patch", vehicleId);
        Vehicle fetchedVehicle = vehicleRepo.findById(vehicleId).
                orElseThrow(() -> new ResourceNotFoundException("Vehicle with ID: " + vehicleId));

        log.info("check if mileage is valid for Vehicle id: {}", vehicleId);
        if (patchVehicleRequest.getMileage() < fetchedVehicle.getMileage()) {
            log.warn("Mileage decreased for vehicle id: {} from {} to {}",
                    vehicleId, fetchedVehicle.getMileage(), patchVehicleRequest.getMileage());
        }

        fetchedVehicle.setMileage(patchVehicleRequest.getMileage());

        VehicleResponse response = DtoConversion.mapVehicletoDTO(vehicleRepo.save(fetchedVehicle));

        //call Rule Eval API to run on newly saved vehicle.
        webClientBuilder.build().get()
                .uri("http://rule-evaluation-service/api/rule-eval/{vehicleId}", response.getVehicleId())
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe();

        return response;
    }

    @Override
    public void deleteVehicle(Integer vehicleId) {
        log.info("Deleting vehicle with id: {}", vehicleId);
        try {vehicleRepo.deleteById(vehicleId);}
        catch(EmptyResultDataAccessException exception){
            throw new ResourceNotFoundException("Vehicle with ID: " + vehicleId);
        }
    }
}

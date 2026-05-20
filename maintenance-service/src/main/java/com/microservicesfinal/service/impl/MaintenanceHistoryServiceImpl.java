package com.microservicesfinal.service.impl;

import com.microservicesfinal.dto.DtoConversion;
import com.microservicesfinal.dto.requests.CreateMaintenanceHistoryRequest;
import com.microservicesfinal.dto.responses.MaintenanceHistoryResponse;
import com.microservicesfinal.exceptions.ResourceNotFoundException;
import com.microservicesfinal.model.MaintenanceHistory;
import com.microservicesfinal.model.enums.MaintenanceType;
import com.microservicesfinal.repository.MaintenanceHistoryRepo;
import com.microservicesfinal.service.MaintenanceHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MaintenanceHistoryServiceImpl implements MaintenanceHistoryService {

    private final MaintenanceHistoryRepo maintenanceHistoryRepo;

    @Override
    public MaintenanceHistoryResponse getMaintenanceHistory(Integer maintenanceHistoryId) {
        log.info("fetching Maintenance History with historyId {}", maintenanceHistoryId);
        MaintenanceHistory maintenanceHistory = maintenanceHistoryRepo.findById(maintenanceHistoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance History with id: " + maintenanceHistoryId));
        return DtoConversion.mapMaintenanceHistoryToDto(maintenanceHistory);
    }

    @Override
    public List<MaintenanceHistoryResponse> getAllMaintenanceHistory() {
        log.info("fetching All Maintenance History for all vehicles");
        List<MaintenanceHistory> histories = maintenanceHistoryRepo.findAll();
        return histories.stream().map(DtoConversion::mapMaintenanceHistoryToDto).toList();
    }

    @Override
    public List<MaintenanceHistoryResponse>
    getAllRecentMaintenanceHistoryForVehicleWithId(Integer vehicleId) {

        log.info("Fetching all recent Maintenance History for vehicle with id {}", vehicleId);
        List<MaintenanceHistoryResponse> histories = new ArrayList<>();

        for(MaintenanceType type : MaintenanceType.values()) {
            Optional<MaintenanceHistory> history =
                    maintenanceHistoryRepo
                            .findTopByVehicleIdAndMaintenanceTypeOrderByCompletedDateDesc(vehicleId, type);

            if(history.isPresent()) {
                histories.add(DtoConversion.mapMaintenanceHistoryToDto(history.get()));
            }
            else {
                MaintenanceHistoryResponse emptyHistory =
                        MaintenanceHistoryResponse.builder()
                            .vehicleId(vehicleId)
                            .maintenanceType(type)
                            .completedDate(null)
                            .completedMileage(null)
                            .build();

                histories.add(emptyHistory);
            }
        }
        return histories;
    }

    @Override
    public List<MaintenanceHistoryResponse> getAllMaintenanceHistoryByVehicleId(Integer vehicleId) {
        log.info("fetching All Maintenance History of vehicle with id {}", vehicleId);
        List<MaintenanceHistory> histories = maintenanceHistoryRepo.findAllByVehicleId(vehicleId);
        return histories.stream().map(DtoConversion::mapMaintenanceHistoryToDto).toList();
    }

    @Override
    public MaintenanceHistoryResponse saveMaintenanceHistory(CreateMaintenanceHistoryRequest createMaintenanceHistoryRequest) {
        log.info("creating Maintenance History for vehicle with ID: {}", createMaintenanceHistoryRequest.getVehicleId());

        MaintenanceHistory history = DtoConversion.mapDtoToMaintenanceHistory(createMaintenanceHistoryRequest);
        history.setCompletedDate(LocalDateTime.now()); //when a history record is saved, add a timestamp to the record.

        return DtoConversion.mapMaintenanceHistoryToDto(maintenanceHistoryRepo.save(history));
    }
}

package com.microservicesfinal.service.impl;

import com.microservicesfinal.dto.DtoConversion;
import com.microservicesfinal.dto.requests.CreateAlertRequest;
import com.microservicesfinal.dto.requests.CreateMaintenanceHistoryRequest;
import com.microservicesfinal.dto.requests.PatchAlertRequest;
import com.microservicesfinal.dto.responses.AlertResponse;
import com.microservicesfinal.exceptions.ResourceNotFoundException;
import com.microservicesfinal.model.Alert;
import com.microservicesfinal.model.enums.AlertStatus;
import com.microservicesfinal.repository.AlertRepo;
import com.microservicesfinal.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepo alertRepo;
    private final KafkaTemplate<String, CreateMaintenanceHistoryRequest> kafkaTemplate;
    private final EmailService emailService;

    @Override
    public AlertResponse getAlert(Integer alertId) {
        log.info("fetching Alert with id: {}", alertId);
        Alert fetchedAlert =  alertRepo.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert with id: " + alertId));
        return DtoConversion.mapAlertToDTO(fetchedAlert);
    }


    @Override
    public List<AlertResponse> getAllActiveAlerts() {
        log.info("fetching ALL ACTIVE Alerts");
        List<Alert> alerts = alertRepo.findAllByStatus(AlertStatus.ACTIVE);
        return alerts.stream().map(DtoConversion::mapAlertToDTO).toList();
    }

    @Override
    public List<AlertResponse> getAllAlerts() {
        log.info("fetching ALL Alerts");
        List<Alert> alerts = alertRepo.findAll();
        return alerts.stream().map(DtoConversion::mapAlertToDTO).toList();
    }

    @Override
    public AlertResponse saveAlert(CreateAlertRequest createAlertRequest) {

        log.info(
                "Checking for existing ACTIVE alert for vehicleId: {} and maintenanceType: {}",
                createAlertRequest.getVehicleId(),
                createAlertRequest.getMaintenanceType()
        );

        Optional<Alert> existingAlert =
                alertRepo.findByVehicleIdAndMaintenanceTypeAndStatus(
                        createAlertRequest.getVehicleId(),
                        createAlertRequest.getMaintenanceType(),
                        AlertStatus.ACTIVE
                );

        if(existingAlert.isPresent()) {
            log.warn("ACTIVE alert already exists.");

            return DtoConversion.mapAlertToDTO(existingAlert.get());
        }

        log.warn(
                "Creating new ACTIVE alert for vehicle id: {}",
                createAlertRequest.getVehicleId()
        );

        Alert alert = DtoConversion.mapDTOToAlert(createAlertRequest);
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setCreatedOn(LocalDateTime.now());

        AlertResponse response = DtoConversion.mapAlertToDTO(alertRepo.save(alert));

        try{
            emailService.sendEmail(
                    "isaiah.dolan95@gmail.com",
                    "Maintenance Due: " + response.getMaintenanceType(),
                    "Maintenance due for vehicle ID: " + response.getVehicleId() +
                            "\nMaintenance Type: " + response.getMaintenanceType() +
                            "\nStatus: " + response.getStatus()
            );
        }
        catch(Exception ex){
            log.error("Failed to send SMS notification: {}", ex.getMessage());
        }

        return response;
    }

    @Override
    public AlertResponse patchAlert(PatchAlertRequest patchAlertRequest, Integer alertId) {

        Alert fetchedAlert = alertRepo.findById(alertId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Alert with id: " + alertId));

        // If status of alert is already completed
        if (fetchedAlert.getStatus() == AlertStatus.COMPLETED) {
            return DtoConversion.mapAlertToDTO(fetchedAlert);
        }

        // transition ACTIVE -> COMPLETED
        if (patchAlertRequest.getStatus() == AlertStatus.COMPLETED) {

            fetchedAlert.setStatus(AlertStatus.COMPLETED);
            fetchedAlert.setClosedOn(LocalDateTime.now());

            Alert savedAlert = alertRepo.save(fetchedAlert);

            // create history event for kafka broker.
            kafkaTemplate.send(
                    "createHistoryTopic",
                    CreateMaintenanceHistoryRequest.builder()
                            .vehicleId(savedAlert.getVehicleId())
                            .maintenanceType(savedAlert.getMaintenanceType())
                            .completedMileage(savedAlert.getVehicleMileage())
                            .build()
            );

            return DtoConversion.mapAlertToDTO(savedAlert);
        }

        return DtoConversion.mapAlertToDTO(fetchedAlert);
    }

    @Override
    public void deleteAlert(Integer alertId) {
        log.info("Deleting alert with id: {}", alertId);
        try {alertRepo.deleteById(alertId);}
        catch(EmptyResultDataAccessException exception){
            throw new ResourceNotFoundException("Alert with ID: " + alertId);
        }
    }
}

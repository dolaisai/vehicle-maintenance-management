package com.microservicesfinal.service.impl;

import com.microservicesfinal.clientRequest.ClientMaintenance;
import com.microservicesfinal.clientRequest.ClientVehicle;
import com.microservicesfinal.dto.requests.CreateAlertRequest;
import com.microservicesfinal.dto.responses.MaintenanceHistoryResponse;
import com.microservicesfinal.dto.responses.MaintenanceIntervalResponse;
import com.microservicesfinal.dto.responses.VehicleResponse;
import com.microservicesfinal.enums.AlertStatus;
import com.microservicesfinal.enums.MaintenanceType;
import com.microservicesfinal.service.RuleEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEvaluationServiceImpl implements RuleEvaluationService {

    private final ClientVehicle clientVehicle;
    private final ClientMaintenance clientMaintenance;
    private final KafkaTemplate<String, CreateAlertRequest> kafkaTemplate;

    @Override
    public void syncOne(Integer vehicleId) {
        //region GET VEHICLE INFO from vehicle service API
        CompletableFuture<VehicleResponse> vehicleFuture = clientVehicle.requestSingleVehicle(vehicleId);

        //get vehicle out of completable future.
        VehicleResponse vehicleResponse = vehicleFuture.join();

        if(vehicleResponse == null) {
            log.error("request to vehicle service unable to retrieve vehicle response");
            return;
        }
        //endregion

        //region GET MAINTENANCE TEMPLATE INFO from maintenance service API
        CompletableFuture<MaintenanceIntervalResponse> maintenanceIntervalResponseFuture =
                clientMaintenance.requestSingleMaintenanceInterval(vehicleResponse.getMake(), vehicleResponse.getModel());

        //get maintenance interval template out of completed future
        MaintenanceIntervalResponse maintenanceIntervalResponse = maintenanceIntervalResponseFuture.join();

        if(maintenanceIntervalResponse == null) {
            log.error("request to maintenance unable to retrieve maintenance interval");
            return;
        }
        //endregion

        //region GET VEHICLE HISTORY INFO from maintenance service API
        CompletableFuture<List<MaintenanceHistoryResponse>> maintenanceHistoryResponseFutures =
                clientMaintenance.requestRecentVehicleHistoryRecords(vehicleId);

        //get history responses out of completable future.
        List<MaintenanceHistoryResponse> maintenanceHistoryResponses =
                maintenanceHistoryResponseFutures.join();

        if(maintenanceHistoryResponses == null) {
            log.error("request to maintenance unable to retrieve maintenance history records");
            return;
        }
        //endregion

        //do comparison logic based on the vehicle,
        // maintenance interval,
        // and recent history records received from other services

        evaluation(vehicleResponse,
                maintenanceIntervalResponse,
                maintenanceHistoryResponses
        );
    }

    @Override
    public void syncAll() {
        //region GET ALL VEHICLE INFO from vehicle service API
        CompletableFuture<List<VehicleResponse>> vehiclesFutures = clientVehicle.requestAllVehicles();

        //get vehicles out of completable future.
        List<VehicleResponse> vehiclesResponses = vehiclesFutures.join();

        if(vehiclesResponses == null || vehiclesResponses.isEmpty()) {
            log.error("request to vehicle service unable to request all vehicles");
            return;
        }
        //endregion

        for(VehicleResponse vehicle : vehiclesResponses){
            //GET MAINTENANCE INTERVAL
            CompletableFuture<MaintenanceIntervalResponse> maintenanceIntervalResponseFuture =
                    clientMaintenance.requestSingleMaintenanceInterval(vehicle.getMake(), vehicle.getModel());

            //get maintenance interval out of completable future.
            MaintenanceIntervalResponse maintenanceIntervalResponse =
                    maintenanceIntervalResponseFuture.join();

            if(maintenanceIntervalResponse == null) {
                log.error("request to maintenance service unable to retrieve maintenance interval");
                continue;
            }

            //GET VEHICLE'S RECENT HISTORY
            CompletableFuture<List<MaintenanceHistoryResponse>> maintenanceHistoryResponseFutures =
                    clientMaintenance.requestRecentVehicleHistoryRecords(vehicle.getVehicleId());

            //get maintenance history responses out of completable future.
            List<MaintenanceHistoryResponse> maintenanceHistoryResponses =
                    maintenanceHistoryResponseFutures.join();

            if(maintenanceHistoryResponses == null) {
                log.error("request to maintenance service unable to retrieve maintenance history records");
                continue;
            }

            //DO EVALUATION
            evaluation(vehicle, maintenanceIntervalResponse, maintenanceHistoryResponses);
        }
    }


    private void evaluation(VehicleResponse vehicle,
                            MaintenanceIntervalResponse maintenanceInterval,
                            List<MaintenanceHistoryResponse> maintenanceHistoryResponses

    ){
        for(MaintenanceHistoryResponse history : maintenanceHistoryResponses){
            switch(history.getMaintenanceType()){
                //region OIL_CHANGE
                case OIL_CHANGE -> {
                    int pastMileage;

                    //check to see if history record is null.
                    if(history.getMaintenanceHistoryId() == null){
                        //vehicle has never been serviced for an OIL_CHANGE
                        pastMileage = 0;
                    }
                    else{
                        pastMileage = history.getCompletedMileage();
                    }
                    //vehicle has history record for oil change, check to see if mileage is over.
                    if(vehicle.getMileage() - pastMileage
                            >= maintenanceInterval.getOilChangeIntervalMiles()
                    ){
                        log.warn("VEHICLE with ID: {} is over the recommended OIL_CHANGE interval. \nOIL_CHANGE recommended.", vehicle.getVehicleId());
                        kafkaTemplate.send(
                                "createAlertTopic",
                                CreateAlertRequest.builder()
                                        .vehicleId(vehicle.getVehicleId())
                                        .vehicleMileage(vehicle.getMileage())
                                        .maintenanceType(MaintenanceType.OIL_CHANGE)
                                        .status(AlertStatus.ACTIVE)
                                        .build()
                        );
                    }
                    //endregion
                }
                case TIRE_ROTATION -> {
                    LocalDateTime pastTireRotationDate;

                    //check to see if history record is null.
                    if(history.getMaintenanceHistoryId() == null){
                        //vehicle has never been serviced for a TIRE_ROTATION
                        //fallback to createDate of the vehicle.
                        pastTireRotationDate = vehicle.getCreatedAt();
                    }
                    else{
                        pastTireRotationDate = history.getCompletedDate();
                    }

                    //vehicle has history record for a TIRE_ROTATION, check to see if time interval is over.
                    LocalDateTime today = LocalDateTime.now();

                    boolean overdue = pastTireRotationDate
                            .plusMonths(maintenanceInterval.getTireRotationIntervalMonths())
                            .isBefore(today);
                    if(overdue){
                        log.warn("VEHICLE with ID: {} is over the recommended TIRE_ROTATION interval. \nTIRE_ROTATION recommended.", vehicle.getVehicleId());
                        kafkaTemplate.send(
                                "createAlertTopic",
                                CreateAlertRequest.builder()
                                        .vehicleId(vehicle.getVehicleId())
                                        .vehicleMileage(vehicle.getMileage())
                                        .maintenanceType(MaintenanceType.TIRE_ROTATION)
                                        .status(AlertStatus.ACTIVE)
                                        .build()
                        );
                    }
                }
                case SPARK_PLUGS -> {
                    int pastMileage;

                    //check to see if history record is null.
                    if(history.getMaintenanceHistoryId() == null){
                        //vehicle has never been serviced for a SPARK_PLUG change
                        pastMileage = 0;
                    }
                    else{
                        pastMileage = history.getCompletedMileage();
                    }

                    //vehicle has history record for a SPARK_PLUG, check to see if mileage is over.
                    if(vehicle.getMileage() - pastMileage
                            >= maintenanceInterval.getSparkPlugChangeIntervalMiles()
                    ){
                        log.warn("VEHICLE with ID: {} is over the recommended SPARK_PLUGS change interval. \nSPARK_PLUGS change recommended.", vehicle.getVehicleId());
                        kafkaTemplate.send(
                                "createAlertTopic",
                                CreateAlertRequest.builder()
                                        .vehicleId(vehicle.getVehicleId())
                                        .vehicleMileage(vehicle.getMileage())
                                        .maintenanceType(MaintenanceType.SPARK_PLUGS)
                                        .status(AlertStatus.ACTIVE)
                                        .build()
                        );
                    }

                }
                case CABIN_FILTER -> {
                    int pastMileage;

                    //check to see if history record is null.
                    if(history.getMaintenanceHistoryId() == null){
                        //vehicle has never been serviced for a CABIN_FILTER change
                        pastMileage = 0;
                    }
                    else{
                        pastMileage = history.getCompletedMileage();
                    }
                    //vehicle has history record for a SPARK_PLUG, check to see if mileage is over.
                    if(vehicle.getMileage() - pastMileage
                            >= maintenanceInterval.getCabinFilterChangeIntervalMiles()
                    ){
                        log.warn("VEHICLE with ID: {} is over the recommended CABIN_FILTER change interval. \nCABIN_FILTER change recommended.", vehicle.getVehicleId());
                        kafkaTemplate.send(
                                "createAlertTopic",
                                CreateAlertRequest.builder()
                                        .vehicleId(vehicle.getVehicleId())
                                        .vehicleMileage(vehicle.getMileage())
                                        .maintenanceType(MaintenanceType.CABIN_FILTER)
                                        .status(AlertStatus.ACTIVE)
                                        .build()
                        );
                    }
                }
            }
        }
    }
//end of class
}

package com.microservicesfinal.kafkaConsumer;

import com.microservicesfinal.dto.requests.CreateMaintenanceHistoryRequest;
import com.microservicesfinal.dto.responses.MaintenanceHistoryResponse;
import com.microservicesfinal.service.MaintenanceHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HistoryEventListener {

    private final MaintenanceHistoryService maintenanceHistoryService;

    @KafkaListener(topics = "createHistoryTopic")
    public void handleHistoryCreateRequest(CreateMaintenanceHistoryRequest createMaintenanceHistoryRequest){
        MaintenanceHistoryResponse historyResponse = maintenanceHistoryService.saveMaintenanceHistory(createMaintenanceHistoryRequest);
        log.warn("history event caught and saved into DB for " +
                        "Vehicle ID: {}\n" +
                        "Maintenance Type: {}\n" +
                        "Current Mileage: {}\n" +
                        "Date Completed: {}",
                historyResponse.getMaintenanceHistoryId(),
                historyResponse.getMaintenanceType(),
                historyResponse.getCompletedMileage(),
                historyResponse.getCompletedDate()

        );
    }
}

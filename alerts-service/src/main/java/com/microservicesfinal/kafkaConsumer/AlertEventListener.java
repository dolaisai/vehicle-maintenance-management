package com.microservicesfinal.kafkaConsumer;

import com.microservicesfinal.dto.requests.CreateAlertRequest;
import com.microservicesfinal.dto.responses.AlertResponse;
import com.microservicesfinal.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertEventListener {

    private final AlertService alertService;

    @KafkaListener(topics = "createAlertTopic")
    public void handleAlertCreateRequest(CreateAlertRequest createAlertRequest){
        AlertResponse alert = alertService.saveAlert(createAlertRequest);
        log.info("alert event caught and saved into DB with ID: {} and status: {}", alert.getAlertId(), alert.getStatus());
    }
}

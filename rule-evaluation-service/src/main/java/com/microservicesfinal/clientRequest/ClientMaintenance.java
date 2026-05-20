package com.microservicesfinal.clientRequest;

import com.microservicesfinal.dto.responses.MaintenanceHistoryResponse;
import com.microservicesfinal.dto.responses.MaintenanceIntervalResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientMaintenance {

    private final WebClient.Builder webClientBuilder;

    @CircuitBreaker(name = "maintenance", fallbackMethod = "requestSingleMaintenanceIntervalFallback")
    @Retry(name = "maintenance")
    @TimeLimiter(name = "maintenance")
    public CompletableFuture<MaintenanceIntervalResponse> requestSingleMaintenanceInterval(String make, String model){
        return webClientBuilder.build().get()
                .uri("http://maintenance-service/api/maintenance-intervals?make={make}&model={model}", make, model)
                .retrieve()
                .bodyToMono(MaintenanceIntervalResponse.class)
                .toFuture();
    }

    @CircuitBreaker(name = "maintenance", fallbackMethod = "requestRecentVehicleHistoryRecordsFallback")
    @Retry(name = "maintenance")
    @TimeLimiter(name = "maintenance")
    public CompletableFuture<List<MaintenanceHistoryResponse>> requestRecentVehicleHistoryRecords(Integer vehicleId){

        return webClientBuilder.build().get()
                .uri("http://maintenance-service/api/maintenance-histories/vehicle/{vehicleId}/recent", vehicleId)
                .retrieve()
                .bodyToFlux(MaintenanceHistoryResponse.class)
                .collectList()
                .toFuture();
    }

    //endregion


    public CompletableFuture<MaintenanceIntervalResponse> requestSingleMaintenanceIntervalFallback(String make, String model, Throwable t) {
        log.error("Maintenance service (Maintenance Intervals) unavailable for make name: {}, model name: {} \n{}",make, model, t.getMessage());
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<List<MaintenanceHistoryResponse>> requestRecentVehicleHistoryRecordsFallback(Integer vehicleId, Throwable t) {
        log.error("Maintenance service (Maintenance Histories) unavailable for vehicleId: {}, \n{}", vehicleId, t.getMessage());
        return CompletableFuture.completedFuture(null);
    }
}

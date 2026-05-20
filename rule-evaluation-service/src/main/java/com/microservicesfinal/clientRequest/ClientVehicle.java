package com.microservicesfinal.clientRequest;

import com.microservicesfinal.dto.responses.VehicleResponse;
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
public class ClientVehicle {

    private final WebClient.Builder webClientBuilder;

    @CircuitBreaker(name = "vehicle", fallbackMethod = "requestSingleVehicleFallback")
    @Retry(name = "vehicle")
    @TimeLimiter(name = "vehicle")
    public CompletableFuture<VehicleResponse> requestSingleVehicle(Integer vehicleId) {

        return webClientBuilder.build().get()
                .uri("http://vehicle-service/api/vehicles/{vehicleId}", vehicleId)
                .retrieve()
                .bodyToMono(VehicleResponse.class)
                .toFuture();
    }

    @CircuitBreaker(name = "vehicle", fallbackMethod = "requestAllVehiclesFallback")
    @Retry(name = "vehicle")
    @TimeLimiter(name = "vehicle")
    public CompletableFuture<List<VehicleResponse>> requestAllVehicles(){
            return webClientBuilder.build().get()
                    .uri("http://vehicle-service/api/vehicles")
                    .retrieve()
                    .bodyToFlux(VehicleResponse.class)
                    .collectList()
                    .toFuture();
    }

    //fallback methods
    public CompletableFuture<VehicleResponse> requestSingleVehicleFallback(Integer vehicleId, Throwable t) {
        log.error("Vehicle service unavailable for id {}: \n{}", vehicleId, t.getMessage());
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<List<VehicleResponse>> requestAllVehiclesFallback(Throwable t) {
        log.error("Vehicle service unavailable: {}", t.getMessage());
        return CompletableFuture.completedFuture(null);
    }
}

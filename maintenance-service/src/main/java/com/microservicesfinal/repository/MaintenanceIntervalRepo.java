package com.microservicesfinal.repository;

import com.microservicesfinal.model.MaintenanceInterval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaintenanceIntervalRepo extends JpaRepository<MaintenanceInterval, Integer> {
    public Optional<MaintenanceInterval> findByMakeIgnoreCaseAndModelIgnoreCase(String make, String model);
}

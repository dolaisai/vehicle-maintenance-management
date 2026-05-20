package com.microservicesfinal.repository;

import com.microservicesfinal.model.Alert;
import com.microservicesfinal.model.enums.AlertStatus;
import com.microservicesfinal.model.enums.MaintenanceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepo extends JpaRepository<Alert, Integer> {
    public List<Alert> findAllByStatus(AlertStatus status);
    public boolean existsByVehicleIdAndMaintenanceTypeAndStatus(
            Integer vehicleId,
            MaintenanceType maintenanceType,
            AlertStatus status
    );
    public Optional<Alert> findByVehicleIdAndMaintenanceTypeAndStatus(
            Integer vehicleId,
            MaintenanceType maintenanceType,
            AlertStatus status
    );
}

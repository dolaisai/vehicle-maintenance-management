package com.microservicesfinal.repository;

import com.microservicesfinal.model.MaintenanceHistory;
import com.microservicesfinal.model.enums.MaintenanceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceHistoryRepo extends JpaRepository<MaintenanceHistory, Integer> {
    public List<MaintenanceHistory> findAllByVehicleId(Integer vehicleId);
    Optional<MaintenanceHistory>
    findTopByVehicleIdAndMaintenanceTypeOrderByCompletedDateDesc(
            Integer vehicleId,
            MaintenanceType maintenanceType
    );
}

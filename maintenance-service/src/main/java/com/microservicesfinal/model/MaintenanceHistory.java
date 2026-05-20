package com.microservicesfinal.model;

import com.microservicesfinal.model.enums.MaintenanceType;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_maintenance_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Integer version;

    private Integer vehicleId;

    @Enumerated(EnumType.STRING)
    private MaintenanceType maintenanceType;

    private Integer completedMileage;
    private LocalDateTime completedDate;
}

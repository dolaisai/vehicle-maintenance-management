package com.microservicesfinal.model;

import com.microservicesfinal.model.enums.AlertStatus;
import com.microservicesfinal.model.enums.MaintenanceType;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts_t")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Integer version;

    private Integer vehicleId;
    private Integer vehicleMileage;

    @Enumerated(EnumType.STRING)
    private MaintenanceType maintenanceType;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private LocalDateTime createdOn;
    private LocalDateTime closedOn;
}

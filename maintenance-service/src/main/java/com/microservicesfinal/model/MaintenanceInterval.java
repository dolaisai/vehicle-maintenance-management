package com.microservicesfinal.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "maintenance_interval_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceInterval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Integer version;

    @Column(name = "template_name")
    private String templateName;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    //mileage based intervals (miles)
    @Column(name = "oil_change_interval_miles")
    private Integer oilChangeIntervalMiles;
    @Column(name = "cabin_filter_change_interval_miles")
    private Integer cabinFilterChangeIntervalMiles;
    @Column(name = "spark_plug_change_interval_miles")
    private Integer sparkPlugChangeIntervalMiles;

    //time based intervals (months)
    @Column(name = "tire_rotation_interval_months")
    private Integer tireRotationIntervalMonths;

}

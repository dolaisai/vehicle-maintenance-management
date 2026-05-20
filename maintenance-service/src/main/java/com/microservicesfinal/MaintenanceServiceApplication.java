package com.microservicesfinal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class MaintenanceServiceApplication {
    public static void main(String[] args){
        SpringApplication.run(MaintenanceServiceApplication.class, args);
    }
}

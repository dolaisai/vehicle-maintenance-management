package com.microservicesfinal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.kafka.annotation.KafkaListener;

@SpringBootApplication
@EnableEurekaClient
public class AlertsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertsServiceApplication.class, args);
    }
}

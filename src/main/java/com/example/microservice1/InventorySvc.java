package com.example.microservice1; // Fixed package

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication(exclude = {
    org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration.class 
})
public class InventorySvc {

    public static void main(String[] args) {
        SpringApplication.run(InventorySvc.class, args);
    }
}
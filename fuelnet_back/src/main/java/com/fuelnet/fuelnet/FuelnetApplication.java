package com.fuelnet.fuelnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FuelnetApplication {

    public static void main(String[] args) {
        SpringApplication.run(FuelnetApplication.class, args);
    }
}

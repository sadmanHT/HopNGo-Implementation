package com.hopngo.tripplanning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.hopngo.tripplanning.entity")
@EnableJpaRepositories(basePackages = "com.hopngo.tripplanning.repository")
public class TripPlanningServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TripPlanningServiceApplication.class, args);
    }

}
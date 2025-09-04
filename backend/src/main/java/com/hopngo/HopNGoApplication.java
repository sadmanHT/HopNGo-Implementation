package com.hopngo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class HopNGoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HopNGoApplication.class, args);
    }

    @GetMapping("/api/health")
    public String health() {
        return "HopNGo Backend is running!";
    }
}
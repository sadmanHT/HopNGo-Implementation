package com.hopngo.notification.controller;

import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.boot.actuator.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class HealthController implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> healthStatus = new HashMap<>();
        boolean overallHealthy = true;
        
        healthStatus.put("timestamp", LocalDateTime.now());
        healthStatus.put("service", "notification-service");
        healthStatus.put("version", "1.0.0");
        
        // Check RabbitMQ connection
        Map<String, Object> rabbitMQStatus = checkRabbitMQHealth();
        healthStatus.put("rabbitmq", rabbitMQStatus);
        if (!"UP".equals(rabbitMQStatus.get("status"))) {
            overallHealthy = false;
        }
        
        // Check Database connection
        Map<String, Object> databaseStatus = checkDatabaseHealth();
        healthStatus.put("database", databaseStatus);
        if (!"UP".equals(databaseStatus.get("status"))) {
            overallHealthy = false;
        }
        
        // Check Firebase Cloud Messaging
        Map<String, Object> fcmStatus = checkFirebaseHealth();
        healthStatus.put("firebase", fcmStatus);
        if (!"UP".equals(fcmStatus.get("status"))) {
            overallHealthy = false;
        }
        
        // Check system resources
        Map<String, Object> systemStatus = checkSystemHealth();
        healthStatus.put("system", systemStatus);
        
        healthStatus.put("status", overallHealthy ? "UP" : "DOWN");
        
        return ResponseEntity.ok(healthStatus);
    }
    
    private Map<String, Object> checkRabbitMQHealth() {
        Map<String, Object> status = new HashMap<>();
        try {
            // Try to get connection factory and test connection
            rabbitTemplate.getConnectionFactory().createConnection().close();
            status.put("status", "UP");
            status.put("details", "RabbitMQ connection is healthy");
            logger.debug("RabbitMQ health check passed");
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("details", "RabbitMQ connection failed: " + e.getMessage());
            status.put("error", e.getClass().getSimpleName());
            logger.error("RabbitMQ health check failed", e);
        }
        return status;
    }
    
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> status = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                status.put("status", "UP");
                status.put("details", "Database connection is healthy");
                status.put("database", connection.getMetaData().getDatabaseProductName());
                logger.debug("Database health check passed");
            } else {
                status.put("status", "DOWN");
                status.put("details", "Database connection is not valid");
            }
        } catch (SQLException e) {
            status.put("status", "DOWN");
            status.put("details", "Database connection failed: " + e.getMessage());
            status.put("error", e.getClass().getSimpleName());
            logger.error("Database health check failed", e);
        }
        return status;
    }
    
    private Map<String, Object> checkFirebaseHealth() {
        Map<String, Object> status = new HashMap<>();
        try {
            if (firebaseMessaging != null) {
                // Firebase is configured and available
                status.put("status", "UP");
                status.put("details", "Firebase Cloud Messaging is configured and available");
                status.put("configured", true);
                logger.debug("Firebase health check passed");
            } else {
                status.put("status", "DOWN");
                status.put("details", "Firebase Cloud Messaging is not configured");
                status.put("configured", false);
                logger.warn("Firebase is not configured");
            }
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("details", "Firebase health check failed: " + e.getMessage());
            status.put("error", e.getClass().getSimpleName());
            status.put("configured", false);
            logger.error("Firebase health check failed", e);
        }
        return status;
    }
    
    private Map<String, Object> checkSystemHealth() {
        Map<String, Object> status = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        status.put("memory", Map.of(
            "used", usedMemory,
            "free", freeMemory,
            "total", totalMemory,
            "max", maxMemory,
            "usagePercent", Math.round(memoryUsagePercent * 100.0) / 100.0
        ));
        
        status.put("processors", runtime.availableProcessors());
        status.put("uptime", System.currentTimeMillis());
        
        return status;
    }
    
    @Override
    public Health health() {
        try {
            Map<String, Object> healthDetails = (Map<String, Object>) health().getBody();
            if ("UP".equals(healthDetails.get("status"))) {
                return Health.up()
                    .withDetails(healthDetails)
                    .build();
            } else {
                return Health.down()
                    .withDetails(healthDetails)
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
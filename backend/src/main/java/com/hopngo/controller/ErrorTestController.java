package com.hopngo.controller;

import com.hopngo.common.exception.BusinessException;
import com.hopngo.common.exception.ResourceNotFoundException;
import com.hopngo.common.exception.ServiceUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Controller for testing error monitoring and Sentry integration
 * Only available in non-production environments
 */
@RestController
@RequestMapping("/api/test/error")
@Tag(name = "Error Testing", description = "Endpoints for testing error monitoring (non-production only)")
@ConditionalOnProperty(name = "app.environment", havingValue = "production", matchIfMissing = false)
@Slf4j
public class ErrorTestController {

    private final Random random = new Random();

    @PostMapping
    @Operation(summary = "Trigger test error", description = "Triggers different types of errors for testing monitoring")
    public ResponseEntity<?> triggerError(
            @Parameter(description = "Type of error to trigger")
            @RequestBody Map<String, String> request) {
        
        String errorType = request.get("errorType");
        log.info("Triggering test error of type: {}", errorType);
        
        switch (errorType) {
            case "ResourceNotFoundException":
                throw ResourceNotFoundException.userNotFound("test-user-123");
            
            case "ServiceUnavailableException":
                throw ServiceUnavailableException.databaseUnavailable("Test database connection failure");
            
            case "DatabaseConnectionException":
                throw ServiceUnavailableException.databaseUnavailable("Connection pool exhausted");
            
            case "PaymentException":
                throw new BusinessException(
                    "PAYMENT_FAILED",
                    "Payment processing failed for testing",
                    Map.of(
                        "paymentId", "test-payment-123",
                        "amount", "99.99",
                        "currency", "USD"
                    )
                );
            
            case "AuthenticationException":
                throw new BusinessException(
                    "AUTH_FAILED",
                    "Authentication failed for testing",
                    Map.of("userId", "test-user-456")
                );
            
            case "ValidationException":
                throw new BusinessException(
                    "VALIDATION_FAILED",
                    "Validation failed for testing",
                    Map.of(
                        "field", "email",
                        "value", "invalid-email",
                        "constraint", "must be valid email"
                    )
                );
            
            case "TimeoutException":
                simulateTimeout();
                throw ServiceUnavailableException.externalApiUnavailable(
                    "external-service",
                    "Request timeout for testing"
                );
            
            case "MemoryException":
                simulateMemoryIssue();
                throw new RuntimeException("Simulated memory issue for testing");
            
            case "ConcurrencyException":
                throw new BusinessException(
                    "CONCURRENCY_ERROR",
                    "Concurrent modification detected",
                    Map.of(
                        "resource", "booking",
                        "resourceId", "booking-789",
                        "version", "2"
                    )
                );
            
            case "RateLimitException":
                throw new BusinessException(
                    "RATE_LIMIT_EXCEEDED",
                    "Rate limit exceeded for testing",
                    Map.of(
                        "limit", "100",
                        "window", "1h",
                        "current", "101"
                    )
                );
            
            default:
                throw new IllegalArgumentException("Unknown error type: " + errorType);
        }
    }

    @GetMapping("/random")
    @Operation(summary = "Trigger random error", description = "Triggers a random error for chaos testing")
    public ResponseEntity<?> triggerRandomError() {
        String[] errorTypes = {
            "ResourceNotFoundException",
            "ServiceUnavailableException",
            "PaymentException",
            "ValidationException",
            "TimeoutException"
        };
        
        String randomErrorType = errorTypes[random.nextInt(errorTypes.length)];
        log.info("Triggering random error: {}", randomErrorType);
        
        return triggerError(Map.of("errorType", randomErrorType));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Trigger bulk errors", description = "Triggers multiple errors for load testing")
    public ResponseEntity<?> triggerBulkErrors(
            @Parameter(description = "Number of errors to trigger")
            @RequestParam(defaultValue = "10") int count,
            @Parameter(description = "Type of error to trigger")
            @RequestParam(defaultValue = "ServiceUnavailableException") String errorType) {
        
        log.info("Triggering {} bulk errors of type: {}", count, errorType);
        
        for (int i = 0; i < count; i++) {
            try {
                triggerError(Map.of("errorType", errorType));
            } catch (Exception e) {
                // Expected - we're testing error handling
                log.debug("Triggered error {}/{}: {}", i + 1, count, e.getMessage());
            }
            
            // Small delay to avoid overwhelming the system
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "Triggered " + count + " errors of type " + errorType,
            "count", count,
            "errorType", errorType
        ));
    }

    @PostMapping("/cascade")
    @Operation(summary = "Trigger cascading errors", description = "Triggers errors that cause other errors")
    public ResponseEntity<?> triggerCascadingErrors() {
        log.info("Triggering cascading errors");
        
        try {
            // First error - database issue
            throw ServiceUnavailableException.databaseUnavailable("Primary database connection failed");
        } catch (Exception e) {
            log.error("Primary error occurred", e);
            
            try {
                // Second error - fallback also fails
                throw ServiceUnavailableException.databaseUnavailable("Fallback database also failed");
            } catch (Exception e2) {
                log.error("Fallback error occurred", e2);
                
                // Final error - complete system failure
                throw new RuntimeException("Complete system failure - all databases unavailable", e2);
            }
        }
    }

    @GetMapping("/slow")
    @Operation(summary = "Trigger slow request", description = "Simulates a slow request that may timeout")
    public ResponseEntity<?> triggerSlowRequest(
            @Parameter(description = "Delay in seconds")
            @RequestParam(defaultValue = "5") int delaySeconds) {
        
        log.info("Simulating slow request with {}s delay", delaySeconds);
        
        try {
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }
        
        // Randomly fail after the delay
        if (random.nextBoolean()) {
            throw ServiceUnavailableException.externalApiUnavailable(
                "slow-service",
                "Service timed out after " + delaySeconds + " seconds"
            );
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "Slow request completed",
            "delaySeconds", delaySeconds
        ));
    }

    @PostMapping("/feature/{feature}")
    @Operation(summary = "Trigger feature-specific error", description = "Triggers errors for specific features")
    public ResponseEntity<?> triggerFeatureError(
            @Parameter(description = "Feature name")
            @PathVariable String feature,
            @RequestBody Map<String, String> request) {
        
        String errorType = request.getOrDefault("errorType", "BusinessException");
        log.info("Triggering {} error for feature: {}", errorType, feature);
        
        switch (feature.toLowerCase()) {
            case "payment":
                throw new BusinessException(
                    "PAYMENT_GATEWAY_ERROR",
                    "Payment gateway is temporarily unavailable",
                    Map.of(
                        "feature", "payment",
                        "gateway", "stripe",
                        "errorCode", "GATEWAY_TIMEOUT"
                    )
                );
            
            case "booking":
                throw new BusinessException(
                    "BOOKING_CONFLICT",
                    "Booking slot is no longer available",
                    Map.of(
                        "feature", "booking",
                        "tripId", "trip-123",
                        "requestedSlots", "2",
                        "availableSlots", "0"
                    )
                );
            
            case "search":
                throw ServiceUnavailableException.externalApiUnavailable(
                    "elasticsearch",
                    "Search service is temporarily unavailable"
                );
            
            case "auth":
                throw new BusinessException(
                    "TOKEN_EXPIRED",
                    "Authentication token has expired",
                    Map.of(
                        "feature", "auth",
                        "tokenType", "JWT",
                        "expiredAt", "2024-01-15T10:30:00Z"
                    )
                );
            
            case "map":
                throw ServiceUnavailableException.externalApiUnavailable(
                    "google-maps",
                    "Map service quota exceeded"
                );
            
            default:
                throw new BusinessException(
                    "UNKNOWN_FEATURE",
                    "Unknown feature: " + feature,
                    Map.of("feature", feature)
                );
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Test endpoint health", description = "Returns health status of test endpoints")
    public ResponseEntity<?> getHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "message", "Error test endpoints are available",
            "environment", System.getProperty("app.environment", "development"),
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Simulate a timeout by sleeping
     */
    private void simulateTimeout() {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(30));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Simulate memory issues by creating large objects
     */
    private void simulateMemoryIssue() {
        try {
            // Create a large array to simulate memory pressure
            @SuppressWarnings("unused")
            byte[] largeArray = new byte[100 * 1024 * 1024]; // 100MB
            
            // Force garbage collection
            System.gc();
        } catch (OutOfMemoryError e) {
            log.error("Simulated memory issue occurred", e);
            throw new RuntimeException("Memory issue simulation", e);
        }
    }
}
package com.hopngo.gateway.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Composite controller that aggregates data from multiple microservices
 * for comprehensive trip information
 */
@RestController
@RequestMapping("/api/trip")
public class TripCompositeController {
    
    private static final Logger logger = LoggerFactory.getLogger(TripCompositeController.class);
    
    @Autowired
    private WebClient webClient;
    
    /**
     * Composite endpoint that fans out to multiple services to get complete trip details
     * 
     * @param tripId The trip identifier
     * @param includeAI Whether to include AI recommendations (optional)
     * @return Aggregated trip information from multiple services
     */
    @GetMapping("/full-details/{tripId}")
    public Mono<ResponseEntity<Map<String, Object>>> getFullTripDetails(
            @PathVariable String tripId,
            @RequestParam(defaultValue = "true") boolean includeAI,
            @RequestHeader("Authorization") String authHeader) {
        
        logger.info("Fetching full trip details for tripId: {}, includeAI: {}", tripId, includeAI);
        
        // Parallel calls to multiple services using RestTemplate wrapped in Mono
        Mono<Map<String, Object>> tripDetailsMono = getTripDetails(tripId, authHeader);
        Mono<Map<String, Object>> bookingDetailsMono = getBookingDetails(tripId, authHeader);
        Mono<Map<String, Object>> aiRecommendationsMono = includeAI ? 
            getAIRecommendations(tripId, authHeader) : 
            Mono.just(createEmptyAIResponse());
        
        return Mono.zip(tripDetailsMono, bookingDetailsMono, aiRecommendationsMono)
            .map(tuple -> {
                Map<String, Object> result = new HashMap<>();
                result.put("tripDetails", tuple.getT1());
                result.put("bookingDetails", tuple.getT2());
                result.put("aiRecommendations", tuple.getT3());
                result.put("aggregatedAt", java.time.Instant.now().toString());
                result.put("success", true);
                
                // Add summary information
                Map<String, Object> summary = new HashMap<>();
                summary.put("servicesQueried", 3);
                summary.put("tripDetailsAvailable", !isErrorResponse(tuple.getT1()));
                summary.put("bookingDetailsAvailable", !isErrorResponse(tuple.getT2()));
                summary.put("aiRecommendationsAvailable", !isErrorResponse(tuple.getT3()));
                result.put("summary", summary);
                
                return result;
            })
            .map(result -> ResponseEntity.ok(result))
            .onErrorResume(throwable -> {
                logger.error("Error fetching full trip details for tripId: {}", tripId, throwable);
                return Mono.just(ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .body(createErrorResponse(tripId, throwable)));
            })
            .timeout(Duration.ofSeconds(15));
    }
    
    @CircuitBreaker(name = "trip-planning-service", fallbackMethod = "getTripDetailsFallback")
    @Retry(name = "trip-planning-service")
    private Mono<Map<String, Object>> getTripDetails(String tripId, String authHeader) {
        return webClient.get()
                .uri("http://trip-planning-service:8087/trips/" + tripId)
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorResume(e -> {
                    logger.warn("Failed to fetch trip details for tripId: {}", tripId, e);
                    return Mono.just(createServiceErrorResponse("trip-planning-service", e));
                });
    }
    
    @CircuitBreaker(name = "booking-service", fallbackMethod = "getBookingDetailsFallback")
    @Retry(name = "booking-service")
    private Mono<Map<String, Object>> getBookingDetails(String tripId, String authHeader) {
        return webClient.get()
                .uri("http://booking-service:8083/bookings/trip/" + tripId)
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorResume(e -> {
                    logger.warn("Failed to fetch booking details for tripId: {}", tripId, e);
                    return Mono.just(createServiceErrorResponse("booking-service", e));
                });
    }
    
    @CircuitBreaker(name = "ai-service", fallbackMethod = "getAIRecommendationsFallback")
    @Retry(name = "ai-service")
    private Mono<Map<String, Object>> getAIRecommendations(String tripId, String authHeader) {
        return webClient.get()
                .uri("http://ai-service:8088/api/v1/ai/recommendations/" + tripId)
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorResume(e -> {
                    logger.warn("Failed to fetch AI recommendations for tripId: {}", tripId, e);
                    return Mono.just(createAIFallbackResponse(e));
                });
    }
    

    
    private boolean isErrorResponse(Map<String, Object> response) {
        return response.containsKey("error") || response.containsKey("fallback");
    }
    
    private Map<String, Object> createServiceErrorResponse(String serviceName, Throwable throwable) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Service Unavailable");
        errorResponse.put("service", serviceName);
        errorResponse.put("message", "Failed to fetch data from " + serviceName);
        errorResponse.put("timestamp", java.time.Instant.now().toString());
        errorResponse.put("fallback", true);
        return errorResponse;
    }
    
    private Map<String, Object> createAIFallbackResponse(Throwable throwable) {
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("error", "AI Service Unavailable");
        fallbackResponse.put("message", "AI recommendations are temporarily unavailable");
        fallbackResponse.put("fallback", true);
        fallbackResponse.put("timestamp", java.time.Instant.now().toString());
        fallbackResponse.put("alternativeActions", new String[]{
            "Browse popular destinations", 
            "Check recent bookings", 
            "Contact support for assistance"
        });
        return fallbackResponse;
    }
    
    private Map<String, Object> createEmptyAIResponse() {
        Map<String, Object> emptyResponse = new HashMap<>();
        emptyResponse.put("message", "AI recommendations disabled for this request");
        emptyResponse.put("enabled", false);
        return emptyResponse;
    }
    
    private Map<String, Object> createErrorResponse(String tripId, Throwable throwable) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Partial Failure");
        errorResponse.put("tripId", tripId);
        errorResponse.put("message", "Some services failed to respond");
        errorResponse.put("timestamp", java.time.Instant.now().toString());
        errorResponse.put("success", false);
        return errorResponse;
    }
    
    // Fallback methods for circuit breaker
    public Mono<Map<String, Object>> getTripDetailsFallback(String tripId, String authHeader, Exception ex) {
        logger.error("Trip details service fallback triggered for tripId: {}", tripId, ex);
        return Mono.just(createServiceErrorResponse("trip-planning-service", ex));
    }
    
    public Mono<Map<String, Object>> getBookingDetailsFallback(String tripId, String authHeader, Exception ex) {
        logger.error("Booking details service fallback triggered for tripId: {}", tripId, ex);
        return Mono.just(createServiceErrorResponse("booking-service", ex));
    }
    
    public Mono<Map<String, Object>> getAIRecommendationsFallback(String tripId, String authHeader, Exception ex) {
        logger.error("AI recommendations service fallback triggered for tripId: {}", tripId, ex);
        return Mono.just(createAIFallbackResponse(ex));
    }
}
package com.hopngo.analytics.controller;

import com.hopngo.analytics.dto.BatchEventRequest;
import com.hopngo.analytics.dto.BatchEventResponse;
import com.hopngo.analytics.dto.EventRequest;
import com.hopngo.analytics.dto.EventResponse;
import com.hopngo.analytics.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Event Tracking", description = "Analytics event tracking API")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(
        summary = "Track a single event",
        description = "Submit a single analytics event for tracking. Supports deduplication and privacy filtering."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Event processed successfully",
            content = @Content(schema = @Schema(implementation = EventResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid event data",
            content = @Content(schema = @Schema(implementation = EventResponse.class))
        ),
        @ApiResponse(
            responseCode = "429", 
            description = "Rate limit exceeded"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error"
        )
    })
    @PostMapping("/events")
    public ResponseEntity<EventResponse> trackEvent(
            @Parameter(description = "Event data to track", required = true)
            @Valid @RequestBody EventRequest eventRequest,
            HttpServletRequest request) {
        
        try {
            String clientIp = getClientIpAddress(request);
            logger.debug("Tracking event: {} from IP: {}", eventRequest.getEventId(), clientIp);
            
            EventResponse response = eventService.processEvent(eventRequest, clientIp);
            
            // Return appropriate HTTP status based on processing result
            HttpStatus status = switch (response.getStatus()) {
                case "SUCCESS" -> HttpStatus.OK;
                case "DUPLICATE" -> HttpStatus.OK; // Still OK, just inform client
                case "FILTERED" -> HttpStatus.OK; // Still OK, privacy filtering is normal
                case "FAILED" -> HttpStatus.BAD_REQUEST;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            
            return ResponseEntity.status(status).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid event request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(EventResponse.failed(eventRequest.getEventId(), e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing event: {}", eventRequest.getEventId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EventResponse.failed(eventRequest.getEventId(), "Internal server error"));
        }
    }

    @Operation(
        summary = "Track multiple events in batch",
        description = "Submit multiple analytics events in a single request for better performance. Maximum 100 events per batch."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Batch processed successfully",
            content = @Content(schema = @Schema(implementation = BatchEventResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid batch data or exceeds size limit"
        ),
        @ApiResponse(
            responseCode = "429", 
            description = "Rate limit exceeded"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error"
        )
    })
    @PostMapping("/events/batch")
    public ResponseEntity<BatchEventResponse> trackEventsBatch(
            @Parameter(description = "Batch of events to track", required = true)
            @Valid @RequestBody BatchEventRequest batchRequest,
            HttpServletRequest request) {
        
        try {
            String clientIp = getClientIpAddress(request);
            logger.info("Processing batch: {} with {} events from IP: {}", 
                    batchRequest.getBatchId(), batchRequest.getEvents().size(), clientIp);
            
            BatchEventResponse response = eventService.processBatchEvents(batchRequest, clientIp);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid batch request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error processing batch: {}", batchRequest.getBatchId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Check if event exists",
        description = "Check if an event with the given ID has already been processed (for deduplication)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Event existence check completed"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid event ID"
        )
    })
    @GetMapping("/events/{eventId}/exists")
    public ResponseEntity<Map<String, Boolean>> checkEventExists(
            @Parameter(description = "Event ID to check", required = true)
            @PathVariable String eventId) {
        
        try {
            boolean exists = eventService.eventExists(eventId);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            logger.error("Error checking event existence: {}", eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Health check endpoint",
        description = "Simple health check to verify the analytics service is running."
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "analytics-service",
            "timestamp", java.time.OffsetDateTime.now().toString()
        ));
    }

    @Operation(
        summary = "Get service info",
        description = "Get information about the analytics service configuration."
    )
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "service", "HopNGo Analytics Service",
            "version", "1.0.0",
            "features", Map.of(
                "batch_processing", true,
                "privacy_filtering", true,
                "deduplication", true,
                "real_time_tracking", true
            ),
            "limits", Map.of(
                "max_batch_size", 100,
                "max_event_size", "1MB"
            )
        ));
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Global exception handler for this controller
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("Unhandled exception in EventController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
                ));
    }
}
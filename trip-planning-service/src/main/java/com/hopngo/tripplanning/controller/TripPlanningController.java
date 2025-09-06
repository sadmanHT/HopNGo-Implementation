package com.hopngo.tripplanning.controller;

import com.hopngo.tripplanning.dto.ItineraryResponse;
import com.hopngo.tripplanning.dto.ItineraryUpdateRequest;
import com.hopngo.tripplanning.dto.CreateItineraryRequest;
import com.hopngo.tripplanning.security.UserContext;
import com.hopngo.tripplanning.service.ItineraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Trip Planning", description = "APIs for managing trip itineraries")
public class TripPlanningController {

    private static final Logger logger = LoggerFactory.getLogger(TripPlanningController.class);

    private final ItineraryService itineraryService;

    public TripPlanningController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @PostMapping("/plan")
    @Operation(summary = "Create a new trip plan", description = "Generate a new itinerary using AI service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Trip plan created successfully",
                    content = @Content(schema = @Schema(implementation = ItineraryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ItineraryResponse> createTripPlan(
            @Valid @RequestBody CreateItineraryRequest request) {
        
        String userId = UserContext.requireUserId();
        logger.info("Creating trip plan for user: {} with title: {}", userId, request.getTitle());
        
        try {
            ItineraryResponse response = itineraryService.createTripPlan(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid trip plan request from user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating trip plan for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get trip itinerary by ID", description = "Retrieve a specific itinerary by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Itinerary found",
                    content = @Content(schema = @Schema(implementation = ItineraryResponse.class))),
        @ApiResponse(responseCode = "404", description = "Itinerary not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ItineraryResponse> getItinerary(
            @Parameter(description = "Itinerary ID") @PathVariable UUID id) {
        
        String userId = UserContext.requireUserId();
        logger.debug("Fetching itinerary {} for user: {}", id, userId);
        
        Optional<ItineraryResponse> itinerary = itineraryService.getItinerary(id, userId);
        
        return itinerary.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update trip itinerary", description = "Partially update an existing itinerary")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Itinerary updated successfully",
                    content = @Content(schema = @Schema(implementation = ItineraryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Itinerary not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ItineraryResponse> updateItinerary(
            @Parameter(description = "Itinerary ID") @PathVariable UUID id,
            @Valid @RequestBody ItineraryUpdateRequest request) {
        
        String userId = UserContext.requireUserId();
        logger.info("Updating itinerary {} for user: {}", id, userId);
        
        try {
            Optional<ItineraryResponse> updatedItinerary = itineraryService.updateItinerary(id, request, userId);
            
            return updatedItinerary.map(ResponseEntity::ok)
                                  .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid update request for itinerary {} from user {}: {}", id, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating itinerary {} for user {}: {}", id, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete trip itinerary", description = "Delete an existing itinerary")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Itinerary deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Itinerary not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteItinerary(
            @Parameter(description = "Itinerary ID") @PathVariable UUID id) {
        
        String userId = UserContext.requireUserId();
        logger.info("Deleting itinerary {} for user: {}", id, userId);
        
        boolean deleted = itineraryService.deleteItinerary(id, userId);
        
        return deleted ? ResponseEntity.noContent().build() 
                      : ResponseEntity.notFound().build();
    }

    @GetMapping
    @Operation(summary = "Get user's trip itineraries", description = "Retrieve paginated list of user's itineraries")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Itineraries retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<ItineraryResponse>> getUserItineraries(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {
        
        String userId = UserContext.requireUserId();
        logger.debug("Fetching itineraries for user: {} - page: {}, size: {}", userId, page, size);
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<ItineraryResponse> itineraries = itineraryService.getUserItineraries(userId, pageable);
        
        return ResponseEntity.ok(itineraries);
    }

    @GetMapping("/search")
    @Operation(summary = "Search trip itineraries", description = "Search user's itineraries by various criteria")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<ItineraryResponse>> searchItineraries(
            @Parameter(description = "Search by title") @RequestParam(required = false) String title,
            @Parameter(description = "Minimum budget") @RequestParam(required = false) Integer minBudget,
            @Parameter(description = "Maximum budget") @RequestParam(required = false) Integer maxBudget,
            @Parameter(description = "Minimum days") @RequestParam(required = false) Integer minDays,
            @Parameter(description = "Maximum days") @RequestParam(required = false) Integer maxDays,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {
        
        String userId = UserContext.requireUserId();
         logger.debug("Searching itineraries for user: {} with criteria - title: {}, budget: {}-{}, days: {}-{}", 
                     userId, title, minBudget, maxBudget, minDays, maxDays);
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<ItineraryResponse> itineraries = itineraryService.searchItineraries(
            userId, title, minBudget, maxBudget, minDays, maxDays, pageable);
        
        return ResponseEntity.ok(itineraries);
    }

    @GetMapping("/count")
    @Operation(summary = "Get user's itinerary count", description = "Get the total number of itineraries for the user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Long> getUserItineraryCount() {
        String userId = UserContext.requireUserId();
         logger.debug("Getting itinerary count for user: {}", userId);
        
        long count = itineraryService.getUserItineraryCount(userId);
        
        return ResponseEntity.ok(count);
    }
}
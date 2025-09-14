package com.hopngo.tripplanning.controller;

import com.hopngo.tripplanning.dto.ItineraryResponse;
import com.hopngo.tripplanning.dto.ItineraryUpdateRequest;
import com.hopngo.tripplanning.dto.CreateItineraryRequest;
import com.hopngo.tripplanning.dto.TripPlanRequest;
import com.hopngo.tripplanning.dto.ItineraryListResponse;
import com.hopngo.tripplanning.security.UserContext;
import com.hopngo.tripplanning.service.ItineraryService;
import com.hopngo.tripplanning.service.TripPlanningService;
import com.hopngo.tripplanning.service.AIServiceClient;
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

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Trip Planning", description = "APIs for managing trip itineraries")
public class TripPlanningController {

    private static final Logger logger = LoggerFactory.getLogger(TripPlanningController.class);

    private final ItineraryService itineraryService;
    private final TripPlanningService tripPlanningService;
    private final AIServiceClient aiServiceClient;

    public TripPlanningController(ItineraryService itineraryService, 
                                 TripPlanningService tripPlanningService,
                                 AIServiceClient aiServiceClient) {
        this.itineraryService = itineraryService;
        this.tripPlanningService = tripPlanningService;
        this.aiServiceClient = aiServiceClient;
    }

    @PostMapping("/planTrip")
    @Operation(summary = "Plan a personalized trip", description = "Generate a new itinerary with AI and personalization")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Trip plan created successfully",
                    content = @Content(schema = @Schema(implementation = ItineraryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ItineraryResponse> planTrip(
            @Valid @RequestBody TripPlanRequest request) {
        
        String userId = UserContext.requireUserId();
        logger.info("Planning trip for user: {} with title: {}", userId, request.getTitle());
        
        try {
            ItineraryResponse response = tripPlanningService.planTrip(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid trip plan request from user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error planning trip for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

    @GetMapping("/itineraries")
    @Operation(summary = "Get user's trip itineraries with recommendations", description = "Retrieve paginated list of user's itineraries with personalized recommendations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Itineraries retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ItineraryListResponse> getItineraries(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {
        
        String userId = UserContext.requireUserId();
        logger.debug("Fetching itineraries with recommendations for user: {} - page: {}, size: {}", userId, page, size);
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        ItineraryListResponse response = tripPlanningService.getUserItineraries(userId, pageable, true);
        
        return ResponseEntity.ok(response);
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

    @PostMapping("/{id}/rate")
    @Operation(summary = "Rate an itinerary", description = "Record user interaction and rating for an itinerary")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rating recorded successfully"),
        @ApiResponse(responseCode = "404", description = "Itinerary not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "400", description = "Invalid rating data")
    })
    public ResponseEntity<Void> rateItinerary(
            @Parameter(description = "Itinerary ID") @PathVariable UUID id,
            @Parameter(description = "Rating (1-5)") @RequestParam Integer rating,
            @Parameter(description = "Interaction type") @RequestParam(defaultValue = "RATING") String interactionType) {
        
        String userId = UserContext.requireUserId();
        logger.info("Recording rating {} for itinerary {} by user: {}", rating, id, userId);
        
        try {
            tripPlanningService.recordItineraryInteraction(userId, id, rating, interactionType);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid rating request for itinerary {} from user {}: {}", id, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error recording rating for itinerary {} by user {}: {}", id, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/ai/destinations")
    @Operation(summary = "Get AI destination suggestions", description = "Get personalized destination suggestions from AI service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Suggestions retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "503", description = "AI service unavailable")
    })
    public ResponseEntity<String> getDestinationSuggestions(
            @Parameter(description = "User preferences") @RequestParam(required = false) String preferences,
            @Parameter(description = "Budget range") @RequestParam(required = false) String budget,
            @Parameter(description = "Travel style") @RequestParam(required = false) String travelStyle) {
        
        String userId = UserContext.requireUserId();
        logger.debug("Getting destination suggestions for user: {}", userId);
        
        try {
            // Parse budget parameter
            Integer budgetInt = null;
            if (budget != null && !budget.trim().isEmpty()) {
                try {
                    budgetInt = Integer.parseInt(budget.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    logger.warn("Invalid budget format: {}", budget);
                }
            }
            
            String suggestions = aiServiceClient.getNextDestinationSuggestion(
                null, // currentDestination
                new ArrayList<>(), // previousDestinations
                travelStyle,
                budgetInt,
                null // days
            );
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            logger.error("Error getting destination suggestions for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping("/ai/travel-tips")
    @Operation(summary = "Get AI travel tips", description = "Get personalized travel tips from AI service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Travel tips retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "503", description = "AI service unavailable")
    })
    public ResponseEntity<String> getTravelTips(
            @Parameter(description = "Destination") @RequestParam String destination,
            @Parameter(description = "Travel dates") @RequestParam(required = false) String travelDates,
            @Parameter(description = "Travel style") @RequestParam(required = false) String travelStyle) {
        
        String userId = UserContext.requireUserId();
        logger.debug("Getting travel tips for user: {} and destination: {}", userId, destination);
        
        try {
            // Parse travelStyle to integer if it's a numeric string, otherwise use default
            int travelStyleInt = 1; // default value
            try {
                if (travelStyle != null && !travelStyle.isEmpty()) {
                    travelStyleInt = Integer.parseInt(travelStyle);
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid travel style format, using default: {}", travelStyle);
            }
            String tips = aiServiceClient.getTravelTips(destination, travelDates, travelStyleInt);
            return ResponseEntity.ok(tips);
        } catch (Exception e) {
            logger.error("Error getting travel tips for user {} and destination {}: {}", userId, destination, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
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
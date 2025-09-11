package com.hopngo.tripplanning.controller;

import com.hopngo.tripplanning.dto.CreateSavedRouteRequest;
import com.hopngo.tripplanning.dto.SavedRouteResponse;
import com.hopngo.tripplanning.dto.UpdateSavedRouteRequest;
import com.hopngo.tripplanning.security.UserContext;
import com.hopngo.tripplanning.service.SavedRouteService;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/saved-routes")
@Tag(name = "Saved Routes", description = "APIs for managing saved routes")
public class SavedRouteController {

    private static final Logger logger = LoggerFactory.getLogger(SavedRouteController.class);

    private final SavedRouteService savedRouteService;

    public SavedRouteController(SavedRouteService savedRouteService) {
        this.savedRouteService = savedRouteService;
    }

    @PostMapping
    @Operation(summary = "Create a new saved route", description = "Save a route with waypoints and metadata")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Saved route created successfully",
                    content = @Content(schema = @Schema(implementation = SavedRouteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<SavedRouteResponse> createSavedRoute(
            @Valid @RequestBody CreateSavedRouteRequest request) {
        
        String userId = UserContext.requireUserId();
        logger.info("Creating saved route for user: {} with name: {}", userId, request.getName());
        
        try {
            SavedRouteResponse response = savedRouteService.createSavedRoute(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid saved route request from user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating saved route for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get saved route by ID", description = "Retrieve a specific saved route by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Saved route found",
                    content = @Content(schema = @Schema(implementation = SavedRouteResponse.class))),
        @ApiResponse(responseCode = "404", description = "Saved route not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<SavedRouteResponse> getSavedRoute(
            @Parameter(description = "Saved route ID") @PathVariable UUID id) {
        
        String userId = UserContext.requireUserId();
        logger.debug("Fetching saved route {} for user: {}", id, userId);
        
        Optional<SavedRouteResponse> savedRoute = savedRouteService.getSavedRoute(id, userId);
        
        return savedRoute.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update saved route", description = "Partially update an existing saved route")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Saved route updated successfully",
                    content = @Content(schema = @Schema(implementation = SavedRouteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Saved route not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<SavedRouteResponse> updateSavedRoute(
            @Parameter(description = "Saved route ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateSavedRouteRequest request) {
        
        String userId = UserContext.requireUserId();
        logger.info("Updating saved route {} for user: {}", id, userId);
        
        try {
            Optional<SavedRouteResponse> updatedRoute = savedRouteService.updateSavedRoute(id, request, userId);
            
            return updatedRoute.map(ResponseEntity::ok)
                              .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid update request for saved route {} from user {}: {}", id, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating saved route {} for user {}: {}", id, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete saved route", description = "Delete an existing saved route")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Saved route deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Saved route not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteSavedRoute(
            @Parameter(description = "Saved route ID") @PathVariable UUID id) {
        
        String userId = UserContext.requireUserId();
        logger.info("Deleting saved route {} for user: {}", id, userId);
        
        boolean deleted = savedRouteService.deleteSavedRoute(id, userId);
        
        return deleted ? ResponseEntity.noContent().build() 
                      : ResponseEntity.notFound().build();
    }

    @GetMapping
    @Operation(summary = "Get user's saved routes", description = "Retrieve paginated list of user's saved routes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Saved routes retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<SavedRouteResponse>> getUserSavedRoutes(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {
        
        String userId = UserContext.requireUserId();
        logger.debug("Fetching saved routes for user: {} - page: {}, size: {}", userId, page, size);
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<SavedRouteResponse> savedRoutes = savedRouteService.getSavedRoutes(userId, pageable);
        
        return ResponseEntity.ok(savedRoutes);
    }

    @GetMapping("/search")
    @Operation(summary = "Search saved routes", description = "Search user's saved routes by various criteria")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<SavedRouteResponse>> searchSavedRoutes(
            @Parameter(description = "Search by name") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by transportation mode") @RequestParam(required = false) String mode,
            @Parameter(description = "Minimum distance in km") @RequestParam(required = false) BigDecimal minDistance,
            @Parameter(description = "Maximum distance in km") @RequestParam(required = false) BigDecimal maxDistance,
            @Parameter(description = "Minimum duration in minutes") @RequestParam(required = false) Integer minDuration,
            @Parameter(description = "Maximum duration in minutes") @RequestParam(required = false) Integer maxDuration,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {
        
        String userId = UserContext.requireUserId();
        logger.debug("Searching saved routes for user: {} with criteria - name: {}, mode: {}, distance: {}-{}, duration: {}-{}", 
                    userId, name, mode, minDistance, maxDistance, minDuration, maxDuration);
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<SavedRouteResponse> savedRoutes;
        
        // Handle different search scenarios
        if (name != null && !name.trim().isEmpty()) {
            savedRoutes = savedRouteService.searchSavedRoutesByName(userId, name.trim(), pageable);
        } else if (mode != null && !mode.trim().isEmpty()) {
            savedRoutes = savedRouteService.getSavedRoutesByMode(userId, mode.trim(), pageable);
        } else if (minDistance != null || maxDistance != null) {
            BigDecimal min = minDistance != null ? minDistance : BigDecimal.ZERO;
            BigDecimal max = maxDistance != null ? maxDistance : new BigDecimal("999999");
            savedRoutes = savedRouteService.getSavedRoutesByDistanceRange(userId, min, max, pageable);
        } else if (minDuration != null || maxDuration != null) {
            Integer min = minDuration != null ? minDuration : 0;
            Integer max = maxDuration != null ? maxDuration : Integer.MAX_VALUE;
            savedRoutes = savedRouteService.getSavedRoutesByDurationRange(userId, min, max, pageable);
        } else {
            // No specific criteria, return all saved routes
            savedRoutes = savedRouteService.getSavedRoutes(userId, pageable);
        }
        
        return ResponseEntity.ok(savedRoutes);
    }

    @GetMapping("/count")
    @Operation(summary = "Get user's saved route count", description = "Get the total number of saved routes for the user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Long> getUserSavedRouteCount() {
        String userId = UserContext.requireUserId();
        logger.debug("Getting saved route count for user: {}", userId);
        
        long count = savedRouteService.countSavedRoutes(userId);
        
        return ResponseEntity.ok(count);
    }
}
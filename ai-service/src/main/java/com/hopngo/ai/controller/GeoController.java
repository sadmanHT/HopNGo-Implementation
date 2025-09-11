package com.hopngo.ai.controller;

import com.hopngo.ai.dto.MatrixRequest;
import com.hopngo.ai.dto.MatrixResponse;
import com.hopngo.ai.dto.RouteRequest;
import com.hopngo.ai.dto.RouteResponse;
import com.hopngo.ai.service.RoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/geo")
@Tag(name = "Geo", description = "Geographic and routing services")
@CrossOrigin(origins = "*")
public class GeoController {
    
    private static final Logger logger = LoggerFactory.getLogger(GeoController.class);
    
    private final RoutingService routingService;
    
    public GeoController(RoutingService routingService) {
        this.routingService = routingService;
    }
    
    @GetMapping("/route")
    @Operation(
        summary = "Calculate route between two points",
        description = "Returns route information including polyline, distance, and duration"
    )
    public Mono<ResponseEntity<RouteResponse>> getRoute(
            @Parameter(description = "Starting point as lat,lng", example = "23.8103,90.4125")
            @RequestParam String from,
            
            @Parameter(description = "Destination point as lat,lng", example = "23.7808,90.2792")
            @RequestParam String to,
            
            @Parameter(description = "Transportation mode", example = "driving")
            @RequestParam(defaultValue = "driving") String mode,
            
            @Parameter(description = "Include alternative routes")
            @RequestParam(defaultValue = "false") boolean alternatives,
            
            @Parameter(description = "Include turn-by-turn steps")
            @RequestParam(defaultValue = "false") boolean steps,
            
            @Parameter(description = "Geometry format")
            @RequestParam(defaultValue = "geojson") String geometries) {
        
        logger.info("Route request: {} -> {} (mode: {})", from, to, mode);
        
        RouteRequest request = new RouteRequest(from, to, mode, alternatives, steps, geometries);
        
        return routingService.calculateRoute(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> logger.debug("Route calculation completed successfully"))
                .doOnError(error -> logger.error("Route calculation failed: {}", error.getMessage()))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }
    
    @PostMapping("/matrix")
    @Operation(
        summary = "Calculate duration/distance matrix",
        description = "Returns travel times and distances between multiple points"
    )
    public Mono<ResponseEntity<MatrixResponse>> getMatrix(
            @Valid @RequestBody MatrixRequest request) {
        
        logger.info("Matrix request for {} coordinates (mode: {})", 
                   request.getCoordinates().size(), request.getMode());
        
        // Limit coordinates to prevent abuse
        if (request.getCoordinates().size() > 25) {
            logger.warn("Matrix request rejected: too many coordinates ({})", 
                       request.getCoordinates().size());
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        return routingService.calculateMatrix(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> logger.debug("Matrix calculation completed successfully"))
                .doOnError(error -> logger.error("Matrix calculation failed: {}", error.getMessage()))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "Check routing service health",
        description = "Returns the health status of the routing backend"
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Routing service is healthy");
    }
}
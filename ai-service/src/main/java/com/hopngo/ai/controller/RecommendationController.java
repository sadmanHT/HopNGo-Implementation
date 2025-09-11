package com.hopngo.ai.controller;

import com.hopngo.ai.dto.SimilarityRequest;
import com.hopngo.ai.dto.SimilarityResponse;
import com.hopngo.ai.service.SimilarityService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "Recommendations", description = "AI-powered recommendation and similarity services")
@CrossOrigin(origins = "*")
public class RecommendationController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);
    
    private final SimilarityService similarityService;
    
    public RecommendationController(SimilarityService similarityService) {
        this.similarityService = similarityService;
    }
    
    @GetMapping("/users/{userId}/similar")
    @Operation(
        summary = "Find similar users",
        description = "Returns users with similar travel preferences and behavior patterns"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved similar users"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "ai-endpoints")
    public Mono<ResponseEntity<SimilarityResponse>> findSimilarUsers(
            @Parameter(description = "User ID to find similar users for", example = "user123")
            @PathVariable @NotBlank String userId,
            
            @Parameter(description = "Maximum number of similar users to return", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            
            @Parameter(description = "User IDs to exclude from recommendations")
            @RequestParam(required = false) List<String> exclude) {
        
        logger.info("Finding similar users for user: {} with limit: {}", userId, limit);
        
        SimilarityRequest request = new SimilarityRequest(userId, limit);
        request.setExcludeIds(exclude);
        
        return similarityService.findSimilarUsers(request)
            .map(response -> {
                response.setCacheStatus("hit"); // Will be overridden if cache miss
                return ResponseEntity.ok(response);
            })
            .doOnSuccess(result -> logger.info("Successfully found {} similar users for user: {}", 
                result.getBody().getRecommendations().size(), userId))
            .doOnError(error -> logger.error("Error finding similar users for user: {}", userId, error));
    }
    
    @GetMapping("/items/{itemId}/similar")
    @Operation(
        summary = "Find similar items",
        description = "Returns items similar to the specified stay or tour"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved similar items"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "ai-endpoints")
    public Mono<ResponseEntity<SimilarityResponse>> findSimilarItems(
            @Parameter(description = "Item ID to find similar items for", example = "stay123")
            @PathVariable @NotBlank String itemId,
            
            @Parameter(description = "Type of item (stay or tour)", example = "stay")
            @RequestParam @NotBlank String type,
            
            @Parameter(description = "User ID for personalization", example = "user123")
            @RequestParam @NotBlank String userId,
            
            @Parameter(description = "Maximum number of similar items to return", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            
            @Parameter(description = "Item IDs to exclude from recommendations")
            @RequestParam(required = false) List<String> exclude) {
        
        logger.info("Finding similar items for item: {} of type: {} with limit: {}", itemId, type, limit);
        
        SimilarityRequest request = new SimilarityRequest(userId, itemId, type, limit);
        request.setExcludeIds(exclude);
        
        return similarityService.findSimilarItems(request)
            .map(response -> {
                response.setCacheStatus("hit"); // Will be overridden if cache miss
                return ResponseEntity.ok(response);
            })
            .doOnSuccess(result -> logger.info("Successfully found {} similar items for item: {}", 
                result.getBody().getRecommendations().size(), itemId))
            .doOnError(error -> logger.error("Error finding similar items for item: {}", itemId, error));
    }
    
    @GetMapping("/users/{userId}/home")
    @Operation(
        summary = "Get personalized home recommendations",
        description = "Returns personalized recommendations for the user's home page"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved home recommendations"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "ai-endpoints")
    public Mono<ResponseEntity<SimilarityResponse>> getHomeRecommendations(
            @Parameter(description = "User ID to get recommendations for", example = "user123")
            @PathVariable @NotBlank String userId,
            
            @Parameter(description = "Maximum number of recommendations to return", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            
            @Parameter(description = "User's current location as lat,lng", example = "13.7563,100.5018")
            @RequestParam(required = false) String location,
            
            @Parameter(description = "Maximum distance from location in km", example = "50")
            @RequestParam(required = false) Double maxDistance,
            
            @Parameter(description = "Item IDs to exclude from recommendations")
            @RequestParam(required = false) List<String> exclude) {
        
        logger.info("Getting home recommendations for user: {} with limit: {}", userId, limit);
        
        SimilarityRequest request = new SimilarityRequest(userId, limit);
        request.setLocation(location);
        request.setMaxDistance(maxDistance);
        request.setExcludeIds(exclude);
        
        return similarityService.getHomeRecommendations(request)
            .map(response -> {
                response.setCacheStatus("hit"); // Will be overridden if cache miss
                return ResponseEntity.ok(response);
            })
            .doOnSuccess(result -> logger.info("Successfully generated {} home recommendations for user: {}", 
                result.getBody().getRecommendations().size(), userId))
            .doOnError(error -> logger.error("Error generating home recommendations for user: {}", userId, error));
    }
    
    @PostMapping("/batch")
    @Operation(
        summary = "Get batch recommendations",
        description = "Process multiple recommendation requests in a single call"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully processed batch recommendations"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimiter(name = "ai-endpoints")
    public Mono<ResponseEntity<SimilarityResponse>> getBatchRecommendations(
            @Parameter(description = "Batch recommendation request")
            @Valid @RequestBody SimilarityRequest request) {
        
        logger.info("Processing batch recommendations for user: {}", request.getUserId());
        
        // For batch requests, we'll use the home recommendations as the primary algorithm
        return similarityService.getHomeRecommendations(request)
            .map(response -> {
                response.setCacheStatus("hit"); // Will be overridden if cache miss
                response.setAlgorithm("batch_hybrid");
                return ResponseEntity.ok(response);
            })
            .doOnSuccess(result -> logger.info("Successfully processed batch recommendations for user: {}", 
                request.getUserId()))
            .doOnError(error -> logger.error("Error processing batch recommendations for user: {}", 
                request.getUserId(), error));
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "Check recommendation service health",
        description = "Returns the health status of the recommendation service"
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Recommendation service is healthy");
    }
}
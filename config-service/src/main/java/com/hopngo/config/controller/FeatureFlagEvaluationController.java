package com.hopngo.config.controller;

import com.hopngo.config.dto.FeatureFlagEvaluationRequest;
import com.hopngo.config.dto.FeatureFlagEvaluationResponse;
import com.hopngo.config.service.FeatureFlagEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/config/flags/evaluate")
@Tag(name = "Feature Flag Evaluation", description = "Feature flag evaluation API for dark launches")
public class FeatureFlagEvaluationController {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagEvaluationController.class);
    
    private final FeatureFlagEvaluationService evaluationService;
    
    @Autowired
    public FeatureFlagEvaluationController(FeatureFlagEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }
    
    @PostMapping
    @Operation(summary = "Evaluate multiple feature flags", 
               description = "Evaluate multiple feature flags for a user with rollout percentage and targeting")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully evaluated feature flags"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<FeatureFlagEvaluationResponse> evaluateFlags(
            @Parameter(description = "Feature flag evaluation request") 
            @Valid @RequestBody FeatureFlagEvaluationRequest request) {
        logger.debug("POST /api/v1/config/flags/evaluate - request: {}", request);
        
        FeatureFlagEvaluationResponse response = evaluationService.evaluateFlags(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{flagKey}")
    @Operation(summary = "Evaluate single feature flag", 
               description = "Evaluate a single feature flag for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully evaluated feature flag"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Map<String, Boolean>> evaluateFlag(
            @Parameter(description = "Feature flag key") @PathVariable String flagKey,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "User segment (optional)") @RequestParam(required = false) String userSegment,
            @Parameter(description = "App version (optional)") @RequestParam(required = false) String appVersion) {
        logger.debug("GET /api/v1/config/flags/evaluate/{} - userId: {}", flagKey, userId);
        
        Map<String, Object> context = Map.of();
        if (userSegment != null || appVersion != null) {
            context = Map.of(
                "userSegment", userSegment != null ? userSegment : "",
                "appVersion", appVersion != null ? appVersion : ""
            );
        }
        
        boolean result = evaluationService.evaluateFlag(flagKey, userId, context);
        return ResponseEntity.ok(Map.of(flagKey, result));
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all enabled flags for user", 
               description = "Get all feature flags that are enabled for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user flags")
    })
    public ResponseEntity<Map<String, Boolean>> getUserFlags(
            @Parameter(description = "User ID") @PathVariable String userId) {
        logger.debug("GET /api/v1/config/flags/evaluate/user/{}", userId);
        
        Map<String, Boolean> userFlags = evaluationService.getEnabledFlagsForUser(userId);
        return ResponseEntity.ok(userFlags);
    }
    
    @PostMapping("/dark-launch")
    @Operation(summary = "Dark launch evaluation", 
               description = "Evaluate feature flags specifically for dark launch scenarios")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully evaluated dark launch flags")
    })
    public ResponseEntity<FeatureFlagEvaluationResponse> evaluateDarkLaunch(
            @Parameter(description = "Dark launch evaluation request") 
            @Valid @RequestBody FeatureFlagEvaluationRequest request) {
        logger.info("POST /api/v1/config/flags/evaluate/dark-launch - userId: {}, flags: {}", 
                   request.getUserId(), request.getFlagKeys());
        
        // Add dark launch context
        if (request.getContext() == null) {
            request.setContext(Map.of("darkLaunch", true));
        } else {
            request.getContext().put("darkLaunch", true);
        }
        
        FeatureFlagEvaluationResponse response = evaluationService.evaluateFlags(request);
        
        // Log dark launch evaluation for monitoring
        logger.info("Dark launch evaluation completed for user {}: {}", 
                   request.getUserId(), response.getFlags());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Health check endpoint for feature flag evaluation service")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "feature-flag-evaluation",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
}
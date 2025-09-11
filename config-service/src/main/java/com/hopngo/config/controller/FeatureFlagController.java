package com.hopngo.config.controller;

import com.hopngo.config.dto.FeatureFlagDto;
import com.hopngo.config.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/config/flags")
@Tag(name = "Feature Flags", description = "Feature flag management API")
public class FeatureFlagController {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagController.class);
    
    private final FeatureFlagService featureFlagService;
    
    @Autowired
    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }
    
    @GetMapping
    @Operation(summary = "Get all feature flags", description = "Retrieve all feature flags with optional pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved feature flags")
    })
    public ResponseEntity<Page<FeatureFlagDto>> getAllFlags(
            @Parameter(description = "Pagination parameters") Pageable pageable) {
        logger.debug("GET /api/v1/config/flags - pageable: {}", pageable);
        Page<FeatureFlagDto> flags = featureFlagService.getAllFlags(pageable);
        return ResponseEntity.ok(flags);
    }
    
    @GetMapping("/enabled")
    @Operation(summary = "Get enabled feature flags", description = "Retrieve only enabled feature flags")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved enabled feature flags")
    })
    public ResponseEntity<List<FeatureFlagDto>> getEnabledFlags() {
        logger.debug("GET /api/v1/config/flags/enabled");
        List<FeatureFlagDto> flags = featureFlagService.getEnabledFlags();
        return ResponseEntity.ok(flags);
    }
    
    @GetMapping("/{key}")
    @Operation(summary = "Get feature flag by key", description = "Retrieve a specific feature flag by its key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved feature flag"),
        @ApiResponse(responseCode = "404", description = "Feature flag not found")
    })
    public ResponseEntity<FeatureFlagDto> getFlagByKey(
            @Parameter(description = "Feature flag key") @PathVariable String key) {
        logger.debug("GET /api/v1/config/flags/{}", key);
        return featureFlagService.getFlagByKey(key)
                .map(flag -> ResponseEntity.ok(flag))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/batch")
    @Operation(summary = "Get multiple feature flags", description = "Retrieve multiple feature flags by their keys")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved feature flags")
    })
    public ResponseEntity<List<FeatureFlagDto>> getFlagsByKeys(
            @Parameter(description = "List of feature flag keys") @RequestBody List<String> keys) {
        logger.debug("POST /api/v1/config/flags/batch - keys: {}", keys);
        List<FeatureFlagDto> flags = featureFlagService.getFlagsByKeys(keys);
        return ResponseEntity.ok(flags);
    }
    
    @PostMapping
    @Operation(summary = "Create feature flag", description = "Create a new feature flag")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Feature flag created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or feature flag already exists")
    })
    public ResponseEntity<FeatureFlagDto> createFlag(
            @Parameter(description = "Feature flag data") @Valid @RequestBody FeatureFlagDto flagDto) {
        logger.info("POST /api/v1/config/flags - creating flag: {}", flagDto.getKey());
        try {
            FeatureFlagDto createdFlag = featureFlagService.createFlag(flagDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdFlag);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create feature flag: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update feature flag", description = "Update an existing feature flag")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feature flag updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Feature flag not found")
    })
    public ResponseEntity<FeatureFlagDto> updateFlag(
            @Parameter(description = "Feature flag ID") @PathVariable Long id,
            @Parameter(description = "Updated feature flag data") @Valid @RequestBody FeatureFlagDto flagDto) {
        logger.info("PUT /api/v1/config/flags/{} - updating flag", id);
        try {
            FeatureFlagDto updatedFlag = featureFlagService.updateFlag(id, flagDto);
            return ResponseEntity.ok(updatedFlag);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update feature flag {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete feature flag", description = "Delete a feature flag")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Feature flag deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Feature flag not found")
    })
    public ResponseEntity<Void> deleteFlag(
            @Parameter(description = "Feature flag ID") @PathVariable Long id) {
        logger.info("DELETE /api/v1/config/flags/{}", id);
        try {
            featureFlagService.deleteFlag(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to delete feature flag {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{key}/toggle")
    @Operation(summary = "Toggle feature flag", description = "Toggle the enabled state of a feature flag")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feature flag toggled successfully"),
        @ApiResponse(responseCode = "404", description = "Feature flag not found")
    })
    public ResponseEntity<FeatureFlagDto> toggleFlag(
            @Parameter(description = "Feature flag key") @PathVariable String key) {
        logger.info("POST /api/v1/config/flags/{}/toggle", key);
        try {
            FeatureFlagDto toggledFlag = featureFlagService.toggleFlag(key);
            return ResponseEntity.ok(toggledFlag);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to toggle feature flag {}: {}", key, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/check")
    @Operation(summary = "Check feature flags", description = "Check multiple feature flags and return their status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully checked feature flags")
    })
    public ResponseEntity<Map<String, Boolean>> checkFlags(
            @Parameter(description = "Comma-separated list of feature flag keys") 
            @RequestParam("keys") List<String> keys) {
        logger.debug("GET /api/v1/config/flags/check - keys: {}", keys);
        
        List<FeatureFlagDto> flags = featureFlagService.getFlagsByKeys(keys);
        Map<String, Boolean> flagStatus = flags.stream()
                .collect(java.util.stream.Collectors.toMap(
                    FeatureFlagDto::getKey,
                    FeatureFlagDto::isEnabled
                ));
        
        // Add false for missing keys
        keys.forEach(key -> flagStatus.putIfAbsent(key, false));
        
        return ResponseEntity.ok(flagStatus);
    }
}
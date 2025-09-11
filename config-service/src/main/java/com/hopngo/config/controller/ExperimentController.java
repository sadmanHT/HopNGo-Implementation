package com.hopngo.config.controller;

import com.hopngo.config.dto.AssignmentDto;
import com.hopngo.config.dto.ExperimentDto;
import com.hopngo.config.service.ExperimentService;
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

@RestController
@RequestMapping("/api/v1/config/experiments")
@Tag(name = "Experiments", description = "A/B testing experiment management API")
public class ExperimentController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExperimentController.class);
    
    private final ExperimentService experimentService;
    
    @Autowired
    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }
    
    @GetMapping
    @Operation(summary = "Get all experiments", description = "Retrieve all experiments with optional pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved experiments")
    })
    public ResponseEntity<Page<ExperimentDto>> getAllExperiments(
            @Parameter(description = "Pagination parameters") Pageable pageable) {
        logger.debug("GET /api/v1/config/experiments - pageable: {}", pageable);
        Page<ExperimentDto> experiments = experimentService.getAllExperiments(pageable);
        return ResponseEntity.ok(experiments);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active experiments", description = "Retrieve only running experiments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved active experiments")
    })
    public ResponseEntity<List<ExperimentDto>> getActiveExperiments() {
        logger.debug("GET /api/v1/config/experiments/active");
        List<ExperimentDto> experiments = experimentService.getActiveExperiments();
        return ResponseEntity.ok(experiments);
    }
    
    @GetMapping("/{key}")
    @Operation(summary = "Get experiment by key", description = "Retrieve a specific experiment by its key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved experiment"),
        @ApiResponse(responseCode = "404", description = "Experiment not found")
    })
    public ResponseEntity<ExperimentDto> getExperimentByKey(
            @Parameter(description = "Experiment key") @PathVariable String key) {
        logger.debug("GET /api/v1/config/experiments/{}", key);
        return experimentService.getExperimentByKey(key)
                .map(experiment -> ResponseEntity.ok(experiment))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(summary = "Create experiment", description = "Create a new A/B testing experiment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Experiment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or experiment already exists")
    })
    public ResponseEntity<ExperimentDto> createExperiment(
            @Parameter(description = "Experiment data") @Valid @RequestBody ExperimentDto experimentDto) {
        logger.info("POST /api/v1/config/experiments - creating experiment: {}", experimentDto.getKey());
        try {
            ExperimentDto createdExperiment = experimentService.createExperiment(experimentDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdExperiment);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create experiment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update experiment", description = "Update an existing experiment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Experiment updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Experiment not found")
    })
    public ResponseEntity<ExperimentDto> updateExperiment(
            @Parameter(description = "Experiment ID") @PathVariable Long id,
            @Parameter(description = "Updated experiment data") @Valid @RequestBody ExperimentDto experimentDto) {
        logger.info("PUT /api/v1/config/experiments/{} - updating experiment", id);
        try {
            ExperimentDto updatedExperiment = experimentService.updateExperiment(id, experimentDto);
            return ResponseEntity.ok(updatedExperiment);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update experiment {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete experiment", description = "Delete an experiment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Experiment deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Experiment not found")
    })
    public ResponseEntity<Void> deleteExperiment(
            @Parameter(description = "Experiment ID") @PathVariable Long id) {
        logger.info("DELETE /api/v1/config/experiments/{}", id);
        try {
            experimentService.deleteExperiment(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to delete experiment {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{key}/assign")
    @Operation(summary = "Assign user to experiment", description = "Assign a user to an experiment variant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User assigned successfully"),
        @ApiResponse(responseCode = "204", description = "User excluded from experiment"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Experiment not found")
    })
    public ResponseEntity<AssignmentDto> assignUserToExperiment(
            @Parameter(description = "Experiment key") @PathVariable String key,
            @Parameter(description = "User ID") @RequestParam("userId") String userId) {
        logger.debug("POST /api/v1/config/experiments/{}/assign - userId: {}", key, userId);
        try {
            AssignmentDto assignment = experimentService.assignUserToExperiment(key, userId);
            if (assignment == null) {
                // User excluded from experiment due to traffic percentage
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(assignment);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to assign user {} to experiment {}: {}", userId, key, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Cannot assign user {} to experiment {}: {}", userId, key, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/assignments")
    @Operation(summary = "Get user assignments", description = "Get all experiment assignments for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user assignments")
    })
    public ResponseEntity<List<AssignmentDto>> getUserAssignments(
            @Parameter(description = "User ID") @RequestParam("userId") String userId) {
        logger.debug("GET /api/v1/config/experiments/assignments - userId: {}", userId);
        List<AssignmentDto> assignments = experimentService.getUserAssignments(userId);
        return ResponseEntity.ok(assignments);
    }
}
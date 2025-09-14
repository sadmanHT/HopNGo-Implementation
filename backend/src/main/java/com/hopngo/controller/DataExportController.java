package com.hopngo.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hopngo.entity.DataExportJob;
import com.hopngo.entity.DataExportJob.ExportType;
import com.hopngo.service.DataExportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/auth/me")
@Tag(name = "Data Export", description = "User data export and privacy compliance APIs")
public class DataExportController {

    private static final Logger logger = LoggerFactory.getLogger(DataExportController.class);

    @Autowired
    private DataExportService dataExportService;

    /**
     * Request data export
     */
    @PostMapping("/export")
    @Operation(summary = "Request user data export", description = "Create a new data export job for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Export job created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user already has active export"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "429", description = "Too many concurrent export jobs")
    })
    public ResponseEntity<Map<String, Object>> requestDataExport(
            @Valid @RequestBody DataExportRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            DataExportJob job = dataExportService.requestDataExport(userId, request.getExportType());
            
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", job.getId());
            response.put("status", job.getStatus());
            response.put("exportType", job.getExportType());
            response.put("requestedAt", job.getCreatedAt());
            response.put("message", "Data export job created successfully. You will be notified when it's ready.");
            
            logger.info("Data export requested by user {} with job ID {}", userId, job.getId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid request", e.getMessage()));
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Maximum concurrent")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(createErrorResponse("Too many requests", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid state", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create export job for user {}", getUserIdFromAuth(authentication), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to create export job"));
        }
    }

    /**
     * Get export job status
     */
    @GetMapping("/export/{jobId}")
    @Operation(summary = "Get export job status", description = "Get the status and details of a specific export job")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export job details retrieved"),
            @ApiResponse(responseCode = "404", description = "Export job not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getExportJobStatus(
            @Parameter(description = "Export job ID") @PathVariable Long jobId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            Optional<DataExportJob> jobOpt = dataExportService.getExportJob(jobId, userId);
            
            if (jobOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            DataExportJob job = jobOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", job.getId());
            response.put("status", job.getStatus());
            response.put("exportType", job.getExportType());
            response.put("requestedAt", job.getCreatedAt());
            response.put("startedAt", job.getStartedAt());
            response.put("completedAt", job.getCompletedAt());
            response.put("errorMessage", job.getErrorMessage());
            response.put("downloadAvailable", job.getStatus() == DataExportJob.ExportStatus.COMPLETED && job.getFilePath() != null);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get export job status for job {} and user {}", jobId, getUserIdFromAuth(authentication), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to get export job status"));
        }
    }

    /**
     * Get export history
     */
    @GetMapping("/export")
    @Operation(summary = "Get export history", description = "Get the user's data export history")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export history retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getExportHistory(
            @Parameter(description = "Maximum number of records to return") @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            List<DataExportJob> jobs = dataExportService.getUserExportHistory(userId, Math.min(limit, 50));
            
            Map<String, Object> response = new HashMap<>();
            response.put("jobs", jobs.stream().map(job -> {
                Map<String, Object> jobData = new HashMap<>();
                jobData.put("jobId", job.getId());
                jobData.put("status", job.getStatus());
                jobData.put("exportType", job.getExportType());
                jobData.put("requestedAt", job.getCreatedAt());
                jobData.put("completedAt", job.getCompletedAt());
                jobData.put("downloadAvailable", job.getStatus() == DataExportJob.ExportStatus.COMPLETED && job.getFilePath() != null);
                return jobData;
            }).toList());
            response.put("total", jobs.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get export history for user {}", getUserIdFromAuth(authentication), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to get export history"));
        }
    }

    /**
     * Download export file
     */
    @GetMapping("/export/{jobId}/download")
    @Operation(summary = "Download export file", description = "Download the exported data file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "Export job or file not found"),
            @ApiResponse(responseCode = "400", description = "Export not completed or file not available"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Resource> downloadExportFile(
            @Parameter(description = "Export job ID") @PathVariable Long jobId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            Resource file = dataExportService.downloadExportFile(jobId, userId);
            
            String filename = "user_data_export_" + jobId + ".json";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(file);
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            logger.error("Failed to download export file for job {} and user {}", jobId, getUserIdFromAuth(authentication), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error downloading export file for job {} and user {}", jobId, getUserIdFromAuth(authentication), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cancel export job
     */
    @DeleteMapping("/export/{jobId}")
    @Operation(summary = "Cancel export job", description = "Cancel a pending export job")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export job cancelled"),
            @ApiResponse(responseCode = "404", description = "Export job not found"),
            @ApiResponse(responseCode = "400", description = "Export job cannot be cancelled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> cancelExportJob(
            @Parameter(description = "Export job ID") @PathVariable Long jobId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            boolean cancelled = dataExportService.cancelExportJob(jobId, userId);
            
            if (cancelled) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Export job cancelled successfully");
                response.put("jobId", jobId);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Cannot cancel", "Export job cannot be cancelled or not found"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to cancel export job {} for user {}", jobId, getUserIdFromAuth(authentication), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to cancel export job"));
        }
    }

    /**
     * Extract user ID from authentication
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        // TODO: Implement based on your authentication mechanism
        // This is a placeholder - replace with actual user ID extraction
        return 1L; // Placeholder
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Data export request DTO
     */
    public static class DataExportRequest {
        @NotNull(message = "Export type is required")
        private ExportType exportType = ExportType.FULL;

        public ExportType getExportType() {
            return exportType;
        }

        public void setExportType(ExportType exportType) {
            this.exportType = exportType;
        }
    }
}
package com.hopngo.controller;

import com.hopngo.service.DataRetentionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/data-retention")
@Tag(name = "Data Retention Admin", description = "Administrative APIs for data retention and cleanup management")
@PreAuthorize("hasRole('ADMIN')")
public class DataRetentionController {

    private static final Logger logger = LoggerFactory.getLogger(DataRetentionController.class);

    @Autowired
    private DataRetentionService dataRetentionService;

    /**
     * Get retention policies
     */
    @GetMapping("/policies")
    @Operation(summary = "Get data retention policies", description = "Retrieve current data retention policies and settings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retention policies retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public ResponseEntity<Map<String, Object>> getRetentionPolicies() {
        try {
            Map<String, Object> policies = dataRetentionService.getRetentionPolicies();
            return ResponseEntity.ok(policies);
        } catch (Exception e) {
            logger.error("Failed to get retention policies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to retrieve retention policies"));
        }
    }

    /**
     * Get cleanup statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get cleanup statistics", description = "Retrieve statistics about data cleanup operations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cleanup statistics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public ResponseEntity<Map<String, Object>> getCleanupStatistics() {
        try {
            Map<String, Object> stats = dataRetentionService.getCleanupStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to get cleanup statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to retrieve cleanup statistics"));
        }
    }

    /**
     * Trigger manual cleanup
     */
    @PostMapping("/cleanup/{type}")
    @Operation(summary = "Trigger manual cleanup", description = "Manually trigger a specific type of data cleanup")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cleanup triggered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid cleanup type or retention disabled"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public ResponseEntity<Map<String, Object>> triggerManualCleanup(
            @Parameter(description = "Cleanup type (daily, weekly, monthly, exports, deletions)") 
            @PathVariable String type) {
        
        try {
            dataRetentionService.triggerManualCleanup(type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Manual cleanup triggered successfully");
            response.put("cleanupType", type);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("Manual cleanup triggered: {}", type);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid cleanup type", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Cleanup disabled", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to trigger manual cleanup: {}", type, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to trigger cleanup"));
        }
    }

    /**
     * Get cleanup schedule information
     */
    @GetMapping("/schedule")
    @Operation(summary = "Get cleanup schedule", description = "Get information about scheduled cleanup jobs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Schedule information retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public ResponseEntity<Map<String, Object>> getCleanupSchedule() {
        try {
            Map<String, Object> schedule = new HashMap<>();
            
            // Daily cleanup schedule
            Map<String, Object> dailySchedule = new HashMap<>();
            dailySchedule.put("cron", "0 0 2 * * *");
            dailySchedule.put("description", "Runs at 2 AM every day");
            dailySchedule.put("tasks", new String[]{
                "Clean up expired export files",
                "Process pending account deletions",
                "Clean up session data",
                "Clean up temporary files"
            });
            schedule.put("daily", dailySchedule);
            
            // Weekly cleanup schedule
            Map<String, Object> weeklySchedule = new HashMap<>();
            weeklySchedule.put("cron", "0 0 3 * * SUN");
            weeklySchedule.put("description", "Runs at 3 AM every Sunday");
            weeklySchedule.put("tasks", new String[]{
                "Process hard deletions",
                "Clean up audit logs",
                "Clean up analytics data",
                "Optimize database"
            });
            schedule.put("weekly", weeklySchedule);
            
            // Monthly cleanup schedule
            Map<String, Object> monthlySchedule = new HashMap<>();
            monthlySchedule.put("cron", "0 0 4 1 * *");
            monthlySchedule.put("description", "Runs at 4 AM on the 1st of every month");
            monthlySchedule.put("tasks", new String[]{
                "Clean up old backups",
                "Generate retention reports",
                "Vacuum database"
            });
            schedule.put("monthly", monthlySchedule);
            
            return ResponseEntity.ok(schedule);
            
        } catch (Exception e) {
            logger.error("Failed to get cleanup schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to retrieve schedule information"));
        }
    }

    /**
     * Get data retention compliance information
     */
    @GetMapping("/compliance")
    @Operation(summary = "Get compliance information", description = "Get data retention compliance status and information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compliance information retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public ResponseEntity<Map<String, Object>> getComplianceInformation() {
        try {
            Map<String, Object> compliance = new HashMap<>();
            
            // GDPR compliance
            Map<String, Object> gdpr = new HashMap<>();
            gdpr.put("rightToErasure", "Implemented via account deletion API");
            gdpr.put("dataPortability", "Implemented via data export API");
            gdpr.put("dataMinimization", "Automated cleanup of unnecessary data");
            gdpr.put("storageLimitation", "Data retention policies enforce storage limits");
            compliance.put("gdpr", gdpr);
            
            // CCPA compliance
            Map<String, Object> ccpa = new HashMap<>();
            ccpa.put("rightToDelete", "Implemented via account deletion API");
            ccpa.put("rightToKnow", "Implemented via data export API");
            ccpa.put("dataRetention", "Automated data retention policies");
            compliance.put("ccpa", ccpa);
            
            // General compliance features
            Map<String, Object> features = new HashMap<>();
            features.put("cookieConsent", "Cookie banner and preference management");
            features.put("dataExport", "User-initiated data export with async processing");
            features.put("accountDeletion", "Soft delete with grace period and hard delete");
            features.put("dataRetention", "Automated cleanup with configurable retention periods");
            features.put("auditTrail", "Comprehensive logging of data operations");
            compliance.put("features", features);
            
            return ResponseEntity.ok(compliance);
            
        } catch (Exception e) {
            logger.error("Failed to get compliance information", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to retrieve compliance information"));
        }
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
}
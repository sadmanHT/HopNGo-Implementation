package com.hopngo.controller;

import com.hopngo.service.AccountDeletionService;
import com.hopngo.service.AccountDeletionService.AccountDeletionStatus;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/me")
@Tag(name = "Account Deletion", description = "Account deletion and privacy compliance APIs")
public class AccountDeletionController {

    private static final Logger logger = LoggerFactory.getLogger(AccountDeletionController.class);

    @Autowired
    private AccountDeletionService accountDeletionService;

    /**
     * Request account deletion
     */
    @PostMapping("/delete")
    @Operation(summary = "Request account deletion", description = "Schedule the authenticated user's account for deletion with a grace period")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account deletion requested successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or account already scheduled for deletion"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> requestAccountDeletion(
            @Valid @RequestBody AccountDeletionRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            boolean success = accountDeletionService.requestAccountDeletion(userId, request.getReason());
            
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Account deletion requested successfully");
                response.put("gracePeriodDays", 30); // TODO: Get from configuration
                response.put("status", "scheduled");
                response.put("canCancel", true);
                response.put("warning", "Your account will be permanently deleted after the grace period. You can cancel this request before then.");
                
                logger.info("Account deletion requested by user {}", userId);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Request failed", "Unable to process account deletion request"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to request account deletion for user {}", getUserIdFromAuth(authentication), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to process account deletion request"));
        }
    }

    /**
     * Cancel account deletion
     */
    @PostMapping("/delete/cancel")
    @Operation(summary = "Cancel account deletion", description = "Cancel a pending account deletion request during the grace period")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account deletion cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "No pending deletion or grace period expired"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> cancelAccountDeletion(Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            boolean success = accountDeletionService.cancelAccountDeletion(userId);
            
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Account deletion cancelled successfully");
                response.put("status", "active");
                
                logger.info("Account deletion cancelled by user {}", userId);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Cannot cancel", "No pending deletion request or grace period has expired"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to cancel account deletion for user {}", getUserIdFromAuth(authentication), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to cancel account deletion"));
        }
    }

    /**
     * Get account deletion status
     */
    @GetMapping("/delete/status")
    @Operation(summary = "Get account deletion status", description = "Get the current deletion status of the authenticated user's account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account deletion status retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getAccountDeletionStatus(Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            AccountDeletionStatus status = accountDeletionService.getAccountDeletionStatus(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("isDeleted", status.isDeleted());
            response.put("deletedAt", status.getDeletedAt());
            response.put("scheduledForDeletionAt", status.getScheduledForDeletionAt());
            response.put("status", status.getStatus());
            response.put("canCancel", status.getScheduledForDeletionAt() != null && 
                    status.getScheduledForDeletionAt().isAfter(java.time.LocalDateTime.now()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get account deletion status for user {}", getUserIdFromAuth(authentication), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal error", "Failed to get account deletion status"));
        }
    }

    /**
     * Get account deletion information (public endpoint for information)
     */
    @GetMapping("/delete/info")
    @Operation(summary = "Get account deletion information", description = "Get information about the account deletion process")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account deletion information retrieved")
    })
    public ResponseEntity<Map<String, Object>> getAccountDeletionInfo() {
        
        Map<String, Object> response = new HashMap<>();
        response.put("gracePeriodDays", 30); // TODO: Get from configuration
        response.put("hardDeleteAfterDays", 90); // TODO: Get from configuration
        response.put("process", Map.of(
                "step1", "Request account deletion with optional reason",
                "step2", "Grace period begins - you can cancel during this time",
                "step3", "Account is soft-deleted - personal data is anonymized",
                "step4", "After retention period, account is permanently deleted"
        ));
        response.put("dataHandling", Map.of(
                "personalData", "Immediately anonymized during soft deletion",
                "activityData", "Retained for legal/business purposes, then deleted",
                "backups", "Purged according to backup retention policies"
        ));
        response.put("cancellation", Map.of(
                "canCancel", "Yes, during grace period only",
                "howToCancel", "Use the cancel deletion endpoint or contact support"
        ));
        
        return ResponseEntity.ok(response);
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
     * Account deletion request DTO
     */
    public static class AccountDeletionRequest {
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        private String reason;

        private boolean confirmDataLoss = false;
        private boolean confirmUnderstanding = false;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public boolean isConfirmDataLoss() {
            return confirmDataLoss;
        }

        public void setConfirmDataLoss(boolean confirmDataLoss) {
            this.confirmDataLoss = confirmDataLoss;
        }

        public boolean isConfirmUnderstanding() {
            return confirmUnderstanding;
        }

        public void setConfirmUnderstanding(boolean confirmUnderstanding) {
            this.confirmUnderstanding = confirmUnderstanding;
        }
    }
}
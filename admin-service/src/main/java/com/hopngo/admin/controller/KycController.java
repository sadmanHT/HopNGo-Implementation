package com.hopngo.admin.controller;

import com.hopngo.admin.dto.KycDecisionRequest;
import com.hopngo.admin.dto.KycRequestDto;
import com.hopngo.admin.service.KycAdminService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/kyc")
@PreAuthorize("hasRole('ADMIN')")
public class KycController {
    
    private static final Logger logger = LoggerFactory.getLogger(KycController.class);
    
    private final KycAdminService kycAdminService;
    
    public KycController(KycAdminService kycAdminService) {
        this.kycAdminService = kycAdminService;
    }
    
    /**
     * Get pending KYC requests with pagination
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<KycRequestDto>> getPendingKycRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<KycRequestDto> requests = kycAdminService.getPendingKycRequests(pageable);
            
            return ResponseEntity.ok(requests);
            
        } catch (Exception e) {
            logger.error("Error fetching pending KYC requests", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get KYC request by ID
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<KycRequestDto> getKycRequest(@PathVariable Long requestId) {
        try {
            KycRequestDto request = kycAdminService.getKycRequestById(requestId);
            
            if (request != null) {
                return ResponseEntity.ok(request);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching KYC request {}", requestId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Process KYC decision (approve/reject)
     */
    @PostMapping("/{requestId}/decision")
    public ResponseEntity<Map<String, String>> processKycDecision(
            @PathVariable Long requestId,
            @Valid @RequestBody KycDecisionRequest decision,
            Authentication authentication) {
        
        try {
            // Validate decision
            if (!decision.isValid()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid decision request"));
            }
            
            // Extract admin user ID from authentication
            Long adminUserId = extractUserIdFromAuth(authentication);
            
            boolean success = kycAdminService.processKycDecision(requestId, decision, adminUserId);
            
            if (success) {
                String message = decision.isApproval() ? 
                    "KYC request approved successfully" : 
                    "KYC request rejected successfully";
                
                return ResponseEntity.ok(Map.of("message", message));
            } else {
                return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process KYC decision"));
            }
            
        } catch (Exception e) {
            logger.error("Error processing KYC decision for request {}", requestId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * Get KYC statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getKycStatistics() {
        try {
            Map<String, Object> statistics = kycAdminService.getKycStatistics();
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error fetching KYC statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Search KYC requests by user email or name
     */
    @GetMapping("/search")
    public ResponseEntity<Page<KycRequestDto>> searchKycRequests(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<KycRequestDto> requests = kycAdminService.searchKycRequests(query.trim(), pageable);
            
            return ResponseEntity.ok(requests);
            
        } catch (Exception e) {
            logger.error("Error searching KYC requests with query: {}", query, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Approve KYC request (convenience endpoint)
     */
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<Map<String, String>> approveKycRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        
        try {
            String adminNotes = body != null ? body.get("adminNotes") : null;
            
            KycDecisionRequest decision = new KycDecisionRequest("APPROVE", null, adminNotes);
            Long adminUserId = extractUserIdFromAuth(authentication);
            
            boolean success = kycAdminService.processKycDecision(requestId, decision, adminUserId);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "KYC request approved successfully"));
            } else {
                return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to approve KYC request"));
            }
            
        } catch (Exception e) {
            logger.error("Error approving KYC request {}", requestId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * Reject KYC request (convenience endpoint)
     */
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<Map<String, String>> rejectKycRequest(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        
        try {
            String rejectionReason = body.get("rejectionReason");
            String adminNotes = body.get("adminNotes");
            
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Rejection reason is required"));
            }
            
            KycDecisionRequest decision = new KycDecisionRequest("REJECT", rejectionReason, adminNotes);
            Long adminUserId = extractUserIdFromAuth(authentication);
            
            boolean success = kycAdminService.processKycDecision(requestId, decision, adminUserId);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "KYC request rejected successfully"));
            } else {
                return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reject KYC request"));
            }
            
        } catch (Exception e) {
            logger.error("Error rejecting KYC request {}", requestId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    // Helper method to extract user ID from authentication
    private Long extractUserIdFromAuth(Authentication authentication) {
        // TODO: Implement proper user ID extraction from JWT or authentication context
        // For now, return a placeholder - this should be implemented based on your auth setup
        return 1L; // Placeholder admin user ID
    }
}
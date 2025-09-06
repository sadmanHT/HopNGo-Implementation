package com.hopngo.auth.controller;

import com.hopngo.auth.dto.KycDecisionDto;
import com.hopngo.auth.dto.KycResponseDto;
import com.hopngo.auth.entity.User;
import com.hopngo.auth.entity.UserFlags;
import com.hopngo.auth.mapper.UserMapper;
import com.hopngo.auth.repository.UserFlagsRepository;
import com.hopngo.auth.service.AuthService;
import com.hopngo.auth.service.KycService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal controller for service-to-service communication
 * These endpoints are not exposed to external clients
 */
@RestController
@RequestMapping("/internal")
public class InternalController {
    
    private static final Logger logger = LoggerFactory.getLogger(InternalController.class);
    
    private final KycService kycService;
    private final AuthService authService;
    private final UserFlagsRepository userFlagsRepository;
    private final UserMapper userMapper;
    
    public InternalController(KycService kycService, AuthService authService, UserFlagsRepository userFlagsRepository, UserMapper userMapper) {
        this.kycService = kycService;
        this.authService = authService;
        this.userFlagsRepository = userFlagsRepository;
        this.userMapper = userMapper;
    }
    
    /**
     * Get pending KYC requests with user details for admin service
     * GET /internal/kyc/pending
     */
    @GetMapping("/kyc/pending")
    public ResponseEntity<Page<Map<String, Object>>> getPendingKycRequestsWithUserDetails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<KycResponseDto> kycRequests = kycService.getPendingKycRequests(pageable);
            
            // Enrich with user details
            Page<Map<String, Object>> enrichedRequests = kycRequests.map(this::enrichKycRequestWithUserDetails);
            
            return ResponseEntity.ok(enrichedRequests);
            
        } catch (Exception e) {
            logger.error("Error fetching pending KYC requests for admin service", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get KYC request by ID with user details
     * GET /internal/kyc/{id}
     */
    @GetMapping("/kyc/{id}")
    public ResponseEntity<Map<String, Object>> getKycRequestWithUserDetails(@PathVariable Long id) {
        try {
            KycResponseDto kycRequest = kycService.getKycRequestById(id);
            
            if (kycRequest != null) {
                Map<String, Object> enrichedRequest = enrichKycRequestWithUserDetails(kycRequest);
                return ResponseEntity.ok(enrichedRequest);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching KYC request {} for admin service", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Process KYC decision from admin service
     * POST /internal/kyc/{id}/decision
     */
    @PostMapping("/kyc/{id}/decision")
    public ResponseEntity<Map<String, String>> processKycDecisionFromAdmin(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> decisionRequest) {
        
        try {
            String decision = (String) decisionRequest.get("decision");
            String rejectionReason = (String) decisionRequest.get("rejectionReason");
            String adminNotes = (String) decisionRequest.get("adminNotes");
            Long adminUserId = ((Number) decisionRequest.get("adminUserId")).longValue();
            
            // Get admin user email
            User adminUser = authService.getUserById(adminUserId);
            if (adminUser == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Admin user not found"));
            }
            
            String adminEmail = adminUser.getEmail();
            
            // Create decision DTO
            KycDecisionDto decisionDto = new KycDecisionDto();
            decisionDto.setDecision(KycDecisionDto.Decision.valueOf(decision));
            decisionDto.setRejectionReason(rejectionReason);
            decisionDto.setAdminNotes(adminNotes);
            
            // Process decision
            KycResponseDto response = kycService.processKycDecision(id, decisionDto);
            
            if (response != null) {
                String message = "APPROVE".equals(decision) ? 
                    "KYC request approved successfully" : 
                    "KYC request rejected successfully";
                
                return ResponseEntity.ok(Map.of("message", message));
            } else {
                return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process KYC decision"));
            }
            
        } catch (Exception e) {
            logger.error("Error processing KYC decision from admin service for request {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * Get KYC statistics
     * GET /internal/kyc/statistics
     */
    @GetMapping("/kyc/statistics")
    public ResponseEntity<Map<String, Object>> getKycStatistics() {
        try {
            Map<String, Object> statistics = kycService.getKycStatistics();
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error fetching KYC statistics for admin service", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Search KYC requests by user email or name
     * GET /internal/kyc/search
     */
    @GetMapping("/kyc/search")
    public ResponseEntity<Page<Map<String, Object>>> searchKycRequestsWithUserDetails(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<KycResponseDto> kycRequests = kycService.searchKycRequests(query, pageable);
            
            // Enrich with user details
            Page<Map<String, Object>> enrichedRequests = kycRequests.map(this::enrichKycRequestWithUserDetails);
            
            return ResponseEntity.ok(enrichedRequests);
            
        } catch (Exception e) {
            logger.error("Error searching KYC requests for admin service with query: {}", query, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get user verification status (for other services)
     */
    @GetMapping("/users/{userId}/verification-status")
    public ResponseEntity<Map<String, Object>> getUserVerificationStatus(@PathVariable Long userId) {
        try {
            UserFlags userFlags = userFlagsRepository.findByUserId(userId)
                .orElse(new UserFlags(userId));
            
            Map<String, Object> status = new HashMap<>();
            status.put("userId", userId);
            status.put("verifiedProvider", userFlags.getVerifiedProvider());
            status.put("banned", userFlags.getBanned());
            status.put("suspended", userFlags.isBanned());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error fetching verification status for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get user by ID (for admin service)
     * GET /internal/users/{id}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long userId) {
        try {
            User user = authService.getUserById(userId);
            
            if (user != null) {
                Map<String, Object> userDetails = new HashMap<>();
                userDetails.put("id", user.getId());
                userDetails.put("email", user.getEmail());
                userDetails.put("firstName", user.getFirstName());
                userDetails.put("lastName", user.getLastName());
                userDetails.put("role", user.getRole());
                userDetails.put("isActive", user.getIsActive());
                userDetails.put("createdAt", user.getCreatedAt());
                
                return ResponseEntity.ok(userDetails);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching user {} for admin service", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper methods
    
    private Map<String, Object> enrichKycRequestWithUserDetails(KycResponseDto kycRequest) {
        Map<String, Object> enriched = new HashMap<>();
        
        // Copy KYC request data
        enriched.put("id", kycRequest.getId());
        enriched.put("userId", kycRequest.getUserId());
        enriched.put("status", kycRequest.getStatus());
        enriched.put("fields", kycRequest.getFields());
        enriched.put("createdAt", kycRequest.getCreatedAt());
        enriched.put("updatedAt", kycRequest.getUpdatedAt());
        enriched.put("verifiedProvider", kycRequest.getVerifiedProvider());
        enriched.put("rejectionReason", kycRequest.getRejectionReason());
        
        // Add user details
        try {
            User user = authService.getUserById(kycRequest.getUserId());
            if (user != null) {
                enriched.put("userEmail", user.getEmail());
                enriched.put("userName", user.getFirstName() + " " + user.getLastName());
            }
        } catch (Exception e) {
            logger.warn("Could not fetch user details for KYC request {}: {}", kycRequest.getId(), e.getMessage());
        }
        
        return enriched;
    }
}
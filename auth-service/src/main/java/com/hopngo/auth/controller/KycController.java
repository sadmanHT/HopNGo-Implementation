package com.hopngo.auth.controller;

import com.hopngo.auth.dto.KycDecisionDto;
import com.hopngo.auth.dto.KycRequestDto;
import com.hopngo.auth.dto.KycResponseDto;
import com.hopngo.auth.service.AuthService;
import com.hopngo.auth.service.KycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller for KYC (Know Your Customer) operations
 */
@RestController
@RequestMapping("/auth/kyc")
public class KycController {
    
    private final KycService kycService;
    private final AuthService authService;
    
    @Autowired
    public KycController(KycService kycService, AuthService authService) {
        this.kycService = kycService;
        this.authService = authService;
    }
    
    /**
     * Submit KYC request
     * POST /auth/kyc
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<KycResponseDto> submitKycRequest(
            @Valid @RequestBody KycRequestDto kycRequestDto,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        // Get user ID from email
        Long userId = authService.getUserByEmail(userEmail).getId();
        KycResponseDto response = kycService.submitKycRequest(userId, kycRequestDto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get KYC status for current user
     * GET /auth/kyc/status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<KycResponseDto> getKycStatus(Authentication authentication) {
        
        String userEmail = authentication.getName();
        // Get user ID from email
        Long userId = authService.getUserByEmail(userEmail).getId();
        KycResponseDto response = kycService.getKycStatus(userId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Approve KYC request (Admin only)
     * POST /auth/kyc/{id}/approve
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KycResponseDto> approveKycRequest(
            @PathVariable Long id,
            @Valid @RequestBody KycDecisionDto decisionDto,
            Authentication authentication) {
        
        KycResponseDto response = kycService.processKycDecision(id, decisionDto);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Reject KYC request (Admin only)
     * POST /auth/kyc/{id}/reject
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KycResponseDto> rejectKycRequest(
            @PathVariable Long id,
            @Valid @RequestBody KycDecisionDto decisionDto,
            Authentication authentication) {
        
        KycResponseDto response = kycService.processKycDecision(id, decisionDto);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get pending KYC requests (Admin only)
     * GET /auth/kyc/pending
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<KycResponseDto>> getPendingKycRequests(Pageable pageable) {
        
        Page<KycResponseDto> pendingRequests = kycService.getPendingKycRequests(pageable);
        
        return ResponseEntity.ok(pendingRequests);
    }
    
    /**
     * Get KYC request by ID (Admin only)
     * GET /auth/kyc/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KycResponseDto> getKycRequestById(@PathVariable Long id) {
        
        KycResponseDto response = kycService.getKycRequestById(id);
        
        return ResponseEntity.ok(response);
    }
}
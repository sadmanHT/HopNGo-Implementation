package com.hopngo.auth.controller;

import com.hopngo.auth.dto.TwoFactorEnableRequest;
import com.hopngo.auth.dto.TwoFactorSetupResponse;
import com.hopngo.auth.dto.TwoFactorVerifyRequest;
import com.hopngo.auth.entity.User;
import com.hopngo.auth.service.TwoFactorAuthService;
import com.hopngo.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/2fa")
public class TwoFactorAuthController {
    
    private final TwoFactorAuthService twoFactorAuthService;
    private final AuthService authService;
    
    public TwoFactorAuthController(TwoFactorAuthService twoFactorAuthService,
                                  AuthService authService) {
        this.twoFactorAuthService = twoFactorAuthService;
        this.authService = authService;
    }
    
    /**
     * Setup 2FA - Generate QR code and backup codes
     */
    @PostMapping("/setup")
    public ResponseEntity<TwoFactorSetupResponse> setup2FA(Authentication authentication) {
        User user = authService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.is2faEnabled()) {
            return ResponseEntity.badRequest().build();
        }
        
        TwoFactorSetupResponse response = twoFactorAuthService.setup2FA(user);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Enable 2FA after verifying TOTP code
     */
    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enable2FA(
            @Valid @RequestBody TwoFactorEnableRequest request,
            Authentication authentication) {
        
        User user = authService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.is2faEnabled()) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean success = twoFactorAuthService.enable2FA(user, request.getTotpCode());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "2FA enabled successfully" : "Invalid TOTP code");
        
        return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Verify 2FA code during login
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify2FA(
            @Valid @RequestBody TwoFactorVerifyRequest request,
            Authentication authentication) {
        
        User user = authService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        boolean success = twoFactorAuthService.verify2FA(user, request.getCode(), request.isBackupCode());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "2FA verification successful" : "Invalid code");
        
        if (success && request.isBackupCode()) {
            int remainingCodes = twoFactorAuthService.getRemainingBackupCodesCount(user);
            response.put("remainingBackupCodes", remainingCodes);
        }
        
        return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Disable 2FA
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, String>> disable2FA(Authentication authentication) {
        User user = authService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        twoFactorAuthService.disable2FA(user);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "2FA disabled successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get 2FA status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> get2FAStatus(Authentication authentication) {
        User user = authService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", user.is2faEnabled());
        
        if (user.is2faEnabled()) {
            response.put("remainingBackupCodes", twoFactorAuthService.getRemainingBackupCodesCount(user));
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Regenerate backup codes
     */
    @PostMapping("/backup-codes/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateBackupCodes(Authentication authentication) {
        User user = authService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.is2faEnabled()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<String> newBackupCodes = twoFactorAuthService.regenerateBackupCodes(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("backupCodes", newBackupCodes);
        response.put("message", "New backup codes generated successfully");
        
        return ResponseEntity.ok(response);
    }
}

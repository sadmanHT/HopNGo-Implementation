package com.hopngo.auth.controller;

import com.hopngo.auth.service.AccountLockoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "${app.frontend.url:http://localhost:3000}")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final AccountLockoutService accountLockoutService;
    
    public AdminController(AccountLockoutService accountLockoutService) {
        this.accountLockoutService = accountLockoutService;
    }
    
    /**
     * Get lockout status for a user and IP
     */
    @GetMapping("/lockout-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LockoutStatusResponse> getLockoutStatus(
            @RequestParam String email,
            @RequestParam String ipAddress) {
        try {
            AccountLockoutService.LockoutStatus status = 
                    accountLockoutService.getLockoutStatus(email, ipAddress);
            
            return ResponseEntity.ok(new LockoutStatusResponse(
                    status.isUserLocked(),
                    status.isIpLocked(),
                    status.getUserLockoutRemainingSeconds(),
                    status.getIpLockoutRemainingSeconds(),
                    status.getUserFailedAttempts(),
                    status.getIpFailedAttempts(),
                    status.getUserRemainingAttempts(),
                    status.getIpRemainingAttempts()
            ));
            
        } catch (Exception e) {
            logger.error("Error getting lockout status", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Unlock user account
     */
    @PostMapping("/unlock-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> unlockUser(@Valid @RequestBody UnlockUserRequest request) {
        try {
            accountLockoutService.unlockUserAccount(request.getEmail());
            
            logger.info("Admin unlocked user account: {}", request.getEmail());
            return ResponseEntity.ok(new ApiResponse(
                    "success",
                    "User account unlocked successfully."
            ));
            
        } catch (Exception e) {
            logger.error("Error unlocking user account", e);
            return ResponseEntity.badRequest().body(new ApiResponse(
                    "error",
                    "Failed to unlock user account: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Unlock IP address
     */
    @PostMapping("/unlock-ip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> unlockIp(@Valid @RequestBody UnlockIpRequest request) {
        try {
            accountLockoutService.unlockIpAddress(request.getIpAddress());
            
            logger.info("Admin unlocked IP address: {}", request.getIpAddress());
            return ResponseEntity.ok(new ApiResponse(
                    "success",
                    "IP address unlocked successfully."
            ));
            
        } catch (Exception e) {
            logger.error("Error unlocking IP address", e);
            return ResponseEntity.badRequest().body(new ApiResponse(
                    "error",
                    "Failed to unlock IP address: " + e.getMessage()
            ));
        }
    }
    
    // Request DTOs
    public static class UnlockUserRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
        
        public UnlockUserRequest() {}
        
        public UnlockUserRequest(String email) {
            this.email = email;
        }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
    
    public static class UnlockIpRequest {
        @NotBlank(message = "IP address is required")
        private String ipAddress;
        
        public UnlockIpRequest() {}
        
        public UnlockIpRequest(String ipAddress) {
            this.ipAddress = ipAddress;
        }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    }
    
    // Response DTOs
    public static class LockoutStatusResponse {
        private boolean userLocked;
        private boolean ipLocked;
        private long userLockoutRemainingSeconds;
        private long ipLockoutRemainingSeconds;
        private int userFailedAttempts;
        private int ipFailedAttempts;
        private int userRemainingAttempts;
        private int ipRemainingAttempts;
        
        public LockoutStatusResponse() {}
        
        public LockoutStatusResponse(boolean userLocked, boolean ipLocked,
                                   long userLockoutRemainingSeconds, long ipLockoutRemainingSeconds,
                                   int userFailedAttempts, int ipFailedAttempts,
                                   int userRemainingAttempts, int ipRemainingAttempts) {
            this.userLocked = userLocked;
            this.ipLocked = ipLocked;
            this.userLockoutRemainingSeconds = userLockoutRemainingSeconds;
            this.ipLockoutRemainingSeconds = ipLockoutRemainingSeconds;
            this.userFailedAttempts = userFailedAttempts;
            this.ipFailedAttempts = ipFailedAttempts;
            this.userRemainingAttempts = userRemainingAttempts;
            this.ipRemainingAttempts = ipRemainingAttempts;
        }
        
        // Getters and setters
        public boolean isUserLocked() { return userLocked; }
        public void setUserLocked(boolean userLocked) { this.userLocked = userLocked; }
        
        public boolean isIpLocked() { return ipLocked; }
        public void setIpLocked(boolean ipLocked) { this.ipLocked = ipLocked; }
        
        public long getUserLockoutRemainingSeconds() { return userLockoutRemainingSeconds; }
        public void setUserLockoutRemainingSeconds(long userLockoutRemainingSeconds) { 
            this.userLockoutRemainingSeconds = userLockoutRemainingSeconds; 
        }
        
        public long getIpLockoutRemainingSeconds() { return ipLockoutRemainingSeconds; }
        public void setIpLockoutRemainingSeconds(long ipLockoutRemainingSeconds) { 
            this.ipLockoutRemainingSeconds = ipLockoutRemainingSeconds; 
        }
        
        public int getUserFailedAttempts() { return userFailedAttempts; }
        public void setUserFailedAttempts(int userFailedAttempts) { this.userFailedAttempts = userFailedAttempts; }
        
        public int getIpFailedAttempts() { return ipFailedAttempts; }
        public void setIpFailedAttempts(int ipFailedAttempts) { this.ipFailedAttempts = ipFailedAttempts; }
        
        public int getUserRemainingAttempts() { return userRemainingAttempts; }
        public void setUserRemainingAttempts(int userRemainingAttempts) { 
            this.userRemainingAttempts = userRemainingAttempts; 
        }
        
        public int getIpRemainingAttempts() { return ipRemainingAttempts; }
        public void setIpRemainingAttempts(int ipRemainingAttempts) { 
            this.ipRemainingAttempts = ipRemainingAttempts; 
        }
    }
    
    public static class ApiResponse {
        private String status;
        private String message;
        
        public ApiResponse() {}
        
        public ApiResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
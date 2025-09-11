package com.hopngo.auth.controller;

import com.hopngo.auth.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth/password-reset")
@CrossOrigin(origins = "${app.frontend.url:http://localhost:3000}")
public class PasswordResetController {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);
    
    private final PasswordResetService passwordResetService;
    
    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }
    
    /**
     * Initiate password reset process
     */
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse> initiatePasswordReset(@Valid @RequestBody InitiateResetRequest request) {
        try {
            passwordResetService.initiatePasswordReset(request.getEmail());
            
            // Always return success to prevent email enumeration
            return ResponseEntity.ok(new ApiResponse(
                    "success",
                    "If the email exists, a password reset link has been sent."
            ));
            
        } catch (Exception e) {
            logger.error("Error initiating password reset", e);
            return ResponseEntity.ok(new ApiResponse(
                    "success",
                    "If the email exists, a password reset link has been sent."
            ));
        }
    }
    
    /**
     * Reset password using token
     */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            
            return ResponseEntity.ok(new ApiResponse(
                    "success",
                    "Password has been reset successfully."
            ));
            
        } catch (Exception e) {
            logger.error("Error resetting password", e);
            return ResponseEntity.badRequest().body(new ApiResponse(
                    "error",
                    e.getMessage()
            ));
        }
    }
    
    /**
     * Validate reset token (optional endpoint for frontend validation)
     */
    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        try {
            passwordResetService.validateResetToken(request.getToken());
            
            return ResponseEntity.ok(new ApiResponse(
                    "success",
                    "Token is valid."
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                    "error",
                    "Invalid or expired token."
            ));
        }
    }
    
    // Request DTOs
    public static class InitiateResetRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
        
        public InitiateResetRequest() {}
        
        public InitiateResetRequest(String email) {
            this.email = email;
        }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
    
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;
        
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        private String newPassword;
        
        public ResetPasswordRequest() {}
        
        public ResetPasswordRequest(String token, String newPassword) {
            this.token = token;
            this.newPassword = newPassword;
        }
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
    
    public static class ValidateTokenRequest {
        @NotBlank(message = "Token is required")
        private String token;
        
        public ValidateTokenRequest() {}
        
        public ValidateTokenRequest(String token) {
            this.token = token;
        }
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
    
    // Response DTO
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
package com.hopngo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private String totpCode; // Optional 2FA code
    private boolean isBackupCode = false; // Flag for backup code usage
    
    public LoginRequest() {}
    
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
    
    public LoginRequest(String email, String password, String totpCode, boolean isBackupCode) {
        this.email = email;
        this.password = password;
        this.totpCode = totpCode;
        this.isBackupCode = isBackupCode;
    }
    
    // Getters and Setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getTotpCode() {
        return totpCode;
    }
    
    public void setTotpCode(String totpCode) {
        this.totpCode = totpCode;
    }
    
    public boolean isBackupCode() {
        return isBackupCode;
    }
    
    public void setBackupCode(boolean backupCode) {
        isBackupCode = backupCode;
    }
     
    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}
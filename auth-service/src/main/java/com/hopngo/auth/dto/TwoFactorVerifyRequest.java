package com.hopngo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class TwoFactorVerifyRequest {
    
    @NotBlank(message = "Code is required")
    private String code; // TOTP code or backup code
    
    private boolean isBackupCode = false;
    
    public TwoFactorVerifyRequest() {}
    
    public TwoFactorVerifyRequest(String code, boolean isBackupCode) {
        this.code = code;
        this.isBackupCode = isBackupCode;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public boolean isBackupCode() {
        return isBackupCode;
    }
    
    public void setBackupCode(boolean backupCode) {
        isBackupCode = backupCode;
    }
}
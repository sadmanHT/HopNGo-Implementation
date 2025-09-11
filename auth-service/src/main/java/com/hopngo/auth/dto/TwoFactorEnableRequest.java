package com.hopngo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class TwoFactorEnableRequest {
    
    @NotBlank(message = "TOTP code is required")
    private String totpCode;
    
    public TwoFactorEnableRequest() {}
    
    public TwoFactorEnableRequest(String totpCode) {
        this.totpCode = totpCode;
    }
    
    public String getTotpCode() {
        return totpCode;
    }
    
    public void setTotpCode(String totpCode) {
        this.totpCode = totpCode;
    }
}
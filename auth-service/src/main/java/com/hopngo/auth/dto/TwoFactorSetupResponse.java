package com.hopngo.auth.dto;

import java.util.List;

public class TwoFactorSetupResponse {
    
    private String qrCodeImage; // Base64 encoded QR code image
    private String secret; // TOTP secret (for manual entry)
    private List<String> backupCodes;
    
    public TwoFactorSetupResponse() {}
    
    public TwoFactorSetupResponse(String qrCodeImage, String secret, List<String> backupCodes) {
        this.qrCodeImage = qrCodeImage;
        this.secret = secret;
        this.backupCodes = backupCodes;
    }
    
    public String getQrCodeImage() {
        return qrCodeImage;
    }
    
    public void setQrCodeImage(String qrCodeImage) {
        this.qrCodeImage = qrCodeImage;
    }
    
    public String getSecret() {
        return secret;
    }
    
    public void setSecret(String secret) {
        this.secret = secret;
    }
    
    public List<String> getBackupCodes() {
        return backupCodes;
    }
    
    public void setBackupCodes(List<String> backupCodes) {
        this.backupCodes = backupCodes;
    }
}
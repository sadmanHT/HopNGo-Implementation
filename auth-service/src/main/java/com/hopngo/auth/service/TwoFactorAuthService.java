package com.hopngo.auth.service;

import com.hopngo.auth.dto.TwoFactorSetupResponse;
import com.hopngo.auth.entity.User;
import com.hopngo.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TwoFactorAuthService {
    
    private final TotpService totpService;
    private final BackupCodeService backupCodeService;
    private final UserRepository userRepository;
    
    public TwoFactorAuthService(TotpService totpService, 
                               BackupCodeService backupCodeService,
                               UserRepository userRepository) {
        this.totpService = totpService;
        this.backupCodeService = backupCodeService;
        this.userRepository = userRepository;
    }
    
    /**
     * Setup 2FA for a user (generate secret and QR code)
     */
    public TwoFactorSetupResponse setup2FA(User user) {
        try {
            // Generate TOTP secret
            String secret = totpService.generateSecret();
            
            // Generate QR code
            String totpUri = totpService.generateTotpUri(secret, user.getEmail());
            String qrCodeImage = totpService.generateQrCode(totpUri);
            
            // Generate backup codes
            List<String> backupCodes = backupCodeService.generateBackupCodes();
            List<String> formattedCodes = backupCodeService.formatBackupCodesForDisplay(backupCodes);
            
            // Store secret and backup codes (but don't enable 2FA yet)
            user.setTotpSecret(secret);
            user.setBackupCodes(backupCodeService.backupCodesToJson(backupCodes));
            userRepository.save(user);
            
            return new TwoFactorSetupResponse(qrCodeImage, secret, formattedCodes);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup 2FA", e);
        }
    }
    
    /**
     * Enable 2FA after verifying TOTP code
     */
    public boolean enable2FA(User user, String totpCode) {
        if (user.getTotpSecret() == null) {
            throw new IllegalStateException("2FA setup required before enabling");
        }
        
        if (totpService.verifyTotp(user.getTotpSecret(), totpCode)) {
            user.setIs2faEnabled(true);
            userRepository.save(user);
            return true;
        }
        
        return false;
    }
    
    /**
     * Disable 2FA
     */
    public void disable2FA(User user) {
        user.setIs2faEnabled(false);
        user.setTotpSecret(null);
        user.setBackupCodes(null);
        userRepository.save(user);
    }
    
    /**
     * Verify 2FA code (TOTP or backup code)
     */
    public boolean verify2FA(User user, String code, boolean isBackupCode) {
        if (!user.is2faEnabled()) {
            return false;
        }
        
        if (isBackupCode) {
            return verifyBackupCode(user, code);
        } else {
            return totpService.verifyTotp(user.getTotpSecret(), code);
        }
    }
    
    /**
     * Verify and consume backup code
     */
    private boolean verifyBackupCode(User user, String inputCode) {
        String normalizedCode = backupCodeService.normalizeBackupCode(inputCode);
        List<String> backupCodes = backupCodeService.jsonToBackupCodes(user.getBackupCodes());
        
        if (backupCodeService.verifyAndConsumeBackupCode(normalizedCode, backupCodes)) {
            // Update user's backup codes (remove used code)
            user.setBackupCodes(backupCodeService.backupCodesToJson(backupCodes));
            userRepository.save(user);
            return true;
        }
        
        return false;
    }
    
    /**
     * Generate new backup codes
     */
    public List<String> regenerateBackupCodes(User user) {
        if (!user.is2faEnabled()) {
            throw new IllegalStateException("2FA must be enabled to regenerate backup codes");
        }
        
        List<String> newBackupCodes = backupCodeService.generateBackupCodes();
        user.setBackupCodes(backupCodeService.backupCodesToJson(newBackupCodes));
        userRepository.save(user);
        
        return backupCodeService.formatBackupCodesForDisplay(newBackupCodes);
    }
    
    /**
     * Get remaining backup codes count
     */
    public int getRemainingBackupCodesCount(User user) {
        if (!user.is2faEnabled() || user.getBackupCodes() == null) {
            return 0;
        }
        
        List<String> backupCodes = backupCodeService.jsonToBackupCodes(user.getBackupCodes());
        return backupCodes.size();
    }
}
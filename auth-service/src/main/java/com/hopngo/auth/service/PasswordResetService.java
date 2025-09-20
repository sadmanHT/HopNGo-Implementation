package com.hopngo.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hopngo.auth.entity.User;
import com.hopngo.auth.repository.UserRepository;

@Service
@Transactional
public class PasswordResetService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;
    private final EmailService emailService;
    
    @Value("${app.password-reset.secret:default-reset-secret}")
    private String resetSecret;
    
    @Value("${app.password-reset.expiration-hours:1}")
    private int expirationHours;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    public PasswordResetService(UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               PasswordValidationService passwordValidationService,
                               EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = passwordValidationService;
        this.emailService = emailService;
    }
    
    /**
     * Initiate password reset process
     */
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists - always return success
            logger.warn("Password reset requested for non-existent email: {}", email);
            return;
        }
        
        User user = userOpt.get();
        
        if (!user.getIsActive()) {
            logger.warn("Password reset requested for inactive user: {}", email);
            return;
        }
        
        try {
            // Generate secure reset token
            String resetToken = generateResetToken(user.getId(), user.getEmail());
            
            // Send reset email
            sendPasswordResetEmail(user, resetToken);
            
            logger.info("Password reset initiated for user: {}", user.getId());
            
        } catch (Exception e) {
            logger.error("Failed to initiate password reset for user: {}", user.getId(), e);
            throw new RuntimeException("Failed to initiate password reset");
        }
    }
    
    /**
     * Validate reset token without resetting password
     */
    public void validateResetToken(String token) {
        try {
            validateAndParseResetToken(token);
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            throw new RuntimeException("Invalid or expired token: " + e.getMessage());
        }
    }
    
    /**
     * Reset password using token
     */
    public void resetPassword(String token, String newPassword) {
        try {
            // Validate and parse token
            ResetTokenData tokenData = validateAndParseResetToken(token);
            
            // Find user
            User user = userRepository.findById(tokenData.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate new password
            PasswordValidationService.PasswordValidationResult validation = 
                    passwordValidationService.validatePassword(newPassword, user.getEmail());
            
            if (!validation.isValid()) {
                throw new RuntimeException("Password validation failed: " + 
                        String.join(", ", validation.getErrors()));
            }
            
            // Update password
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            logger.info("Password reset completed for user: {}", user.getId());
            
        } catch (Exception e) {
            logger.error("Password reset failed", e);
            throw new RuntimeException("Password reset failed: " + e.getMessage());
        }
    }
    
    /**
     * Generate signed reset token
     */
    private String generateResetToken(Long userId, String email) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        long expirationTime = Instant.now().plus(expirationHours, ChronoUnit.HOURS).getEpochSecond();
        
        // Create payload: userId:email:expiration:nonce
        SecureRandom random = new SecureRandom();
        String nonce = Base64.getEncoder().encodeToString(random.generateSeed(16));
        
        String payload = userId + ":" + email + ":" + expirationTime + ":" + nonce;
        String encodedPayload = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        
        // Generate HMAC signature
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(resetSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);
        
        byte[] signature = mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
        String encodedSignature = Base64.getEncoder().encodeToString(signature);
        
        return encodedPayload + "." + encodedSignature;
    }
    
    /**
     * Validate reset token and extract data
     */
    private ResetTokenData validateAndParseResetToken(String token) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new RuntimeException("Invalid token format");
        }
        
        String encodedPayload = parts[0];
        String providedSignature = parts[1];
        
        // Verify signature
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(resetSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);
        
        byte[] expectedSignature = mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
        String expectedSignatureStr = Base64.getEncoder().encodeToString(expectedSignature);
        
        if (!expectedSignatureStr.equals(providedSignature)) {
            throw new RuntimeException("Invalid token signature");
        }
        
        // Decode payload
        String payload = new String(Base64.getDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        String[] payloadParts = payload.split(":");
        
        if (payloadParts.length != 4) {
            throw new RuntimeException("Invalid token payload");
        }
        
        Long userId = Long.parseLong(payloadParts[0]);
        String email = payloadParts[1];
        long expirationTime = Long.parseLong(payloadParts[2]);
        
        // Check expiration
        if (Instant.now().getEpochSecond() > expirationTime) {
            throw new RuntimeException("Reset token has expired");
        }
        
        return new ResetTokenData(userId, email, expirationTime);
    }
    
    /**
     * Send password reset email
     */
    private void sendPasswordResetEmail(User user, String resetToken) {
        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
        
        String subject = "Password Reset Request - HopNGo";
        String body = String.format(
                "Hello %s,\n\n" +
                "You have requested to reset your password. Please click the link below to reset your password:\n\n" +
                "%s\n\n" +
                "This link will expire in %d hour(s).\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "HopNGo Team",
                user.getFirstName(),
                resetUrl,
                expirationHours
        );
        
        emailService.sendEmail(user.getEmail(), subject, body);
    }
    
    /**
     * Data class for reset token information
     */
    private static class ResetTokenData {
        private final Long userId;
        private final String email;
        private final long expirationTime;
        
        public ResetTokenData(Long userId, String email, long expirationTime) {
            this.userId = userId;
            this.email = email;
            this.expirationTime = expirationTime;
        }
        
        public Long getUserId() { return userId; }
        public String getEmail() { return email; }
        public long getExpirationTime() { return expirationTime; }
    }
}
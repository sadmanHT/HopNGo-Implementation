package com.hopngo.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hopngo.auth.entity.RefreshToken;
import com.hopngo.auth.entity.User;
import com.hopngo.auth.repository.RefreshTokenRepository;

@Service
@Transactional
public class RefreshTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String REDIS_KEY_PREFIX = "refresh_token:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklisted_token:";
    private static final String TOKEN_FAMILY_PREFIX = "token_family:";
    private static final String REUSE_DETECTION_PREFIX = "token_reuse:";
    private static final long BLACKLIST_RETENTION_DAYS = 90;
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    
    @Value("${jwt.refresh.expiration:604800000}") // 7 days in milliseconds
    private long refreshTokenExpiration;
    
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                              @org.springframework.beans.factory.annotation.Qualifier("redisObjectTemplate") 
                              @org.springframework.beans.factory.annotation.Autowired(required = false)
                              RedisTemplate<String, Object> redisTemplate,
                              UserService userService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
    }
    
    /**
     * Create a new refresh token for the user with token family
     */
    public RefreshToken createRefreshToken(User user) {
        return createRefreshToken(user, null);
    }
    
    /**
     * Create a new refresh token for the user with optional session ID
     */
    public RefreshToken createRefreshToken(User user, String sessionId) {
        logger.info("Creating refresh token for user: {}", user.getEmail());
        
        // Generate new token with family
        String tokenValue = UUID.randomUUID().toString();
        String tokenFamily = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);
        
        RefreshToken refreshToken = new RefreshToken(tokenValue, user, expiresAt);
        // Note: These fields would need to be added to RefreshToken entity
        // refreshToken.setTokenFamily(tokenFamily);
        // refreshToken.setSessionId(sessionId);
        // refreshToken.setUsageCount(0);
        
        refreshToken = refreshTokenRepository.save(refreshToken);
        
        // Cache in Redis
        cacheRefreshToken(refreshToken);
        
        logger.info("Refresh token created successfully for user: {}", user.getEmail());
        return refreshToken;
    }
    
    /**
     * Rotate refresh token with reuse detection
     */
    public RefreshToken rotateRefreshToken(String oldToken) {
        logger.info("Rotating refresh token: {}", oldToken.substring(0, 8) + "...");
        
        Optional<RefreshToken> oldTokenOpt = validateRefreshToken(oldToken);
        if (oldTokenOpt.isEmpty()) {
            throw new SecurityException("Invalid refresh token for rotation");
        }
        
        RefreshToken oldRefreshToken = oldTokenOpt.get();
        
        // Check for token reuse (security violation)
        if (isTokenReused(oldToken)) {
            logger.error("SECURITY ALERT: Refresh token reuse detected for user: {}", 
                oldRefreshToken.getUser().getEmail());
            
            // Revoke all tokens for this user as security measure
            revokeAllUserTokens(oldRefreshToken.getUser().getId());
            
            // Log security incident
            logSecurityIncident(oldRefreshToken.getUser().getId(), "REFRESH_TOKEN_REUSE", 
                "Token reuse detected - all user tokens revoked");
            
            throw new SecurityException("Token reuse detected - all tokens revoked");
        }
        
        // Mark token as used for reuse detection
        markTokenAsUsed(oldToken);
        
        // Invalidate old token
        invalidateRefreshToken(oldToken);
        
        // Create new token
        String newTokenValue = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);
        
        RefreshToken newRefreshToken = new RefreshToken(newTokenValue, oldRefreshToken.getUser(), expiresAt);
        // Note: Copy family and session info if entity supports it
        // newRefreshToken.setTokenFamily(oldRefreshToken.getTokenFamily());
        // newRefreshToken.setSessionId(oldRefreshToken.getSessionId());
        // newRefreshToken.setUsageCount(0);
        
        newRefreshToken = refreshTokenRepository.save(newRefreshToken);
        
        // Cache new token
        cacheRefreshToken(newRefreshToken);
        
        logger.info("Refresh token rotated successfully for user: {}", 
            oldRefreshToken.getUser().getEmail());
        
        return newRefreshToken;
    }
    
    /**
     * Validate and retrieve refresh token
     */
    public Optional<RefreshToken> validateRefreshToken(String token) {
        logger.debug("Validating refresh token: {}", token.substring(0, 8) + "...");
        
        // Check if token is blacklisted in Redis
        if (isTokenBlacklisted(token)) {
            logger.warn("Refresh token is blacklisted: {}", token.substring(0, 8) + "...");
            return Optional.empty();
        }
        
        // Try to get from Redis cache first
        RefreshToken cachedToken = getCachedRefreshToken(token);
        if (cachedToken != null) {
            if (cachedToken.isExpired()) {
                logger.warn("Cached refresh token is expired: {}", token.substring(0, 8) + "...");
                invalidateRefreshToken(token);
                return Optional.empty();
            }
            return Optional.of(cachedToken);
        }
        
        // Fallback to database
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(token);
        if (tokenOpt.isPresent()) {
            RefreshToken refreshToken = tokenOpt.get();
            if (refreshToken.isExpired()) {
                logger.warn("Database refresh token is expired: {}", token.substring(0, 8) + "...");
                refreshTokenRepository.delete(refreshToken);
                return Optional.empty();
            }
            
            // Cache the token for future use
            cacheRefreshToken(refreshToken);
            return Optional.of(refreshToken);
        }
        
        logger.warn("Refresh token not found: {}", token.substring(0, 8) + "...");
        return Optional.empty();
    }
    
    /**
     * Invalidate a refresh token (add to blacklist)
     */
    public void invalidateRefreshToken(String token) {
        logger.info("Invalidating refresh token: {}", token.substring(0, 8) + "...");
        
        // Add to Redis blacklist
        blacklistToken(token);
        
        // Remove from cache
        removeCachedRefreshToken(token);
        
        // Remove from database
        refreshTokenRepository.findByToken(token)
            .ifPresent(refreshTokenRepository::delete);
    }
    
    /**
     * Revoke all refresh tokens for a user by username
     */
    public void revokeAllUserTokens(String username) {
        var userOpt = userService.findByUsername(username);
        if (userOpt.isPresent()) {
            revokeAllUserTokens(userOpt.get().getId());
        }
    }
    
    /**
     * Revoke all refresh tokens for a user by user ID
     */
    public void revokeAllUserTokens(Long userId) {
        logger.info("Revoking all refresh tokens for user: {}", userId);
        
        refreshTokenRepository.findByUserId(userId)
            .forEach(token -> {
                blacklistToken(token.getToken());
                removeCachedRefreshToken(token.getToken());
            });
        
        refreshTokenRepository.deleteByUserId(userId);
    }
    
    /**
     * Clean up expired tokens from database and cache
     */
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = refreshTokenRepository.deleteExpiredTokens(now);
            logger.info("Cleaned up {} expired refresh tokens", deletedCount);
        } catch (Exception e) {
            logger.error("Error cleaning up expired tokens: {}", e.getMessage());
        }
        
        // Note: Redis TTL will automatically clean up expired cached tokens
    }
    
    /**
     * Check if token has been reused (security check)
     */
    private boolean isTokenReused(String token) {
        String key = REUSE_DETECTION_PREFIX + token;
        return redisTemplate.hasKey(key);
    }
    
    /**
     * Mark token as used for reuse detection
     */
    private void markTokenAsUsed(String token) {
        String key = REUSE_DETECTION_PREFIX + token;
        // Store for longer than token expiration to detect reuse attempts
        redisTemplate.opsForValue().set(key, "USED", BLACKLIST_RETENTION_DAYS, TimeUnit.DAYS);
    }
    
    /**
     * Log security incidents for monitoring
     */
    private void logSecurityIncident(Long userId, String incidentType, String details) {
        logger.error("SECURITY_INCIDENT: {} - User: {} - {} - {}", 
            LocalDateTime.now(), userId, incidentType, details);
        
        // In production, integrate with SIEM/monitoring system
        // Example: securityEventPublisher.publishSecurityEvent(userId, incidentType, details);
    }
    
    // Private helper methods
    
    private void cacheRefreshToken(RefreshToken token) {
        String key = REDIS_KEY_PREFIX + token.getToken();
        Duration ttl = Duration.between(LocalDateTime.now(), token.getExpiresAt());
        
        redisTemplate.opsForValue().set(key, token, ttl);
        logger.debug("Cached refresh token with TTL: {} seconds", ttl.getSeconds());
    }
    
    private RefreshToken getCachedRefreshToken(String token) {
        String key = REDIS_KEY_PREFIX + token;
        return (RefreshToken) redisTemplate.opsForValue().get(key);
    }
    
    private void removeCachedRefreshToken(String token) {
        String key = REDIS_KEY_PREFIX + token;
        redisTemplate.delete(key);
    }
    
    private void blacklistToken(String token) {
        String key = BLACKLIST_KEY_PREFIX + token;
        // Set with expiration equal to original token expiration
        redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(refreshTokenExpiration));
    }
    
    private boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_KEY_PREFIX + token;
        return redisTemplate.hasKey(key);
    }
}
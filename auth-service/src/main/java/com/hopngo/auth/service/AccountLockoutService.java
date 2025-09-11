package com.hopngo.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class AccountLockoutService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountLockoutService.class);
    
    private static final String FAILED_ATTEMPTS_KEY_PREFIX = "failed_attempts:";
    private static final String LOCKOUT_KEY_PREFIX = "lockout:";
    private static final String IP_FAILED_ATTEMPTS_KEY_PREFIX = "ip_failed_attempts:";
    private static final String IP_LOCKOUT_KEY_PREFIX = "ip_lockout:";
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;
    
    @Value("${app.security.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;
    
    @Value("${app.security.failed-attempts-window-minutes:15}")
    private int failedAttemptsWindowMinutes;
    
    @Value("${app.security.ip-max-failed-attempts:10}")
    private int ipMaxFailedAttempts;
    
    @Value("${app.security.ip-lockout-duration-minutes:60}")
    private int ipLockoutDurationMinutes;
    
    public AccountLockoutService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Record a failed login attempt for a user
     */
    public void recordFailedAttempt(String email, String ipAddress) {
        recordUserFailedAttempt(email);
        recordIpFailedAttempt(ipAddress);
    }
    
    /**
     * Record successful login (clear failed attempts)
     */
    public void recordSuccessfulLogin(String email, String ipAddress) {
        clearUserFailedAttempts(email);
        clearIpFailedAttempts(ipAddress);
    }
    
    /**
     * Check if user account is locked
     */
    public boolean isUserLocked(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey));
    }
    
    /**
     * Check if IP address is locked
     */
    public boolean isIpLocked(String ipAddress) {
        String lockoutKey = IP_LOCKOUT_KEY_PREFIX + ipAddress;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey));
    }
    
    /**
     * Get remaining lockout time for user in seconds
     */
    public long getUserLockoutRemainingTime(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        Long ttl = redisTemplate.getExpire(lockoutKey, TimeUnit.SECONDS);
        return ttl != null ? Math.max(0, ttl) : 0;
    }
    
    /**
     * Get remaining lockout time for IP in seconds
     */
    public long getIpLockoutRemainingTime(String ipAddress) {
        String lockoutKey = IP_LOCKOUT_KEY_PREFIX + ipAddress;
        Long ttl = redisTemplate.getExpire(lockoutKey, TimeUnit.SECONDS);
        return ttl != null ? Math.max(0, ttl) : 0;
    }
    
    /**
     * Get current failed attempts count for user
     */
    public int getUserFailedAttempts(String email) {
        String key = FAILED_ATTEMPTS_KEY_PREFIX + email;
        String attempts = redisTemplate.opsForValue().get(key);
        return attempts != null ? Integer.parseInt(attempts) : 0;
    }
    
    /**
     * Get current failed attempts count for IP
     */
    public int getIpFailedAttempts(String ipAddress) {
        String key = IP_FAILED_ATTEMPTS_KEY_PREFIX + ipAddress;
        String attempts = redisTemplate.opsForValue().get(key);
        return attempts != null ? Integer.parseInt(attempts) : 0;
    }
    
    /**
     * Manually unlock user account (admin function)
     */
    public void unlockUserAccount(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        String failedAttemptsKey = FAILED_ATTEMPTS_KEY_PREFIX + email;
        
        redisTemplate.delete(lockoutKey);
        redisTemplate.delete(failedAttemptsKey);
        
        logger.info("User account unlocked manually: {}", email);
    }
    
    /**
     * Manually unlock IP address (admin function)
     */
    public void unlockIpAddress(String ipAddress) {
        String lockoutKey = IP_LOCKOUT_KEY_PREFIX + ipAddress;
        String failedAttemptsKey = IP_FAILED_ATTEMPTS_KEY_PREFIX + ipAddress;
        
        redisTemplate.delete(lockoutKey);
        redisTemplate.delete(failedAttemptsKey);
        
        logger.info("IP address unlocked manually: {}", ipAddress);
    }
    
    /**
     * Record failed attempt for user
     */
    private void recordUserFailedAttempt(String email) {
        String key = FAILED_ATTEMPTS_KEY_PREFIX + email;
        String lockoutKey = LOCKOUT_KEY_PREFIX + email;
        
        // Increment failed attempts
        Long attempts = redisTemplate.opsForValue().increment(key);
        
        // Set expiration for failed attempts counter
        redisTemplate.expire(key, Duration.ofMinutes(failedAttemptsWindowMinutes));
        
        logger.warn("Failed login attempt {} for user: {}", attempts, email);
        
        // Check if lockout threshold reached
        if (attempts != null && attempts >= maxFailedAttempts) {
            // Lock the account
            redisTemplate.opsForValue().set(lockoutKey, "locked", Duration.ofMinutes(lockoutDurationMinutes));
            
            // Clear failed attempts counter since account is now locked
            redisTemplate.delete(key);
            
            logger.warn("User account locked due to {} failed attempts: {}", attempts, email);
        }
    }
    
    /**
     * Record failed attempt for IP address
     */
    private void recordIpFailedAttempt(String ipAddress) {
        String key = IP_FAILED_ATTEMPTS_KEY_PREFIX + ipAddress;
        String lockoutKey = IP_LOCKOUT_KEY_PREFIX + ipAddress;
        
        // Increment failed attempts
        Long attempts = redisTemplate.opsForValue().increment(key);
        
        // Set expiration for failed attempts counter
        redisTemplate.expire(key, Duration.ofMinutes(failedAttemptsWindowMinutes));
        
        logger.warn("Failed login attempt {} from IP: {}", attempts, ipAddress);
        
        // Check if lockout threshold reached
        if (attempts != null && attempts >= ipMaxFailedAttempts) {
            // Lock the IP
            redisTemplate.opsForValue().set(lockoutKey, "locked", Duration.ofMinutes(ipLockoutDurationMinutes));
            
            // Clear failed attempts counter since IP is now locked
            redisTemplate.delete(key);
            
            logger.warn("IP address locked due to {} failed attempts: {}", attempts, ipAddress);
        }
    }
    
    /**
     * Clear failed attempts for user
     */
    private void clearUserFailedAttempts(String email) {
        String key = FAILED_ATTEMPTS_KEY_PREFIX + email;
        redisTemplate.delete(key);
    }
    
    /**
     * Clear failed attempts for IP
     */
    private void clearIpFailedAttempts(String ipAddress) {
        String key = IP_FAILED_ATTEMPTS_KEY_PREFIX + ipAddress;
        redisTemplate.delete(key);
    }
    
    /**
     * Get lockout status information
     */
    public LockoutStatus getLockoutStatus(String email, String ipAddress) {
        boolean userLocked = isUserLocked(email);
        boolean ipLocked = isIpLocked(ipAddress);
        
        long userLockoutTime = userLocked ? getUserLockoutRemainingTime(email) : 0;
        long ipLockoutTime = ipLocked ? getIpLockoutRemainingTime(ipAddress) : 0;
        
        int userFailedAttempts = userLocked ? 0 : getUserFailedAttempts(email);
        int ipFailedAttempts = ipLocked ? 0 : getIpFailedAttempts(ipAddress);
        
        return new LockoutStatus(
                userLocked, ipLocked,
                userLockoutTime, ipLockoutTime,
                userFailedAttempts, ipFailedAttempts,
                maxFailedAttempts, ipMaxFailedAttempts
        );
    }
    
    /**
     * Lockout status information
     */
    public static class LockoutStatus {
        private final boolean userLocked;
        private final boolean ipLocked;
        private final long userLockoutRemainingSeconds;
        private final long ipLockoutRemainingSeconds;
        private final int userFailedAttempts;
        private final int ipFailedAttempts;
        private final int maxUserFailedAttempts;
        private final int maxIpFailedAttempts;
        
        public LockoutStatus(boolean userLocked, boolean ipLocked,
                           long userLockoutRemainingSeconds, long ipLockoutRemainingSeconds,
                           int userFailedAttempts, int ipFailedAttempts,
                           int maxUserFailedAttempts, int maxIpFailedAttempts) {
            this.userLocked = userLocked;
            this.ipLocked = ipLocked;
            this.userLockoutRemainingSeconds = userLockoutRemainingSeconds;
            this.ipLockoutRemainingSeconds = ipLockoutRemainingSeconds;
            this.userFailedAttempts = userFailedAttempts;
            this.ipFailedAttempts = ipFailedAttempts;
            this.maxUserFailedAttempts = maxUserFailedAttempts;
            this.maxIpFailedAttempts = maxIpFailedAttempts;
        }
        
        // Getters
        public boolean isUserLocked() { return userLocked; }
        public boolean isIpLocked() { return ipLocked; }
        public boolean isLocked() { return userLocked || ipLocked; }
        public long getUserLockoutRemainingSeconds() { return userLockoutRemainingSeconds; }
        public long getIpLockoutRemainingSeconds() { return ipLockoutRemainingSeconds; }
        public int getUserFailedAttempts() { return userFailedAttempts; }
        public int getIpFailedAttempts() { return ipFailedAttempts; }
        public int getMaxUserFailedAttempts() { return maxUserFailedAttempts; }
        public int getMaxIpFailedAttempts() { return maxIpFailedAttempts; }
        
        public int getUserRemainingAttempts() {
            return Math.max(0, maxUserFailedAttempts - userFailedAttempts);
        }
        
        public int getIpRemainingAttempts() {
            return Math.max(0, maxIpFailedAttempts - ipFailedAttempts);
        }
    }
}
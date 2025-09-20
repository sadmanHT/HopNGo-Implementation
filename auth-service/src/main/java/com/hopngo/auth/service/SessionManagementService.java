package com.hopngo.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.hopngo.auth.model.UserSession;
import com.hopngo.auth.repository.UserSessionRepository;

/**
 * Service for managing user sessions and device tracking
 * Provides session listing, revocation, and security monitoring
 */
@Service
public class SessionManagementService {

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final long SESSION_TIMEOUT_HOURS = 24;
    private static final int MAX_SESSIONS_PER_USER = 5;

    /**
     * Create a new user session with device tracking
     */
    public UserSession createSession(Long userId, String deviceInfo, String ipAddress, String userAgent) {
        // Check session limit
        List<UserSession> existingSessions = getActiveSessions(userId);
        if (existingSessions.size() >= MAX_SESSIONS_PER_USER) {
            // Remove oldest session
            UserSession oldestSession = existingSessions.stream()
                .min((s1, s2) -> s1.getLastActivity().compareTo(s2.getLastActivity()))
                .orElse(null);
            if (oldestSession != null) {
                revokeSession(oldestSession.getSessionId());
            }
        }

        UserSession session = new UserSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setDeviceInfo(deviceInfo);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());
        session.setActive(true);
        session.setTwoFactorVerified(false);

        // Save to database
        session = sessionRepository.save(session);

        // Cache in Redis if available
        if (redisTemplate != null) {
            String sessionKey = SESSION_PREFIX + session.getSessionId();
            redisTemplate.opsForValue().set(sessionKey, session, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);

            // Add to user sessions set
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            redisTemplate.opsForSet().add(userSessionsKey, session.getSessionId());
            redisTemplate.expire(userSessionsKey, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
        }

        return session;
    }

    /**
     * Get all active sessions for a user
     */
    public List<UserSession> getActiveSessions(Long userId) {
        return sessionRepository.findByUserIdAndActiveTrue(userId);
    }

    public UserSession getSession(String sessionId) {
        // Try Redis first if available
        if (redisTemplate != null) {
            String sessionKey = SESSION_PREFIX + sessionId;
            UserSession session = (UserSession) redisTemplate.opsForValue().get(sessionKey);
            
            if (session == null) {
                // Fallback to database
                session = sessionRepository.findBySessionIdAndActiveTrue(sessionId);
                if (session != null) {
                    // Re-cache
                    redisTemplate.opsForValue().set(sessionKey, session, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
                }
            }
            
            return session;
        } else {
            // Redis not available, use database only
            return sessionRepository.findBySessionIdAndActiveTrue(sessionId);
        }
    }

    public void updateSessionActivity(String sessionId) {
        UserSession session = getSession(sessionId);
        if (session != null) {
            session.setLastActivity(LocalDateTime.now());
            
            // Update in database
            sessionRepository.save(session);
            
            // Update in Redis if available
            if (redisTemplate != null) {
                String sessionKey = SESSION_PREFIX + sessionId;
                redisTemplate.opsForValue().set(sessionKey, session, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
            }
        }
    }

    public void mark2FAVerified(String sessionId) {
        UserSession session = getSession(sessionId);
        if (session != null) {
            session.setTwoFactorVerified(true);
            session.setTwoFactorVerifiedAt(LocalDateTime.now());
            
            // Update in database
            sessionRepository.save(session);
            
            // Update in Redis if available
            if (redisTemplate != null) {
                String sessionKey = SESSION_PREFIX + sessionId;
                redisTemplate.opsForValue().set(sessionKey, session, SESSION_TIMEOUT_HOURS, TimeUnit.HOURS);
            }
        }
    }

    public boolean revokeSession(String sessionId) {
        UserSession session = getSession(sessionId);
        if (session != null) {
            // Mark as inactive in database
            session.setActive(false);
            session.setRevokedAt(LocalDateTime.now());
            sessionRepository.save(session);
            
            // Remove from Redis if available
            if (redisTemplate != null) {
                String sessionKey = SESSION_PREFIX + sessionId;
                redisTemplate.delete(sessionKey);
                
                // Remove from user sessions set
                String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId();
                redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
            }
            
            return true;
        }
        return false;
    }

    /**
     * Revoke all sessions for a user
     */
    public void invalidateAllUserSessions(String username) {
        // This would need to be implemented with user lookup
        // For now, assuming we have userId
    }

    public void invalidateAllUserSessions(Long userId) {
        List<UserSession> sessions = getActiveSessions(userId);
        for (UserSession session : sessions) {
            revokeSession(session.getSessionId());
        }
        
        // Clear user sessions set if Redis available
        if (redisTemplate != null) {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            redisTemplate.delete(userSessionsKey);
        }
    }

    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(SESSION_TIMEOUT_HOURS);
        List<UserSession> expiredSessions = sessionRepository.findByLastActivityBeforeAndActiveTrue(cutoff);
        
        for (UserSession session : expiredSessions) {
            revokeSession(session.getSessionId());
        }
    }

    /**
     * Get session statistics for monitoring
     */
    public SessionStats getSessionStats(Long userId) {
        List<UserSession> sessions = getActiveSessions(userId);
        
        SessionStats stats = new SessionStats();
        stats.setTotalActiveSessions(sessions.size());
        stats.setVerified2FASessions((int) sessions.stream().filter(UserSession::isTwoFactorVerified).count());
        stats.setUniqueDevices((int) sessions.stream().map(UserSession::getDeviceInfo).distinct().count());
        stats.setUniqueIPs((int) sessions.stream().map(UserSession::getIpAddress).distinct().count());
        
        return stats;
    }

    public static class SessionStats {
        private int totalActiveSessions;
        private int verified2FASessions;
        private int uniqueDevices;
        private int uniqueIPs;

        // Getters and setters
        public int getTotalActiveSessions() { return totalActiveSessions; }
        public void setTotalActiveSessions(int totalActiveSessions) { this.totalActiveSessions = totalActiveSessions; }
        
        public int getVerified2FASessions() { return verified2FASessions; }
        public void setVerified2FASessions(int verified2FASessions) { this.verified2FASessions = verified2FASessions; }
        
        public int getUniqueDevices() { return uniqueDevices; }
        public void setUniqueDevices(int uniqueDevices) { this.uniqueDevices = uniqueDevices; }
        
        public int getUniqueIPs() { return uniqueIPs; }
        public void setUniqueIPs(int uniqueIPs) { this.uniqueIPs = uniqueIPs; }
    }
}
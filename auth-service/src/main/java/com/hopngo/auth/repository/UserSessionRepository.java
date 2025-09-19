package com.hopngo.auth.repository;

import com.hopngo.auth.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for UserSession entity
 * Provides database operations for user session management
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    /**
     * Find all active sessions for a specific user
     */
    List<UserSession> findByUserIdAndActiveTrue(Long userId);
    
    /**
     * Find an active session by session ID
     */
    UserSession findBySessionIdAndActiveTrue(String sessionId);
    
    /**
     * Find sessions that have been inactive since before the specified time
     */
    List<UserSession> findByLastActivityBeforeAndActiveTrue(LocalDateTime cutoffTime);
    
    /**
     * Find session by session ID (regardless of active status)
     */
    UserSession findBySessionId(String sessionId);
    
    /**
     * Find all sessions for a user (regardless of active status)
     */
    List<UserSession> findByUserId(Long userId);
}
package com.hopngo.auth.repository;

import com.hopngo.auth.entity.RefreshToken;
import com.hopngo.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    /**
     * Find refresh token by token string
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * Find all refresh tokens for a user
     */
    List<RefreshToken> findByUser(User user);
    
    /**
     * Find all refresh tokens for a user ID
     */
    List<RefreshToken> findByUserId(Long userId);
    
    /**
     * Find valid (non-expired) refresh tokens for a user
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    /**
     * Find expired refresh tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt <= :now")
    List<RefreshToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Delete all refresh tokens for a user
     */
    @Modifying
    @Transactional
    void deleteByUser(User user);
    
    /**
     * Delete all refresh tokens for a user ID
     */
    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
    
    /**
     * Delete refresh token by token string
     */
    @Modifying
    @Transactional
    void deleteByToken(String token);
    
    /**
     * Delete all expired refresh tokens
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt <= :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Count refresh tokens for a user
     */
    long countByUser(User user);
    
    /**
     * Count valid refresh tokens for a user
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.expiresAt > :now")
    long countValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    /**
     * Check if token exists and is valid
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END " +
           "FROM RefreshToken rt WHERE rt.token = :token AND rt.expiresAt > :now")
    boolean existsByTokenAndNotExpired(@Param("token") String token, @Param("now") LocalDateTime now);
}
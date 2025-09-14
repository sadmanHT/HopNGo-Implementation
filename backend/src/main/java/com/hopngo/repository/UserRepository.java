package com.hopngo.repository;

import com.hopngo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by email address
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find user by email address (case insensitive)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if email exists (case insensitive)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);
    
    /**
     * Find active users
     */
    List<User> findByIsActiveTrue();
    
    /**
     * Find verified users
     */
    List<User> findByIsVerifiedTrue();
    
    /**
     * Find users by verification status
     */
    List<User> findByIsVerified(Boolean isVerified);
    
    /**
     * Find users created after a specific date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Find users created between dates
     */
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find users who logged in recently
     */
    List<User> findByLastLoginAtAfter(LocalDateTime date);
    
    /**
     * Find users with deletion requests
     */
    List<User> findByDeletionRequestedAtIsNotNull();
    
    /**
     * Find users scheduled for deletion
     */
    List<User> findByDeletionScheduledAtIsNotNull();
    
    /**
     * Find users scheduled for deletion before a specific date
     */
    List<User> findByDeletionScheduledAtBefore(LocalDateTime date);
    
    /**
     * Find users by phone number
     */
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    /**
     * Search users by name (first name or last name)
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchByName(@Param("searchTerm") String searchTerm);
    
    /**
     * Find users by preferred language
     */
    List<User> findByPreferredLanguage(String language);
    
    /**
     * Find users by timezone
     */
    List<User> findByTimezone(String timezone);
    
    /**
     * Update user's last login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);
    
    /**
     * Update user's email verification status
     */
    @Modifying
    @Query("UPDATE User u SET u.isVerified = :verified, u.emailVerifiedAt = :verifiedAt WHERE u.id = :userId")
    void updateEmailVerification(@Param("userId") Long userId, 
                                @Param("verified") Boolean verified, 
                                @Param("verifiedAt") LocalDateTime verifiedAt);
    
    /**
     * Update user's phone verification status
     */
    @Modifying
    @Query("UPDATE User u SET u.phoneVerifiedAt = :verifiedAt WHERE u.id = :userId")
    void updatePhoneVerification(@Param("userId") Long userId, @Param("verifiedAt") LocalDateTime verifiedAt);
    
    /**
     * Schedule user for deletion
     */
    @Modifying
    @Query("UPDATE User u SET u.deletionRequestedAt = :requestedAt, " +
           "u.deletionScheduledAt = :scheduledAt, u.deletionReason = :reason " +
           "WHERE u.id = :userId")
    void scheduleForDeletion(@Param("userId") Long userId, 
                            @Param("requestedAt") LocalDateTime requestedAt,
                            @Param("scheduledAt") LocalDateTime scheduledAt,
                            @Param("reason") String reason);
    
    /**
     * Cancel deletion request
     */
    @Modifying
    @Query("UPDATE User u SET u.deletionRequestedAt = null, " +
           "u.deletionScheduledAt = null, u.deletionReason = null " +
           "WHERE u.id = :userId")
    void cancelDeletionRequest(@Param("userId") Long userId);
    
    /**
     * Soft delete user (mark as inactive)
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = false WHERE u.id = :userId")
    void softDeleteUser(@Param("userId") Long userId);
    
    /**
     * Reactivate user
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = true WHERE u.id = :userId")
    void reactivateUser(@Param("userId") Long userId);
    
    /**
     * Count active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();
    
    /**
     * Count verified users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isVerified = true")
    long countVerifiedUsers();
    
    /**
     * Count users registered in the last N days
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :sinceDate")
    long countUsersRegisteredSince(@Param("sinceDate") LocalDateTime sinceDate);
    
    /**
     * Count users created after a specific date
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    long countUsersCreatedAfter(@Param("startDate") LocalDateTime startDate);

    // Account deletion related queries
    @Query("SELECT u FROM User u WHERE u.scheduledForDeletionAt IS NOT NULL AND u.scheduledForDeletionAt <= :cutoffDate AND u.deletedAt IS NULL")
    List<User> findUsersScheduledForDeletion(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL AND u.deletedAt <= :cutoffDate")
    List<User> findUsersForHardDeletion(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT COUNT(u) FROM User u WHERE u.scheduledForDeletionAt IS NOT NULL AND u.deletedAt IS NULL")
    long countUsersScheduledForDeletion();

    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NOT NULL")
    long countSoftDeletedUsers();
    
    /**
     * Count users with pending deletion
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletionRequestedAt IS NOT NULL")
    long countUsersWithPendingDeletion();
    
    /**
     * Find users for data retention cleanup (inactive for specified period)
     */
    @Query("SELECT u FROM User u WHERE u.isActive = false AND " +
           "(u.lastLoginAt IS NULL OR u.lastLoginAt < :cutoffDate) AND " +
           "u.createdAt < :cutoffDate")
    List<User> findUsersForRetentionCleanup(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find users who haven't logged in for a specified period
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(u.lastLoginAt IS NULL OR u.lastLoginAt < :cutoffDate)")
    List<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
}
package com.hopngo.notification.repository;

import com.hopngo.notification.entity.WebPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {
    
    /**
     * Find all active subscriptions for a user
     */
    List<WebPushSubscription> findByUserIdAndIsActiveTrue(String userId);
    
    /**
     * Find all subscriptions for a user (active and inactive)
     */
    List<WebPushSubscription> findByUserId(String userId);
    
    /**
     * Find subscription by user ID and endpoint
     */
    Optional<WebPushSubscription> findByUserIdAndEndpoint(String userId, String endpoint);
    
    /**
     * Find subscription by endpoint only
     */
    Optional<WebPushSubscription> findByEndpoint(String endpoint);
    
    /**
     * Check if a subscription exists for user and endpoint
     */
    boolean existsByUserIdAndEndpoint(String userId, String endpoint);
    
    /**
     * Deactivate all subscriptions for a user
     */
    @Modifying
    @Transactional
    @Query("UPDATE WebPushSubscription w SET w.isActive = false WHERE w.userId = :userId")
    int deactivateAllByUserId(@Param("userId") String userId);
    
    /**
     * Deactivate a specific subscription by endpoint
     */
    @Modifying
    @Transactional
    @Query("UPDATE WebPushSubscription w SET w.isActive = false WHERE w.endpoint = :endpoint")
    int deactivateByEndpoint(@Param("endpoint") String endpoint);
    
    /**
     * Delete inactive subscriptions older than specified days
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM WebPushSubscription w WHERE w.isActive = false AND w.updatedAt < :cutoffDate")
    int deleteInactiveOlderThan(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
    
    /**
     * Count active subscriptions for a user
     */
    @Query("SELECT COUNT(w) FROM WebPushSubscription w WHERE w.userId = :userId AND w.isActive = true")
    long countActiveByUserId(@Param("userId") String userId);
    
    /**
     * Find all active subscriptions (for broadcast notifications)
     */
    List<WebPushSubscription> findByIsActiveTrue();
}
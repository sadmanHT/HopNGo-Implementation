package com.hopngo.market.repository;

import com.hopngo.market.entity.WebhookEvent;
import com.hopngo.market.entity.WebhookEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for WebhookEvent entity operations.
 * Provides methods for webhook event persistence and querying.
 */
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    
    /**
     * Find webhook event by webhook ID for idempotency checking.
     */
    Optional<WebhookEvent> findByWebhookId(String webhookId);
    
    /**
     * Find webhook event by webhook ID and provider for idempotency checking.
     */
    Optional<WebhookEvent> findByWebhookIdAndProvider(String webhookId, String provider);
    
    /**
     * Check if webhook event exists by webhook ID.
     */
    boolean existsByWebhookId(String webhookId);
    
    /**
     * Find webhook events by provider.
     */
    List<WebhookEvent> findByProvider(String provider);
    
    /**
     * Find webhook events by status.
     */
    Page<WebhookEvent> findByStatusOrderByCreatedAtDesc(WebhookEventStatus status, Pageable pageable);
    
    /**
     * Find webhook events by provider and status.
     */
    Page<WebhookEvent> findByProviderAndStatusOrderByCreatedAtDesc(String provider, WebhookEventStatus status, Pageable pageable);
    
    /**
     * Find webhook events by payment ID.
     */
    List<WebhookEvent> findByPaymentId(UUID paymentId);
    
    /**
     * Find webhook events by order ID.
     */
    List<WebhookEvent> findByOrderId(UUID orderId);
    
    /**
     * Find failed webhook events that can be retried.
     */
    @Query("SELECT w FROM WebhookEvent w WHERE w.status = :status AND w.retryCount < :maxRetries ORDER BY w.createdAt ASC")
    List<WebhookEvent> findRetryableEvents(@Param("status") WebhookEventStatus status, @Param("maxRetries") int maxRetries);
    
    /**
     * Find unprocessed webhook events (RECEIVED or PROCESSING status).
     */
    @Query("SELECT w FROM WebhookEvent w WHERE w.status IN ('RECEIVED', 'PROCESSING') ORDER BY w.createdAt ASC")
    List<WebhookEvent> findUnprocessedEvents();
    
    /**
     * Find webhook events created after a specific date.
     */
    List<WebhookEvent> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAfter);
    
    /**
     * Count webhook events by status.
     */
    long countByStatus(WebhookEventStatus status);
    
    /**
     * Count webhook events by provider.
     */
    long countByProvider(String provider);
    
    /**
     * Count webhook events by provider and status.
     */
    long countByProviderAndStatus(String provider, WebhookEventStatus status);
    
    /**
     * Delete old processed webhook events (cleanup).
     */
    @Query("DELETE FROM WebhookEvent w WHERE w.status = 'PROCESSED' AND w.createdAt < :cutoffDate")
    void deleteOldProcessedEvents(@Param("cutoffDate") LocalDateTime cutoffDate);
}
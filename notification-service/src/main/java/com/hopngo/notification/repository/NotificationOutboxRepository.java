package com.hopngo.notification.repository;

import com.hopngo.notification.entity.NotificationOutbox;
import com.hopngo.notification.entity.NotificationOutbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    
    // Find by event ID
    Optional<NotificationOutbox> findByEventId(String eventId);
    
    // Find by status
    List<NotificationOutbox> findByStatus(OutboxStatus status);
    
    // Find retryable outbox entries
    @Query("SELECT o FROM NotificationOutbox o WHERE o.status = 'RETRY' AND o.retryCount < o.maxRetries AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now)")
    List<NotificationOutbox> findRetryableEntries(@Param("now") LocalDateTime now);
    
    // Find pending entries
    List<NotificationOutbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
    
    // Find failed entries
    @Query("SELECT o FROM NotificationOutbox o WHERE o.status = 'FAILED' AND o.retryCount >= o.maxRetries")
    List<NotificationOutbox> findFailedEntries();
    
    // Find entries by event type
    List<NotificationOutbox> findByEventType(String eventType);
    
    // Find entries created within a time range
    List<NotificationOutbox> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Count by status
    long countByStatus(OutboxStatus status);
    
    // Find stale pending entries
    @Query("SELECT o FROM NotificationOutbox o WHERE o.status = 'PENDING' AND o.createdAt < :cutoffTime")
    List<NotificationOutbox> findStaleEntries(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Delete old processed entries
    @Query("DELETE FROM NotificationOutbox o WHERE o.status = 'PROCESSED' AND o.processedAt < :cutoffTime")
    void deleteOldProcessedEntries(@Param("cutoffTime") LocalDateTime cutoffTime);
}
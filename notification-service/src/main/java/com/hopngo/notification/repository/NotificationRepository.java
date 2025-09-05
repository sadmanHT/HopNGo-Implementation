package com.hopngo.notification.repository;

import com.hopngo.notification.entity.Notification;
import com.hopngo.notification.entity.NotificationStatus;
import com.hopngo.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Find notifications by recipient
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
    
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);
    
    // Find notifications by status
    List<Notification> findByStatus(NotificationStatus status);
    
    // Find notifications that can be retried
    @Query("SELECT n FROM Notification n WHERE n.status = 'RETRY' AND n.retryCount < n.maxRetries AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= :now)")
    List<Notification> findRetryableNotifications(@Param("now") LocalDateTime now);
    
    // Find notifications by event ID
    Optional<Notification> findByEventId(String eventId);
    
    List<Notification> findByEventIdAndEventType(String eventId, String eventType);
    
    // Find notifications by type and status
    List<Notification> findByTypeAndStatus(NotificationType type, NotificationStatus status);
    
    // Find failed notifications
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount >= n.maxRetries")
    List<Notification> findFailedNotifications();
    
    // Find notifications created within a time range
    List<Notification> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Count notifications by status
    long countByStatus(NotificationStatus status);
    
    // Count notifications by recipient and status
    long countByRecipientIdAndStatus(String recipientId, NotificationStatus status);
    
    // Find pending notifications older than specified time
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.createdAt < :cutoffTime")
    List<Notification> findStaleNotifications(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Delete old processed notifications
    @Query("DELETE FROM Notification n WHERE n.status = 'SENT' AND n.sentAt < :cutoffTime")
    void deleteOldProcessedNotifications(@Param("cutoffTime") LocalDateTime cutoffTime);
}
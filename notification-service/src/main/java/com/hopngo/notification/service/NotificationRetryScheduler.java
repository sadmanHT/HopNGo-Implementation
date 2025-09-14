package com.hopngo.notification.service;

import com.hopngo.notification.dto.PushNotificationRequest;
import com.hopngo.notification.entity.Notification;
import com.hopngo.notification.entity.NotificationStatus;
import com.hopngo.notification.repository.NotificationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationRetryScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationRetryScheduler.class);

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final FirebaseMessagingService firebaseMessagingService;
    
    public NotificationRetryScheduler(NotificationRepository notificationRepository, 
                                     NotificationService notificationService, 
                                     EmailService emailService, 
                                     SmsService smsService,
                                     FirebaseMessagingService firebaseMessagingService) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.firebaseMessagingService = firebaseMessagingService;
    }

    @Value("${notification.retry.max-attempts:5}")
    private int maxRetryAttempts;

    @Value("${notification.retry.initial-delay:60000}")
    private long initialRetryDelay; // 1 minute

    @Value("${notification.retry.max-delay:3600000}")
    private long maxRetryDelay; // 1 hour

    @Value("${notification.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${notification.retry.batch-size:50}")
    private int batchSize;

    /**
     * Scheduled task to retry failed notifications
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void retryFailedNotifications() {
        log.info("Starting retry process for failed notifications");
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(1);
            Pageable pageable = PageRequest.of(0, batchSize);
            
            List<Notification> failedNotifications = notificationRepository
                    .findFailedNotificationsForRetry(cutoffTime, maxRetryAttempts, pageable);
            
            if (failedNotifications.isEmpty()) {
                log.debug("No failed notifications found for retry");
                return;
            }
            
            log.info("Found {} failed notifications to retry", failedNotifications.size());
            
            // Process notifications asynchronously
            List<CompletableFuture<Void>> futures = failedNotifications.stream()
                    .map(this::retryNotificationAsync)
                    .toList();
            
            // Wait for all retries to complete (with timeout)
            CompletableFuture<Void> allRetries = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );
            
            try {
                allRetries.get(30, TimeUnit.SECONDS);
                log.info("Completed retry process for {} notifications", failedNotifications.size());
            } catch (Exception e) {
                log.warn("Some notification retries did not complete within timeout", e);
            }
            
        } catch (Exception e) {
            log.error("Error during notification retry process", e);
        }
    }

    /**
     * Scheduled task to clean up old notifications
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Starting cleanup of old notifications");
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            
            // Delete old sent notifications
            int deletedSent = notificationRepository.deleteOldNotifications(
                    cutoffDate, NotificationStatus.SENT
            );
            
            // Delete old failed notifications that exceeded max retry attempts
            int deletedFailed = notificationRepository.deleteOldFailedNotifications(
                    cutoffDate, maxRetryAttempts
            );
            
            log.info("Cleanup completed: {} sent notifications and {} failed notifications deleted", 
                    deletedSent, deletedFailed);
            
        } catch (Exception e) {
            log.error("Error during notification cleanup", e);
        }
    }

    /**
     * Scheduled task to update notification statistics
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void updateNotificationStatistics() {
        try {
            LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
            
            long totalSent = notificationRepository.countByStatus(NotificationStatus.SENT);
            long totalFailed = notificationRepository.countByStatus(NotificationStatus.FAILED);
            long totalPending = notificationRepository.countByStatus(NotificationStatus.PENDING);
            
            log.info("Notification statistics (last 24h): Sent={}, Failed={}, Pending={}", 
                    totalSent, totalFailed, totalPending);
            
            // Calculate success rate
            long total = totalSent + totalFailed;
            if (total > 0) {
                double successRate = (double) totalSent / total * 100;
                log.info("Notification success rate (last 24h): {:.2f}%", successRate);
                
                // Alert if success rate is too low
                if (successRate < 90.0) {
                    log.warn("Low notification success rate detected: {:.2f}%", successRate);
                }
            }
            
        } catch (Exception e) {
            log.error("Error updating notification statistics", e);
        }
    }

    private CompletableFuture<Void> retryNotificationAsync(Notification notification) {
        return CompletableFuture.runAsync(() -> {
            try {
                retryNotification(notification);
            } catch (Exception e) {
                log.error("Error retrying notification {}", notification.getId(), e);
            }
        });
    }

    @Transactional
    public void retryNotification(Notification notification) {
        log.debug("Retrying notification: {}", notification.getId());
        
        try {
            // Check if enough time has passed since last attempt
            if (!shouldRetryNow(notification)) {
                log.debug("Not yet time to retry notification {}", notification.getId());
                return;
            }
            
            // Increment retry count
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setLastAttemptAt(LocalDateTime.now());
            notification.setStatus(NotificationStatus.PENDING);
            
            // Calculate next retry time with exponential backoff
            long nextRetryDelay = calculateNextRetryDelay(notification.getRetryCount());
            notification.setNextRetryAt(LocalDateTime.now().plusSeconds(nextRetryDelay / 1000));
            
            // Save updated notification
            notificationRepository.save(notification);
            
            // Attempt to send the notification
            boolean success = attemptNotificationDelivery(notification);
            
            if (success) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notification.setNextRetryAt(null);
                log.info("Successfully retried notification {} after {} attempts", 
                        notification.getId(), notification.getRetryCount());
            } else {
                // Check if we've exceeded max retry attempts
                if (notification.getRetryCount() >= maxRetryAttempts) {
                    notification.setStatus(NotificationStatus.FAILED);
                    notification.setNextRetryAt(null);
                    log.warn("Notification {} failed permanently after {} attempts", 
                            notification.getId(), notification.getRetryCount());
                } else {
                    notification.setStatus(NotificationStatus.FAILED);
                    log.debug("Notification {} retry failed, will retry again later (attempt {}/{})", 
                            notification.getId(), notification.getRetryCount(), maxRetryAttempts);
                }
            }
            
            notificationRepository.save(notification);
            
        } catch (Exception e) {
            log.error("Error during notification retry for {}", notification.getId(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
        }
    }

    private boolean shouldRetryNow(Notification notification) {
        if (notification.getNextRetryAt() == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(notification.getNextRetryAt());
    }

    private long calculateNextRetryDelay(int retryCount) {
        long delay = (long) (initialRetryDelay * Math.pow(retryMultiplier, retryCount - 1));
        return Math.min(delay, maxRetryDelay);
    }

    private boolean attemptNotificationDelivery(Notification notification) {
        try {
            // Determine delivery method based on channel
            String channel = notification.getChannel();
            if ("email".equalsIgnoreCase(channel) && notification.getRecipientEmail() != null) {
                emailService.sendEmail(
                        notification.getRecipientEmail(),
                        notification.getSubject(),
                        notification.getContent()
                );
                return true;
            } else if ("sms".equalsIgnoreCase(channel) && notification.getRecipientPhone() != null) {
                smsService.sendSms(
                        notification.getRecipientPhone(),
                        notification.getContent()
                );
                return true;
            } else if ("push".equalsIgnoreCase(channel) && notification.getDeviceToken() != null && firebaseMessagingService.isEnabled()) {
                 PushNotificationRequest pushRequest = PushNotificationRequest.builder()
                         .token(notification.getDeviceToken())
                         .title(notification.getSubject())
                         .body(notification.getContent())
                         .build();
                 firebaseMessagingService.sendNotification(pushRequest);
                 return true;
            } else {
                log.warn("Unknown notification channel or missing recipient info: {}", channel);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to deliver notification {} via {}", 
                    notification.getId(), notification.getChannel(), e);
            return false;
        }
    }
}
package com.hopngo.notification.channel;

import com.hopngo.notification.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Push notification channel stub implementation
 * Currently logs messages instead of sending actual push notifications
 * TODO: Integrate with FCM/WebPush providers
 */
@Component
public class PushNotificationChannel implements NotificationChannel {
    
    private static final Logger logger = LoggerFactory.getLogger(PushNotificationChannel.class);
    
    @Override
    public boolean supports(String channel) {
        return "PUSH".equalsIgnoreCase(channel);
    }
    
    @Override
    public void send(Notification notification) throws Exception {
        if (notification.getRecipientId() == null || notification.getRecipientId().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient ID is required for push notifications");
        }
        
        // Simulate push notification sending by logging
        logger.info("[PUSH STUB] Sending push notification to user: {}", notification.getRecipientId());
        logger.info("[PUSH STUB] Title: {}", notification.getSubject());
        logger.info("[PUSH STUB] Body: {}", notification.getContent());
        logger.info("[PUSH STUB] Notification ID: {}", notification.getId());
        logger.info("[PUSH STUB] Type: {}", notification.getType());
        
        // Log additional metadata if available
        if (notification.getVariables() != null && !notification.getVariables().isEmpty()) {
            logger.info("[PUSH STUB] Metadata: {}", notification.getVariables());
        }
        
        // Simulate processing delay
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate occasional failures for testing retry logic
        if (notification.getRetryCount() == 0 && Math.random() < 0.05) {
            logger.warn("[PUSH STUB] Simulated failure for notification: {}", notification.getId());
            throw new Exception("Simulated push notification service failure");
        }
        
        logger.info("[PUSH STUB] Push notification sent successfully to user: {} for notification: {}", 
                   notification.getRecipientId(), notification.getId());
    }
    
    @Override
    public String getChannelName() {
        return "PUSH";
    }
    
    @Override
    public boolean isAvailable() {
        // Always available for stub implementation
        return true;
    }
    
    @Override
    public int getMaxRetries() {
        return 3;
    }
    
    @Override
    public long getRetryDelayMs() {
        return 2000; // 2 seconds
    }
}
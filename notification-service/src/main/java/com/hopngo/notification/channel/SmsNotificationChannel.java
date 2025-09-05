package com.hopngo.notification.channel;

import com.hopngo.notification.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SMS notification channel stub implementation
 * Currently logs messages instead of sending actual SMS
 * TODO: Integrate with SMS gateway provider
 */
@Component
public class SmsNotificationChannel implements NotificationChannel {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationChannel.class);
    
    @Override
    public boolean supports(String channel) {
        return "SMS".equalsIgnoreCase(channel);
    }
    
    @Override
    public void send(Notification notification) throws Exception {
        if (notification.getRecipientPhone() == null || notification.getRecipientPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient phone number is required for SMS notifications");
        }
        
        // Simulate SMS sending by logging
        logger.info("[SMS STUB] Sending SMS to: {}", notification.getRecipientPhone());
        logger.info("[SMS STUB] Message: {}", notification.getContent());
        logger.info("[SMS STUB] Notification ID: {}", notification.getId());
        logger.info("[SMS STUB] Type: {}", notification.getType());
        
        // Simulate processing delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate occasional failures for testing retry logic
        if (notification.getRetryCount() == 0 && Math.random() < 0.1) {
            logger.warn("[SMS STUB] Simulated failure for notification: {}", notification.getId());
            throw new Exception("Simulated SMS gateway failure");
        }
        
        logger.info("[SMS STUB] SMS sent successfully to: {} for notification: {}", 
                   notification.getRecipientPhone(), notification.getId());
    }
    
    @Override
    public String getChannelName() {
        return "SMS";
    }
    
    @Override
    public boolean isAvailable() {
        // Always available for stub implementation
        return true;
    }
    
    @Override
    public int getMaxRetries() {
        return 2;
    }
    
    @Override
    public long getRetryDelayMs() {
        return 3000; // 3 seconds
    }
}
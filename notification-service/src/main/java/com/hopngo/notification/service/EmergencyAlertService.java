package com.hopngo.notification.service;

import com.hopngo.notification.dto.PushNotificationRequest;
import com.hopngo.notification.entity.Notification;
import com.hopngo.notification.entity.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class EmergencyAlertService {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyAlertService.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    @Autowired(required = false)
    private FirebaseMessagingService firebaseMessagingService;

    /**
     * Send emergency alert to a single user across all available channels
     */
    @Async
    @Retryable(
        value = {RuntimeException.class},
        maxAttempts = 10,
        backoff = @Backoff(delay = 500, multiplier = 1.5, maxDelay = 5000)
    )
    public CompletableFuture<Void> sendEmergencyAlert(
            String userId, 
            String email, 
            String phoneNumber, 
            String pushToken,
            String alertTitle, 
            String alertMessage,
            EmergencyAlertType alertType) {
        
        logger.warn("EMERGENCY ALERT: Sending {} alert to user {}: {}", alertType, userId, alertTitle);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("alertType", alertType.toString());
        variables.put("timestamp", LocalDateTime.now().toString());
        variables.put("severity", "CRITICAL");
        
        try {
            // Send via all channels simultaneously
            CompletableFuture<Void> emailFuture = sendEmergencyEmail(userId, email, alertTitle, alertMessage, variables);
            CompletableFuture<Void> smsFuture = sendEmergencySms(userId, phoneNumber, alertTitle, alertMessage);
            CompletableFuture<Void> pushFuture = sendEmergencyPush(userId, pushToken, alertTitle, alertMessage, alertType);
            
            // Wait for all channels to complete
            CompletableFuture.allOf(emailFuture, smsFuture, pushFuture).join();
            
            logger.info("Emergency alert sent successfully to user {} via all channels", userId);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("Failed to send emergency alert to user {}", userId, e);
            throw new RuntimeException("Emergency alert delivery failed", e);
        }
    }
    
    /**
     * Send emergency alert to multiple users (broadcast)
     */
    @Async
    public CompletableFuture<Void> broadcastEmergencyAlert(
            List<EmergencyContact> contacts,
            String alertTitle,
            String alertMessage,
            EmergencyAlertType alertType) {
        
        logger.warn("EMERGENCY BROADCAST: Sending {} alert to {} recipients: {}", 
                   alertType, contacts.size(), alertTitle);
        
        List<CompletableFuture<Void>> futures = contacts.stream()
            .map(contact -> sendEmergencyAlert(
                contact.getUserId(),
                contact.getEmail(),
                contact.getPhoneNumber(),
                contact.getPushToken(),
                alertTitle,
                alertMessage,
                alertType
            ))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    @Async
    private CompletableFuture<Void> sendEmergencyEmail(String userId, String email, String title, String message, Map<String, Object> variables) {
        if (email == null || email.trim().isEmpty()) {
            logger.warn("No email provided for emergency alert to user {}", userId);
            return CompletableFuture.completedFuture(null);
        }
        
        try {
            Notification notification = notificationService.createNotification(
                userId,
                email,
                null,
                NotificationType.EMERGENCY_ALERT,
                "EMAIL",
                "emergency-alert",
                "🚨 EMERGENCY: " + title,
                message,
                variables,
                "emergency-" + System.currentTimeMillis(),
                "emergency.alert"
            );
            
            notificationService.processNotification(notification);
            logger.info("Emergency email sent to user {}", userId);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("Failed to send emergency email to user {}", userId, e);
            throw new RuntimeException("Emergency email failed", e);
        }
    }
    
    @Async
    private CompletableFuture<Void> sendEmergencySms(String userId, String phoneNumber, String title, String message) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            logger.warn("No phone number provided for emergency alert to user {}", userId);
            return CompletableFuture.completedFuture(null);
        }
        
        try {
            String smsMessage = String.format("🚨 EMERGENCY: %s\n%s", title, message);
            smsService.sendSms(phoneNumber, smsMessage);
            logger.info("Emergency SMS sent to user {}", userId);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("Failed to send emergency SMS to user {}", userId, e);
            throw new RuntimeException("Emergency SMS failed", e);
        }
    }
    
    @Async
    private CompletableFuture<Void> sendEmergencyPush(String userId, String pushToken, String title, String message, EmergencyAlertType alertType) {
        if (pushToken == null || pushToken.trim().isEmpty() || firebaseMessagingService == null) {
            logger.warn("No push token provided or FCM not configured for emergency alert to user {}", userId);
            return CompletableFuture.completedFuture(null);
        }
        
        try {
            PushNotificationRequest pushRequest = PushNotificationRequest.builder()
                .token(pushToken)
                .title("🚨 EMERGENCY ALERT")
                .body(title + ": " + message)
                .priority("high")
                .sound("emergency")
                .badge(1)
                .contentAvailable(true)
                .data(Map.of(
                    "userId", userId,
                    "alertType", alertType.toString(),
                    "severity", "CRITICAL",
                    "timestamp", LocalDateTime.now().toString(),
                    "requiresAction", "true"
                ))
                .build();
                
            firebaseMessagingService.sendNotification(pushRequest);
            logger.info("Emergency push notification sent to user {}", userId);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("Failed to send emergency push notification to user {}", userId, e);
            throw new RuntimeException("Emergency push notification failed", e);
        }
    }
    
    @Recover
    public CompletableFuture<Void> recoverFromEmergencyAlertFailure(
            RuntimeException ex, 
            String userId, 
            String email, 
            String phoneNumber, 
            String pushToken,
            String alertTitle, 
            String alertMessage,
            EmergencyAlertType alertType) {
        
        logger.error("CRITICAL: All retry attempts exhausted for emergency alert to user: {}. Alert: {}", 
                    userId, alertTitle, ex);
        
        // In a real system, this would trigger:
        // 1. Escalation to system administrators
        // 2. Alternative communication channels (e.g., phone call)
        // 3. Incident management system notification
        // 4. Dead letter queue for manual processing
        
        // For now, log the critical failure
        logger.error("EMERGENCY ALERT DELIVERY FAILED - MANUAL INTERVENTION REQUIRED for user: {} - Alert: {}", 
                   userId, alertTitle);
        
        return CompletableFuture.completedFuture(null);
    }
    
    public enum EmergencyAlertType {
        SECURITY_BREACH,
        SYSTEM_OUTAGE,
        PAYMENT_FRAUD,
        ACCOUNT_COMPROMISE,
        SERVICE_DISRUPTION,
        MAINTENANCE_URGENT,
        SAFETY_ALERT,
        BOOKING_EMERGENCY
    }
    
    public static class EmergencyContact {
        private String userId;
        private String email;
        private String phoneNumber;
        private String pushToken;
        
        public EmergencyContact(String userId, String email, String phoneNumber, String pushToken) {
            this.userId = userId;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.pushToken = pushToken;
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getPushToken() { return pushToken; }
    }
}
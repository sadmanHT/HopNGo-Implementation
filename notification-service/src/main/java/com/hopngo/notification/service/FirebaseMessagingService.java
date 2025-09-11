package com.hopngo.notification.service;

import com.google.firebase.messaging.*;
import com.hopngo.notification.dto.PushNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class FirebaseMessagingService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseMessagingService.class);

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    @Autowired
    private boolean isFirebaseEnabled;

    @Value("${firebase.default-icon:https://hopngo.com/icon-192x192.png}")
    private String defaultIcon;

    @Value("${firebase.default-click-action:https://hopngo.com}")
    private String defaultClickAction;

    @Retryable(
        value = {FirebaseMessagingException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public String sendNotification(PushNotificationRequest request) throws FirebaseMessagingException {
        if (!isFirebaseEnabled || firebaseMessaging == null) {
            logger.warn("Firebase is not enabled or configured, skipping FCM notification");
            return null;
        }

        try {
            Message message = buildMessage(request);
            String response = firebaseMessaging.send(message);
            logger.info("Successfully sent FCM message: {} to token: {}", response, maskToken(request.getToken()));
            return response;
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send FCM notification to token: {}, error: {}", 
                maskToken(request.getToken()), e.getMessage());
            throw e;
        }
    }

    @Retryable(
        value = {FirebaseMessagingException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public BatchResponse sendMulticastNotification(List<String> tokens, PushNotificationRequest request) throws FirebaseMessagingException {
        if (!isFirebaseEnabled || firebaseMessaging == null) {
            logger.warn("Firebase is not enabled or configured, skipping FCM multicast notification");
            return null;
        }

        try {
            MulticastMessage message = buildMulticastMessage(tokens, request);
            BatchResponse response = firebaseMessaging.sendMulticast(message);
            
            logger.info("Successfully sent FCM multicast message. Success: {}, Failure: {}", 
                response.getSuccessCount(), response.getFailureCount());
            
            // Log failed tokens for debugging
            if (response.getFailureCount() > 0) {
                for (int i = 0; i < response.getResponses().size(); i++) {
                    SendResponse sendResponse = response.getResponses().get(i);
                    if (!sendResponse.isSuccessful()) {
                        logger.warn("Failed to send to token: {}, error: {}", 
                            maskToken(tokens.get(i)), sendResponse.getException().getMessage());
                    }
                }
            }
            
            return response;
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send FCM multicast notification, error: {}", e.getMessage());
            throw e;
        }
    }

    public CompletableFuture<String> sendNotificationAsync(PushNotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendNotification(request);
            } catch (FirebaseMessagingException e) {
                logger.error("Async FCM notification failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    public void sendBookingNotification(String token, String bookingId, String propertyName) {
        PushNotificationRequest request = PushNotificationRequest.builder()
            .token(token)
            .title("Booking Confirmed")
            .body("Your booking for " + propertyName + " has been confirmed!")
            .data(Map.of(
                "type", "booking_confirmed",
                "bookingId", bookingId,
                "propertyName", propertyName
            ))
            .build();

        try {
            sendNotification(request);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send booking notification via FCM", e);
        }
    }

    public void sendChatNotification(String token, String senderName, String message, String conversationId) {
        PushNotificationRequest request = PushNotificationRequest.builder()
            .token(token)
            .title("New Message from " + senderName)
            .body(message.length() > 100 ? message.substring(0, 97) + "..." : message)
            .data(Map.of(
                "type", "chat_message",
                "conversationId", conversationId,
                "senderName", senderName
            ))
            .build();

        try {
            sendNotification(request);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send chat notification via FCM", e);
        }
    }

    public void sendEmergencyNotification(List<String> tokens, String title, String message) {
        PushNotificationRequest request = PushNotificationRequest.builder()
            .title("🚨 EMERGENCY: " + title)
            .body(message)
            .data(Map.of(
                "type", "emergency",
                "priority", "high"
            ))
            .priority("high")
            .build();

        try {
            sendMulticastNotification(tokens, request);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send emergency notification via FCM", e);
        }
    }

    @Recover
    public String recoverFromFCMFailure(FirebaseMessagingException ex, PushNotificationRequest request) {
        logger.error("All FCM retry attempts failed for token: {}, scheduling for later retry", 
            maskToken(request.getToken()), ex);
        
        // Here you could implement a fallback mechanism like:
        // - Store in database for later retry
        // - Send via alternative push service
        // - Send email notification instead
        
        return null;
    }

    private Message buildMessage(PushNotificationRequest request) {
        Message.Builder messageBuilder = Message.builder()
            .setToken(request.getToken());

        // Build notification
        if (request.getTitle() != null || request.getBody() != null) {
            Notification.Builder notificationBuilder = Notification.builder();
            
            if (request.getTitle() != null) {
                notificationBuilder.setTitle(request.getTitle());
            }
            
            if (request.getBody() != null) {
                notificationBuilder.setBody(request.getBody());
            }
            
            if (request.getImageUrl() != null) {
                notificationBuilder.setImage(request.getImageUrl());
            }
            
            messageBuilder.setNotification(notificationBuilder.build());
        }

        // Add data payload
        if (request.getData() != null && !request.getData().isEmpty()) {
            messageBuilder.putAllData(request.getData());
        }

        // Configure Android-specific options
        AndroidConfig.Builder androidConfigBuilder = AndroidConfig.builder()
            .setPriority("high".equals(request.getPriority()) ? 
                AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL);

        if (request.getTitle() != null || request.getBody() != null) {
            AndroidNotification.Builder androidNotificationBuilder = AndroidNotification.builder()
                .setIcon(defaultIcon)
                .setClickAction(defaultClickAction);
            
            if (request.getTitle() != null) {
                androidNotificationBuilder.setTitle(request.getTitle());
            }
            
            if (request.getBody() != null) {
                androidNotificationBuilder.setBody(request.getBody());
            }
            
            androidConfigBuilder.setNotification(androidNotificationBuilder.build());
        }

        messageBuilder.setAndroidConfig(androidConfigBuilder.build());

        // Configure iOS-specific options
        if (request.getTitle() != null || request.getBody() != null) {
            Aps.Builder apsBuilder = Aps.builder()
                .setSound("default");
            
            if ("high".equals(request.getPriority())) {
                apsBuilder.setContentAvailable(true);
            }
            
            ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(apsBuilder.build())
                .build();
            
            messageBuilder.setApnsConfig(apnsConfig);
        }

        return messageBuilder.build();
    }

    private MulticastMessage buildMulticastMessage(List<String> tokens, PushNotificationRequest request) {
        MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
            .addAllTokens(tokens);

        // Build notification
        if (request.getTitle() != null || request.getBody() != null) {
            Notification.Builder notificationBuilder = Notification.builder();
            
            if (request.getTitle() != null) {
                notificationBuilder.setTitle(request.getTitle());
            }
            
            if (request.getBody() != null) {
                notificationBuilder.setBody(request.getBody());
            }
            
            if (request.getImageUrl() != null) {
                notificationBuilder.setImage(request.getImageUrl());
            }
            
            messageBuilder.setNotification(notificationBuilder.build());
        }

        // Add data payload
        if (request.getData() != null && !request.getData().isEmpty()) {
            messageBuilder.putAllData(request.getData());
        }

        // Configure Android-specific options
        AndroidConfig.Builder androidConfigBuilder = AndroidConfig.builder()
            .setPriority("high".equals(request.getPriority()) ? 
                AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL);

        messageBuilder.setAndroidConfig(androidConfigBuilder.build());

        return messageBuilder.build();
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "[MASKED]";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }

    public boolean isEnabled() {
        return isFirebaseEnabled && firebaseMessaging != null;
    }
}
package com.hopngo.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.notification.dto.WebPushNotificationRequest;
import com.hopngo.notification.dto.WebPushSubscriptionRequest;
import com.hopngo.notification.entity.WebPushSubscription;
import com.hopngo.notification.repository.WebPushSubscriptionRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.Base64;

@Service
@Transactional
public class WebPushService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebPushService.class);
    
    @Autowired
    private WebPushSubscriptionRepository subscriptionRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${webpush.vapid.public-key:}")
    private String vapidPublicKey;
    
    @Value("${webpush.vapid.private-key:}")
    private String vapidPrivateKey;
    
    @Value("${webpush.vapid.subject:mailto:admin@hopngo.com}")
    private String vapidSubject;
    
    private PushService pushService;
    
    @PostConstruct
    public void init() {
        try {
            // Add BouncyCastle provider for cryptographic operations
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            
            // Initialize PushService with VAPID keys
            if (vapidPublicKey.isEmpty() || vapidPrivateKey.isEmpty()) {
                logger.warn("VAPID keys not configured. Generating new keys...");
                // Generate new VAPID keys if not configured
                var keyPair = generateKeyPair();
                vapidPublicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.encode((ECPublicKey) keyPair.getPublic()));
                vapidPrivateKey = Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.encode((ECPrivateKey) keyPair.getPrivate()));
                logger.info("Generated VAPID Public Key: {}", vapidPublicKey);
                logger.info("Generated VAPID Private Key: {}", vapidPrivateKey);
            }
            
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            logger.info("WebPush service initialized successfully");
            
        } catch (GeneralSecurityException e) {
            logger.error("Failed to initialize WebPush service", e);
            throw new RuntimeException("Failed to initialize WebPush service", e);
        }
    }

    private KeyPair generateKeyPair() throws InvalidAlgorithmParameterException, NoSuchProviderException, NoSuchAlgorithmException {
        ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC");
        keyPairGenerator.initialize(parameterSpec);
        return keyPairGenerator.generateKeyPair();
    }
    
    /**
     * Subscribe a user to web push notifications
     */
    public WebPushSubscription subscribe(WebPushSubscriptionRequest request) {
        logger.info("Subscribing user {} to web push notifications", request.getUserId());
        
        // Check if subscription already exists
        Optional<WebPushSubscription> existing = subscriptionRepository
                .findByUserIdAndEndpoint(request.getUserId(), request.getEndpoint());
        
        if (existing.isPresent()) {
            WebPushSubscription subscription = existing.get();
            // Update existing subscription
            subscription.setP256dh(request.getP256dh());
            subscription.setAuth(request.getAuth());
            subscription.setUserAgent(request.getUserAgent());
            subscription.activate();
            return subscriptionRepository.save(subscription);
        } else {
            // Create new subscription
            WebPushSubscription subscription = new WebPushSubscription(
                    request.getUserId(),
                    request.getEndpoint(),
                    request.getP256dh(),
                    request.getAuth()
            );
            subscription.setUserAgent(request.getUserAgent());
            return subscriptionRepository.save(subscription);
        }
    }
    
    /**
     * Unsubscribe a user from web push notifications
     */
    public boolean unsubscribe(String userId, String endpoint) {
        logger.info("Unsubscribing user {} from endpoint {}", userId, endpoint);
        
        Optional<WebPushSubscription> subscription = subscriptionRepository
                .findByUserIdAndEndpoint(userId, endpoint);
        
        if (subscription.isPresent()) {
            subscription.get().deactivate();
            subscriptionRepository.save(subscription.get());
            return true;
        }
        
        return false;
    }
    
    /**
     * Send web push notification to a specific user
     */
    public void sendNotificationToUser(String userId, WebPushNotificationRequest request) {
        logger.info("Sending web push notification to user {}", userId);
        
        List<WebPushSubscription> subscriptions = subscriptionRepository
                .findByUserIdAndIsActiveTrue(userId);
        
        if (subscriptions.isEmpty()) {
            logger.warn("No active web push subscriptions found for user {}", userId);
            return;
        }
        
        for (WebPushSubscription subscription : subscriptions) {
            sendNotificationToSubscription(subscription, request);
        }
    }
    
    /**
     * Send test notification
     */
    public void sendTestNotification(String userId) {
        WebPushNotificationRequest request = new WebPushNotificationRequest(
                userId,
                "HopNGo Test Notification",
                "This is a test notification from HopNGo!"
        );
        request.setIcon("/icons/icon-192x192.svg");
        request.setUrl("/");
        request.setTag("test");
        
        sendNotificationToUser(userId, request);
    }
    
    /**
     * Send notification for chat message
     */
    public void sendChatNotification(String userId, String senderName, String message, String conversationId) {
        WebPushNotificationRequest request = new WebPushNotificationRequest(
                userId,
                "New message from " + senderName,
                message
        );
        request.setIcon("/icons/icon-192x192.svg");
        request.setUrl("/chat/" + conversationId);
        request.setTag("chat-" + conversationId);
        request.setRequireInteraction(true);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "chat");
        data.put("conversationId", conversationId);
        data.put("senderName", senderName);
        request.setData(data);
        
        sendNotificationToUser(userId, request);
    }
    
    /**
     * Send notification for booking confirmation
     */
    public void sendBookingNotification(String userId, String bookingId, String destination) {
        WebPushNotificationRequest request = new WebPushNotificationRequest(
                userId,
                "Booking Confirmed!",
                "Your trip to " + destination + " has been confirmed."
        );
        request.setIcon("/icons/icon-192x192.svg");
        request.setUrl("/bookings/" + bookingId);
        request.setTag("booking-" + bookingId);
        request.setRequireInteraction(true);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "booking");
        data.put("bookingId", bookingId);
        data.put("destination", destination);
        request.setData(data);
        
        sendNotificationToUser(userId, request);
    }
    
    /**
     * Get user's active subscriptions
     */
    public List<WebPushSubscription> getUserSubscriptions(String userId) {
        return subscriptionRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    /**
     * Get VAPID public key for client-side subscription
     */
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }
    
    /**
     * Send notification to a specific subscription
     */
    private void sendNotificationToSubscription(WebPushSubscription subscription, WebPushNotificationRequest request) {
        try {
            // Create notification payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", request.getTitle());
            payload.put("body", request.getBody());
            
            if (request.getIcon() != null) payload.put("icon", request.getIcon());
            if (request.getBadge() != null) payload.put("badge", request.getBadge());
            if (request.getImage() != null) payload.put("image", request.getImage());
            if (request.getUrl() != null) payload.put("url", request.getUrl());
            if (request.getTag() != null) payload.put("tag", request.getTag());
            if (request.getRequireInteraction() != null) payload.put("requireInteraction", request.getRequireInteraction());
            if (request.getSilent() != null) payload.put("silent", request.getSilent());
            if (request.getData() != null) payload.put("data", request.getData());
            
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            // Create notification
            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dh(),
                    subscription.getAuth(),
                    payloadJson
            );
            
            // Send notification
            pushService.send(notification);
            logger.info("Web push notification sent successfully to subscription {}", subscription.getId());
            
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Failed to send web push notification to subscription {}", subscription.getId(), e);
            // Deactivate subscription if it's no longer valid
            if (e.getCause() instanceof GeneralSecurityException) {
                subscription.deactivate();
                subscriptionRepository.save(subscription);
                logger.info("Deactivated invalid subscription {}", subscription.getId());
            }
        } catch (Exception e) {
            logger.error("Unexpected error sending web push notification to subscription {}", subscription.getId(), e);
        }
    }
    
    /**
     * Cleanup inactive subscriptions older than specified days
     */
    public int cleanupOldSubscriptions(int daysToKeep) {
        var cutoffDate = java.time.LocalDateTime.now().minusDays(daysToKeep);
        int deleted = subscriptionRepository.deleteInactiveOlderThan(cutoffDate);
        logger.info("Cleaned up {} inactive web push subscriptions older than {} days", deleted, daysToKeep);
        return deleted;
    }
}
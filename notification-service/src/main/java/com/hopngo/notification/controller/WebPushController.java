package com.hopngo.notification.controller;

import com.hopngo.notification.dto.WebPushNotificationRequest;
import com.hopngo.notification.dto.WebPushSubscriptionRequest;
import com.hopngo.notification.entity.WebPushSubscription;
import com.hopngo.notification.service.WebPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notify/webpush")
@Tag(name = "Web Push", description = "Web Push notification management endpoints")
public class WebPushController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebPushController.class);
    
    @Autowired
    private WebPushService webPushService;
    
    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to web push notifications", 
               description = "Register a user's device for web push notifications")
    public ResponseEntity<Map<String, Object>> subscribe(
            @Valid @RequestBody WebPushSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Security: Enforce same-origin registration
            String origin = httpRequest.getHeader("Origin");
            String referer = httpRequest.getHeader("Referer");
            
            if (origin == null && referer == null) {
                logger.warn("Web push subscription attempt without Origin or Referer header");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid request origin"));
            }
            
            // Extract user agent for tracking
            String userAgent = httpRequest.getHeader("User-Agent");
            request.setUserAgent(userAgent);
            
            WebPushSubscription subscription = webPushService.subscribe(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully subscribed to web push notifications");
            response.put("subscriptionId", subscription.getId());
            response.put("userId", subscription.getUserId());
            
            logger.info("User {} successfully subscribed to web push notifications", request.getUserId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to subscribe user {} to web push notifications", request.getUserId(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to subscribe to notifications"));
        }
    }
    
    @PostMapping("/unsubscribe")
    @Operation(summary = "Unsubscribe from web push notifications", 
               description = "Remove a user's device from web push notifications")
    public ResponseEntity<Map<String, Object>> unsubscribe(
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Push endpoint") @RequestParam String endpoint) {
        
        try {
            boolean success = webPushService.unsubscribe(userId, endpoint);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? 
                "Successfully unsubscribed from web push notifications" : 
                "Subscription not found");
            
            logger.info("User {} unsubscribed from web push notifications: {}", userId, success);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to unsubscribe user {} from web push notifications", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to unsubscribe from notifications"));
        }
    }
    
    @PostMapping("/send")
    @Operation(summary = "Send web push notification", 
               description = "Send a web push notification to a specific user")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @Valid @RequestBody WebPushNotificationRequest request) {
        
        try {
            webPushService.sendNotificationToUser(request.getUserId(), request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification sent successfully");
            response.put("userId", request.getUserId());
            
            logger.info("Web push notification sent to user {}", request.getUserId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to send web push notification to user {}", request.getUserId(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to send notification"));
        }
    }
    
    @PostMapping("/test")
    @Operation(summary = "Send test web push notification", 
               description = "Send a test web push notification to verify the system is working")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @Parameter(description = "User ID to send test notification to") 
            @RequestParam String userId) {
        
        try {
            webPushService.sendTestNotification(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test notification sent successfully");
            response.put("userId", userId);
            
            logger.info("Test web push notification sent to user {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to send test web push notification to user {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to send test notification"));
        }
    }
    
    @GetMapping("/subscriptions/{userId}")
    @Operation(summary = "Get user subscriptions", 
               description = "Retrieve all active web push subscriptions for a user")
    public ResponseEntity<Map<String, Object>> getUserSubscriptions(
            @Parameter(description = "User ID") @PathVariable String userId) {
        
        try {
            List<WebPushSubscription> subscriptions = webPushService.getUserSubscriptions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("subscriptions", subscriptions);
            response.put("count", subscriptions.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get web push subscriptions for user {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve subscriptions"));
        }
    }
    
    @GetMapping("/vapid-public-key")
    @Operation(summary = "Get VAPID public key", 
               description = "Retrieve the VAPID public key for client-side subscription")
    public ResponseEntity<Map<String, Object>> getVapidPublicKey() {
        
        try {
            String publicKey = webPushService.getVapidPublicKey();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("publicKey", publicKey);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get VAPID public key", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve VAPID public key"));
        }
    }
    
    @DeleteMapping("/cleanup")
    @Operation(summary = "Cleanup old subscriptions", 
               description = "Remove old inactive web push subscriptions")
    public ResponseEntity<Map<String, Object>> cleanupOldSubscriptions(
            @Parameter(description = "Days to keep (default: 30)") 
            @RequestParam(defaultValue = "30") int daysToKeep) {
        
        try {
            int deletedCount = webPushService.cleanupOldSubscriptions(daysToKeep);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cleanup completed successfully");
            response.put("deletedCount", deletedCount);
            response.put("daysToKeep", daysToKeep);
            
            logger.info("Cleaned up {} old web push subscriptions", deletedCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to cleanup old web push subscriptions", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to cleanup subscriptions"));
        }
    }
}
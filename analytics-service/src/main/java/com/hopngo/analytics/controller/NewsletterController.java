package com.hopngo.analytics.controller;

import com.hopngo.analytics.entity.Subscriber;
import com.hopngo.analytics.service.NewsletterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/newsletter")
@CrossOrigin(origins = "*")
public class NewsletterController {

    @Autowired
    private NewsletterService newsletterService;

    /**
     * Subscribe to newsletter
     */
    @PostMapping("/subscribe")
    public ResponseEntity<SubscriptionResponse> subscribe(@RequestBody SubscribeRequest request) {
        try {
            Subscriber subscriber = newsletterService.subscribe(
                request.getEmail(),
                request.getSource(),
                request.getUserId(),
                request.getUtmSource(),
                request.getUtmMedium(),
                request.getUtmCampaign()
            );
            
            return ResponseEntity.ok(new SubscriptionResponse(subscriber));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new SubscriptionResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SubscriptionResponse(false, "Failed to subscribe: " + e.getMessage()));
        }
    }

    /**
     * Subscribe from footer
     */
    @PostMapping("/subscribe/footer")
    public ResponseEntity<SubscriptionResponse> subscribeFromFooter(@RequestBody FooterSubscribeRequest request) {
        try {
            Subscriber subscriber = newsletterService.subscribeFromFooter(
                request.getEmail(),
                request.getUserId()
            );
            
            return ResponseEntity.ok(new SubscriptionResponse(subscriber));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new SubscriptionResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SubscriptionResponse(false, "Failed to subscribe: " + e.getMessage()));
        }
    }

    /**
     * Subscribe from popup
     */
    @PostMapping("/subscribe/popup")
    public ResponseEntity<SubscriptionResponse> subscribeFromPopup(@RequestBody PopupSubscribeRequest request) {
        try {
            Subscriber subscriber = newsletterService.subscribeFromPopup(
                request.getEmail(),
                request.getUserId(),
                request.getUtmSource(),
                request.getUtmMedium(),
                request.getUtmCampaign()
            );
            
            return ResponseEntity.ok(new SubscriptionResponse(subscriber));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new SubscriptionResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SubscriptionResponse(false, "Failed to subscribe: " + e.getMessage()));
        }
    }

    /**
     * Unsubscribe from newsletter
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestBody UnsubscribeRequest request) {
        try {
            boolean success = newsletterService.unsubscribe(request.getEmail());
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully unsubscribed"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Email not found in subscription list"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Failed to unsubscribe: " + e.getMessage()
                ));
        }
    }

    /**
     * Unsubscribe using token (for email links)
     */
    @GetMapping("/unsubscribe/{token}")
    public ResponseEntity<String> unsubscribeByToken(@PathVariable String token) {
        try {
            boolean success = newsletterService.unsubscribeByToken(token);
            
            if (success) {
                return ResponseEntity.ok(
                    "<html><body><h2>Successfully Unsubscribed</h2><p>You have been unsubscribed from our newsletter.</p></body></html>"
                );
            } else {
                return ResponseEntity.badRequest().body(
                    "<html><body><h2>Invalid Token</h2><p>The unsubscribe link is invalid or expired.</p></body></html>"
                );
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                "<html><body><h2>Error</h2><p>An error occurred while processing your request.</p></body></html>"
            );
        }
    }

    /**
     * Check subscription status
     */
    @GetMapping("/status/{email}")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus(@PathVariable String email) {
        try {
            boolean isSubscribed = newsletterService.isSubscribed(email);
            Optional<Subscriber> subscriber = newsletterService.getSubscriber(email);
            
            return ResponseEntity.ok(Map.of(
                "subscribed", isSubscribed,
                "subscriber", subscriber.map(SubscriberResponse::new).orElse(null)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to check subscription status: " + e.getMessage()
                ));
        }
    }

    /**
     * Get subscription statistics (admin only)
     */
    @GetMapping("/stats")
    public ResponseEntity<NewsletterService.SubscriptionStats> getSubscriptionStats() {
        try {
            NewsletterService.SubscriptionStats stats = newsletterService.getSubscriptionStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get recent subscribers (admin only)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<SubscriberResponse>> getRecentSubscribers(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Subscriber> subscribers = newsletterService.getRecentSubscribers(limit);
            List<SubscriberResponse> responses = subscribers.stream()
                .map(SubscriberResponse::new)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Add tag to subscriber
     */
    @PostMapping("/tag/add")
    public ResponseEntity<Map<String, String>> addTag(@RequestBody TagRequest request) {
        try {
            newsletterService.addTagToSubscriber(request.getEmail(), request.getTag());
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Tag added successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to add tag: " + e.getMessage()
                ));
        }
    }

    /**
     * Remove tag from subscriber
     */
    @PostMapping("/tag/remove")
    public ResponseEntity<Map<String, String>> removeTag(@RequestBody TagRequest request) {
        try {
            newsletterService.removeTagFromSubscriber(request.getEmail(), request.getTag());
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Tag removed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to remove tag: " + e.getMessage()
                ));
        }
    }

    // Request/Response DTOs
    
    public static class SubscribeRequest {
        private String email;
        private String source;
        private String userId;
        private String utmSource;
        private String utmMedium;
        private String utmCampaign;

        // Constructors
        public SubscribeRequest() {}

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUtmSource() { return utmSource; }
        public void setUtmSource(String utmSource) { this.utmSource = utmSource; }
        public String getUtmMedium() { return utmMedium; }
        public void setUtmMedium(String utmMedium) { this.utmMedium = utmMedium; }
        public String getUtmCampaign() { return utmCampaign; }
        public void setUtmCampaign(String utmCampaign) { this.utmCampaign = utmCampaign; }
    }

    public static class FooterSubscribeRequest {
        private String email;
        private String userId;

        // Constructors
        public FooterSubscribeRequest() {}

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class PopupSubscribeRequest {
        private String email;
        private String userId;
        private String utmSource;
        private String utmMedium;
        private String utmCampaign;

        // Constructors
        public PopupSubscribeRequest() {}

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUtmSource() { return utmSource; }
        public void setUtmSource(String utmSource) { this.utmSource = utmSource; }
        public String getUtmMedium() { return utmMedium; }
        public void setUtmMedium(String utmMedium) { this.utmMedium = utmMedium; }
        public String getUtmCampaign() { return utmCampaign; }
        public void setUtmCampaign(String utmCampaign) { this.utmCampaign = utmCampaign; }
    }

    public static class UnsubscribeRequest {
        private String email;

        // Constructors
        public UnsubscribeRequest() {}

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class TagRequest {
        private String email;
        private String tag;

        // Constructors
        public TagRequest() {}

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
    }

    public static class SubscriptionResponse {
        private boolean success;
        private String message;
        private String email;
        private String status;
        private String source;
        private String unsubscribeToken;

        // Constructor for success
        public SubscriptionResponse(Subscriber subscriber) {
            this.success = true;
            this.message = "Successfully subscribed";
            this.email = subscriber.getEmail();
            this.status = subscriber.getStatus().toString();
            this.source = subscriber.getSource();
            this.unsubscribeToken = subscriber.getUnsubscribeToken();
        }

        // Constructor for error
        public SubscriptionResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public String getSource() { return source; }
        public String getUnsubscribeToken() { return unsubscribeToken; }
    }

    public static class SubscriberResponse {
        private String email;
        private String status;
        private String source;
        private String userId;
        private String createdAt;
        private String tags;

        public SubscriberResponse(Subscriber subscriber) {
            this.email = subscriber.getEmail();
            this.status = subscriber.getStatus().toString();
            this.source = subscriber.getSource();
            this.userId = subscriber.getUserId();
            this.createdAt = subscriber.getCreatedAt().toString();
            this.tags = subscriber.getTags() != null ? String.join(",", subscriber.getTags()) : null;
        }

        // Getters
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public String getSource() { return source; }
        public String getUserId() { return userId; }
        public String getCreatedAt() { return createdAt; }
        public String getTags() { return tags; }
    }
}
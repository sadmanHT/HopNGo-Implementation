package com.hopngo.analytics.controller;

import com.hopngo.analytics.entity.Referral;
import com.hopngo.analytics.entity.Subscriber;
import com.hopngo.analytics.service.ReferralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/referrals")
@CrossOrigin(origins = "*")
public class ReferralController {

    @Autowired
    private ReferralService referralService;

    /**
     * Create a new referral code for a user
     */
    @PostMapping("/create")
    public ResponseEntity<ReferralResponse> createReferral(@RequestBody CreateReferralRequest request) {
        try {
            Referral referral = referralService.createReferral(request.getUserId(), request.getCampaign());
            return ResponseEntity.ok(new ReferralResponse(referral));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ReferralResponse("Failed to create referral: " + e.getMessage()));
        }
    }

    /**
     * Track a referral click/visit
     */
    @PostMapping("/track/{referralCode}")
    public ResponseEntity<Map<String, String>> trackReferralClick(
            @PathVariable String referralCode,
            HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            referralService.trackReferralClick(referralCode, ipAddress, userAgent);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Referral click tracked successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to track referral click: " + e.getMessage()
                ));
        }
    }

    /**
     * Process a referral conversion
     */
    @PostMapping("/convert")
    public ResponseEntity<Map<String, String>> processConversion(@RequestBody ConversionRequest request) {
        try {
            referralService.processReferralConversion(
                request.getReferralCode(),
                request.getNewUserId(),
                request.getConversionType()
            );
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Referral conversion processed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to process conversion: " + e.getMessage()
                ));
        }
    }

    /**
     * Validate a referral code
     */
    @GetMapping("/validate/{referralCode}")
    public ResponseEntity<ValidationResponse> validateReferralCode(@PathVariable String referralCode) {
        try {
            Optional<Referral> referralOpt = referralService.validateReferralCode(referralCode);
            
            if (referralOpt.isPresent()) {
                return ResponseEntity.ok(new ValidationResponse(true, "Valid referral code", referralOpt.get()));
            } else {
                return ResponseEntity.ok(new ValidationResponse(false, "Invalid or expired referral code", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ValidationResponse(false, "Validation failed: " + e.getMessage(), null));
        }
    }

    /**
     * Get referral statistics for a user
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<ReferralService.ReferralStats> getReferralStats(@PathVariable String userId) {
        try {
            ReferralService.ReferralStats stats = referralService.getReferralStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active referral codes for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReferralResponse>> getUserReferrals(@PathVariable String userId) {
        try {
            List<Referral> referrals = referralService.getActiveReferrals(userId);
            List<ReferralResponse> responses = referrals.stream()
                .map(ReferralResponse::new)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Subscribe via referral
     */
    @PostMapping("/subscribe")
    public ResponseEntity<SubscriberResponse> subscribeViaReferral(@RequestBody SubscribeRequest request) {
        try {
            Subscriber subscriber = referralService.subscribeViaReferral(
                request.getEmail(),
                request.getReferralCode(),
                request.getUserId()
            );
            
            return ResponseEntity.ok(new SubscriberResponse(subscriber));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SubscriberResponse("Failed to subscribe: " + e.getMessage()));
        }
    }

    // Helper method to get client IP address
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    // Request/Response DTOs
    
    public static class CreateReferralRequest {
        private String userId;
        private String campaign;

        // Constructors
        public CreateReferralRequest() {}
        public CreateReferralRequest(String userId, String campaign) {
            this.userId = userId;
            this.campaign = campaign;
        }

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getCampaign() { return campaign; }
        public void setCampaign(String campaign) { this.campaign = campaign; }
    }

    public static class ConversionRequest {
        private String referralCode;
        private String newUserId;
        private String conversionType;

        // Constructors
        public ConversionRequest() {}
        public ConversionRequest(String referralCode, String newUserId, String conversionType) {
            this.referralCode = referralCode;
            this.newUserId = newUserId;
            this.conversionType = conversionType;
        }

        // Getters and setters
        public String getReferralCode() { return referralCode; }
        public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
        public String getNewUserId() { return newUserId; }
        public void setNewUserId(String newUserId) { this.newUserId = newUserId; }
        public String getConversionType() { return conversionType; }
        public void setConversionType(String conversionType) { this.conversionType = conversionType; }
    }

    public static class SubscribeRequest {
        private String email;
        private String referralCode;
        private String userId;

        // Constructors
        public SubscribeRequest() {}
        public SubscribeRequest(String email, String referralCode, String userId) {
            this.email = email;
            this.referralCode = referralCode;
            this.userId = userId;
        }

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getReferralCode() { return referralCode; }
        public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class ReferralResponse {
        private boolean success;
        private String message;
        private String referralCode;
        private String userId;
        private String campaign;
        private String status;
        private int clickCount;
        private int conversionCount;

        // Constructor for success
        public ReferralResponse(Referral referral) {
            this.success = true;
            this.message = "Success";
            this.referralCode = referral.getReferralCode();
            this.userId = referral.getUserId();
            this.campaign = referral.getCampaign();
            this.status = referral.getStatus().toString();
            this.clickCount = referral.getClickCount();
            this.conversionCount = referral.getConversionCount();
        }

        // Constructor for error
        public ReferralResponse(String errorMessage) {
            this.success = false;
            this.message = errorMessage;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getReferralCode() { return referralCode; }
        public String getUserId() { return userId; }
        public String getCampaign() { return campaign; }
        public String getStatus() { return status; }
        public int getClickCount() { return clickCount; }
        public int getConversionCount() { return conversionCount; }
    }

    public static class ValidationResponse {
        private boolean valid;
        private String message;
        private ReferralResponse referral;

        public ValidationResponse(boolean valid, String message, Referral referral) {
            this.valid = valid;
            this.message = message;
            this.referral = referral != null ? new ReferralResponse(referral) : null;
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public ReferralResponse getReferral() { return referral; }
    }

    public static class SubscriberResponse {
        private boolean success;
        private String message;
        private String email;
        private String status;
        private String source;

        // Constructor for success
        public SubscriberResponse(Subscriber subscriber) {
            this.success = true;
            this.message = "Successfully subscribed";
            this.email = subscriber.getEmail();
            this.status = subscriber.getStatus().toString();
            this.source = subscriber.getSource();
        }

        // Constructor for error
        public SubscriberResponse(String errorMessage) {
            this.success = false;
            this.message = errorMessage;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public String getSource() { return source; }
    }
}
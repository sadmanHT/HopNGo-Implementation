package com.hopngo.controller;

import com.hopngo.service.DisputeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/disputes")
public class DisputeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(DisputeWebhookController.class);

    @Autowired
    private DisputeService disputeService;

    // Webhook secrets for signature verification
    @Value("${stripe.webhook.secret:whsec_dummy}")
    private String stripeWebhookSecret;

    @Value("${bkash.webhook.secret:dummy_secret}")
    private String bkashWebhookSecret;

    @Value("${nagad.webhook.secret:dummy_secret}")
    private String nagadWebhookSecret;

    /**
     * Stripe dispute webhook endpoint
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeDispute(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature,
            HttpServletRequest request) {
        
        logger.info("Received Stripe dispute webhook");
        
        try {
            // Verify webhook signature
            if (!verifyStripeSignature(payload, signature)) {
                logger.warn("Invalid Stripe webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
            
            // Parse the webhook payload
            Map<String, Object> event = parseJsonPayload(payload);
            String eventType = (String) event.get("type");
            
            logger.info("Processing Stripe webhook event: {}", eventType);
            
            // Handle different dispute event types
            switch (eventType) {
                case "charge.dispute.created":
                    handleStripeDisputeCreated(event);
                    break;
                case "charge.dispute.updated":
                    handleStripeDisputeUpdated(event);
                    break;
                case "charge.dispute.closed":
                    handleStripeDisputeClosed(event);
                    break;
                case "charge.dispute.funds_withdrawn":
                    handleStripeDisputeFundsWithdrawn(event);
                    break;
                case "charge.dispute.funds_reinstated":
                    handleStripeDisputeFundsReinstated(event);
                    break;
                default:
                    logger.info("Unhandled Stripe event type: {}", eventType);
            }
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            logger.error("Error processing Stripe dispute webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    /**
     * bKash dispute webhook endpoint
     */
    @PostMapping("/bkash")
    public ResponseEntity<String> handleBkashDispute(
            @RequestBody String payload,
            @RequestHeader(value = "X-Bkash-Signature", required = false) String signature,
            HttpServletRequest request) {
        
        logger.info("Received bKash dispute webhook");
        
        try {
            // Verify webhook signature if provided
            if (signature != null && !verifyBkashSignature(payload, signature)) {
                logger.warn("Invalid bKash webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
            
            // Parse the webhook payload
            Map<String, Object> event = parseJsonPayload(payload);
            String eventType = (String) event.get("eventType");
            
            logger.info("Processing bKash webhook event: {}", eventType);
            
            // Handle different dispute event types
            switch (eventType) {
                case "DISPUTE_CREATED":
                    handleBkashDisputeCreated(event);
                    break;
                case "DISPUTE_UPDATED":
                    handleBkashDisputeUpdated(event);
                    break;
                case "DISPUTE_RESOLVED":
                    handleBkashDisputeResolved(event);
                    break;
                case "CHARGEBACK_INITIATED":
                    handleBkashChargebackInitiated(event);
                    break;
                default:
                    logger.info("Unhandled bKash event type: {}", eventType);
            }
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            logger.error("Error processing bKash dispute webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    /**
     * Nagad dispute webhook endpoint
     */
    @PostMapping("/nagad")
    public ResponseEntity<String> handleNagadDispute(
            @RequestBody String payload,
            @RequestHeader(value = "X-Nagad-Signature", required = false) String signature,
            HttpServletRequest request) {
        
        logger.info("Received Nagad dispute webhook");
        
        try {
            // Verify webhook signature if provided
            if (signature != null && !verifyNagadSignature(payload, signature)) {
                logger.warn("Invalid Nagad webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
            
            // Parse the webhook payload
            Map<String, Object> event = parseJsonPayload(payload);
            String eventType = (String) event.get("event_type");
            
            logger.info("Processing Nagad webhook event: {}", eventType);
            
            // Handle different dispute event types
            switch (eventType) {
                case "dispute.created":
                    handleNagadDisputeCreated(event);
                    break;
                case "dispute.updated":
                    handleNagadDisputeUpdated(event);
                    break;
                case "dispute.closed":
                    handleNagadDisputeClosed(event);
                    break;
                case "chargeback.received":
                    handleNagadChargebackReceived(event);
                    break;
                default:
                    logger.info("Unhandled Nagad event type: {}", eventType);
            }
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            logger.error("Error processing Nagad dispute webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    // Stripe Event Handlers
    
    @SuppressWarnings("unchecked")
    private void handleStripeDisputeCreated(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        Map<String, Object> dispute = (Map<String, Object>) data.get("object");
        
        String disputeId = (String) dispute.get("id");
        String chargeId = (String) dispute.get("charge");
        String reason = (String) dispute.get("reason");
        Integer amountCents = (Integer) dispute.get("amount");
        String currency = (String) dispute.get("currency");
        String status = (String) dispute.get("status");
        
        logger.info("Stripe dispute created: {} for charge: {} amount: {} reason: {}", 
            disputeId, chargeId, amountCents, reason);
        
        disputeService.handleStripeDisputeCreated(disputeId, chargeId, reason, amountCents, currency, status);
    }
    
    @SuppressWarnings("unchecked")
    private void handleStripeDisputeUpdated(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        Map<String, Object> dispute = (Map<String, Object>) data.get("object");
        
        String disputeId = (String) dispute.get("id");
        String status = (String) dispute.get("status");
        
        logger.info("Stripe dispute updated: {} status: {}", disputeId, status);
        
        disputeService.handleStripeDisputeUpdated(disputeId, status);
    }
    
    @SuppressWarnings("unchecked")
    private void handleStripeDisputeClosed(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        Map<String, Object> dispute = (Map<String, Object>) data.get("object");
        
        String disputeId = (String) dispute.get("id");
        String status = (String) dispute.get("status");
        
        logger.info("Stripe dispute closed: {} status: {}", disputeId, status);
        
        disputeService.handleStripeDisputeClosed(disputeId, status);
    }
    
    @SuppressWarnings("unchecked")
    private void handleStripeDisputeFundsWithdrawn(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        Map<String, Object> dispute = (Map<String, Object>) data.get("object");
        
        String disputeId = (String) dispute.get("id");
        Integer amountCents = (Integer) dispute.get("amount");
        
        logger.info("Stripe dispute funds withdrawn: {} amount: {}", disputeId, amountCents);
        
        disputeService.handleStripeDisputeFundsWithdrawn(disputeId, amountCents);
    }
    
    @SuppressWarnings("unchecked")
    private void handleStripeDisputeFundsReinstated(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        Map<String, Object> dispute = (Map<String, Object>) data.get("object");
        
        String disputeId = (String) dispute.get("id");
        Integer amountCents = (Integer) dispute.get("amount");
        
        logger.info("Stripe dispute funds reinstated: {} amount: {}", disputeId, amountCents);
        
        disputeService.handleStripeDisputeFundsReinstated(disputeId, amountCents);
    }

    // bKash Event Handlers
    
    @SuppressWarnings("unchecked")
    private void handleBkashDisputeCreated(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        
        String disputeId = (String) data.get("disputeId");
        String paymentId = (String) data.get("paymentId");
        String reason = (String) data.get("reason");
        String amount = (String) data.get("amount");
        String status = (String) data.get("status");
        
        logger.info("bKash dispute created: {} for payment: {} amount: {} reason: {}", 
            disputeId, paymentId, amount, reason);
        
        disputeService.handleBkashDisputeCreated(disputeId, paymentId, reason, amount, status);
    }
    
    @SuppressWarnings("unchecked")
    private void handleBkashDisputeUpdated(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        
        String disputeId = (String) data.get("disputeId");
        String status = (String) data.get("status");
        
        logger.info("bKash dispute updated: {} status: {}", disputeId, status);
        
        disputeService.handleBkashDisputeUpdated(disputeId, status);
    }
    
    @SuppressWarnings("unchecked")
    private void handleBkashDisputeResolved(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        
        String disputeId = (String) data.get("disputeId");
        String resolution = (String) data.get("resolution");
        
        logger.info("bKash dispute resolved: {} resolution: {}", disputeId, resolution);
        
        disputeService.handleBkashDisputeResolved(disputeId, resolution);
    }
    
    @SuppressWarnings("unchecked")
    private void handleBkashChargebackInitiated(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        
        String chargebackId = (String) data.get("chargebackId");
        String paymentId = (String) data.get("paymentId");
        String amount = (String) data.get("amount");
        
        logger.info("bKash chargeback initiated: {} for payment: {} amount: {}", 
            chargebackId, paymentId, amount);
        
        disputeService.handleBkashChargebackInitiated(chargebackId, paymentId, amount);
    }

    // Nagad Event Handlers
    
    @SuppressWarnings("unchecked")
    private void handleNagadDisputeCreated(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        
        String disputeId = (String) data.get("dispute_id");
        String orderId = (String) data.get("order_id");
        String reason = (String) data.get("reason");
        String amount = (String) data.get("amount");
        String status = (String) data.get("status");
        
        logger.info("Nagad dispute created: {} for order: {} amount: {} reason: {}", 
            disputeId, orderId, amount, reason);
        
        disputeService.handleNagadDisputeCreated(disputeId, orderId, reason, amount, status);
    }
    
    @SuppressWarnings("unchecked")
    private void handleNagadDisputeUpdated(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        
        String disputeId = (String) data.get("dispute_id");
        String status = (String) data.get("status");
        
        logger.info("Nagad dispute updated: {} status: {}", disputeId, status);
        
        disputeService.handleNagadDisputeUpdated(disputeId, status);
    }
    
    @SuppressWarnings("unchecked")
    private void handleNagadDisputeClosed(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        
        String disputeId = (String) data.get("dispute_id");
        String outcome = (String) data.get("outcome");
        
        logger.info("Nagad dispute closed: {} outcome: {}", disputeId, outcome);
        
        disputeService.handleNagadDisputeClosed(disputeId, outcome);
    }
    
    @SuppressWarnings("unchecked")
    private void handleNagadChargebackReceived(Map<String, Object> event) {
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        
        String chargebackId = (String) data.get("chargeback_id");
        String orderId = (String) data.get("order_id");
        String amount = (String) data.get("amount");
        
        logger.info("Nagad chargeback received: {} for order: {} amount: {}", 
            chargebackId, orderId, amount);
        
        disputeService.handleNagadChargebackReceived(chargebackId, orderId, amount);
    }

    // Signature Verification Methods
    
    private boolean verifyStripeSignature(String payload, String signature) {
        try {
            String[] elements = signature.split(",");
            String timestamp = null;
            String v1Signature = null;
            
            for (String element : elements) {
                String[] keyValue = element.split("=");
                if ("t".equals(keyValue[0])) {
                    timestamp = keyValue[1];
                } else if ("v1".equals(keyValue[0])) {
                    v1Signature = keyValue[1];
                }
            }
            
            if (timestamp == null || v1Signature == null) {
                return false;
            }
            
            String signedPayload = timestamp + "." + payload;
            String expectedSignature = computeHmacSha256(signedPayload, stripeWebhookSecret);
            
            return expectedSignature.equals(v1Signature);
            
        } catch (Exception e) {
            logger.error("Error verifying Stripe signature", e);
            return false;
        }
    }
    
    private boolean verifyBkashSignature(String payload, String signature) {
        try {
            String expectedSignature = computeHmacSha256(payload, bkashWebhookSecret);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            logger.error("Error verifying bKash signature", e);
            return false;
        }
    }
    
    private boolean verifyNagadSignature(String payload, String signature) {
        try {
            String expectedSignature = computeHmacSha256(payload, nagadWebhookSecret);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            logger.error("Error verifying Nagad signature", e);
            return false;
        }
    }
    
    private String computeHmacSha256(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonPayload(String payload) {
        try {
            // In a real implementation, use a proper JSON parser like Jackson or Gson
            // For now, return a mock parsed object
            return new java.util.HashMap<>();
        } catch (Exception e) {
            logger.error("Error parsing JSON payload", e);
            throw new RuntimeException("Invalid JSON payload", e);
        }
    }

    /**
     * Health check endpoint for webhook endpoints
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Dispute webhook endpoints are healthy");
    }
}
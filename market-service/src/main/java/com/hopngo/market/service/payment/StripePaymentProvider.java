package com.hopngo.market.service.payment;

import com.hopngo.market.entity.Order;
import com.hopngo.market.dto.PaymentIntentResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Stripe payment provider implementation for test mode.
 * Integrates with Stripe API for payment processing in test environment.
 */
@Component
public class StripePaymentProvider implements PaymentProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(StripePaymentProvider.class);
    private static final String STRIPE_SIGNATURE_HEADER = "Stripe-Signature";
    
    @Value("${stripe.api.key:}")
    private String stripeApiKey;
    
    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;
    
    @Override
    public PaymentIntentResponse createPaymentIntent(Order order) {
        logger.info("Creating Stripe payment intent for order: {}", order.getId());
        
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            throw new IllegalStateException("Stripe API key not configured. Set STRIPE_API_KEY environment variable.");
        }
        
        try {
            // In a real implementation, this would call Stripe API
            // For now, we'll simulate the Stripe API response structure
            Map<String, Object> paymentIntentParams = new HashMap<>();
            paymentIntentParams.put("amount", order.getTotalAmount().longValue() * 100); // Convert to cents
            paymentIntentParams.put("currency", order.getCurrency().toLowerCase());
            paymentIntentParams.put("automatic_payment_methods", Map.of("enabled", true));
            paymentIntentParams.put("metadata", Map.of(
                "order_id", order.getId().toString(),
                "customer_id", order.getCustomerId().toString()
            ));
            
            // Simulate Stripe PaymentIntent creation
            String paymentIntentId = "pi_test_" + System.currentTimeMillis();
            String clientSecret = paymentIntentId + "_secret_" + System.currentTimeMillis();
            
            PaymentIntentResponse response = new PaymentIntentResponse(
                clientSecret,
                paymentIntentId,
                order.getTotalAmount().longValue() * 100,
                order.getCurrency(),
                "requires_payment_method",
                "STRIPE_TEST"
            );
            
            logger.info("Stripe payment intent created: {}", paymentIntentId);
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to create Stripe payment intent for order: {}", order.getId(), e);
            throw new RuntimeException("Failed to create Stripe payment intent", e);
        }
    }
    
    @Override
    public boolean verifyWebhook(HttpServletRequest request) {
        logger.info("Verifying Stripe webhook signature");
        
        if (stripeWebhookSecret == null || stripeWebhookSecret.isEmpty()) {
            logger.error("Stripe webhook secret not configured. Set STRIPE_WEBHOOK_SECRET environment variable.");
            return false;
        }
        
        String signature = request.getHeader(STRIPE_SIGNATURE_HEADER);
        if (signature == null) {
            logger.warn("Missing Stripe signature header");
            return false;
        }
        
        try {
            String payload = getRequestBody(request);
            return verifyStripeSignature(payload, signature, stripeWebhookSecret);
        } catch (Exception e) {
            logger.error("Failed to verify Stripe webhook signature", e);
            return false;
        }
    }
    
    @Override
    public String name() {
        return "STRIPE_TEST";
    }
    
    /**
     * Verifies Stripe webhook signature using payload and HttpHeaders.
     * This method is used by tests and provides an alternative to the HttpServletRequest version.
     */
    public boolean verifyWebhookSignature(String payload, HttpHeaders headers) {
        logger.info("Verifying Stripe webhook signature with HttpHeaders");
        
        if (stripeWebhookSecret == null || stripeWebhookSecret.isEmpty()) {
            logger.error("Stripe webhook secret not configured. Set STRIPE_WEBHOOK_SECRET environment variable.");
            return false;
        }
        
        String signature = headers.getFirst(STRIPE_SIGNATURE_HEADER);
        if (signature == null) {
            logger.warn("Missing Stripe signature header");
            return false;
        }
        
        try {
            return verifyStripeSignature(payload, signature, stripeWebhookSecret);
        } catch (Exception e) {
            logger.error("Failed to verify Stripe webhook signature", e);
            return false;
        }
    }
    
    /**
     * Reads the request body from HttpServletRequest.
     */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }
    
    /**
     * Verifies Stripe webhook signature using HMAC SHA256.
     */
    private boolean verifyStripeSignature(String payload, String signature, String secret) {
        try {
            // Parse signature header (format: t=timestamp,v1=signature)
            String[] signatureParts = signature.split(",");
            String timestamp = null;
            String expectedSignature = null;
            
            for (String part : signatureParts) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2) {
                    if ("t".equals(keyValue[0])) {
                        timestamp = keyValue[1];
                    } else if ("v1".equals(keyValue[0])) {
                        expectedSignature = keyValue[1];
                    }
                }
            }
            
            if (timestamp == null || expectedSignature == null) {
                logger.warn("Invalid Stripe signature format");
                return false;
            }
            
            // Create signed payload
            String signedPayload = timestamp + "." + payload;
            
            // Compute HMAC SHA256
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String computedSignature = hexString.toString();
            boolean isValid = computedSignature.equals(expectedSignature);
            
            if (isValid) {
                logger.info("Stripe webhook signature verified successfully");
            } else {
                logger.warn("Stripe webhook signature verification failed");
            }
            
            return isValid;
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Failed to compute Stripe webhook signature", e);
            return false;
        }
    }
}
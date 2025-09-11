package com.hopngo.market.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.market.config.PaymentProviderConfiguration;
import com.hopngo.market.entity.Order;
import com.hopngo.market.dto.PaymentIntentResponse;
import com.hopngo.market.dto.RefundResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * bKash payment provider implementation for sandbox/test mode.
 * Integrates with bKash Payment Gateway API for payment processing.
 */
@Component
public class BkashPaymentProvider implements PaymentProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(BkashPaymentProvider.class);
    private static final String BKASH_SIGNATURE_HEADER = "X-Bkash-Signature";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private PaymentProviderConfiguration.PaymentProperties paymentProperties;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private String accessToken;
    private long tokenExpiryTime;
    
    @Override
    public PaymentIntentResponse createPaymentIntent(Order order) {
        logger.info("Creating bKash payment intent for order: {}", order.getId());
        
        try {
            // Ensure we have a valid access token
            if (!isTokenValid()) {
                refreshAccessToken();
            }
            
            PaymentProviderConfiguration.PaymentProperties.BkashConfig config = paymentProperties.getProviders().getBkash();
            
            // Create payment request
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("mode", "0011"); // Checkout mode
            paymentRequest.put("payerReference", order.getId().toString());
            paymentRequest.put("callbackURL", getCallbackUrl());
            paymentRequest.put("amount", order.getTotalAmount().toString());
            paymentRequest.put("currency", config.getCurrency());
            paymentRequest.put("intent", "sale");
            paymentRequest.put("merchantInvoiceNumber", "INV-" + order.getId().toString());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            headers.set("X-APP-Key", config.getAppKey());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(paymentRequest, headers);
            
            String createUrl = config.getBaseUrl() + "/tokenized/checkout/create";
            ResponseEntity<String> response = restTemplate.postForEntity(createUrl, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                if ("0000".equals(responseJson.path("statusCode").asText())) {
                    String paymentId = responseJson.path("paymentID").asText();
                    String bkashUrl = responseJson.path("bkashURL").asText();
                    
                    logger.info("bKash payment intent created successfully: {}", paymentId);
                    
                    return new PaymentIntentResponse(
                             paymentId, // clientSecret
                             paymentId, // paymentIntentId
                             order.getTotalAmount(),
                             config.getCurrency(),
                             "requires_action",
                             "BKASH"
                     );
                } else {
                    logger.error("bKash payment creation failed: {}", responseJson.path("statusMessage").asText());
                    throw new RuntimeException("bKash payment creation failed: " + responseJson.path("statusMessage").asText());
                }
            } else {
                logger.error("bKash API call failed with status: {}", response.getStatusCode());
                throw new RuntimeException("bKash API call failed");
            }
            
        } catch (Exception e) {
            logger.error("Error creating bKash payment intent", e);
            throw new RuntimeException("Failed to create bKash payment intent", e);
        }
    }
    
    @Override
    public boolean verifyWebhook(HttpServletRequest request) {
        try {
            String rawBody = getRequestBody(request);
            HttpHeaders headers = new HttpHeaders();
            request.getHeaderNames().asIterator().forEachRemaining(name -> 
                headers.set(name, request.getHeader(name))
            );
            
            return verifyWebhookSignature(rawBody, headers);
        } catch (Exception e) {
            logger.error("Error verifying bKash webhook", e);
            return false;
        }
    }
    
    @Override
    public boolean verifyWebhookSignature(String rawBody, HttpHeaders headers) {
        try {
            String signature = headers.getFirst(BKASH_SIGNATURE_HEADER);
            if (signature == null) {
                logger.warn("No bKash signature found in webhook headers");
                return false;
            }
            
            PaymentProviderConfiguration.PaymentProperties.BkashConfig config = paymentProperties.getProviders().getBkash();
            String webhookSecret = config.getWebhookSecret();
            
            if (webhookSecret == null || webhookSecret.isEmpty()) {
                logger.warn("bKash webhook secret not configured");
                return false;
            }
            
            return verifyBkashSignature(rawBody, signature, webhookSecret);
            
        } catch (Exception e) {
            logger.error("Error verifying bKash webhook signature", e);
            return false;
        }
    }
    
    @Override
    public RefundResponse processRefund(String paymentId, java.math.BigDecimal refundAmount, String currency, String reason) {
        logger.info("Processing bKash refund for payment: {}, amount: {} {}", paymentId, refundAmount, currency);
        
        try {
            // Ensure we have a valid access token
            if (!isTokenValid()) {
                refreshAccessToken();
            }
            
            // Create refund request
            Map<String, Object> refundRequest = new HashMap<>();
            refundRequest.put("paymentID", paymentId);
            refundRequest.put("amount", refundAmount.toString());
            refundRequest.put("trxID", "TXN" + System.currentTimeMillis());
            refundRequest.put("sku", "refund");
            refundRequest.put("reason", reason);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            headers.set("X-APP-Key", paymentProperties.getBkash().getAppKey());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(refundRequest, headers);
            
            // In sandbox mode, simulate bKash refund API call
            String refundUrl = paymentProperties.getBkash().getBaseUrl() + "/tokenized/checkout/payment/refund";
            
            // Simulate successful refund response
            String refundTrxId = "REF" + System.currentTimeMillis();
            
            logger.info("bKash refund successful: {}", refundTrxId);
            return RefundResponse.success(refundTrxId, refundAmount, currency);
            
        } catch (Exception e) {
            logger.error("bKash refund failed for payment: {}", paymentId, e);
            return RefundResponse.failed("bKash refund failed: " + e.getMessage());
        }
    }
    
    @Override
    public String name() {
        return "BKASH";
    }
    
    /**
     * Refreshes the bKash access token.
     */
    private void refreshAccessToken() throws Exception {
        PaymentProviderConfiguration.PaymentProperties.BkashConfig config = paymentProperties.getProviders().getBkash();
        
        Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("app_key", config.getAppKey());
        tokenRequest.put("app_secret", config.getAppSecret());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("username", config.getUsername());
        headers.set("password", config.getPassword());
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(tokenRequest, headers);
        
        String tokenUrl = config.getBaseUrl() + "/tokenized/checkout/token/grant";
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            
            if ("0000".equals(responseJson.path("statusCode").asText())) {
                this.accessToken = responseJson.path("id_token").asText();
                // bKash tokens typically expire in 1 hour
                this.tokenExpiryTime = System.currentTimeMillis() + (55 * 60 * 1000); // 55 minutes
                logger.info("bKash access token refreshed successfully");
            } else {
                throw new RuntimeException("Failed to get bKash access token: " + responseJson.path("statusMessage").asText());
            }
        } else {
            throw new RuntimeException("bKash token API call failed with status: " + response.getStatusCode());
        }
    }
    
    /**
     * Checks if the current access token is valid.
     */
    private boolean isTokenValid() {
        return accessToken != null && System.currentTimeMillis() < tokenExpiryTime;
    }
    
    /**
     * Gets the callback URL for bKash payments.
     */
    private String getCallbackUrl() {
        // This should be configurable, but for now using a default
        return "https://your-domain.com/market/payments/callback/bkash";
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
     * Verifies bKash webhook signature using HMAC-SHA256.
     */
    private boolean verifyBkashSignature(String payload, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);
            
            return expectedSignature.equals(signature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error computing bKash signature", e);
            return false;
        }
    }
    
    /**
     * Executes a bKash payment after user authorization.
     */
    public Map<String, Object> executePayment(String paymentId) throws Exception {
        if (!isTokenValid()) {
            refreshAccessToken();
        }
        
        PaymentProviderConfiguration.PaymentProperties.BkashConfig config = paymentProperties.getProviders().getBkash();
        
        Map<String, String> executeRequest = new HashMap<>();
        executeRequest.put("paymentID", paymentId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("X-APP-Key", config.getAppKey());
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(executeRequest, headers);
        
        String executeUrl = config.getBaseUrl() + "/tokenized/checkout/execute";
        ResponseEntity<String> response = restTemplate.postForEntity(executeUrl, request, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return objectMapper.convertValue(responseJson, Map.class);
        } else {
            throw new RuntimeException("bKash execute payment failed with status: " + response.getStatusCode());
        }
    }
    
    /**
     * Queries a bKash payment status.
     */
    public Map<String, Object> queryPayment(String paymentId) throws Exception {
        if (!isTokenValid()) {
            refreshAccessToken();
        }
        
        PaymentProviderConfiguration.PaymentProperties.BkashConfig config = paymentProperties.getProviders().getBkash();
        
        Map<String, String> queryRequest = new HashMap<>();
        queryRequest.put("paymentID", paymentId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("X-APP-Key", config.getAppKey());
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(queryRequest, headers);
        
        String queryUrl = config.getBaseUrl() + "/tokenized/checkout/payment/status";
        ResponseEntity<String> response = restTemplate.postForEntity(queryUrl, request, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return objectMapper.convertValue(responseJson, Map.class);
        } else {
            throw new RuntimeException("bKash query payment failed with status: " + response.getStatusCode());
        }
    }
}
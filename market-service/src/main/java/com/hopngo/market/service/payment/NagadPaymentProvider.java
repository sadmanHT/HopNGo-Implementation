package com.hopngo.market.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.market.config.PaymentProviderConfiguration;
import com.hopngo.market.entity.Order;
import com.hopngo.market.dto.PaymentIntentResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nagad payment provider implementation for sandbox/test mode.
 * Integrates with Nagad Payment Gateway API for payment processing.
 */
@Component
public class NagadPaymentProvider implements PaymentProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(NagadPaymentProvider.class);
    private static final String NAGAD_SIGNATURE_HEADER = "X-Nagad-Signature";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private PaymentProviderConfiguration.PaymentProperties paymentProperties;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Override
    public PaymentIntentResponse createPaymentIntent(Order order) {
        logger.info("Creating Nagad payment intent for order: {}", order.getId());
        
        try {
            PaymentProviderConfiguration.PaymentProperties.NagadConfig config = paymentProperties.getProviders().getNagad();
            
            // Step 1: Initialize payment
            String orderId = "ORD-" + order.getId().toString();
            Map<String, Object> initRequest = createInitializeRequest(order, orderId, config);
            
            HttpHeaders headers = createHeaders(config);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(initRequest, headers);
            
            String initUrl = config.getBaseUrl() + "/remote-payment-gateway-1.0/api/dfs/check-out/initialize/" + config.getMerchantId() + "/" + orderId;
            ResponseEntity<String> initResponse = restTemplate.postForEntity(initUrl, request, String.class);
            
            if (initResponse.getStatusCode() == HttpStatus.OK) {
                JsonNode initResponseJson = objectMapper.readTree(initResponse.getBody());
                
                if ("Success".equals(initResponseJson.path("status").asText())) {
                    String paymentReferenceId = initResponseJson.path("paymentReferenceId").asText();
                    
                    // Step 2: Complete payment initialization
                    Map<String, Object> completeRequest = createCompleteRequest(order, orderId, paymentReferenceId, config);
                    HttpEntity<Map<String, Object>> completeRequestEntity = new HttpEntity<>(completeRequest, headers);
                    
                    String completeUrl = config.getBaseUrl() + "/remote-payment-gateway-1.0/api/dfs/check-out/complete/" + paymentReferenceId;
                    ResponseEntity<String> completeResponse = restTemplate.postForEntity(completeUrl, completeRequestEntity, String.class);
                    
                    if (completeResponse.getStatusCode() == HttpStatus.OK) {
                        JsonNode completeResponseJson = objectMapper.readTree(completeResponse.getBody());
                        
                        if ("Success".equals(completeResponseJson.path("status").asText())) {
                            String callBackUrl = completeResponseJson.path("callBackUrl").asText();
                            
                            logger.info("Nagad payment intent created successfully: {}", paymentReferenceId);
                            
                            return new PaymentIntentResponse(
                                    paymentReferenceId, // clientSecret
                                    paymentReferenceId, // paymentIntentId
                                    order.getTotalAmount(),
                                    "BDT",
                                    "requires_action",
                                    "NAGAD"
                            );
                        } else {
                            logger.error("Nagad payment completion failed: {}", completeResponseJson.path("message").asText());
                            throw new RuntimeException("Nagad payment completion failed: " + completeResponseJson.path("message").asText());
                        }
                    } else {
                        logger.error("Nagad complete API call failed with status: {}", completeResponse.getStatusCode());
                        throw new RuntimeException("Nagad complete API call failed");
                    }
                } else {
                    logger.error("Nagad payment initialization failed: {}", initResponseJson.path("message").asText());
                    throw new RuntimeException("Nagad payment initialization failed: " + initResponseJson.path("message").asText());
                }
            } else {
                logger.error("Nagad init API call failed with status: {}", initResponse.getStatusCode());
                throw new RuntimeException("Nagad init API call failed");
            }
            
        } catch (Exception e) {
            logger.error("Error creating Nagad payment intent", e);
            throw new RuntimeException("Failed to create Nagad payment intent", e);
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
            logger.error("Error verifying Nagad webhook", e);
            return false;
        }
    }
    
    @Override
    public boolean verifyWebhookSignature(String rawBody, HttpHeaders headers) {
        try {
            String signature = headers.getFirst(NAGAD_SIGNATURE_HEADER);
            if (signature == null) {
                logger.warn("No Nagad signature found in webhook headers");
                return false;
            }
            
            PaymentProviderConfiguration.PaymentProperties.NagadConfig config = paymentProperties.getProviders().getNagad();
            String nagadPublicKey = config.getNagadPublicKey();
            
            if (nagadPublicKey == null || nagadPublicKey.isEmpty()) {
                logger.warn("Nagad public key not configured");
                return false;
            }
            
            return verifyNagadSignature(rawBody, signature, nagadPublicKey);
            
        } catch (Exception e) {
            logger.error("Error verifying Nagad webhook signature", e);
            return false;
        }
    }
    
    @Override
    public String name() {
        return "NAGAD";
    }
    
    /**
     * Creates the initialize payment request.
     */
    private Map<String, Object> createInitializeRequest(Order order, String orderId, PaymentProviderConfiguration.PaymentProperties.NagadConfig config) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("merchantId", config.getMerchantId());
        request.put("orderId", orderId);
        request.put("amount", order.getTotalAmount().toString());
        request.put("currency", config.getCurrency());
        
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        request.put("dateTime", timestamp);
        
        // Create signature for the request
        String dataToSign = config.getMerchantId() + orderId + order.getTotalAmount().toString() + config.getCurrency() + timestamp;
        String signature = createSignature(dataToSign, config.getMerchantPrivateKey());
        request.put("signature", signature);
        
        return request;
    }
    
    /**
     * Creates the complete payment request.
     */
    private Map<String, Object> createCompleteRequest(Order order, String orderId, String paymentReferenceId, PaymentProviderConfiguration.PaymentProperties.NagadConfig config) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("merchantId", config.getMerchantId());
        request.put("orderId", orderId);
        request.put("paymentReferenceId", paymentReferenceId);
        request.put("amount", order.getTotalAmount().toString());
        request.put("currency", config.getCurrency());
        request.put("challenge", generateChallenge());
        
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        request.put("dateTime", timestamp);
        
        // Create signature for the complete request
        String dataToSign = config.getMerchantId() + orderId + paymentReferenceId + order.getTotalAmount().toString() + config.getCurrency() + timestamp;
        String signature = createSignature(dataToSign, config.getMerchantPrivateKey());
        request.put("signature", signature);
        
        return request;
    }
    
    /**
     * Creates HTTP headers for Nagad API calls.
     */
    private HttpHeaders createHeaders(PaymentProviderConfiguration.PaymentProperties.NagadConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-KM-Api-Version", "v-0.2.0");
        headers.set("X-KM-IP-V4", "127.0.0.1");
        headers.set("X-KM-Client-Type", "PC_WEB");
        return headers;
    }
    
    /**
     * Creates a digital signature using RSA private key.
     */
    private String createSignature(String data, String privateKeyString) throws Exception {
        // Remove header and footer from private key
        String cleanPrivateKey = privateKeyString
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] keyBytes = Base64.getDecoder().decode(cleanPrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        
        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }
    
    /**
     * Verifies Nagad webhook signature using RSA public key.
     */
    private boolean verifyNagadSignature(String payload, String signature, String publicKeyString) {
        try {
            // Remove header and footer from public key
            String cleanPublicKey = publicKeyString
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            
            byte[] keyBytes = Base64.getDecoder().decode(cleanPublicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(payload.getBytes(StandardCharsets.UTF_8));
            
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return sig.verify(signatureBytes);
            
        } catch (Exception e) {
            logger.error("Error verifying Nagad signature", e);
            return false;
        }
    }
    
    /**
     * Generates a random challenge string for Nagad API.
     */
    private String generateChallenge() {
        return UUID.randomUUID().toString().replace("-", "");
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
     * Verifies a Nagad payment status.
     */
    public Map<String, Object> verifyPayment(String paymentReferenceId) throws Exception {
        PaymentProviderConfiguration.PaymentProperties.NagadConfig config = paymentProperties.getProviders().getNagad();
        
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String dataToSign = config.getMerchantId() + paymentReferenceId + timestamp;
        String signature = createSignature(dataToSign, config.getMerchantPrivateKey());
        
        Map<String, Object> verifyRequest = new HashMap<>();
        verifyRequest.put("merchantId", config.getMerchantId());
        verifyRequest.put("paymentReferenceId", paymentReferenceId);
        verifyRequest.put("dateTime", timestamp);
        verifyRequest.put("signature", signature);
        
        HttpHeaders headers = createHeaders(config);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(verifyRequest, headers);
        
        String verifyUrl = config.getBaseUrl() + "/remote-payment-gateway-1.0/api/dfs/verify/payment/" + paymentReferenceId;
        ResponseEntity<String> response = restTemplate.postForEntity(verifyUrl, request, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return objectMapper.convertValue(responseJson, Map.class);
        } else {
            throw new RuntimeException("Nagad verify payment failed with status: " + response.getStatusCode());
        }
    }
}
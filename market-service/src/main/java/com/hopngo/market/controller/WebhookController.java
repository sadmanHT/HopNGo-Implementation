package com.hopngo.market.controller;

import com.hopngo.market.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Unified webhook controller for handling payment provider webhooks.
 * Supports multiple payment providers with signature verification and idempotency.
 */
@RestController
@RequestMapping("/market/payments")
public class WebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    
    @Autowired
    private WebhookService webhookService;
    
    /**
     * Unified webhook endpoint for all payment providers.
     * Handles webhook events with signature verification and idempotency.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            HttpServletRequest request,
            @RequestParam(value = "provider", required = false) String provider) {
        
        logger.info("Received webhook request from provider: {}", provider);
        
        try {
            // Extract request body
            String requestBody = getRequestBody(request);
            
            // Extract headers
            Map<String, String> headers = extractHeaders(request);
            
            // Determine provider if not specified in query param
            if (provider == null || provider.isEmpty()) {
                provider = determineProviderFromHeaders(headers);
            }
            
            if (provider == null) {
                logger.warn("Unable to determine payment provider from webhook request");
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "error", "Unable to determine payment provider",
                        "message", "Provider parameter or identifying headers required"
                    ));
            }
            
            // Process webhook through service
            boolean processed = webhookService.processWebhook(provider, requestBody, headers, request);
            
            if (processed) {
                logger.info("Webhook processed successfully for provider: {}", provider);
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Webhook processed successfully"
                ));
            } else {
                logger.warn("Webhook processing failed for provider: {}", provider);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "status", "error",
                        "message", "Webhook processing failed"
                    ));
            }
            
        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Internal server error: " + e.getMessage()
                ));
        }
    }
    
    /**
     * Health check endpoint for webhook service.
     */
    @GetMapping("/webhook/health")
    public ResponseEntity<Map<String, Object>> webhookHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "webhook-handler",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Get webhook statistics.
     */
    @GetMapping("/webhook/stats")
    public ResponseEntity<Map<String, Object>> getWebhookStats() {
        try {
            Map<String, Object> stats = webhookService.getWebhookStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error retrieving webhook stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to retrieve webhook statistics",
                    "message", e.getMessage()
                ));
        }
    }
    
    /**
     * Extracts the request body from HttpServletRequest.
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
     * Extracts all headers from the request.
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        
        return headers;
    }
    
    /**
     * Determines the payment provider from request headers.
     */
    private String determineProviderFromHeaders(Map<String, String> headers) {
        // Check for Stripe signature header
        if (headers.containsKey("Stripe-Signature")) {
            return "STRIPE_TEST";
        }
        
        // Check for Mock signature header
        if (headers.containsKey("Mock-Signature")) {
            return "MOCK";
        }
        
        // Check for other provider-specific headers
        if (headers.containsKey("X-Bkash-Signature")) {
            return "BKASH";
        }
        
        if (headers.containsKey("X-Nagad-Signature")) {
            return "NAGAD";
        }
        
        // Check User-Agent for provider identification
        String userAgent = headers.get("User-Agent");
        if (userAgent != null) {
            if (userAgent.toLowerCase().contains("stripe")) {
                return "STRIPE_TEST";
            }
            if (userAgent.toLowerCase().contains("paypal")) {
                return "PAYPAL";
            }
        }
        
        return null;
    }
}
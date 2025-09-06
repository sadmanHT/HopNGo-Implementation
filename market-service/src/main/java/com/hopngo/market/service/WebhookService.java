package com.hopngo.market.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.market.entity.*;
import com.hopngo.market.repository.WebhookEventRepository;
import com.hopngo.market.repository.PaymentRepository;
import com.hopngo.market.service.payment.PaymentProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for handling webhook events with signature verification and idempotency.
 * Processes webhooks from multiple payment providers.
 */
@Service
@Transactional
public class WebhookService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private WebhookEventRepository webhookEventRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private EventPublishingService eventPublishingService;
    
    @Autowired
    private List<PaymentProvider> paymentProviders;
    
    /**
     * Processes webhook events with signature verification and idempotency.
     */
    public boolean processWebhook(String providerName, String requestBody, Map<String, String> headers, HttpServletRequest request) {
        logger.info("Processing webhook for provider: {}", providerName);
        
        try {
            // Find the appropriate payment provider
            PaymentProvider provider = findPaymentProvider(providerName);
            if (provider == null) {
                logger.error("Payment provider not found: {}", providerName);
                return false;
            }
            
            // Verify webhook signature
            if (!provider.verifyWebhook(request)) {
                logger.warn("Webhook signature verification failed for provider: {}", providerName);
                return false;
            }
            
            // Parse webhook payload
            JsonNode webhookData = objectMapper.readTree(requestBody);
            String webhookId = extractWebhookId(webhookData, providerName);
            String eventType = extractEventType(webhookData, providerName);
            
            if (webhookId == null) {
                logger.error("Unable to extract webhook ID from payload for provider: {}", providerName);
                return false;
            }
            
            // Check for idempotency
            Optional<WebhookEvent> existingEvent = webhookEventRepository.findByWebhookId(webhookId);
            if (existingEvent.isPresent()) {
                WebhookEvent event = existingEvent.get();
                if (event.isProcessed()) {
                    logger.info("Webhook already processed (idempotent): {}", webhookId);
                    return true;
                } else {
                    logger.info("Webhook exists but not processed, retrying: {}", webhookId);
                }
            }
            
            // Create or update webhook event record
            WebhookEvent webhookEvent = existingEvent.orElse(new WebhookEvent(webhookId, providerName, eventType, requestBody));
            webhookEvent.setRequestHeaders(headers);
            webhookEvent.setSignature(headers.get(getSignatureHeaderName(providerName)));
            webhookEvent.markAsProcessing();
            
            webhookEvent = webhookEventRepository.save(webhookEvent);
            
            // Process the webhook based on event type
            boolean processed = processWebhookEvent(webhookEvent, webhookData, providerName);
            
            if (processed) {
                webhookEvent.markAsProcessed();
                logger.info("Webhook processed successfully: {}", webhookId);
            } else {
                webhookEvent.markAsFailed("Failed to process webhook event");
                logger.error("Failed to process webhook: {}", webhookId);
            }
            
            webhookEventRepository.save(webhookEvent);
            return processed;
            
        } catch (Exception e) {
            logger.error("Error processing webhook for provider: {}", providerName, e);
            return false;
        }
    }
    
    /**
     * Processes specific webhook events based on type.
     */
    private boolean processWebhookEvent(WebhookEvent webhookEvent, JsonNode webhookData, String providerName) {
        String eventType = webhookEvent.getEventType();
        logger.info("Processing webhook event type: {} for provider: {}", eventType, providerName);
        
        try {
            switch (eventType.toLowerCase()) {
                case "payment_intent.succeeded":
                case "payment.succeeded":
                case "charge.succeeded":
                    return handlePaymentSucceeded(webhookEvent, webhookData, providerName);
                    
                case "payment_intent.payment_failed":
                case "payment.failed":
                case "charge.failed":
                    return handlePaymentFailed(webhookEvent, webhookData, providerName);
                    
                case "payment_intent.canceled":
                case "payment.canceled":
                    return handlePaymentCanceled(webhookEvent, webhookData, providerName);
                    
                default:
                    logger.info("Unhandled webhook event type: {}", eventType);
                    return true; // Consider unhandled events as successfully processed
            }
        } catch (Exception e) {
            logger.error("Error processing webhook event: {}", eventType, e);
            return false;
        }
    }
    
    /**
     * Handles payment succeeded webhook events.
     */
    private boolean handlePaymentSucceeded(WebhookEvent webhookEvent, JsonNode webhookData, String providerName) {
        try {
            String paymentIntentId = extractPaymentIntentId(webhookData, providerName);
            if (paymentIntentId == null) {
                logger.error("Unable to extract payment intent ID from webhook data");
                return false;
            }
            
            // Find payment by payment intent ID
            Optional<Payment> paymentOpt = paymentRepository.findByPaymentIntentId(paymentIntentId);
            if (paymentOpt.isEmpty()) {
                logger.error("Payment not found for payment intent ID: {}", paymentIntentId);
                return false;
            }
            
            Payment payment = paymentOpt.get();
            webhookEvent.setPaymentId(payment.getId());
            webhookEvent.setOrderId(payment.getOrder().getId());
            
            // Update payment status
            String transactionId = extractTransactionId(webhookData, providerName);
            payment.markAsSucceeded(transactionId);
            paymentRepository.save(payment);
            
            // Publish payment succeeded event
            eventPublishingService.publishPaymentSucceededEvent(payment);
            
            logger.info("Payment succeeded processed for payment: {}", payment.getId());
            return true;
            
        } catch (Exception e) {
            logger.error("Error handling payment succeeded webhook", e);
            return false;
        }
    }
    
    /**
     * Handles payment failed webhook events.
     */
    private boolean handlePaymentFailed(WebhookEvent webhookEvent, JsonNode webhookData, String providerName) {
        try {
            String paymentIntentId = extractPaymentIntentId(webhookData, providerName);
            if (paymentIntentId == null) {
                return false;
            }
            
            Optional<Payment> paymentOpt = paymentRepository.findByPaymentIntentId(paymentIntentId);
            if (paymentOpt.isEmpty()) {
                logger.error("Payment not found for payment intent ID: {}", paymentIntentId);
                return false;
            }
            
            Payment payment = paymentOpt.get();
            webhookEvent.setPaymentId(payment.getId());
            webhookEvent.setOrderId(payment.getOrder().getId());
            
            String failureReason = extractFailureReason(webhookData, providerName);
            payment.markAsFailed(failureReason);
            paymentRepository.save(payment);
            
            logger.info("Payment failed processed for payment: {}", payment.getId());
            return true;
            
        } catch (Exception e) {
            logger.error("Error handling payment failed webhook", e);
            return false;
        }
    }
    
    /**
     * Handles payment canceled webhook events.
     */
    private boolean handlePaymentCanceled(WebhookEvent webhookEvent, JsonNode webhookData, String providerName) {
        try {
            String paymentIntentId = extractPaymentIntentId(webhookData, providerName);
            if (paymentIntentId == null) {
                return false;
            }
            
            Optional<Payment> paymentOpt = paymentRepository.findByPaymentIntentId(paymentIntentId);
            if (paymentOpt.isEmpty()) {
                logger.error("Payment not found for payment intent ID: {}", paymentIntentId);
                return false;
            }
            
            Payment payment = paymentOpt.get();
            webhookEvent.setPaymentId(payment.getId());
            webhookEvent.setOrderId(payment.getOrder().getId());
            
            payment.markAsCancelled("Payment canceled via webhook");
            paymentRepository.save(payment);
            
            logger.info("Payment canceled processed for payment: {}", payment.getId());
            return true;
            
        } catch (Exception e) {
            logger.error("Error handling payment canceled webhook", e);
            return false;
        }
    }
    
    /**
     * Gets webhook statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWebhookStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count by status
        for (WebhookEventStatus status : WebhookEventStatus.values()) {
            long count = webhookEventRepository.countByStatus(status);
            stats.put(status.name().toLowerCase() + "_count", count);
        }
        
        // Count by provider
        Map<String, Long> providerCounts = new HashMap<>();
        for (PaymentProvider provider : paymentProviders) {
            long count = webhookEventRepository.countByProviderAndStatus(provider.name(), WebhookEventStatus.PROCESSED);
            providerCounts.put(provider.name(), count);
        }
        stats.put("provider_counts", providerCounts);
        
        return stats;
    }
    
    // Helper methods for extracting data from webhook payloads
    
    private PaymentProvider findPaymentProvider(String providerName) {
        return paymentProviders.stream()
            .filter(provider -> provider.name().equals(providerName))
            .findFirst()
            .orElse(null);
    }
    
    private String extractWebhookId(JsonNode webhookData, String providerName) {
        switch (providerName) {
            case "STRIPE_TEST":
                return webhookData.path("id").asText(null);
            case "MOCK":
                return webhookData.path("webhook_id").asText(null);
            default:
                return webhookData.path("id").asText(null);
        }
    }
    
    private String extractEventType(JsonNode webhookData, String providerName) {
        switch (providerName) {
            case "STRIPE_TEST":
                return webhookData.path("type").asText(null);
            case "MOCK":
                return webhookData.path("event_type").asText(null);
            default:
                return webhookData.path("type").asText(null);
        }
    }
    
    private String extractPaymentIntentId(JsonNode webhookData, String providerName) {
        switch (providerName) {
            case "STRIPE_TEST":
                return webhookData.path("data").path("object").path("id").asText(null);
            case "MOCK":
                return webhookData.path("payment_intent_id").asText(null);
            default:
                return webhookData.path("payment_intent_id").asText(null);
        }
    }
    
    private String extractTransactionId(JsonNode webhookData, String providerName) {
        switch (providerName) {
            case "STRIPE_TEST":
                return webhookData.path("data").path("object").path("charges").path("data").get(0).path("id").asText(null);
            case "MOCK":
                return webhookData.path("transaction_id").asText(null);
            default:
                return webhookData.path("transaction_id").asText(null);
        }
    }
    
    private String extractFailureReason(JsonNode webhookData, String providerName) {
        switch (providerName) {
            case "STRIPE_TEST":
                return webhookData.path("data").path("object").path("last_payment_error").path("message").asText("Payment failed");
            case "MOCK":
                return webhookData.path("failure_reason").asText("Payment failed");
            default:
                return webhookData.path("failure_reason").asText("Payment failed");
        }
    }
    
    private String getSignatureHeaderName(String providerName) {
        switch (providerName) {
            case "STRIPE_TEST":
                return "Stripe-Signature";
            case "MOCK":
                return "Mock-Signature";
            case "BKASH":
                return "X-Bkash-Signature";
            case "NAGAD":
                return "X-Nagad-Signature";
            default:
                return "Signature";
        }
    }
}
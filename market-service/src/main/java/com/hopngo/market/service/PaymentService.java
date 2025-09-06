package com.hopngo.market.service;

import com.hopngo.market.entity.*;
import com.hopngo.market.repository.PaymentRepository;
import com.hopngo.market.service.payment.PaymentProvider;
import com.hopngo.market.dto.PaymentIntentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private EventPublishingService eventPublishingService;
    
    @Autowired
    private List<PaymentProvider> paymentProviders;
    
    @Value("${payment.default.provider:MOCK}")
    private String defaultProviderName;
    
    // Create payment intent using PaymentProvider abstraction
    public Payment createPaymentIntent(Order order, com.hopngo.market.entity.PaymentProvider providerEnum) {
        logger.info("Creating payment intent for order: {}, provider: {}", order.getId(), providerEnum);
        
        // Check if payment already exists for this order
        Optional<Payment> existingPayment = paymentRepository.findByOrder_Id(order.getId());
        if (existingPayment.isPresent()) {
            logger.warn("Payment already exists for order: {}", order.getId());
            return existingPayment.get();
        }
        
        // Find the appropriate payment provider implementation
        PaymentProvider provider = findPaymentProvider(providerEnum.name());
        if (provider == null) {
            throw new IllegalArgumentException("Payment provider not found: " + providerEnum);
        }
        
        // Create payment entity
        Payment payment = new Payment(order, providerEnum, order.getTotalAmount(), order.getCurrency());
        payment = paymentRepository.save(payment);
        
        try {
            // Use provider to create payment intent
            PaymentIntentResponse response = provider.createPaymentIntent(order);
            
            // Update payment with provider response
            payment.setPaymentIntentId(response.getPaymentIntentId());
            payment.setClientSecret(response.getClientSecret());
            
            if (response.getStatus() != null) {
                // Map provider status to our PaymentStatus if needed
                switch (response.getStatus().toLowerCase()) {
                    case "requires_payment_method":
                    case "requires_confirmation":
                    case "pending":
                        payment.setStatus(PaymentStatus.PENDING);
                        break;
                    case "processing":
                        payment.setStatus(PaymentStatus.PROCESSING);
                        break;
                    case "succeeded":
                        payment.setStatus(PaymentStatus.SUCCEEDED);
                        break;
                    case "canceled":
                        payment.setStatus(PaymentStatus.CANCELLED);
                        break;
                    default:
                        logger.warn("Unknown payment status from provider: {}", response.getStatus());
                }
            }
            
            payment = paymentRepository.save(payment);
            logger.info("Payment intent created successfully: {} with provider: {}", payment.getId(), providerEnum);
            
            return payment;
            
        } catch (Exception e) {
            logger.error("Failed to create payment intent with provider: {}", providerEnum, e);
            payment.markAsFailed("Failed to create payment intent: " + e.getMessage());
            paymentRepository.save(payment);
            throw new RuntimeException("Failed to create payment intent", e);
        }
    }
    
    // Create payment intent with default provider
    public Payment createPaymentIntent(Order order) {
        com.hopngo.market.entity.PaymentProvider defaultProvider = com.hopngo.market.entity.PaymentProvider.valueOf(defaultProviderName);
        return createPaymentIntent(order, defaultProvider);
    }
    
    // Find payment provider implementation by name
    private PaymentProvider findPaymentProvider(String providerName) {
        return paymentProviders.stream()
            .filter(provider -> provider.name().equals(providerName))
            .findFirst()
            .orElse(null);
    }
    
    // Get payment by ID
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentById(UUID paymentId) {
        logger.debug("Fetching payment by ID: {}", paymentId);
        return paymentRepository.findById(paymentId);
    }
    
    // Get payment by order ID
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByOrderId(UUID orderId) {
        logger.debug("Fetching payment by order ID: {}", orderId);
        return paymentRepository.findByOrder_Id(orderId);
    }
    
    // Get payment by transaction reference
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByTransactionReference(String transactionReference) {
        logger.debug("Fetching payment by transaction reference: {}", transactionReference);
        return paymentRepository.findByTransactionReference(transactionReference);
    }
    
    // Get payment by provider transaction ID
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByProviderTransactionId(String providerTransactionId) {
        logger.debug("Fetching payment by provider transaction ID: {}", providerTransactionId);
        return paymentRepository.findByProviderTransactionId(providerTransactionId);
    }
    
    // Get payment by payment intent ID
    @Transactional(readOnly = true)
    public Optional<Payment> findByPaymentIntentId(String paymentIntentId) {
        logger.debug("Fetching payment by payment intent ID: {}", paymentIntentId);
        return paymentRepository.findByPaymentIntentId(paymentIntentId);
    }
    
    // Get payment provider by name
    public PaymentProvider getProviderByName(String providerName) {
        logger.debug("Getting payment provider by name: {}", providerName);
        return findPaymentProvider(providerName);
    }
    
    // Update payment status (used by webhook processing)
    public Payment updatePaymentStatus(Payment payment, PaymentStatus newStatus) {
        logger.info("Updating payment status: {} -> {}", payment.getStatus(), newStatus);
        
        switch (newStatus) {
            case SUCCEEDED:
                payment.markAsSucceeded(payment.getProviderTransactionId());
                break;
            case FAILED:
                payment.markAsFailed(payment.getFailureReason() != null ? payment.getFailureReason() : "Payment failed");
                break;
            case PROCESSING:
                payment.markAsProcessing();
                break;
            case CANCELLED:
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setProcessedAt(LocalDateTime.now());
                break;
            default:
                payment.setStatus(newStatus);
                break;
        }
        
        return paymentRepository.save(payment);
    }
    
    // Get payments by status
    @Transactional(readOnly = true)
    public Page<Payment> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        logger.debug("Fetching payments by status: {}", status);
        return paymentRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }
    
    // Process mock payment (simulate payment processing)
    public Payment processMockPayment(UUID paymentId, boolean shouldSucceed) {
        logger.info("Processing mock payment: {}, shouldSucceed: {}", paymentId, shouldSucceed);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        if (!payment.getProvider().name().equals(PaymentProvider.MOCK)) {
            throw new IllegalArgumentException("Only mock payments can be processed through this method");
        }
        
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment is not in pending status: " + payment.getStatus());
        }
        
        // Mark as processing
        payment.markAsProcessing();
        payment = paymentRepository.save(payment);
        
        // Simulate processing delay
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 3000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (shouldSucceed) {
            // Mark as succeeded
            String mockTransactionId = "mock_txn_" + UUID.randomUUID().toString().substring(0, 12);
            payment.markAsSucceeded(mockTransactionId);
            
            // Publish success event
            eventPublishingService.publishPaymentSucceededEvent(payment);
            
            logger.info("Mock payment succeeded: {}", paymentId);
        } else {
            // Mark as failed
            payment.markAsFailed("Mock payment failure for testing");
            
            // Publish failure event
            eventPublishingService.publishPaymentFailedEvent(payment);
            
            logger.info("Mock payment failed: {}", paymentId);
        }
        
        return paymentRepository.save(payment);
    }
    
    // Handle webhook for payment status update
    public Payment handlePaymentWebhook(String transactionReference, PaymentStatus status, 
                                      String providerTransactionId, String failureReason) {
        logger.info("Handling payment webhook - txnRef: {}, status: {}", transactionReference, status);
        
        Payment payment = paymentRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for transaction reference: " + transactionReference));
        
        // Prevent duplicate webhook processing
        if (payment.getWebhookReceivedAt() != null) {
            logger.warn("Webhook already processed for payment: {}", payment.getId());
            return payment;
        }
        
        payment.setWebhookReceivedAt(LocalDateTime.now());
        
        switch (status) {
            case SUCCEEDED:
                if (payment.getStatus() == PaymentStatus.PROCESSING || payment.getStatus() == PaymentStatus.PENDING) {
                    payment.markAsSucceeded(providerTransactionId);
                    eventPublishingService.publishPaymentSucceededEvent(payment);
                    logger.info("Payment marked as succeeded via webhook: {}", payment.getId());
                }
                break;
                
            case FAILED:
                if (payment.getStatus() == PaymentStatus.PROCESSING || payment.getStatus() == PaymentStatus.PENDING) {
                    payment.markAsFailed(failureReason != null ? failureReason : "Payment failed");
                    eventPublishingService.publishPaymentFailedEvent(payment);
                    logger.info("Payment marked as failed via webhook: {}", payment.getId());
                }
                break;
                
            case PROCESSING:
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.markAsProcessing();
                    logger.info("Payment marked as processing via webhook: {}", payment.getId());
                }
                break;
                
            default:
                logger.warn("Unhandled payment status in webhook: {}", status);
        }
        
        return paymentRepository.save(payment);
    }
    
    // Handle webhook with idempotency key
    public Payment handlePaymentWebhookIdempotent(String transactionReference, PaymentStatus status, 
                                                 String providerTransactionId, String failureReason, 
                                                 String idempotencyKey) {
        logger.info("Handling idempotent payment webhook - txnRef: {}, status: {}, idempotencyKey: {}", 
                   transactionReference, status, idempotencyKey);
        
        // In a real implementation, you would store and check idempotency keys
        // For now, we'll just delegate to the regular webhook handler
        return handlePaymentWebhook(transactionReference, status, providerTransactionId, failureReason);
    }
    
    // Cancel payment
    public Payment cancelPayment(UUID paymentId, String reason) {
        logger.info("Cancelling payment: {}, reason: {}", paymentId, reason);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Payment cannot be cancelled. Current status: " + payment.getStatus());
        }
        
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setFailureReason(reason);
        payment.setProcessedAt(LocalDateTime.now());
        
        return paymentRepository.save(payment);
    }
    
    // Refund payment (placeholder for future implementation)
    public Payment refundPayment(UUID paymentId, String reason) {
        logger.info("Refunding payment: {}, reason: {}", paymentId, reason);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Only succeeded payments can be refunded. Current status: " + payment.getStatus());
        }
        
        // In a real implementation, you would call the payment provider's refund API
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setMetadata("Refund reason: " + reason);
        
        return paymentRepository.save(payment);
    }
    
    // Get payment statistics
    @Transactional(readOnly = true)
    public PaymentStats getPaymentStats() {
        logger.debug("Calculating payment statistics");
        
        PaymentStats stats = new PaymentStats();
        
        // Get counts by status
        paymentRepository.countPaymentsByStatus().forEach(result -> {
            PaymentStatus status = (PaymentStatus) result[0];
            Long count = (Long) result[1];
            stats.addStatusCount(status, count);
        });
        
        // Get total successful amount
        stats.setTotalSuccessfulAmount(paymentRepository.calculateTotalSuccessfulPaymentAmount());
        
        return stats;
    }
    
    // Inner class for payment statistics
    public static class PaymentStats {
        private final java.util.Map<PaymentStatus, Long> statusCounts = new java.util.HashMap<>();
        private java.math.BigDecimal totalSuccessfulAmount = java.math.BigDecimal.ZERO;
        
        public void addStatusCount(PaymentStatus status, Long count) {
            statusCounts.put(status, count);
        }
        
        public java.util.Map<PaymentStatus, Long> getStatusCounts() {
            return statusCounts;
        }
        
        public java.math.BigDecimal getTotalSuccessfulAmount() {
            return totalSuccessfulAmount;
        }
        
        public void setTotalSuccessfulAmount(java.math.BigDecimal totalSuccessfulAmount) {
            this.totalSuccessfulAmount = totalSuccessfulAmount != null ? totalSuccessfulAmount : java.math.BigDecimal.ZERO;
        }
        
        public Long getSuccessfulPaymentCount() {
            return statusCounts.getOrDefault(PaymentStatus.SUCCEEDED, 0L);
        }
        
        public Long getFailedPaymentCount() {
            return statusCounts.getOrDefault(PaymentStatus.FAILED, 0L);
        }
        
        public Long getPendingPaymentCount() {
            return statusCounts.getOrDefault(PaymentStatus.PENDING, 0L);
        }
        
        public double getSuccessRate() {
            long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();
            if (total == 0) return 0.0;
            return (getSuccessfulPaymentCount() * 100.0) / total;
        }
    }
}
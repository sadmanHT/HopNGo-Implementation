package com.hopngo.market.controller;

import com.hopngo.market.entity.Payment;
import com.hopngo.market.entity.PaymentStatus;
import com.hopngo.market.service.PaymentService;
import com.hopngo.market.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/market/payments")
@Tag(name = "Payments", description = "Payment processing and webhook endpoints")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private OrderService orderService;
    
    // Get payment by ID
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Retrieve payment details by payment ID")
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId) {
        
        logger.info("Getting payment: {}", paymentId);
        
        Optional<Payment> payment = paymentService.getPaymentById(paymentId);
        
        if (payment.isPresent()) {
            PaymentResponse response = new PaymentResponse(payment.get());
            logger.info("Payment found: {}, status: {}", paymentId, payment.get().getStatus());
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Payment not found: {}", paymentId);
            return ResponseEntity.notFound().build();
        }
    }
    
    // Get payment by order ID
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID", description = "Retrieve payment details by order ID")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {
        
        logger.info("Getting payment for order: {}", orderId);
        
        Optional<Payment> payment = paymentService.getPaymentByOrderId(orderId);
        
        if (payment.isPresent()) {
            PaymentResponse response = new PaymentResponse(payment.get());
            logger.info("Payment found for order: {}, paymentId: {}", orderId, payment.get().getId());
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Payment not found for order: {}", orderId);
            return ResponseEntity.notFound().build();
        }
    }
    
    // Process mock payment (for testing)
    @PostMapping("/{paymentId}/process-mock")
    @Operation(summary = "Process mock payment", description = "Process a mock payment for testing purposes")
    public ResponseEntity<PaymentResponse> processMockPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId,
            @Valid @RequestBody MockPaymentRequest request) {
        
        logger.info("Processing mock payment: {}, shouldSucceed: {}", paymentId, request.shouldSucceed());
        
        try {
            Payment payment = paymentService.processMockPayment(paymentId, request.shouldSucceed());
            
            // If payment succeeded, mark order as paid
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                orderService.markOrderAsPaid(payment.getOrder().getId());
            }
            
            PaymentResponse response = new PaymentResponse(payment);
            logger.info("Mock payment processed: {}, status: {}", paymentId, payment.getStatus());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to process mock payment: {}, error: {}", paymentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error processing mock payment: {}", paymentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Note: Webhook handling is now managed by WebhookController for unified processing
    
    // Cancel payment
    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "Cancel payment", description = "Cancel a pending or processing payment")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId,
            @Valid @RequestBody CancelPaymentRequest request) {
        
        logger.info("Cancelling payment: {}, reason: {}", paymentId, request.getReason());
        
        try {
            Payment payment = paymentService.cancelPayment(paymentId, request.getReason());
            
            PaymentResponse response = new PaymentResponse(payment);
            logger.info("Payment cancelled successfully: {}", paymentId);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to cancel payment: {}, error: {}", paymentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Cannot cancel payment: {}, error: {}", paymentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error cancelling payment: {}", paymentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Get payment statistics
    @GetMapping("/stats")
    @Operation(summary = "Get payment statistics", description = "Retrieve payment statistics and metrics")
    public ResponseEntity<PaymentService.PaymentStats> getPaymentStats() {
        logger.info("Getting payment statistics");
        
        PaymentService.PaymentStats stats = paymentService.getPaymentStats();
        
        logger.info("Retrieved payment statistics - successful: {}, failed: {}, total amount: {}", 
                   stats.getSuccessfulPaymentCount(), stats.getFailedPaymentCount(), stats.getTotalSuccessfulAmount());
        
        return ResponseEntity.ok(stats);
    }
    
    // Request DTOs
    public static class MockPaymentRequest {
        private boolean shouldSucceed = true;
        
        public MockPaymentRequest() {}
        
        public MockPaymentRequest(boolean shouldSucceed) {
            this.shouldSucceed = shouldSucceed;
        }
        
        public boolean shouldSucceed() { return shouldSucceed; }
        public void setShouldSucceed(boolean shouldSucceed) { this.shouldSucceed = shouldSucceed; }
    }
    
    public static class PaymentWebhookRequest {
        @NotBlank
        private String transactionReference;
        
        @NotNull
        private PaymentStatus status;
        
        private String providerTransactionId;
        private String failureReason;
        
        public PaymentWebhookRequest() {}
        
        public PaymentWebhookRequest(String transactionReference, PaymentStatus status, 
                                   String providerTransactionId, String failureReason) {
            this.transactionReference = transactionReference;
            this.status = status;
            this.providerTransactionId = providerTransactionId;
            this.failureReason = failureReason;
        }
        
        // Getters and setters
        public String getTransactionReference() { return transactionReference; }
        public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
        
        public PaymentStatus getStatus() { return status; }
        public void setStatus(PaymentStatus status) { this.status = status; }
        
        public String getProviderTransactionId() { return providerTransactionId; }
        public void setProviderTransactionId(String providerTransactionId) { this.providerTransactionId = providerTransactionId; }
        
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    }
    
    public static class CancelPaymentRequest {
        @NotBlank
        private String reason;
        
        public CancelPaymentRequest() {}
        
        public CancelPaymentRequest(String reason) {
            this.reason = reason;
        }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    // Response DTOs
    public static class PaymentResponse {
        private UUID id;
        private UUID orderId;
        private String status;
        private String provider;
        private java.math.BigDecimal amount;
        private String currency;
        private String transactionReference;
        private String providerTransactionId;
        private String paymentIntentId;
        private String failureReason;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime processedAt;
        
        public PaymentResponse(Payment payment) {
            this.id = payment.getId();
            this.orderId = payment.getOrder().getId();
            this.status = payment.getStatus().toString();
            this.provider = payment.getProvider().toString();
            this.amount = payment.getAmount();
            this.currency = payment.getCurrency();
            this.transactionReference = payment.getTransactionReference();
            this.providerTransactionId = payment.getProviderTransactionId();
            this.paymentIntentId = payment.getPaymentIntentId();
            this.failureReason = payment.getFailureReason();
            this.createdAt = payment.getCreatedAt();
            this.processedAt = payment.getProcessedAt();
        }
        
        // Getters
        public UUID getId() { return id; }
        public UUID getOrderId() { return orderId; }
        public String getStatus() { return status; }
        public String getProvider() { return provider; }
        public java.math.BigDecimal getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getTransactionReference() { return transactionReference; }
        public String getProviderTransactionId() { return providerTransactionId; }
        public String getPaymentIntentId() { return paymentIntentId; }
        public String getFailureReason() { return failureReason; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public java.time.LocalDateTime getProcessedAt() { return processedAt; }
    }
    
    public static class WebhookResponse {
        private String status;
        private String message;
        private String paymentId;
        
        public WebhookResponse(String status, String message, String paymentId) {
            this.status = status;
            this.message = message;
            this.paymentId = paymentId;
        }
        
        // Getters
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public String getPaymentId() { return paymentId; }
    }
}
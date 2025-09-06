package com.hopngo.market.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;
    
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String currency = "USD";
    
    @Column(name = "transaction_reference", unique = true)
    private String transactionReference;
    
    @Column(name = "provider_transaction_id")
    private String providerTransactionId;
    
    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "webhook_received_at")
    private LocalDateTime webhookReceivedAt;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public Payment() {}
    
    public Payment(Order order, PaymentProvider provider, BigDecimal amount, String currency) {
        this.order = order;
        this.provider = provider;
        this.amount = amount;
        this.currency = currency;
        this.transactionReference = generateTransactionReference();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Order getOrder() {
        return order;
    }
    
    public void setOrder(Order order) {
        this.order = order;
    }
    
    public void setOrderId(UUID orderId) {
        if (this.order == null) {
            this.order = new Order();
        }
        this.order.setId(orderId);
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
    
    public PaymentProvider getProvider() {
        return provider;
    }
    
    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getTransactionReference() {
        return transactionReference;
    }
    
    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
    
    public String getProviderTransactionId() {
        return providerTransactionId;
    }
    
    public void setProviderTransactionId(String providerTransactionId) {
        this.providerTransactionId = providerTransactionId;
    }
    
    public String getPaymentIntentId() {
        return paymentIntentId;
    }
    
    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public LocalDateTime getWebhookReceivedAt() {
        return webhookReceivedAt;
    }
    
    public void setWebhookReceivedAt(LocalDateTime webhookReceivedAt) {
        this.webhookReceivedAt = webhookReceivedAt;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Business methods
    public void markAsSucceeded(String providerTransactionId) {
        this.status = PaymentStatus.SUCCEEDED;
        this.providerTransactionId = providerTransactionId;
        this.processedAt = LocalDateTime.now();
        this.webhookReceivedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String failureReason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
        this.processedAt = LocalDateTime.now();
        this.webhookReceivedAt = LocalDateTime.now();
    }
    
    public void markAsProcessing() {
        this.status = PaymentStatus.PROCESSING;
    }
    
    public void markAsCancelled(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public boolean isSucceeded() {
        return status == PaymentStatus.SUCCEEDED;
    }
    
    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }
    
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }
    
    public boolean isProcessing() {
        return status == PaymentStatus.PROCESSING;
    }
    
    private String generateTransactionReference() {
        return "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
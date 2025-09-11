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
@Table(name = "refunds")
@EntityListeners(AuditingEntityListener.class)
public class Refund {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    
    @NotNull
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status = RefundStatus.PENDING;
    
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String currency = "USD";
    
    @Column(name = "refund_reference", unique = true)
    private String refundReference;
    
    @Column(name = "provider_refund_id")
    private String providerRefundId;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public Refund() {}
    
    public Refund(Payment payment, UUID bookingId, BigDecimal amount, String currency, String reason) {
        this.payment = payment;
        this.bookingId = bookingId;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
        this.refundReference = generateRefundReference();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Payment getPayment() {
        return payment;
    }
    
    public void setPayment(Payment payment) {
        this.payment = payment;
    }
    
    public UUID getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }
    
    public RefundStatus getStatus() {
        return status;
    }
    
    public void setStatus(RefundStatus status) {
        this.status = status;
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
    
    public String getRefundReference() {
        return refundReference;
    }
    
    public void setRefundReference(String refundReference) {
        this.refundReference = refundReference;
    }
    
    public String getProviderRefundId() {
        return providerRefundId;
    }
    
    public void setProviderRefundId(String providerRefundId) {
        this.providerRefundId = providerRefundId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
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
    public void markAsSucceeded(String providerRefundId) {
        this.status = RefundStatus.SUCCEEDED;
        this.providerRefundId = providerRefundId;
        this.processedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String failureReason) {
        this.status = RefundStatus.FAILED;
        this.failureReason = failureReason;
        this.processedAt = LocalDateTime.now();
    }
    
    public void markAsProcessing() {
        this.status = RefundStatus.PROCESSING;
    }
    
    public boolean isSucceeded() {
        return RefundStatus.SUCCEEDED.equals(this.status);
    }
    
    public boolean isFailed() {
        return RefundStatus.FAILED.equals(this.status);
    }
    
    public boolean isPending() {
        return RefundStatus.PENDING.equals(this.status);
    }
    
    public boolean isProcessing() {
        return RefundStatus.PROCESSING.equals(this.status);
    }
    
    private String generateRefundReference() {
        return "REF-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
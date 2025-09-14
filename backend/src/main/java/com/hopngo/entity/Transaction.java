package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider")
    private PaymentProvider paymentProvider;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status = TransactionStatus.PENDING;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "fee", precision = 19, scale = 4)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "processing_fee", precision = 19, scale = 4)
    private BigDecimal processingFee = BigDecimal.ZERO;

    @Column(name = "platform_fee", precision = 19, scale = 4)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @NotNull
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "reconciled")
    private Boolean reconciled = false;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "dispute_id")
    private String disputeId;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    @Column(name = "refund_id")
    private String refundId;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_amount", precision = 19, scale = 4)
    private BigDecimal refundAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // Constructors
    public Transaction() {}

    public Transaction(Order order, String transactionId, PaymentProvider paymentProvider, 
                      TransactionType transactionType, BigDecimal amount, String currency) {
        this.order = order;
        this.transactionId = transactionId;
        this.paymentProvider = paymentProvider;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currency = currency;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public void setProviderTransactionId(String providerTransactionId) {
        this.providerTransactionId = providerTransactionId;
    }

    public PaymentProvider getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(PaymentProvider paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
        if (status == TransactionStatus.SUCCESS && processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getProcessingFee() {
        return processingFee;
    }

    public void setProcessingFee(BigDecimal processingFee) {
        this.processingFee = processingFee;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Boolean getReconciled() {
        return reconciled;
    }

    public void setReconciled(Boolean reconciled) {
        this.reconciled = reconciled;
        if (reconciled && reconciledAt == null) {
            reconciledAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(LocalDateTime reconciledAt) {
        this.reconciledAt = reconciledAt;
    }

    public String getDisputeId() {
        return disputeId;
    }

    public void setDisputeId(String disputeId) {
        this.disputeId = disputeId;
        if (disputeId != null && disputedAt == null) {
            disputedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getDisputedAt() {
        return disputedAt;
    }

    public void setDisputedAt(LocalDateTime disputedAt) {
        this.disputedAt = disputedAt;
    }

    public String getRefundId() {
        return refundId;
    }

    public void setRefundId(String refundId) {
        this.refundId = refundId;
        if (refundId != null && refundedAt == null) {
            refundedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
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

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    // Helper methods
    public BigDecimal getNetAmount() {
        return amount.subtract(fee);
    }

    public boolean isSuccessful() {
        return status == TransactionStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }

    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    public boolean isDisputed() {
        return disputeId != null;
    }

    public boolean isRefunded() {
        return refundId != null;
    }

    public enum PaymentProvider {
        STRIPE("Stripe", "stripe"),
        BKASH("bKash", "bkash"),
        NAGAD("Nagad", "nagad");

        private final String displayName;
        private final String code;

        PaymentProvider(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCode() {
            return code;
        }
    }

    public enum TransactionType {
        PAYMENT("Payment", "Regular payment transaction"),
        REFUND("Refund", "Refund transaction"),
        CHARGEBACK("Chargeback", "Chargeback transaction"),
        ADJUSTMENT("Adjustment", "Manual adjustment transaction");

        private final String displayName;
        private final String description;

        TransactionType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum TransactionStatus {
        PENDING("Pending", "Transaction is being processed"),
        SUCCESS("Success", "Transaction completed successfully"),
        FAILED("Failed", "Transaction failed"),
        CANCELLED("Cancelled", "Transaction was cancelled"),
        DISPUTED("Disputed", "Transaction is under dispute");

        private final String displayName;
        private final String description;

        TransactionStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", transactionId='" + transactionId + '\'' +
                ", paymentProvider=" + paymentProvider +
                ", status=" + status +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
package com.hopngo.market.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a provider payout request.
 */
@Entity
@Table(name = "payouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payout {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "provider_id", nullable = false)
    private UUID providerId;
    
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutMethod method;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;
    
    // Bank details (for BANK method)
    @Column(name = "bank_name")
    private String bankName;
    
    @Column(name = "account_number")
    private String accountNumber;
    
    @Column(name = "account_holder_name")
    private String accountHolderName;
    
    @Column(name = "routing_number")
    private String routingNumber;
    
    // Mobile money details (for mobile methods)
    @Column(name = "mobile_number")
    private String mobileNumber;
    
    @Column(name = "mobile_account_name")
    private String mobileAccountName;
    
    // Processing details
    @Column(name = "requested_by")
    private UUID requestedBy;
    
    @Column(name = "approved_by")
    private UUID approvedBy;
    
    @Column(name = "processed_by")
    private UUID processedBy;
    
    // Reference and tracking
    @Column(name = "reference_number")
    private String referenceNumber;
    
    @Column(name = "external_transaction_id")
    private String externalTransactionId;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    // Timestamps
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "executed_at")
    private LocalDateTime executedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Get amount as decimal for display.
     */
    public BigDecimal getAmountDecimal() {
        return new BigDecimal(amountMinor).divide(new BigDecimal("100"));
    }
    
    /**
     * Set amount from decimal.
     */
    public void setAmountFromDecimal(BigDecimal amount) {
        this.amountMinor = amount.multiply(new BigDecimal("100")).longValue();
    }
    
    /**
     * Check if payout is pending.
     */
    public boolean isPending() {
        return PayoutStatus.PENDING.equals(this.status);
    }
    
    /**
     * Check if payout is approved.
     */
    public boolean isApproved() {
        return PayoutStatus.APPROVED.equals(this.status);
    }
    
    /**
     * Check if payout is processing.
     */
    public boolean isProcessing() {
        return PayoutStatus.PROCESSING.equals(this.status);
    }
    
    /**
     * Check if payout is paid.
     */
    public boolean isPaid() {
        return PayoutStatus.PAID.equals(this.status);
    }
    
    /**
     * Check if payout is failed.
     */
    public boolean isFailed() {
        return PayoutStatus.FAILED.equals(this.status);
    }
    
    /**
     * Check if payout is cancelled.
     */
    public boolean isCancelled() {
        return PayoutStatus.CANCELLED.equals(this.status);
    }
    
    /**
     * Check if payout is final (paid, failed, or cancelled).
     */
    public boolean isFinal() {
        return isPaid() || isFailed() || isCancelled();
    }
    
    /**
     * Check if payout can be approved.
     */
    public boolean canBeApproved() {
        return isPending();
    }
    
    /**
     * Check if payout can be cancelled.
     */
    public boolean canBeCancelled() {
        return isPending() || isApproved();
    }
    
    /**
     * Check if payout can be processed.
     */
    public boolean canBeProcessed() {
        return isApproved();
    }
    
    /**
     * Check if payout uses bank method.
     */
    public boolean isBankMethod() {
        return PayoutMethod.BANK.equals(this.method);
    }
    
    /**
     * Check if payout uses mobile money method.
     */
    public boolean isMobileMethod() {
        return method != null && (method.equals(PayoutMethod.BKASH) || 
                                 method.equals(PayoutMethod.NAGAD) || 
                                 method.equals(PayoutMethod.ROCKET));
    }
    
    /**
     * Approve the payout.
     */
    public void approve(UUID approvedBy) {
        if (!canBeApproved()) {
            throw new IllegalStateException("Payout cannot be approved in current status: " + status);
        }
        this.status = PayoutStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }
    
    /**
     * Cancel the payout.
     */
    public void cancel(String reason) {
        if (!canBeCancelled()) {
            throw new IllegalStateException("Payout cannot be cancelled in current status: " + status);
        }
        this.status = PayoutStatus.CANCELLED;
        this.failureReason = reason;
        this.executedAt = LocalDateTime.now();
    }
    
    /**
     * Start processing the payout.
     */
    public void startProcessing(UUID processedBy) {
        if (!canBeProcessed()) {
            throw new IllegalStateException("Payout cannot be processed in current status: " + status);
        }
        this.status = PayoutStatus.PROCESSING;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * Mark payout as paid.
     */
    public void markAsPaid(String externalTransactionId) {
        if (!isProcessing()) {
            throw new IllegalStateException("Only processing payouts can be marked as paid");
        }
        this.status = PayoutStatus.PAID;
        this.externalTransactionId = externalTransactionId;
        this.executedAt = LocalDateTime.now();
    }
    
    /**
     * Mark payout as failed.
     */
    public void markAsFailed(String failureReason) {
        if (!isProcessing()) {
            throw new IllegalStateException("Only processing payouts can be marked as failed");
        }
        this.status = PayoutStatus.FAILED;
        this.failureReason = failureReason;
        this.executedAt = LocalDateTime.now();
    }
    
    /**
     * Get display name for payout method.
     */
    public String getMethodDisplayName() {
        if (method == null) return "Unknown";
        return switch (method) {
            case BANK -> "Bank Transfer";
            case BKASH -> "bKash";
            case NAGAD -> "Nagad";
            case ROCKET -> "Rocket";
        };
    }
    
    /**
     * Get display name for payout status.
     */
    public String getStatusDisplayName() {
        if (status == null) return "Unknown";
        return switch (status) {
            case PENDING -> "Pending Approval";
            case APPROVED -> "Approved";
            case PROCESSING -> "Processing";
            case PAID -> "Paid";
            case FAILED -> "Failed";
            case CANCELLED -> "Cancelled";
        };
    }
    
    /**
     * Get account details for display.
     */
    public String getAccountDetails() {
        if (isBankMethod()) {
            return String.format("%s - %s (%s)", bankName, accountNumber, accountHolderName);
        } else if (isMobileMethod()) {
            return String.format("%s (%s)", mobileNumber, mobileAccountName != null ? mobileAccountName : "N/A");
        }
        return "N/A";
    }
    
    @PrePersist
    private void prePersist() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (referenceNumber == null) {
            referenceNumber = generateReferenceNumber();
        }
        validatePayoutData();
    }
    
    @PreUpdate
    private void preUpdate() {
        validatePayoutData();
    }
    
    private void validatePayoutData() {
        if (currency != null) {
            currency = currency.toUpperCase();
            if (currency.length() != 3) {
                throw new IllegalArgumentException("Currency code must be exactly 3 characters");
            }
        }
        
        if (amountMinor == null || amountMinor <= 0) {
            throw new IllegalArgumentException("Payout amount must be positive");
        }
        
        // Validate method-specific details
        if (isBankMethod()) {
            if (bankName == null || accountNumber == null || accountHolderName == null) {
                throw new IllegalArgumentException("Bank details are required for bank transfers");
            }
        } else if (isMobileMethod()) {
            if (mobileNumber == null) {
                throw new IllegalArgumentException("Mobile number is required for mobile money transfers");
            }
        }
    }
    
    private String generateReferenceNumber() {
        return "PO-" + System.currentTimeMillis() + "-" + 
               id.toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Payout method enumeration.
     */
    public enum PayoutMethod {
        BANK,    // Bank transfer
        BKASH,   // bKash mobile money
        NAGAD,   // Nagad mobile money
        ROCKET   // Rocket mobile money
    }
    
    /**
     * Payout status enumeration.
     */
    public enum PayoutStatus {
        PENDING,     // Waiting for approval
        APPROVED,    // Approved, ready for processing
        PROCESSING,  // Being processed by payment system
        PAID,        // Successfully paid
        FAILED,      // Payment failed
        CANCELLED    // Cancelled before processing
    }
}
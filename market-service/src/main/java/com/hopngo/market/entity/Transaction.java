package com.hopngo.market.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a transaction that groups related ledger entries.
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type")
    private LedgerEntry.ReferenceType referenceType;
    
    @Column(name = "reference_id")
    private UUID referenceId;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "total_amount_minor", nullable = false)
    private Long totalAmountMinor;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "transactionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LedgerEntry> entries = new ArrayList<>();
    
    /**
     * Get total amount as decimal for display.
     */
    public BigDecimal getTotalAmountDecimal() {
        return new BigDecimal(totalAmountMinor).divide(new BigDecimal("100"));
    }
    
    /**
     * Set total amount from decimal.
     */
    public void setTotalAmountFromDecimal(BigDecimal amount) {
        this.totalAmountMinor = amount.multiply(new BigDecimal("100")).longValue();
    }
    
    /**
     * Add a ledger entry to this transaction.
     */
    public void addEntry(LedgerEntry entry) {
        entry.setTransactionId(this.id);
        this.entries.add(entry);
    }
    
    /**
     * Check if transaction is balanced (debits = credits).
     */
    public boolean isBalanced() {
        long totalDebits = entries.stream()
                .filter(LedgerEntry::isDebit)
                .mapToLong(LedgerEntry::getAmountMinor)
                .sum();
        
        long totalCredits = entries.stream()
                .filter(LedgerEntry::isCredit)
                .mapToLong(LedgerEntry::getAmountMinor)
                .sum();
        
        return totalDebits == totalCredits;
    }
    
    /**
     * Get total debit amount.
     */
    public long getTotalDebitsMinor() {
        return entries.stream()
                .filter(LedgerEntry::isDebit)
                .mapToLong(LedgerEntry::getAmountMinor)
                .sum();
    }
    
    /**
     * Get total credit amount.
     */
    public long getTotalCreditsMinor() {
        return entries.stream()
                .filter(LedgerEntry::isCredit)
                .mapToLong(LedgerEntry::getAmountMinor)
                .sum();
    }
    
    /**
     * Mark transaction as completed.
     */
    public void complete() {
        if (!isBalanced()) {
            throw new IllegalStateException("Cannot complete unbalanced transaction");
        }
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Mark transaction as failed.
     */
    public void fail() {
        this.status = TransactionStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Mark transaction as cancelled.
     */
    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Check if transaction is pending.
     */
    public boolean isPending() {
        return TransactionStatus.PENDING.equals(this.status);
    }
    
    /**
     * Check if transaction is completed.
     */
    public boolean isCompleted() {
        return TransactionStatus.COMPLETED.equals(this.status);
    }
    
    /**
     * Check if transaction is failed.
     */
    public boolean isFailed() {
        return TransactionStatus.FAILED.equals(this.status);
    }
    
    /**
     * Check if transaction is cancelled.
     */
    public boolean isCancelled() {
        return TransactionStatus.CANCELLED.equals(this.status);
    }
    
    /**
     * Check if transaction is final (completed, failed, or cancelled).
     */
    public boolean isFinal() {
        return isCompleted() || isFailed() || isCancelled();
    }
    
    /**
     * Get entry count.
     */
    public int getEntryCount() {
        return entries.size();
    }
    
    /**
     * Check if transaction is related to a specific reference.
     */
    public boolean isRelatedTo(LedgerEntry.ReferenceType type, UUID id) {
        return type.equals(this.referenceType) && id.equals(this.referenceId);
    }
    
    @PrePersist
    @PreUpdate
    private void validateTransaction() {
        if (currency != null) {
            currency = currency.toUpperCase();
            if (currency.length() != 3) {
                throw new IllegalArgumentException("Currency code must be exactly 3 characters");
            }
        }
        
        if (totalAmountMinor == null || totalAmountMinor <= 0) {
            throw new IllegalArgumentException("Transaction total amount must be positive");
        }
        
        if (status == null) {
            throw new IllegalArgumentException("Transaction status is required");
        }
    }
    
    /**
     * Transaction type enumeration.
     */
    public enum TransactionType {
        BOOKING_PAYMENT,    // Payment for a booking
        PROVIDER_PAYOUT,    // Payout to a service provider
        REFUND,            // Refund to a customer
        FEE_COLLECTION,    // Collection of platform fees
        ADJUSTMENT,        // Manual adjustment
        TRANSFER           // Transfer between accounts
    }
    
    /**
     * Transaction status enumeration.
     */
    public enum TransactionStatus {
        PENDING,    // Transaction is pending
        COMPLETED,  // Transaction is completed successfully
        FAILED,     // Transaction failed
        CANCELLED   // Transaction was cancelled
    }
}
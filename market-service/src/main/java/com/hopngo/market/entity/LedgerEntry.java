package com.hopngo.market.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a ledger entry in the double-entry bookkeeping system.
 */
@Entity
@Table(name = "ledger_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;
    
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type")
    private ReferenceType referenceType;
    
    @Column(name = "reference_id")
    private UUID referenceId;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
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
     * Check if this is a debit entry.
     */
    public boolean isDebit() {
        return EntryType.DEBIT.equals(this.entryType);
    }
    
    /**
     * Check if this is a credit entry.
     */
    public boolean isCredit() {
        return EntryType.CREDIT.equals(this.entryType);
    }
    
    /**
     * Get the signed amount (positive for credits, negative for debits).
     */
    public long getSignedAmountMinor() {
        return isCredit() ? amountMinor : -amountMinor;
    }
    
    /**
     * Get the signed amount as decimal.
     */
    public BigDecimal getSignedAmountDecimal() {
        return new BigDecimal(getSignedAmountMinor()).divide(new BigDecimal("100"));
    }
    
    /**
     * Check if entry is related to a specific reference.
     */
    public boolean isRelatedTo(ReferenceType type, UUID id) {
        return type.equals(this.referenceType) && id.equals(this.referenceId);
    }
    
    /**
     * Create a debit entry.
     */
    public static LedgerEntry createDebit(Account account, long amountMinor, String currency,
                                         ReferenceType referenceType, UUID referenceId,
                                         String description, UUID transactionId) {
        return LedgerEntry.builder()
                .account(account)
                .entryType(EntryType.DEBIT)
                .amountMinor(amountMinor)
                .currency(currency)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description(description)
                .transactionId(transactionId)
                .build();
    }
    
    /**
     * Create a credit entry.
     */
    public static LedgerEntry createCredit(Account account, long amountMinor, String currency,
                                          ReferenceType referenceType, UUID referenceId,
                                          String description, UUID transactionId) {
        return LedgerEntry.builder()
                .account(account)
                .entryType(EntryType.CREDIT)
                .amountMinor(amountMinor)
                .currency(currency)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description(description)
                .transactionId(transactionId)
                .build();
    }
    
    @PrePersist
    @PreUpdate
    private void validateEntry() {
        if (currency != null) {
            currency = currency.toUpperCase();
            if (currency.length() != 3) {
                throw new IllegalArgumentException("Currency code must be exactly 3 characters");
            }
        }
        
        if (amountMinor == null || amountMinor <= 0) {
            throw new IllegalArgumentException("Entry amount must be positive");
        }
        
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
    }
    
    /**
     * Entry type enumeration for double-entry bookkeeping.
     */
    public enum EntryType {
        DEBIT,   // Debit entry (increases asset/expense accounts, decreases liability/revenue accounts)
        CREDIT   // Credit entry (decreases asset/expense accounts, increases liability/revenue accounts)
    }
    
    /**
     * Reference type enumeration for linking entries to business objects.
     */
    public enum ReferenceType {
        BOOKING,     // Related to a booking transaction
        ORDER,       // Related to an order transaction
        INVOICE,     // Related to an invoice
        PAYOUT,      // Related to a provider payout
        REFUND,      // Related to a refund
        FEE,         // Related to a fee collection
        ADJUSTMENT,  // Manual adjustment entry
        TRANSFER     // Transfer between accounts
    }
}
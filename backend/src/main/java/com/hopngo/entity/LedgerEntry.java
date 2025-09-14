package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "entry_id", unique = true)
    private String entryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type")
    private EntryType entryType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @NotNull
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "description")
    private String description;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    // Constructors
    public LedgerEntry() {}

    public LedgerEntry(String entryId, AccountType accountType, EntryType entryType, 
                      BigDecimal amount, String currency, String description) {
        this.entryId = entryId;
        this.accountType = accountType;
        this.entryType = entryType;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.effectiveDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
        if (verified && verifiedAt == null) {
            verifiedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
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

    public LocalDateTime getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDateTime effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    // Helper methods
    public boolean isDebit() {
        return entryType == EntryType.DEBIT;
    }

    public boolean isCredit() {
        return entryType == EntryType.CREDIT;
    }

    public BigDecimal getSignedAmount() {
        return entryType == EntryType.DEBIT ? amount : amount.negate();
    }

    public enum AccountType {
        // Asset accounts
        CASH("Cash", "cash", true),
        ACCOUNTS_RECEIVABLE("Accounts Receivable", "accounts_receivable", true),
        STRIPE_BALANCE("Stripe Balance", "stripe_balance", true),
        BKASH_BALANCE("bKash Balance", "bkash_balance", true),
        NAGAD_BALANCE("Nagad Balance", "nagad_balance", true),
        
        // Liability accounts
        ACCOUNTS_PAYABLE("Accounts Payable", "accounts_payable", false),
        PROVIDER_PAYOUTS("Provider Payouts", "provider_payouts", false),
        REFUNDS_PAYABLE("Refunds Payable", "refunds_payable", false),
        
        // Revenue accounts
        PLATFORM_REVENUE("Platform Revenue", "platform_revenue", false),
        TRANSACTION_FEES("Transaction Fees", "transaction_fees", false),
        
        // Expense accounts
        PAYMENT_PROCESSING_FEES("Payment Processing Fees", "payment_processing_fees", true),
        CHARGEBACK_FEES("Chargeback Fees", "chargeback_fees", true),
        DISPUTE_FEES("Dispute Fees", "dispute_fees", true),
        
        // Reserve accounts
        DISPUTE_RESERVE("Dispute Reserve", "dispute_reserve", true),
        AVAILABLE_BALANCE("Available Balance", "available_balance", true);

        private final String displayName;
        private final String code;
        private final boolean isDebitNormal; // true for assets/expenses, false for liabilities/revenue

        AccountType(String displayName, String code, boolean isDebitNormal) {
            this.displayName = displayName;
            this.code = code;
            this.isDebitNormal = isDebitNormal;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCode() {
            return code;
        }

        public boolean isDebitNormal() {
            return isDebitNormal;
        }

        public boolean isCreditNormal() {
            return !isDebitNormal;
        }
    }

    public enum EntryType {
        DEBIT("Debit", "DR"),
        CREDIT("Credit", "CR");

        private final String displayName;
        private final String abbreviation;

        EntryType(String displayName, String abbreviation) {
            this.displayName = displayName;
            this.abbreviation = abbreviation;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getAbbreviation() {
            return abbreviation;
        }
    }

    @Override
    public String toString() {
        return "LedgerEntry{" +
                "id=" + id +
                ", entryId='" + entryId + '\'' +
                ", accountType=" + accountType +
                ", entryType=" + entryType +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
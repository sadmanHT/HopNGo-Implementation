package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_discrepancies")
public class ReconciliationDiscrepancy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_job_id", nullable = false)
    private ReconciliationJob reconciliationJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type")
    private DiscrepancyType discrepancyType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private DiscrepancySeverity severity;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(name = "our_transaction_id")
    private String ourTransactionId;

    @Column(name = "provider_amount", precision = 19, scale = 4)
    private BigDecimal providerAmount;

    @Column(name = "our_amount", precision = 19, scale = 4)
    private BigDecimal ourAmount;

    @Column(name = "amount_difference", precision = 19, scale = 4)
    private BigDecimal amountDifference;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "provider_status")
    private String providerStatus;

    @Column(name = "our_status")
    private String ourStatus;

    @Column(name = "provider_date")
    private LocalDateTime providerDate;

    @Column(name = "our_date")
    private LocalDateTime ourDate;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "provider_data", columnDefinition = "TEXT")
    private String providerData;

    @Column(name = "our_data", columnDefinition = "TEXT")
    private String ourData;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "resolved")
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "auto_resolved")
    private Boolean autoResolved = false;

    @Column(name = "support_ticket_id")
    private String supportTicketId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public ReconciliationDiscrepancy() {}

    public ReconciliationDiscrepancy(ReconciliationJob reconciliationJob, DiscrepancyType discrepancyType,
                                   DiscrepancySeverity severity, String description) {
        this.reconciliationJob = reconciliationJob;
        this.discrepancyType = discrepancyType;
        this.severity = severity;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReconciliationJob getReconciliationJob() {
        return reconciliationJob;
    }

    public void setReconciliationJob(ReconciliationJob reconciliationJob) {
        this.reconciliationJob = reconciliationJob;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public DiscrepancyType getDiscrepancyType() {
        return discrepancyType;
    }

    public void setDiscrepancyType(DiscrepancyType discrepancyType) {
        this.discrepancyType = discrepancyType;
    }

    public DiscrepancySeverity getSeverity() {
        return severity;
    }

    public void setSeverity(DiscrepancySeverity severity) {
        this.severity = severity;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public void setProviderTransactionId(String providerTransactionId) {
        this.providerTransactionId = providerTransactionId;
    }

    public String getOurTransactionId() {
        return ourTransactionId;
    }

    public void setOurTransactionId(String ourTransactionId) {
        this.ourTransactionId = ourTransactionId;
    }

    public BigDecimal getProviderAmount() {
        return providerAmount;
    }

    public void setProviderAmount(BigDecimal providerAmount) {
        this.providerAmount = providerAmount;
    }

    public BigDecimal getOurAmount() {
        return ourAmount;
    }

    public void setOurAmount(BigDecimal ourAmount) {
        this.ourAmount = ourAmount;
    }

    public BigDecimal getAmountDifference() {
        return amountDifference;
    }

    public void setAmountDifference(BigDecimal amountDifference) {
        this.amountDifference = amountDifference;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProviderStatus() {
        return providerStatus;
    }

    public void setProviderStatus(String providerStatus) {
        this.providerStatus = providerStatus;
    }

    public String getOurStatus() {
        return ourStatus;
    }

    public void setOurStatus(String ourStatus) {
        this.ourStatus = ourStatus;
    }

    public LocalDateTime getProviderDate() {
        return providerDate;
    }

    public void setProviderDate(LocalDateTime providerDate) {
        this.providerDate = providerDate;
    }

    public LocalDateTime getOurDate() {
        return ourDate;
    }

    public void setOurDate(LocalDateTime ourDate) {
        this.ourDate = ourDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProviderData() {
        return providerData;
    }

    public void setProviderData(String providerData) {
        this.providerData = providerData;
    }

    public String getOurData() {
        return ourData;
    }

    public void setOurData(String ourData) {
        this.ourData = ourData;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
        if (resolved && resolvedAt == null) {
            resolvedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Boolean getAutoResolved() {
        return autoResolved;
    }

    public void setAutoResolved(Boolean autoResolved) {
        this.autoResolved = autoResolved;
    }

    public String getSupportTicketId() {
        return supportTicketId;
    }

    public void setSupportTicketId(String supportTicketId) {
        this.supportTicketId = supportTicketId;
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

    // Helper methods
    public boolean isResolved() {
        return resolved != null && resolved;
    }

    public boolean isCritical() {
        return severity == DiscrepancySeverity.CRITICAL;
    }

    public boolean isHigh() {
        return severity == DiscrepancySeverity.HIGH;
    }

    public enum DiscrepancyType {
        MISSING_TRANSACTION("Missing Transaction", "Transaction exists in provider but not in our system"),
        EXTRA_TRANSACTION("Extra Transaction", "Transaction exists in our system but not in provider"),
        AMOUNT_MISMATCH("Amount Mismatch", "Transaction amounts differ between provider and our system"),
        STATUS_MISMATCH("Status Mismatch", "Transaction statuses differ between provider and our system"),
        DATE_MISMATCH("Date Mismatch", "Transaction dates differ significantly"),
        CURRENCY_MISMATCH("Currency Mismatch", "Transaction currencies differ"),
        DUPLICATE_TRANSACTION("Duplicate Transaction", "Duplicate transaction found"),
        INVALID_TRANSACTION("Invalid Transaction", "Transaction data is invalid or corrupted");

        private final String displayName;
        private final String description;

        DiscrepancyType(String displayName, String description) {
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

    public enum DiscrepancySeverity {
        LOW("Low", "Minor discrepancy that can be resolved automatically"),
        MEDIUM("Medium", "Moderate discrepancy requiring review"),
        HIGH("High", "Significant discrepancy requiring immediate attention"),
        CRITICAL("Critical", "Critical discrepancy requiring urgent resolution");

        private final String displayName;
        private final String description;

        DiscrepancySeverity(String displayName, String description) {
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
        return "ReconciliationDiscrepancy{" +
                "id=" + id +
                ", discrepancyType=" + discrepancyType +
                ", severity=" + severity +
                ", providerTransactionId='" + providerTransactionId + '\'' +
                ", ourTransactionId='" + ourTransactionId + '\'' +
                ", amountDifference=" + amountDifference +
                ", resolved=" + resolved +
                ", createdAt=" + createdAt +
                '}';
    }
}
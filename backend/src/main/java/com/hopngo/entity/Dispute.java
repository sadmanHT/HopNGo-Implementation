package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
public class Dispute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @NotNull
    @Column(name = "dispute_id", unique = true)
    private String disputeId;

    @Column(name = "provider_dispute_id")
    private String providerDisputeId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_type")
    private DisputeType disputeType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DisputeStatus status = DisputeStatus.RECEIVED;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "reason")
    private DisputeReason reason;

    @NotNull
    @Column(name = "disputed_amount", precision = 19, scale = 4)
    private BigDecimal disputedAmount;

    @Column(name = "dispute_fee", precision = 19, scale = 4)
    private BigDecimal disputeFee = BigDecimal.ZERO;

    @NotNull
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "evidence_due_by")
    private LocalDateTime evidenceDueBy;

    @Column(name = "customer_message", length = 2000)
    private String customerMessage;

    @Column(name = "provider_data", columnDefinition = "TEXT")
    private String providerData;

    @Column(name = "our_response", columnDefinition = "TEXT")
    private String ourResponse;

    @Column(name = "evidence_submitted")
    private Boolean evidenceSubmitted = false;

    @Column(name = "evidence_submitted_at")
    private LocalDateTime evidenceSubmittedAt;

    @Column(name = "funds_frozen")
    private Boolean fundsFrozen = false;

    @Column(name = "funds_frozen_at")
    private LocalDateTime fundsFrozenAt;

    @Column(name = "funds_released_at")
    private LocalDateTime fundsReleasedAt;

    @Column(name = "admin_notified")
    private Boolean adminNotified = false;

    @Column(name = "admin_notified_at")
    private LocalDateTime adminNotifiedAt;

    @Column(name = "provider_notified")
    private Boolean providerNotified = false;

    @Column(name = "provider_notified_at")
    private LocalDateTime providerNotifiedAt;

    @Column(name = "resolution_notes", length = 2000)
    private String resolutionNotes;

    @Column(name = "internal_notes", length = 2000)
    private String internalNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // Constructors
    public Dispute() {}

    public Dispute(Transaction transaction, String disputeId, DisputeType disputeType,
                  DisputeReason reason, BigDecimal disputedAmount, String currency) {
        this.transaction = transaction;
        this.disputeId = disputeId;
        this.disputeType = disputeType;
        this.reason = reason;
        this.disputedAmount = disputedAmount;
        this.currency = currency;
        this.receivedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public String getDisputeId() {
        return disputeId;
    }

    public void setDisputeId(String disputeId) {
        this.disputeId = disputeId;
    }

    public String getProviderDisputeId() {
        return providerDisputeId;
    }

    public void setProviderDisputeId(String providerDisputeId) {
        this.providerDisputeId = providerDisputeId;
    }

    public DisputeType getDisputeType() {
        return disputeType;
    }

    public void setDisputeType(DisputeType disputeType) {
        this.disputeType = disputeType;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public void setStatus(DisputeStatus status) {
        this.status = status;
        if ((status == DisputeStatus.WON || status == DisputeStatus.LOST || 
             status == DisputeStatus.ACCEPTED) && resolvedAt == null) {
            resolvedAt = LocalDateTime.now();
        }
    }

    public DisputeReason getReason() {
        return reason;
    }

    public void setReason(DisputeReason reason) {
        this.reason = reason;
    }

    public BigDecimal getDisputedAmount() {
        return disputedAmount;
    }

    public void setDisputedAmount(BigDecimal disputedAmount) {
        this.disputedAmount = disputedAmount;
    }

    public BigDecimal getDisputeFee() {
        return disputeFee;
    }

    public void setDisputeFee(BigDecimal disputeFee) {
        this.disputeFee = disputeFee;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getEvidenceDueBy() {
        return evidenceDueBy;
    }

    public void setEvidenceDueBy(LocalDateTime evidenceDueBy) {
        this.evidenceDueBy = evidenceDueBy;
    }

    public String getCustomerMessage() {
        return customerMessage;
    }

    public void setCustomerMessage(String customerMessage) {
        this.customerMessage = customerMessage;
    }

    public String getProviderData() {
        return providerData;
    }

    public void setProviderData(String providerData) {
        this.providerData = providerData;
    }

    public String getOurResponse() {
        return ourResponse;
    }

    public void setOurResponse(String ourResponse) {
        this.ourResponse = ourResponse;
    }

    public Boolean getEvidenceSubmitted() {
        return evidenceSubmitted;
    }

    public void setEvidenceSubmitted(Boolean evidenceSubmitted) {
        this.evidenceSubmitted = evidenceSubmitted;
        if (evidenceSubmitted && evidenceSubmittedAt == null) {
            evidenceSubmittedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getEvidenceSubmittedAt() {
        return evidenceSubmittedAt;
    }

    public void setEvidenceSubmittedAt(LocalDateTime evidenceSubmittedAt) {
        this.evidenceSubmittedAt = evidenceSubmittedAt;
    }

    public Boolean getFundsFrozen() {
        return fundsFrozen;
    }

    public void setFundsFrozen(Boolean fundsFrozen) {
        this.fundsFrozen = fundsFrozen;
        if (fundsFrozen && fundsFrozenAt == null) {
            fundsFrozenAt = LocalDateTime.now();
        } else if (!fundsFrozen && fundsReleasedAt == null && fundsFrozenAt != null) {
            fundsReleasedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getFundsFrozenAt() {
        return fundsFrozenAt;
    }

    public void setFundsFrozenAt(LocalDateTime fundsFrozenAt) {
        this.fundsFrozenAt = fundsFrozenAt;
    }

    public LocalDateTime getFundsReleasedAt() {
        return fundsReleasedAt;
    }

    public void setFundsReleasedAt(LocalDateTime fundsReleasedAt) {
        this.fundsReleasedAt = fundsReleasedAt;
    }

    public Boolean getAdminNotified() {
        return adminNotified;
    }

    public void setAdminNotified(Boolean adminNotified) {
        this.adminNotified = adminNotified;
        if (adminNotified && adminNotifiedAt == null) {
            adminNotifiedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getAdminNotifiedAt() {
        return adminNotifiedAt;
    }

    public void setAdminNotifiedAt(LocalDateTime adminNotifiedAt) {
        this.adminNotifiedAt = adminNotifiedAt;
    }

    public Boolean getProviderNotified() {
        return providerNotified;
    }

    public void setProviderNotified(Boolean providerNotified) {
        this.providerNotified = providerNotified;
        if (providerNotified && providerNotifiedAt == null) {
            providerNotifiedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getProviderNotifiedAt() {
        return providerNotifiedAt;
    }

    public void setProviderNotifiedAt(LocalDateTime providerNotifiedAt) {
        this.providerNotifiedAt = providerNotifiedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
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

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    // Helper methods
    public boolean isResolved() {
        return status == DisputeStatus.WON || status == DisputeStatus.LOST || 
               status == DisputeStatus.ACCEPTED;
    }

    public boolean isActive() {
        return !isResolved();
    }

    public boolean isEvidenceRequired() {
        return evidenceDueBy != null && !evidenceSubmitted && 
               evidenceDueBy.isAfter(LocalDateTime.now());
    }

    public boolean isEvidenceOverdue() {
        return evidenceDueBy != null && !evidenceSubmitted && 
               evidenceDueBy.isBefore(LocalDateTime.now());
    }

    public enum DisputeType {
        CHARGEBACK("Chargeback", "Customer initiated chargeback"),
        INQUIRY("Inquiry", "Customer inquiry or pre-dispute"),
        RETRIEVAL("Retrieval", "Retrieval request for transaction details"),
        FRAUD("Fraud", "Fraud-related dispute");

        private final String displayName;
        private final String description;

        DisputeType(String displayName, String description) {
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

    public enum DisputeStatus {
        RECEIVED("Received", "Dispute has been received"),
        UNDER_REVIEW("Under Review", "Dispute is being reviewed"),
        EVIDENCE_REQUIRED("Evidence Required", "Evidence is required to contest the dispute"),
        EVIDENCE_SUBMITTED("Evidence Submitted", "Evidence has been submitted"),
        WON("Won", "Dispute was won in our favor"),
        LOST("Lost", "Dispute was lost"),
        ACCEPTED("Accepted", "Dispute was accepted without contest"),
        EXPIRED("Expired", "Dispute expired without response"),
        CLOSED("Closed", "Dispute has been closed");

        private final String displayName;
        private final String description;

        DisputeStatus(String displayName, String description) {
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

    public enum DisputeReason {
        FRAUDULENT("Fraudulent", "Transaction was fraudulent"),
        UNRECOGNIZED("Unrecognized", "Customer doesn't recognize the transaction"),
        DUPLICATE("Duplicate", "Duplicate transaction"),
        SUBSCRIPTION_CANCELLED("Subscription Cancelled", "Subscription was cancelled"),
        PRODUCT_NOT_RECEIVED("Product Not Received", "Product or service not received"),
        PRODUCT_UNACCEPTABLE("Product Unacceptable", "Product or service was unacceptable"),
        CREDIT_NOT_PROCESSED("Credit Not Processed", "Expected credit was not processed"),
        GENERAL("General", "General dispute reason"),
        OTHER("Other", "Other reason not specified");

        private final String displayName;
        private final String description;

        DisputeReason(String displayName, String description) {
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
        return "Dispute{" +
                "id=" + id +
                ", disputeId='" + disputeId + '\'' +
                ", disputeType=" + disputeType +
                ", status=" + status +
                ", reason=" + reason +
                ", disputedAmount=" + disputedAmount +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
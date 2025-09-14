package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reconciliation_jobs")
public class ReconciliationJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "job_id", unique = true)
    private String jobId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private Transaction.PaymentProvider provider;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReconciliationStatus status = ReconciliationStatus.PENDING;

    @NotNull
    @Column(name = "reconciliation_date")
    private LocalDateTime reconciliationDate;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "total_provider_transactions")
    private Integer totalProviderTransactions = 0;

    @Column(name = "total_our_transactions")
    private Integer totalOurTransactions = 0;

    @Column(name = "matched_transactions")
    private Integer matchedTransactions = 0;

    @Column(name = "discrepancies_found")
    private Integer discrepanciesFound = 0;

    @Column(name = "provider_total_amount", precision = 19, scale = 4)
    private BigDecimal providerTotalAmount = BigDecimal.ZERO;

    @Column(name = "our_total_amount", precision = 19, scale = 4)
    private BigDecimal ourTotalAmount = BigDecimal.ZERO;

    @Column(name = "amount_difference", precision = 19, scale = 4)
    private BigDecimal amountDifference = BigDecimal.ZERO;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "provider_data", columnDefinition = "TEXT")
    private String providerData;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "support_ticket_id")
    private String supportTicketId;

    @Column(name = "auto_resolved")
    private Boolean autoResolved = false;

    @OneToMany(mappedBy = "reconciliationJob", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public ReconciliationJob() {}

    public ReconciliationJob(String jobId, Transaction.PaymentProvider provider, 
                           LocalDateTime reconciliationDate) {
        this.jobId = jobId;
        this.provider = provider;
        this.reconciliationDate = reconciliationDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Transaction.PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(Transaction.PaymentProvider provider) {
        this.provider = provider;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationStatus status) {
        this.status = status;
        if (status == ReconciliationStatus.PROCESSING && startedAt == null) {
            startedAt = LocalDateTime.now();
        } else if ((status == ReconciliationStatus.COMPLETED || status == ReconciliationStatus.FAILED) 
                   && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(LocalDateTime reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Integer getTotalProviderTransactions() {
        return totalProviderTransactions;
    }

    public void setTotalProviderTransactions(Integer totalProviderTransactions) {
        this.totalProviderTransactions = totalProviderTransactions;
    }

    public Integer getTotalOurTransactions() {
        return totalOurTransactions;
    }

    public void setTotalOurTransactions(Integer totalOurTransactions) {
        this.totalOurTransactions = totalOurTransactions;
    }

    public Integer getMatchedTransactions() {
        return matchedTransactions;
    }

    public void setMatchedTransactions(Integer matchedTransactions) {
        this.matchedTransactions = matchedTransactions;
    }

    public Integer getDiscrepanciesFound() {
        return discrepanciesFound;
    }

    public void setDiscrepanciesFound(Integer discrepanciesFound) {
        this.discrepanciesFound = discrepanciesFound;
    }

    public BigDecimal getProviderTotalAmount() {
        return providerTotalAmount;
    }

    public void setProviderTotalAmount(BigDecimal providerTotalAmount) {
        this.providerTotalAmount = providerTotalAmount;
    }

    public BigDecimal getOurTotalAmount() {
        return ourTotalAmount;
    }

    public void setOurTotalAmount(BigDecimal ourTotalAmount) {
        this.ourTotalAmount = ourTotalAmount;
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

    public String getProviderData() {
        return providerData;
    }

    public void setProviderData(String providerData) {
        this.providerData = providerData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSupportTicketId() {
        return supportTicketId;
    }

    public void setSupportTicketId(String supportTicketId) {
        this.supportTicketId = supportTicketId;
    }

    public Boolean getAutoResolved() {
        return autoResolved;
    }

    public void setAutoResolved(Boolean autoResolved) {
        this.autoResolved = autoResolved;
    }

    public List<ReconciliationDiscrepancy> getDiscrepancies() {
        return discrepancies;
    }

    public void setDiscrepancies(List<ReconciliationDiscrepancy> discrepancies) {
        this.discrepancies = discrepancies;
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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Helper methods
    public boolean hasDiscrepancies() {
        return discrepanciesFound != null && discrepanciesFound > 0;
    }

    public boolean isCompleted() {
        return status == ReconciliationStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == ReconciliationStatus.FAILED;
    }

    public boolean isProcessing() {
        return status == ReconciliationStatus.PROCESSING;
    }

    public double getMatchRate() {
        if (totalOurTransactions == null || totalOurTransactions == 0) {
            return 0.0;
        }
        return (double) matchedTransactions / totalOurTransactions * 100;
    }

    public enum ReconciliationStatus {
        PENDING("Pending", "Reconciliation job is queued for processing"),
        PROCESSING("Processing", "Reconciliation is currently running"),
        COMPLETED("Completed", "Reconciliation completed successfully"),
        COMPLETED_WITH_DISCREPANCIES("Completed with Discrepancies", "Reconciliation completed but discrepancies were found"),
        FAILED("Failed", "Reconciliation failed due to an error"),
        CANCELLED("Cancelled", "Reconciliation was cancelled");

        private final String displayName;
        private final String description;

        ReconciliationStatus(String displayName, String description) {
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
        return "ReconciliationJob{" +
                "id=" + id +
                ", jobId='" + jobId + '\'' +
                ", provider=" + provider +
                ", status=" + status +
                ", reconciliationDate=" + reconciliationDate +
                ", discrepanciesFound=" + discrepanciesFound +
                ", createdAt=" + createdAt +
                '}';
    }
}
package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "financial_reports")
public class FinancialReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "report_id", unique = true)
    private String reportId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type")
    private ReportType reportType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type")
    private PeriodType periodType;

    @NotNull
    @Column(name = "period_start")
    private LocalDate periodStart;

    @NotNull
    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "year")
    private Integer year;

    @Column(name = "month")
    private Integer month;

    @Column(name = "quarter")
    private Integer quarter;

    // Revenue metrics
    @Column(name = "total_revenue", precision = 19, scale = 4)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "gross_revenue", precision = 19, scale = 4)
    private BigDecimal grossRevenue = BigDecimal.ZERO;

    @Column(name = "net_revenue", precision = 19, scale = 4)
    private BigDecimal netRevenue = BigDecimal.ZERO;

    // Provider earnings
    @Column(name = "provider_earnings", precision = 19, scale = 4)
    private BigDecimal providerEarnings = BigDecimal.ZERO;

    @Column(name = "stripe_earnings", precision = 19, scale = 4)
    private BigDecimal stripeEarnings = BigDecimal.ZERO;

    @Column(name = "bkash_earnings", precision = 19, scale = 4)
    private BigDecimal bkashEarnings = BigDecimal.ZERO;

    @Column(name = "nagad_earnings", precision = 19, scale = 4)
    private BigDecimal nagadEarnings = BigDecimal.ZERO;

    // Platform fees
    @Column(name = "platform_fees", precision = 19, scale = 4)
    private BigDecimal platformFees = BigDecimal.ZERO;

    @Column(name = "stripe_fees", precision = 19, scale = 4)
    private BigDecimal stripeFees = BigDecimal.ZERO;

    @Column(name = "bkash_fees", precision = 19, scale = 4)
    private BigDecimal bkashFees = BigDecimal.ZERO;

    @Column(name = "nagad_fees", precision = 19, scale = 4)
    private BigDecimal nagadFees = BigDecimal.ZERO;

    // Transaction counts
    @Column(name = "total_transactions")
    private Long totalTransactions = 0L;

    @Column(name = "successful_transactions")
    private Long successfulTransactions = 0L;

    @Column(name = "failed_transactions")
    private Long failedTransactions = 0L;

    @Column(name = "refunded_transactions")
    private Long refundedTransactions = 0L;

    // Refunds
    @Column(name = "total_refunds", precision = 19, scale = 4)
    private BigDecimal totalRefunds = BigDecimal.ZERO;

    @Column(name = "refund_fees", precision = 19, scale = 4)
    private BigDecimal refundFees = BigDecimal.ZERO;

    // Disputes and chargebacks
    @Column(name = "total_disputes")
    private Long totalDisputes = 0L;

    @Column(name = "dispute_amount", precision = 19, scale = 4)
    private BigDecimal disputeAmount = BigDecimal.ZERO;

    @Column(name = "dispute_fees", precision = 19, scale = 4)
    private BigDecimal disputeFees = BigDecimal.ZERO;

    @Column(name = "disputes_won")
    private Long disputesWon = 0L;

    @Column(name = "disputes_lost")
    private Long disputesLost = 0L;

    // Tax information
    @Column(name = "taxable_amount", precision = 19, scale = 4)
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "tax_withheld", precision = 19, scale = 4)
    private BigDecimal taxWithheld = BigDecimal.ZERO;

    @Column(name = "vat_amount", precision = 19, scale = 4)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    // Report status and files
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReportStatus status = ReportStatus.GENERATING;

    @Column(name = "csv_file_path")
    private String csvFilePath;

    @Column(name = "pdf_file_path")
    private String pdfFilePath;

    @Column(name = "generated_by")
    private String generatedBy;

    @Column(name = "generation_started_at")
    private LocalDateTime generationStartedAt;

    @Column(name = "generation_completed_at")
    private LocalDateTime generationCompletedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "notes", length = 2000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public FinancialReport() {}

    public FinancialReport(String reportId, ReportType reportType, PeriodType periodType,
                          LocalDate periodStart, LocalDate periodEnd) {
        this.reportId = reportId;
        this.reportType = reportType;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.generationStartedAt = LocalDateTime.now();
        
        // Set year, month, quarter based on period
        if (periodType == PeriodType.MONTHLY) {
            YearMonth yearMonth = YearMonth.from(periodStart);
            this.year = yearMonth.getYear();
            this.month = yearMonth.getMonthValue();
        } else if (periodType == PeriodType.YEARLY) {
            this.year = periodStart.getYear();
        } else if (periodType == PeriodType.QUARTERLY) {
            this.year = periodStart.getYear();
            this.quarter = ((periodStart.getMonthValue() - 1) / 3) + 1;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(PeriodType periodType) {
        this.periodType = periodType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getQuarter() {
        return quarter;
    }

    public void setQuarter(Integer quarter) {
        this.quarter = quarter;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getGrossRevenue() {
        return grossRevenue;
    }

    public void setGrossRevenue(BigDecimal grossRevenue) {
        this.grossRevenue = grossRevenue;
    }

    public BigDecimal getNetRevenue() {
        return netRevenue;
    }

    public void setNetRevenue(BigDecimal netRevenue) {
        this.netRevenue = netRevenue;
    }

    public BigDecimal getProviderEarnings() {
        return providerEarnings;
    }

    public void setProviderEarnings(BigDecimal providerEarnings) {
        this.providerEarnings = providerEarnings;
    }

    public BigDecimal getStripeEarnings() {
        return stripeEarnings;
    }

    public void setStripeEarnings(BigDecimal stripeEarnings) {
        this.stripeEarnings = stripeEarnings;
    }

    public BigDecimal getBkashEarnings() {
        return bkashEarnings;
    }

    public void setBkashEarnings(BigDecimal bkashEarnings) {
        this.bkashEarnings = bkashEarnings;
    }

    public BigDecimal getNagadEarnings() {
        return nagadEarnings;
    }

    public void setNagadEarnings(BigDecimal nagadEarnings) {
        this.nagadEarnings = nagadEarnings;
    }

    public BigDecimal getPlatformFees() {
        return platformFees;
    }

    public void setPlatformFees(BigDecimal platformFees) {
        this.platformFees = platformFees;
    }

    public BigDecimal getStripeFees() {
        return stripeFees;
    }

    public void setStripeFees(BigDecimal stripeFees) {
        this.stripeFees = stripeFees;
    }

    public BigDecimal getBkashFees() {
        return bkashFees;
    }

    public void setBkashFees(BigDecimal bkashFees) {
        this.bkashFees = bkashFees;
    }

    public BigDecimal getNagadFees() {
        return nagadFees;
    }

    public void setNagadFees(BigDecimal nagadFees) {
        this.nagadFees = nagadFees;
    }

    public Long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(Long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public Long getSuccessfulTransactions() {
        return successfulTransactions;
    }

    public void setSuccessfulTransactions(Long successfulTransactions) {
        this.successfulTransactions = successfulTransactions;
    }

    public Long getFailedTransactions() {
        return failedTransactions;
    }

    public void setFailedTransactions(Long failedTransactions) {
        this.failedTransactions = failedTransactions;
    }

    public Long getRefundedTransactions() {
        return refundedTransactions;
    }

    public void setRefundedTransactions(Long refundedTransactions) {
        this.refundedTransactions = refundedTransactions;
    }

    public BigDecimal getTotalRefunds() {
        return totalRefunds;
    }

    public void setTotalRefunds(BigDecimal totalRefunds) {
        this.totalRefunds = totalRefunds;
    }

    public BigDecimal getRefundFees() {
        return refundFees;
    }

    public void setRefundFees(BigDecimal refundFees) {
        this.refundFees = refundFees;
    }

    public Long getTotalDisputes() {
        return totalDisputes;
    }

    public void setTotalDisputes(Long totalDisputes) {
        this.totalDisputes = totalDisputes;
    }

    public BigDecimal getDisputeAmount() {
        return disputeAmount;
    }

    public void setDisputeAmount(BigDecimal disputeAmount) {
        this.disputeAmount = disputeAmount;
    }

    public BigDecimal getDisputeFees() {
        return disputeFees;
    }

    public void setDisputeFees(BigDecimal disputeFees) {
        this.disputeFees = disputeFees;
    }

    public Long getDisputesWon() {
        return disputesWon;
    }

    public void setDisputesWon(Long disputesWon) {
        this.disputesWon = disputesWon;
    }

    public Long getDisputesLost() {
        return disputesLost;
    }

    public void setDisputesLost(Long disputesLost) {
        this.disputesLost = disputesLost;
    }

    public BigDecimal getTaxableAmount() {
        return taxableAmount;
    }

    public void setTaxableAmount(BigDecimal taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    public BigDecimal getTaxWithheld() {
        return taxWithheld;
    }

    public void setTaxWithheld(BigDecimal taxWithheld) {
        this.taxWithheld = taxWithheld;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }

    public void setVatAmount(BigDecimal vatAmount) {
        this.vatAmount = vatAmount;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
        if (status == ReportStatus.COMPLETED && generationCompletedAt == null) {
            generationCompletedAt = LocalDateTime.now();
        }
    }

    public String getCsvFilePath() {
        return csvFilePath;
    }

    public void setCsvFilePath(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    public String getPdfFilePath() {
        return pdfFilePath;
    }

    public void setPdfFilePath(String pdfFilePath) {
        this.pdfFilePath = pdfFilePath;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public LocalDateTime getGenerationStartedAt() {
        return generationStartedAt;
    }

    public void setGenerationStartedAt(LocalDateTime generationStartedAt) {
        this.generationStartedAt = generationStartedAt;
    }

    public LocalDateTime getGenerationCompletedAt() {
        return generationCompletedAt;
    }

    public void setGenerationCompletedAt(LocalDateTime generationCompletedAt) {
        this.generationCompletedAt = generationCompletedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
    public boolean isCompleted() {
        return status == ReportStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == ReportStatus.FAILED;
    }

    public boolean isGenerating() {
        return status == ReportStatus.GENERATING;
    }

    public String getPeriodDescription() {
        switch (periodType) {
            case MONTHLY:
                return year + "-" + String.format("%02d", month);
            case QUARTERLY:
                return year + " Q" + quarter;
            case YEARLY:
                return year.toString();
            default:
                return periodStart + " to " + periodEnd;
        }
    }

    // Enums
    public enum ReportType {
        REVENUE("Revenue Report", "Comprehensive revenue analysis"),
        TAX("Tax Report", "Tax documentation and withholdings"),
        PROVIDER("Provider Report", "Provider-specific earnings and fees"),
        DISPUTE("Dispute Report", "Disputes and chargebacks analysis"),
        COMPREHENSIVE("Comprehensive Report", "All financial metrics combined");

        private final String displayName;
        private final String description;

        ReportType(String displayName, String description) {
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

    public enum PeriodType {
        MONTHLY("Monthly", "Monthly reporting period"),
        QUARTERLY("Quarterly", "Quarterly reporting period"),
        YEARLY("Yearly", "Yearly reporting period"),
        CUSTOM("Custom", "Custom date range");

        private final String displayName;
        private final String description;

        PeriodType(String displayName, String description) {
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

    public enum ReportStatus {
        GENERATING("Generating", "Report is being generated"),
        COMPLETED("Completed", "Report generation completed successfully"),
        FAILED("Failed", "Report generation failed"),
        CANCELLED("Cancelled", "Report generation was cancelled");

        private final String displayName;
        private final String description;

        ReportStatus(String displayName, String description) {
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
        return "FinancialReport{" +
                "id=" + id +
                ", reportId='" + reportId + '\'' +
                ", reportType=" + reportType +
                ", periodType=" + periodType +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
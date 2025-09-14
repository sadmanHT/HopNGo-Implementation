package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing tax documents generated for compliance purposes
 */
@Entity
@Table(name = "tax_documents", indexes = {
    @Index(name = "idx_tax_documents_jurisdiction", columnList = "jurisdiction"),
    @Index(name = "idx_tax_documents_tax_year", columnList = "tax_year"),
    @Index(name = "idx_tax_documents_type", columnList = "document_type"),
    @Index(name = "idx_tax_documents_status", columnList = "status"),
    @Index(name = "idx_tax_documents_created_at", columnList = "created_at")
})
public class TaxDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @NotNull
    @Size(min = 2, max = 10)
    @Column(name = "jurisdiction", nullable = false, length = 10)
    private String jurisdiction;

    @NotNull
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status;

    @Lob
    @Column(name = "document_content", columnDefinition = "LONGBLOB")
    private byte[] documentContent;

    @Column(name = "content_type", length = 100)
    private String contentType = "text/plain";

    @Column(name = "file_size")
    private Long fileSize;

    // Financial data fields
    @Column(name = "gross_revenue", precision = 19, scale = 4)
    private BigDecimal grossRevenue;

    @Column(name = "net_revenue", precision = 19, scale = 4)
    private BigDecimal netRevenue;

    @Column(name = "total_expenses", precision = 19, scale = 4)
    private BigDecimal totalExpenses;

    @Column(name = "taxable_income", precision = 19, scale = 4)
    private BigDecimal taxableIncome;

    @Column(name = "estimated_tax_liability", precision = 19, scale = 4)
    private BigDecimal estimatedTaxLiability;

    @Column(name = "vat_collected", precision = 19, scale = 4)
    private BigDecimal vatCollected;

    @Column(name = "vat_paid", precision = 19, scale = 4)
    private BigDecimal vatPaid;

    @Column(name = "net_vat_liability", precision = 19, scale = 4)
    private BigDecimal netVatLiability;

    @Column(name = "withholding_tax_amount", precision = 19, scale = 4)
    private BigDecimal withholdingTaxAmount;

    // Metadata fields
    @Size(max = 1000)
    @Column(name = "notes", length = 1000)
    private String notes;

    @Size(max = 100)
    @Column(name = "generated_by", length = 100)
    private String generatedBy = "SYSTEM";

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public TaxDocument() {}

    public TaxDocument(DocumentType documentType, String jurisdiction, Integer taxYear) {
        this.documentType = documentType;
        this.jurisdiction = jurisdiction;
        this.taxYear = taxYear;
        this.status = DocumentStatus.DRAFT;
    }

    // Enums
    public enum DocumentType {
        INCOME_STATEMENT("Income Statement"),
        VAT_REPORT("VAT Report"),
        WITHHOLDING_TAX("Withholding Tax Report"),
        TRANSACTION_SUMMARY("Transaction Summary"),
        PROVIDER_EARNINGS("Provider Earnings Report"),
        ANNUAL_SUMMARY("Annual Tax Summary"),
        COMPLIANCE_REPORT("Compliance Report");

        private final String displayName;

        DocumentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum DocumentStatus {
        DRAFT("Draft"),
        COMPLETED("Completed"),
        APPROVED("Approved"),
        SUBMITTED("Submitted"),
        FAILED("Failed"),
        ARCHIVED("Archived");

        private final String displayName;

        DocumentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public Integer getTaxYear() {
        return taxYear;
    }

    public void setTaxYear(Integer taxYear) {
        this.taxYear = taxYear;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public byte[] getDocumentContent() {
        return documentContent;
    }

    public void setDocumentContent(byte[] documentContent) {
        this.documentContent = documentContent;
        if (documentContent != null) {
            this.fileSize = (long) documentContent.length;
        }
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
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

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getTaxableIncome() {
        return taxableIncome;
    }

    public void setTaxableIncome(BigDecimal taxableIncome) {
        this.taxableIncome = taxableIncome;
    }

    public BigDecimal getEstimatedTaxLiability() {
        return estimatedTaxLiability;
    }

    public void setEstimatedTaxLiability(BigDecimal estimatedTaxLiability) {
        this.estimatedTaxLiability = estimatedTaxLiability;
    }

    public BigDecimal getVatCollected() {
        return vatCollected;
    }

    public void setVatCollected(BigDecimal vatCollected) {
        this.vatCollected = vatCollected;
    }

    public BigDecimal getVatPaid() {
        return vatPaid;
    }

    public void setVatPaid(BigDecimal vatPaid) {
        this.vatPaid = vatPaid;
    }

    public BigDecimal getNetVatLiability() {
        return netVatLiability;
    }

    public void setNetVatLiability(BigDecimal netVatLiability) {
        this.netVatLiability = netVatLiability;
    }

    public BigDecimal getWithholdingTaxAmount() {
        return withholdingTaxAmount;
    }

    public void setWithholdingTaxAmount(BigDecimal withholdingTaxAmount) {
        this.withholdingTaxAmount = withholdingTaxAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
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
    public void approve(String approvedBy) {
        this.status = DocumentStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    public void submit() {
        if (this.status != DocumentStatus.APPROVED) {
            throw new IllegalStateException("Document must be approved before submission");
        }
        this.status = DocumentStatus.SUBMITTED;
    }

    public void archive() {
        this.status = DocumentStatus.ARCHIVED;
    }

    public boolean isEditable() {
        return this.status == DocumentStatus.DRAFT || this.status == DocumentStatus.FAILED;
    }

    public boolean hasContent() {
        return this.documentContent != null && this.documentContent.length > 0;
    }

    public String getFileName() {
        return String.format("%s_%s_%d_%s.txt", 
            documentType.name().toLowerCase(),
            jurisdiction.toLowerCase(),
            taxYear,
            id != null ? id.toString() : "draft"
        );
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaxDocument that = (TaxDocument) o;
        return Objects.equals(id, that.id) &&
               documentType == that.documentType &&
               Objects.equals(jurisdiction, that.jurisdiction) &&
               Objects.equals(taxYear, that.taxYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, documentType, jurisdiction, taxYear);
    }

    @Override
    public String toString() {
        return "TaxDocument{" +
                "id=" + id +
                ", documentType=" + documentType +
                ", jurisdiction='" + jurisdiction + '\'' +
                ", taxYear=" + taxYear +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
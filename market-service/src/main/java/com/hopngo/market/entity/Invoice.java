package com.hopngo.market.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@EntityListeners(AuditingEntityListener.class)
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    
    // Note: booking_id will be added when booking service is integrated
    @Column(name = "booking_id")
    private UUID bookingId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;
    
    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String currency = "BDT";
    
    // Financial amounts stored in minor units (paisa/cents)
    @NotNull
    @Min(0)
    @Column(name = "subtotal_minor", nullable = false)
    private Long subtotalMinor;
    
    @NotNull
    @Min(0)
    @Column(name = "tax_minor", nullable = false)
    private Long taxMinor = 0L;
    
    @NotNull
    @Min(0)
    @Column(name = "fees_minor", nullable = false)
    private Long feesMinor = 0L;
    
    @NotNull
    @Min(0)
    @Column(name = "total_minor", nullable = false)
    private Long totalMinor;
    
    // Tax details
    @DecimalMin(value = "0.0000")
    @DecimalMax(value = "1.0000")
    @Digits(integer = 1, fraction = 4)
    @Column(name = "tax_rate", precision = 5, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;
    
    @Size(max = 3)
    @Column(name = "tax_country", length = 3)
    private String taxCountry;
    
    // Fee details
    @DecimalMin(value = "0.0000")
    @DecimalMax(value = "1.0000")
    @Digits(integer = 1, fraction = 4)
    @Column(name = "platform_fee_rate", precision = 5, scale = 4)
    private BigDecimal platformFeeRate = BigDecimal.ZERO;
    
    @NotNull
    @Min(0)
    @Column(name = "platform_fee_minor", nullable = false)
    private Long platformFeeMinor = 0L;
    
    @NotNull
    @Min(0)
    @Column(name = "payment_processing_fee_minor", nullable = false)
    private Long paymentProcessingFeeMinor = 0L;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;
    
    @Column(name = "issued_at")
    private LocalDateTime issuedAt;
    
    @Column(name = "due_at")
    private LocalDateTime dueAt;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Size(max = 500)
    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;
    
    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "JSONB")
    private String metadata;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public Invoice() {}
    
    public Invoice(Order order, UUID userId, String currency) {
        this.order = order;
        this.userId = userId;
        this.currency = currency;
        this.invoiceNumber = generateInvoiceNumber();
    }
    
    public Invoice(UUID bookingId, UUID userId, String currency) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.currency = currency;
        this.invoiceNumber = generateInvoiceNumber();
    }
    
    // Business methods
    public void calculateTotals() {
        this.feesMinor = this.platformFeeMinor + this.paymentProcessingFeeMinor;
        this.totalMinor = this.subtotalMinor + this.taxMinor + this.feesMinor;
    }
    
    public void markAsIssued() {
        this.status = InvoiceStatus.ISSUED;
        this.issuedAt = LocalDateTime.now();
        if (this.dueAt == null) {
            this.dueAt = this.issuedAt.plusDays(30); // Default 30 days payment term
        }
    }
    
    public void markAsPaid() {
        this.status = InvoiceStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }
    
    public void markAsCancelled() {
        this.status = InvoiceStatus.CANCELLED;
    }
    
    public void markAsRefunded() {
        this.status = InvoiceStatus.REFUNDED;
    }
    
    public void setPdfGenerated(String pdfUrl) {
        this.pdfUrl = pdfUrl;
        this.pdfGeneratedAt = LocalDateTime.now();
    }
    
    // Utility methods for amount conversion
    public BigDecimal getSubtotal() {
        return convertMinorToDecimal(subtotalMinor);
    }
    
    public BigDecimal getSubtotalDecimal() {
        return getSubtotal();
    }
    
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotalMinor = convertDecimalToMinor(subtotal);
    }
    
    public BigDecimal getTax() {
        return convertMinorToDecimal(taxMinor);
    }
    
    public BigDecimal getTaxDecimal() {
        return getTax();
    }
    
    public void setTax(BigDecimal tax) {
        this.taxMinor = convertDecimalToMinor(tax);
    }
    
    public BigDecimal getFees() {
        return convertMinorToDecimal(feesMinor);
    }
    
    public BigDecimal getFeesDecimal() {
        return getFees();
    }
    
    public void setFees(BigDecimal fees) {
        this.feesMinor = convertDecimalToMinor(fees);
    }
    
    public BigDecimal getTotal() {
        return convertMinorToDecimal(totalMinor);
    }
    
    public BigDecimal getTotalDecimal() {
        return getTotal();
    }
    
    public void setTotal(BigDecimal total) {
        this.totalMinor = convertDecimalToMinor(total);
    }
    
    public BigDecimal getPlatformFee() {
        return convertMinorToDecimal(platformFeeMinor);
    }
    
    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFeeMinor = convertDecimalToMinor(platformFee);
    }
    
    public BigDecimal getPaymentProcessingFee() {
        return convertMinorToDecimal(paymentProcessingFeeMinor);
    }
    
    public void setPaymentProcessingFee(BigDecimal paymentProcessingFee) {
        this.paymentProcessingFeeMinor = convertDecimalToMinor(paymentProcessingFee);
    }
    
    private BigDecimal convertMinorToDecimal(Long minorAmount) {
        if (minorAmount == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(minorAmount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    private Long convertDecimalToMinor(BigDecimal decimalAmount) {
        if (decimalAmount == null) return 0L;
        return decimalAmount.multiply(BigDecimal.valueOf(100)).longValue();
    }
    
    private String generateInvoiceNumber() {
        // This will be replaced by database function in production
        return "INV-" + System.currentTimeMillis();
    }
    
    // Status check methods
    public boolean isDraft() {
        return InvoiceStatus.DRAFT.equals(this.status);
    }
    
    public boolean isIssued() {
        return InvoiceStatus.ISSUED.equals(this.status);
    }
    
    public boolean isPaid() {
        return InvoiceStatus.PAID.equals(this.status);
    }
    
    public boolean isCancelled() {
        return InvoiceStatus.CANCELLED.equals(this.status);
    }
    
    public boolean isRefunded() {
        return InvoiceStatus.REFUNDED.equals(this.status);
    }
    
    public boolean isOverdue() {
        return isIssued() && dueAt != null && LocalDateTime.now().isAfter(dueAt);
    }
    
    // Status validation methods
    public boolean canBePaid() {
        return isIssued() && !isOverdue();
    }
    
    public boolean canBeCancelled() {
        return isDraft() || isIssued();
    }
    
    public boolean canBeRefunded() {
        return isPaid();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Order getOrder() {
        return order;
    }
    
    public UUID getOrderId() {
        return order != null ? order.getId() : null;
    }
    
    public void setOrder(Order order) {
        this.order = order;
    }
    
    public UUID getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getInvoiceNumber() {
        return invoiceNumber;
    }
    
    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Long getSubtotalMinor() {
        return subtotalMinor;
    }
    
    public void setSubtotalMinor(Long subtotalMinor) {
        this.subtotalMinor = subtotalMinor;
    }
    
    public Long getTaxMinor() {
        return taxMinor;
    }
    
    public void setTaxMinor(Long taxMinor) {
        this.taxMinor = taxMinor;
    }
    
    public Long getFeesMinor() {
        return feesMinor;
    }
    
    public void setFeesMinor(Long feesMinor) {
        this.feesMinor = feesMinor;
    }
    
    public Long getTotalMinor() {
        return totalMinor;
    }
    
    public void setTotalMinor(Long totalMinor) {
        this.totalMinor = totalMinor;
    }
    
    public BigDecimal getTaxRate() {
        return taxRate;
    }
    
    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
    
    public String getTaxCountry() {
        return taxCountry;
    }
    
    public void setTaxCountry(String taxCountry) {
        this.taxCountry = taxCountry;
    }
    
    public BigDecimal getPlatformFeeRate() {
        return platformFeeRate;
    }
    
    public void setPlatformFeeRate(BigDecimal platformFeeRate) {
        this.platformFeeRate = platformFeeRate;
    }
    
    public Long getPlatformFeeMinor() {
        return platformFeeMinor;
    }
    
    public void setPlatformFeeMinor(Long platformFeeMinor) {
        this.platformFeeMinor = platformFeeMinor;
    }
    
    public Long getPaymentProcessingFeeMinor() {
        return paymentProcessingFeeMinor;
    }
    
    public void setPaymentProcessingFeeMinor(Long paymentProcessingFeeMinor) {
        this.paymentProcessingFeeMinor = paymentProcessingFeeMinor;
    }
    
    public InvoiceStatus getStatus() {
        return status;
    }
    
    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public LocalDateTime getDueAt() {
        return dueAt;
    }
    
    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }
    
    public LocalDateTime getPaidAt() {
        return paidAt;
    }
    
    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
    
    public String getPdfUrl() {
        return pdfUrl;
    }
    
    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }
    
    public LocalDateTime getPdfGeneratedAt() {
        return pdfGeneratedAt;
    }
    
    public void setPdfGeneratedAt(LocalDateTime pdfGeneratedAt) {
        this.pdfGeneratedAt = pdfGeneratedAt;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
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
}
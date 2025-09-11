package com.hopngo.market.service;

import com.hopngo.market.entity.Invoice;
import com.hopngo.market.entity.InvoiceStatus;
import com.hopngo.market.entity.Order;
import com.hopngo.market.entity.Payment;
import com.hopngo.market.repository.InvoiceRepository;
import com.hopngo.market.service.finance.TaxCalculationService;
import com.hopngo.market.service.finance.FeeCalculationService;
import com.hopngo.market.service.PdfGenerationService;
import com.hopngo.market.exception.InvoiceNotFoundException;
import com.hopngo.market.exception.InvoiceAlreadyExistsException;
import com.hopngo.market.exception.InvalidInvoiceStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
    
    private final InvoiceRepository invoiceRepository;
    private final TaxCalculationService taxCalculationService;
    private final FeeCalculationService feeCalculationService;
    private final PdfGenerationService pdfGenerationService;
    
    @Autowired
    public InvoiceService(InvoiceRepository invoiceRepository,
                         TaxCalculationService taxCalculationService,
                         FeeCalculationService feeCalculationService,
                         PdfGenerationService pdfGenerationService) {
        this.invoiceRepository = invoiceRepository;
        this.taxCalculationService = taxCalculationService;
        this.feeCalculationService = feeCalculationService;
        this.pdfGenerationService = pdfGenerationService;
    }
    
    /**
     * Create an invoice for an order.
     */
    public Invoice createInvoiceForOrder(Order order, String currency, String taxCountry) {
        logger.info("Creating invoice for order: {}", order.getId());
        
        // Check if invoice already exists for this order
        if (invoiceRepository.existsByOrderId(order.getId())) {
            throw new InvoiceAlreadyExistsException("Invoice already exists for order: " + order.getId());
        }
        
        Invoice invoice = new Invoice(order, order.getUserId(), currency);
        invoice.setDescription("Invoice for order: " + order.getId());
        invoice.setTaxCountry(taxCountry);
        
        // Calculate amounts
        calculateInvoiceAmounts(invoice, order.getTotalAmount(), taxCountry);
        
        invoice = invoiceRepository.save(invoice);
        logger.info("Created invoice: {} for order: {}", invoice.getInvoiceNumber(), order.getId());
        
        return invoice;
    }
    
    /**
     * Create an invoice for a booking.
     */
    public Invoice createInvoiceForBooking(UUID bookingId, UUID userId, BigDecimal amount, String currency, String taxCountry) {
        logger.info("Creating invoice for booking: {}", bookingId);
        
        // Check if invoice already exists for this booking
        if (invoiceRepository.existsByBookingId(bookingId)) {
            throw new InvoiceAlreadyExistsException("Invoice already exists for booking: " + bookingId);
        }
        
        Invoice invoice = new Invoice(bookingId, userId, currency);
        invoice.setDescription("Invoice for booking: " + bookingId);
        invoice.setTaxCountry(taxCountry);
        
        // Calculate amounts
        calculateInvoiceAmounts(invoice, amount, taxCountry);
        
        invoice = invoiceRepository.save(invoice);
        logger.info("Created invoice: {} for booking: {}", invoice.getInvoiceNumber(), bookingId);
        
        return invoice;
    }
    
    /**
     * Issue an invoice (make it payable).
     */
    public Invoice issueInvoice(UUID invoiceId) {
        logger.info("Issuing invoice: {}", invoiceId);
        
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (!invoice.isDraft()) {
            throw new InvalidInvoiceStateException("Invoice must be in DRAFT status to be issued");
        }
        
        invoice.markAsIssued();
        invoice = invoiceRepository.save(invoice);
        
        // Generate PDF asynchronously
        try {
            generateInvoicePdf(invoice);
        } catch (Exception e) {
            logger.error("Failed to generate PDF for invoice: {}", invoice.getId(), e);
            // Don't fail the invoice issuance if PDF generation fails
        }
        
        logger.info("Issued invoice: {}", invoice.getInvoiceNumber());
        return invoice;
    }
    
    /**
     * Mark an invoice as paid (called when payment is successful).
     */
    public Invoice markInvoiceAsPaid(UUID invoiceId, Payment payment) {
        logger.info("Marking invoice as paid: {}", invoiceId);
        
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (!invoice.canBePaid()) {
            throw new InvalidInvoiceStateException("Invoice cannot be paid in current status: " + invoice.getStatus());
        }
        
        invoice.markAsPaid();
        invoice = invoiceRepository.save(invoice);
        
        logger.info("Marked invoice as paid: {}", invoice.getInvoiceNumber());
        return invoice;
    }
    
    /**
     * Cancel an invoice.
     */
    public Invoice cancelInvoice(UUID invoiceId, String reason) {
        logger.info("Cancelling invoice: {} with reason: {}", invoiceId, reason);
        
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (!invoice.canBeCancelled()) {
            throw new InvalidInvoiceStateException("Invoice cannot be cancelled in current status: " + invoice.getStatus());
        }
        
        invoice.markAsCancelled();
        invoice.setDescription(invoice.getDescription() + " | Cancelled: " + reason);
        invoice = invoiceRepository.save(invoice);
        
        logger.info("Cancelled invoice: {}", invoice.getInvoiceNumber());
        return invoice;
    }
    
    /**
     * Refund an invoice.
     */
    public Invoice refundInvoice(UUID invoiceId, String reason) {
        logger.info("Refunding invoice: {} with reason: {}", invoiceId, reason);
        
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (!invoice.canBeRefunded()) {
            throw new InvalidInvoiceStateException("Invoice cannot be refunded in current status: " + invoice.getStatus());
        }
        
        invoice.markAsRefunded();
        invoice.setDescription(invoice.getDescription() + " | Refunded: " + reason);
        invoice = invoiceRepository.save(invoice);
        
        logger.info("Refunded invoice: {}", invoice.getInvoiceNumber());
        return invoice;
    }
    
    /**
     * Get invoice by ID.
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceById(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found: " + invoiceId));
    }
    
    /**
     * Get invoice by invoice number.
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found: " + invoiceNumber));
    }
    
    /**
     * Get invoices for a user.
     */
    @Transactional(readOnly = true)
    public Page<Invoice> getInvoicesForUser(UUID userId, Pageable pageable) {
        return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * Get invoices for a user with specific status.
     */
    @Transactional(readOnly = true)
    public Page<Invoice> getInvoicesForUser(UUID userId, InvoiceStatus status, Pageable pageable) {
        return invoiceRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
    }
    
    /**
     * Get invoice by order ID.
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceByOrderId(UUID orderId) {
        return invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found for order: " + orderId));
    }
    
    /**
     * Get invoice by booking ID.
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceByBookingId(UUID bookingId) {
        return invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found for booking: " + bookingId));
    }
    
    /**
     * Get all overdue invoices.
     */
    @Transactional(readOnly = true)
    public List<Invoice> getOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices(LocalDateTime.now());
    }
    
    /**
     * Get invoices that need PDF generation.
     */
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesNeedingPdfGeneration() {
        return invoiceRepository.findInvoicesNeedingPdfGeneration();
    }
    
    /**
     * Generate PDF for an invoice.
     */
    public void generateInvoicePdf(Invoice invoice) {
        try {
            logger.info("Generating PDF for invoice: {}", invoice.getInvoiceNumber());
            
            byte[] pdfBytes = pdfGenerationService.generateInvoicePdf(invoice);
            
            // In a real implementation, you would upload the PDF bytes to a storage service
            // and get back a URL. For now, we'll generate a mock URL.
            String pdfUrl = generatePdfUrl(invoice.getInvoiceNumber(), pdfBytes);
            
            invoice.setPdfGenerated(pdfUrl);
            invoiceRepository.save(invoice);
            
            logger.info("Generated PDF for invoice: {} at URL: {}", invoice.getInvoiceNumber(), pdfUrl);
        } catch (Exception e) {
            logger.error("Failed to generate PDF for invoice: {}", invoice.getId(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
    
    /**
     * Generate a mock PDF URL (in production, this would upload to storage and return actual URL)
     */
    private String generatePdfUrl(String invoiceNumber, byte[] pdfBytes) {
        // In production, you would:
        // 1. Upload pdfBytes to cloud storage (S3, Google Cloud, etc.)
        // 2. Return the public URL of the uploaded file
        // For now, return a mock URL
        return "https://storage.hopngo.com/invoices/" + invoiceNumber + ".pdf";
    }
    
    /**
     * Calculate invoice amounts including taxes and fees.
     */
    private void calculateInvoiceAmounts(Invoice invoice, BigDecimal baseAmount, String taxCountry) {
        logger.debug("Calculating amounts for invoice with base amount: {} in country: {}", baseAmount, taxCountry);
        
        // Set subtotal
        invoice.setSubtotal(baseAmount);
        
        // Calculate tax
        BigDecimal taxAmount = taxCalculationService.calculateTax(baseAmount, taxCountry, ""); // Empty state for now
        BigDecimal taxRate = taxCalculationService.getTaxRate(taxCountry, "");
        invoice.setTaxRate(taxRate);
        invoice.setTax(taxAmount);
        
        // Calculate platform fee
        BigDecimal platformFee = feeCalculationService.calculatePlatformFee(baseAmount, "PURCHASE"); // Default to purchase type
        // Calculate the rate from the result for storage
        BigDecimal platformFeeRate = baseAmount.compareTo(BigDecimal.ZERO) > 0 ? 
            platformFee.divide(baseAmount, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        invoice.setPlatformFeeRate(platformFeeRate);
        invoice.setPlatformFee(platformFee);
        
        // Calculate payment processing fee
        BigDecimal paymentProcessingFee = feeCalculationService.calculateProcessingFee(baseAmount, "CREDIT_CARD"); // Default payment method
        invoice.setPaymentProcessingFee(paymentProcessingFee);
        
        // Calculate totals
        invoice.calculateTotals();
        
        logger.debug("Calculated amounts - Subtotal: {}, Tax: {}, Platform Fee: {}, Processing Fee: {}, Total: {}",
                invoice.getSubtotal(), invoice.getTax(), invoice.getPlatformFee(), 
                invoice.getPaymentProcessingFee(), invoice.getTotal());
    }
    
    /**
     * Get invoices by user ID with pagination.
     */
    public Page<Invoice> getInvoicesByUserId(UUID userId, Pageable pageable) {
        logger.info("Getting invoices for user: {}", userId);
        return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * Get all invoices with pagination (admin only).
     */
    public Page<Invoice> getAllInvoices(Pageable pageable) {
        logger.info("Getting all invoices with pagination");
        return invoiceRepository.findAll(pageable);
    }
}
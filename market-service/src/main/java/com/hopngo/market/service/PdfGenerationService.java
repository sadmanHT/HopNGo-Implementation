package com.hopngo.market.service;

import com.hopngo.market.entity.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for generating PDF invoices and receipts.
 * This is a stub implementation that generates simple text-based PDFs.
 * In production, consider using libraries like iText, Apache PDFBox, or Flying Saucer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    /**
     * Generate PDF bytes for an invoice.
     * This is a stub implementation that creates a simple text-based PDF.
     */
    public byte[] generateInvoicePdf(Invoice invoice) {
        log.info("Generating PDF for invoice: {}", invoice.getId());
        
        try {
            String pdfContent = buildInvoiceContent(invoice);
            return createSimplePdf(pdfContent);
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice: {}", invoice.getId(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
    
    /**
     * Build the content for the invoice PDF.
     */
    private String buildInvoiceContent(Invoice invoice) {
        StringBuilder content = new StringBuilder();
        
        // Header
        content.append("\n\n")
                .append("                    HopNGo - Invoice\n")
                .append("                 Travel & Booking Platform\n")
                .append("================================================================\n\n");
        
        // Invoice details
        content.append("Invoice Number: ").append(invoice.getInvoiceNumber()).append("\n")
                .append("Invoice Date: ").append(invoice.getIssuedAt() != null ? 
                        invoice.getIssuedAt().format(DATE_FORMATTER) : "Draft").append("\n")
                .append("Status: ").append(invoice.getStatus()).append("\n")
                .append("Currency: ").append(invoice.getCurrency()).append("\n\n");
        
        // Customer details
        content.append("Customer ID: ").append(invoice.getUserId()).append("\n");
        
        if (invoice.getOrderId() != null) {
            content.append("Order ID: ").append(invoice.getOrderId()).append("\n");
        }
        if (invoice.getBookingId() != null) {
            content.append("Booking ID: ").append(invoice.getBookingId()).append("\n");
        }
        
        content.append("\n================================================================\n\n");
        
        // Amount breakdown
        content.append("AMOUNT BREAKDOWN:\n\n");
        
        BigDecimal subtotal = invoice.getSubtotalDecimal();
        BigDecimal tax = invoice.getTaxDecimal();
        BigDecimal fees = invoice.getFeesDecimal();
        BigDecimal total = invoice.getTotalDecimal();
        
        content.append(String.format("Subtotal:        %s %s\n", 
                formatAmount(subtotal), invoice.getCurrency()))
                .append(String.format("Tax (VAT/GST):   %s %s\n", 
                        formatAmount(tax), invoice.getCurrency()))
                .append(String.format("Platform Fees:   %s %s\n", 
                        formatAmount(fees), invoice.getCurrency()))
                .append("                 ----------------\n")
                .append(String.format("TOTAL:           %s %s\n", 
                        formatAmount(total), invoice.getCurrency()));
        
        content.append("\n================================================================\n\n");
        
        // Payment information
        if (invoice.getStatus().name().equals("PAID")) {
            content.append("PAYMENT STATUS: PAID\n")
                    .append("Payment Date: ").append(invoice.getUpdatedAt().format(DATE_FORMATTER)).append("\n\n");
        } else {
            content.append("PAYMENT STATUS: ").append(invoice.getStatus()).append("\n\n");
        }
        
        // Footer
        content.append("\n\n")
                .append("Thank you for choosing HopNGo!\n")
                .append("\n")
                .append("For support, contact: support@hopngo.com\n")
                .append("Website: https://hopngo.com\n")
                .append("\n")
                .append("================================================================\n")
                .append("This is a computer-generated invoice.\n");
        
        return content.toString();
    }
    
    /**
     * Create a simple PDF from text content.
     * This is a stub implementation. In production, use proper PDF libraries.
     */
    private byte[] createSimplePdf(String content) throws IOException {
        // This is a very basic implementation that creates a "PDF-like" text file
        // In production, replace this with actual PDF generation using libraries like:
        // - iText 7
        // - Apache PDFBox
        // - Flying Saucer (for HTML to PDF)
        // - Thymeleaf + Flying Saucer for template-based PDFs
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Simple PDF header (this is not a real PDF format)
        String pdfHeader = "%PDF-1.4\n" +
                "1 0 obj\n" +
                "<<\n" +
                "/Type /Catalog\n" +
                "/Pages 2 0 R\n" +
                ">>\n" +
                "endobj\n\n";
        
        outputStream.write(pdfHeader.getBytes());
        outputStream.write(content.getBytes());
        
        // Simple PDF footer
        String pdfFooter = "\n\nxref\n" +
                "0 3\n" +
                "0000000000 65535 f \n" +
                "0000000009 00000 n \n" +
                "trailer\n" +
                "<<\n" +
                "/Size 3\n" +
                "/Root 1 0 R\n" +
                ">>\n" +
                "startxref\n" +
                "0\n" +
                "%%EOF";
        
        outputStream.write(pdfFooter.getBytes());
        
        byte[] pdfBytes = outputStream.toByteArray();
        outputStream.close();
        
        log.debug("Generated PDF with {} bytes for content length: {}", 
                pdfBytes.length, content.length());
        
        return pdfBytes;
    }
    
    /**
     * Format amount for display.
     */
    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
    
    /**
     * Generate a simple receipt PDF (smaller format).
     */
    public byte[] generateReceiptPdf(Invoice invoice) {
        log.info("Generating receipt PDF for invoice: {}", invoice.getId());
        
        try {
            String receiptContent = buildReceiptContent(invoice);
            return createSimplePdf(receiptContent);
        } catch (Exception e) {
            log.error("Failed to generate receipt PDF for invoice: {}", invoice.getId(), e);
            throw new RuntimeException("Receipt PDF generation failed", e);
        }
    }
    
    /**
     * Build simplified receipt content.
     */
    private String buildReceiptContent(Invoice invoice) {
        StringBuilder content = new StringBuilder();
        
        content.append("\n\n")
                .append("           HopNGo - Receipt\n")
                .append("================================\n\n")
                .append("Receipt #: ").append(invoice.getInvoiceNumber()).append("\n")
                .append("Date: ").append(invoice.getIssuedAt() != null ? 
                        invoice.getIssuedAt().format(DATE_FORMATTER) : "Draft").append("\n")
                .append("Total: ").append(formatAmount(invoice.getTotalDecimal()))
                .append(" ").append(invoice.getCurrency()).append("\n")
                .append("Status: ").append(invoice.getStatus()).append("\n\n")
                .append("Thank you for your business!\n")
                .append("================================\n");
        
        return content.toString();
    }
}
package com.hopngo.market.controller;

import com.hopngo.market.entity.Invoice;
import com.hopngo.market.service.InvoiceService;
import com.hopngo.market.service.PdfGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for invoice operations.
 */
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {
    
    private final InvoiceService invoiceService;
    private final PdfGenerationService pdfGenerationService;
    
    /**
     * Get invoice by ID with authorization check.
     */
    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Invoice> getInvoice(
            @PathVariable UUID invoiceId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        log.info("User {} requesting invoice {}", userId, invoiceId);
        
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);
        
        // Authorization check - user can only access their own invoices
        if (!invoice.getUserId().equals(UUID.fromString(userId))) {
            log.warn("User {} attempted to access invoice {} belonging to user {}", 
                    userId, invoiceId, invoice.getUserId());
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(invoice);
    }
    
    /**
     * Get user's invoices with pagination.
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Invoice>> getUserInvoices(
            Authentication authentication,
            Pageable pageable) {
        
        String userId = authentication.getName();
        log.info("User {} requesting invoices, page: {}", userId, pageable.getPageNumber());
        
        Page<Invoice> invoices = invoiceService.getInvoicesByUserId(
                UUID.fromString(userId), pageable);
        
        return ResponseEntity.ok(invoices);
    }
    
    /**
     * Download invoice PDF.
     */
    @GetMapping("/{invoiceId}/pdf")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable UUID invoiceId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        log.info("User {} requesting PDF for invoice {}", userId, invoiceId);
        
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);
        
        // Authorization check
        if (!invoice.getUserId().equals(UUID.fromString(userId))) {
            log.warn("User {} attempted to access PDF for invoice {} belonging to user {}", 
                    userId, invoiceId, invoice.getUserId());
            return ResponseEntity.notFound().build();
        }
        
        byte[] pdfBytes = pdfGenerationService.generateInvoicePdf(invoice);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"invoice-" + invoice.getInvoiceNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
    
    /**
     * Admin endpoint to get all invoices.
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Invoice>> getAllInvoices(Pageable pageable) {
        
        log.info("Admin requesting all invoices, page: {}", pageable.getPageNumber());
        Page<Invoice> invoices = invoiceService.getAllInvoices(pageable);
        
        return ResponseEntity.ok(invoices);
    }
    
    /**
     * Admin endpoint to manually issue an invoice.
     */
    @PostMapping("/admin/{invoiceId}/issue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Invoice> issueInvoice(
            @PathVariable UUID invoiceId,
            Authentication authentication) {
        
        String adminId = authentication.getName();
        log.info("Admin {} issuing invoice {}", adminId, invoiceId);
        
        Invoice invoice = invoiceService.issueInvoice(invoiceId);
        
        return ResponseEntity.ok(invoice);
    }
    
    /**
     * Admin endpoint to cancel an invoice.
     */
    @PostMapping("/admin/{invoiceId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Invoice> cancelInvoice(
            @PathVariable UUID invoiceId,
            Authentication authentication) {
        
        String adminId = authentication.getName();
        log.info("Admin {} cancelling invoice {}", adminId, invoiceId);
        
        Invoice invoice = invoiceService.cancelInvoice(invoiceId, "Cancelled by admin: " + adminId);
        
        return ResponseEntity.ok(invoice);
    }
}
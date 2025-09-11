package com.hopngo.market.repository;

import com.hopngo.market.entity.Invoice;
import com.hopngo.market.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    
    /**
     * Find invoice by invoice number.
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    /**
     * Find all invoices for a specific user.
     */
    Page<Invoice> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    /**
     * Find all invoices for a specific user with a specific status.
     */
    Page<Invoice> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, InvoiceStatus status, Pageable pageable);
    
    /**
     * Find invoice by order ID.
     */
    Optional<Invoice> findByOrderId(UUID orderId);
    
    /**
     * Find invoice by booking ID.
     */
    Optional<Invoice> findByBookingId(UUID bookingId);
    
    /**
     * Find all invoices with a specific status.
     */
    Page<Invoice> findByStatusOrderByCreatedAtDesc(InvoiceStatus status, Pageable pageable);
    
    /**
     * Find all invoices in a specific currency.
     */
    Page<Invoice> findByCurrencyOrderByCreatedAtDesc(String currency, Pageable pageable);
    
    /**
     * Find all invoices issued within a date range.
     */
    Page<Invoice> findByIssuedAtBetweenOrderByIssuedAtDesc(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find all overdue invoices (issued but not paid and past due date).
     */
    @Query("SELECT i FROM Invoice i WHERE i.status = 'ISSUED' AND i.dueAt < :currentDate")
    List<Invoice> findOverdueInvoices(@Param("currentDate") LocalDateTime currentDate);
    
    /**
     * Find all invoices that need PDF generation.
     */
    @Query("SELECT i FROM Invoice i WHERE i.status = 'ISSUED' AND i.pdfUrl IS NULL")
    List<Invoice> findInvoicesNeedingPdfGeneration();
    
    /**
     * Get total revenue by currency and status.
     */
    @Query("SELECT i.currency, SUM(i.totalMinor) FROM Invoice i WHERE i.status = :status GROUP BY i.currency")
    List<Object[]> getTotalRevenueByCurrencyAndStatus(@Param("status") InvoiceStatus status);
    
    /**
     * Get total revenue for a specific user.
     */
    @Query("SELECT SUM(i.totalMinor) FROM Invoice i WHERE i.userId = :userId AND i.status = 'PAID'")
    Long getTotalRevenueForUser(@Param("userId") UUID userId);
    
    /**
     * Get invoice statistics for a date range.
     */
    @Query("SELECT i.status, COUNT(i), SUM(i.totalMinor) FROM Invoice i " +
           "WHERE i.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY i.status")
    List<Object[]> getInvoiceStatistics(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get monthly revenue report.
     */
    @Query("SELECT EXTRACT(YEAR FROM i.paidAt) as year, " +
           "EXTRACT(MONTH FROM i.paidAt) as month, " +
           "i.currency, " +
           "COUNT(i) as invoiceCount, " +
           "SUM(i.subtotalMinor) as subtotal, " +
           "SUM(i.taxMinor) as tax, " +
           "SUM(i.feesMinor) as fees, " +
           "SUM(i.totalMinor) as total " +
           "FROM Invoice i " +
           "WHERE i.status = 'PAID' AND i.paidAt BETWEEN :startDate AND :endDate " +
           "GROUP BY EXTRACT(YEAR FROM i.paidAt), EXTRACT(MONTH FROM i.paidAt), i.currency " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyRevenueReport(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get platform fee revenue.
     */
    @Query("SELECT i.currency, SUM(i.platformFeeMinor) FROM Invoice i " +
           "WHERE i.status = 'PAID' AND i.paidAt BETWEEN :startDate AND :endDate " +
           "GROUP BY i.currency")
    List<Object[]> getPlatformFeeRevenue(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get tax revenue by country.
     */
    @Query("SELECT i.taxCountry, i.currency, SUM(i.taxMinor) FROM Invoice i " +
           "WHERE i.status = 'PAID' AND i.paidAt BETWEEN :startDate AND :endDate " +
           "AND i.taxCountry IS NOT NULL " +
           "GROUP BY i.taxCountry, i.currency")
    List<Object[]> getTaxRevenueByCountry(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count invoices by status for a user.
     */
    @Query("SELECT i.status, COUNT(i) FROM Invoice i WHERE i.userId = :userId GROUP BY i.status")
    List<Object[]> countInvoicesByStatusForUser(@Param("userId") UUID userId);
    
    /**
     * Find invoices that are due soon (within specified days).
     */
    @Query("SELECT i FROM Invoice i WHERE i.status = 'ISSUED' " +
           "AND i.dueAt BETWEEN :currentDate AND :futureDate " +
           "ORDER BY i.dueAt ASC")
    List<Invoice> findInvoicesDueSoon(@Param("currentDate") LocalDateTime currentDate, 
                                     @Param("futureDate") LocalDateTime futureDate);
    
    /**
     * Check if an invoice number already exists.
     */
    boolean existsByInvoiceNumber(String invoiceNumber);
    
    /**
     * Check if an order already has an invoice.
     */
    boolean existsByOrderId(UUID orderId);
    
    /**
     * Check if a booking already has an invoice.
     */
    boolean existsByBookingId(UUID bookingId);
}
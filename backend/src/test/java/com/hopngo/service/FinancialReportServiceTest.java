package com.hopngo.service;

import com.hopngo.entity.FinancialReport;
import com.hopngo.entity.Transaction;
import com.hopngo.entity.LedgerEntry;
import com.hopngo.repository.FinancialReportRepository;
import com.hopngo.repository.TransactionRepository;
import com.hopngo.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialReportServiceTest {

    @Mock
    private FinancialReportRepository financialReportRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SupportTicketService supportTicketService;

    @InjectMocks
    private FinancialReportService financialReportService;

    private FinancialReport monthlyReport;
    private FinancialReport yearlyReport;
    private Transaction transaction;
    private LedgerEntry ledgerEntry;

    @BeforeEach
    void setUp() {
        // Create monthly report
        monthlyReport = new FinancialReport();
        ReflectionTestUtils.setField(monthlyReport, "id", 1L);
        ReflectionTestUtils.setField(monthlyReport, "reportId", "monthly_2024_01");
        ReflectionTestUtils.setField(monthlyReport, "type", "MONTHLY");
        ReflectionTestUtils.setField(monthlyReport, "status", "COMPLETED");
        ReflectionTestUtils.setField(monthlyReport, "periodStart", LocalDate.of(2024, 1, 1));
        ReflectionTestUtils.setField(monthlyReport, "periodEnd", LocalDate.of(2024, 1, 31));
        ReflectionTestUtils.setField(monthlyReport, "totalRevenue", new BigDecimal("10000.00"));
        ReflectionTestUtils.setField(monthlyReport, "totalExpenses", new BigDecimal("2000.00"));
        ReflectionTestUtils.setField(monthlyReport, "netIncome", new BigDecimal("8000.00"));
        ReflectionTestUtils.setField(monthlyReport, "createdAt", LocalDateTime.now());

        // Create yearly report
        yearlyReport = new FinancialReport();
        ReflectionTestUtils.setField(yearlyReport, "id", 2L);
        ReflectionTestUtils.setField(yearlyReport, "reportId", "yearly_2024");
        ReflectionTestUtils.setField(yearlyReport, "type", "YEARLY");
        ReflectionTestUtils.setField(yearlyReport, "status", "COMPLETED");
        ReflectionTestUtils.setField(yearlyReport, "periodStart", LocalDate.of(2024, 1, 1));
        ReflectionTestUtils.setField(yearlyReport, "periodEnd", LocalDate.of(2024, 12, 31));
        ReflectionTestUtils.setField(yearlyReport, "totalRevenue", new BigDecimal("120000.00"));
        ReflectionTestUtils.setField(yearlyReport, "totalExpenses", new BigDecimal("24000.00"));
        ReflectionTestUtils.setField(yearlyReport, "netIncome", new BigDecimal("96000.00"));
        ReflectionTestUtils.setField(yearlyReport, "createdAt", LocalDateTime.now());

        // Create transaction
        transaction = new Transaction();
        ReflectionTestUtils.setField(transaction, "id", 1L);
        ReflectionTestUtils.setField(transaction, "transactionId", "txn_123");
        ReflectionTestUtils.setField(transaction, "amount", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(transaction, "status", "COMPLETED");
        ReflectionTestUtils.setField(transaction, "paymentProvider", "STRIPE");
        ReflectionTestUtils.setField(transaction, "createdAt", LocalDateTime.now());

        // Create ledger entry
        ledgerEntry = new LedgerEntry();
        ReflectionTestUtils.setField(ledgerEntry, "id", 1L);
        ReflectionTestUtils.setField(ledgerEntry, "transactionId", "txn_123");
        ReflectionTestUtils.setField(ledgerEntry, "accountType", "REVENUE");
        ReflectionTestUtils.setField(ledgerEntry, "entryType", "CREDIT");
        ReflectionTestUtils.setField(ledgerEntry, "amount", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(ledgerEntry, "createdAt", LocalDateTime.now());
    }

    @Test
    void testGenerateMonthlyReport_Success() {
        // Arrange
        LocalDate reportDate = LocalDate.of(2024, 1, 15);
        when(transactionRepository.findCompletedTransactionsByDateRange(any(), any()))
            .thenReturn(Arrays.asList(transaction));
        when(ledgerEntryRepository.calculateTotalByAccountTypeAndDateRange(eq("REVENUE"), any(), any()))
            .thenReturn(new BigDecimal("10000.00"));
        when(ledgerEntryRepository.calculateTotalByAccountTypeAndDateRange(eq("EXPENSES"), any(), any()))
            .thenReturn(new BigDecimal("2000.00"));
        when(financialReportRepository.save(any(FinancialReport.class))).thenReturn(monthlyReport);

        // Act
        financialReportService.generateMonthlyReport(reportDate);

        // Assert
        verify(financialReportRepository).save(argThat(report -> 
            "MONTHLY".equals(ReflectionTestUtils.getField(report, "type")) &&
            "COMPLETED".equals(ReflectionTestUtils.getField(report, "status")) &&
            new BigDecimal("10000.00").equals(ReflectionTestUtils.getField(report, "totalRevenue")) &&
            new BigDecimal("2000.00").equals(ReflectionTestUtils.getField(report, "totalExpenses"))
        ));
        verify(notificationService).sendFinancialReportNotification(any(), eq("COMPLETED"));
    }

    @Test
    void testGenerateMonthlyReport_Failure() {
        // Arrange
        LocalDate reportDate = LocalDate.of(2024, 1, 15);
        when(transactionRepository.findCompletedTransactionsByDateRange(any(), any()))
            .thenThrow(new RuntimeException("Database error"));
        when(financialReportRepository.save(any(FinancialReport.class))).thenReturn(monthlyReport);

        // Act
        financialReportService.generateMonthlyReport(reportDate);

        // Assert
        verify(financialReportRepository).save(argThat(report -> 
            "FAILED".equals(ReflectionTestUtils.getField(report, "status"))
        ));
        verify(supportTicketService).createFinancialReportFailureTicket(any(), contains("Database error"));
    }

    @Test
    void testGenerateYearlyReport_Success() {
        // Arrange
        int year = 2024;
        when(transactionRepository.findCompletedTransactionsByDateRange(any(), any()))
            .thenReturn(Arrays.asList(transaction));
        when(ledgerEntryRepository.calculateTotalByAccountTypeAndDateRange(eq("REVENUE"), any(), any()))
            .thenReturn(new BigDecimal("120000.00"));
        when(ledgerEntryRepository.calculateTotalByAccountTypeAndDateRange(eq("EXPENSES"), any(), any()))
            .thenReturn(new BigDecimal("24000.00"));
        when(financialReportRepository.save(any(FinancialReport.class))).thenReturn(yearlyReport);

        // Act
        financialReportService.generateYearlyReport(year);

        // Assert
        verify(financialReportRepository).save(argThat(report -> 
            "YEARLY".equals(ReflectionTestUtils.getField(report, "type")) &&
            "COMPLETED".equals(ReflectionTestUtils.getField(report, "status")) &&
            new BigDecimal("120000.00").equals(ReflectionTestUtils.getField(report, "totalRevenue")) &&
            new BigDecimal("24000.00").equals(ReflectionTestUtils.getField(report, "totalExpenses"))
        ));
        verify(notificationService).sendFinancialReportNotification(any(), eq("COMPLETED"));
    }

    @Test
    void testCalculateFinancialMetrics() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        when(transactionRepository.findCompletedTransactionsByDateRange(startDate.atStartOfDay(), endDate.atTime(23, 59, 59)))
            .thenReturn(Arrays.asList(transaction));
        when(ledgerEntryRepository.calculateTotalByAccountTypeAndDateRange("REVENUE", startDate.atStartOfDay(), endDate.atTime(23, 59, 59)))
            .thenReturn(new BigDecimal("10000.00"));
        when(ledgerEntryRepository.calculateTotalByAccountTypeAndDateRange("EXPENSES", startDate.atStartOfDay(), endDate.atTime(23, 59, 59)))
            .thenReturn(new BigDecimal("2000.00"));
        when(transactionRepository.countCompletedTransactionsByDateRange(startDate.atStartOfDay(), endDate.atTime(23, 59, 59)))
            .thenReturn(100L);
        when(transactionRepository.calculateAverageTransactionAmount(startDate.atStartOfDay(), endDate.atTime(23, 59, 59)))
            .thenReturn(new BigDecimal("100.00"));

        // Act
        FinancialReportService.FinancialMetrics metrics = 
            financialReportService.calculateFinancialMetrics(startDate, endDate);

        // Assert
        assertNotNull(metrics);
        assertEquals(new BigDecimal("10000.00"), metrics.getTotalRevenue());
        assertEquals(new BigDecimal("2000.00"), metrics.getTotalExpenses());
        assertEquals(new BigDecimal("8000.00"), metrics.getNetIncome());
        assertEquals(100L, metrics.getTransactionCount());
        assertEquals(new BigDecimal("100.00"), metrics.getAverageTransactionAmount());
    }

    @Test
    void testExportToCsv() {
        // Arrange
        when(financialReportRepository.findByReportId("monthly_2024_01"))
            .thenReturn(Optional.of(monthlyReport));

        // Act
        byte[] csvData = financialReportService.exportToCsv("monthly_2024_01");

        // Assert
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
        
        String csvContent = new String(csvData);
        assertTrue(csvContent.contains("Report ID,Type,Period Start,Period End"));
        assertTrue(csvContent.contains("monthly_2024_01,MONTHLY,2024-01-01,2024-01-31"));
        assertTrue(csvContent.contains("10000.00,2000.00,8000.00"));
    }

    @Test
    void testExportToPdf() {
        // Arrange
        when(financialReportRepository.findByReportId("monthly_2024_01"))
            .thenReturn(Optional.of(monthlyReport));

        // Act
        byte[] pdfData = financialReportService.exportToPdf("monthly_2024_01");

        // Assert
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        // PDF files start with %PDF
        String pdfHeader = new String(pdfData, 0, Math.min(4, pdfData.length));
        assertEquals("%PDF", pdfHeader);
    }

    @Test
    void testExportToCsv_ReportNotFound() {
        // Arrange
        when(financialReportRepository.findByReportId("nonexistent"))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            financialReportService.exportToCsv("nonexistent");
        });
    }

    @Test
    void testGetProviderSummary() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        when(transactionRepository.calculateTotalAmountByProviderAndDateRange("STRIPE", startDate.atStartOfDay(), endDate.atTime(23, 59, 59)))
            .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.countTransactionsByProviderAndDateRange("STRIPE", startDate.atStartOfDay(), endDate.atTime(23, 59, 59)))
            .thenReturn(50L);
        when(transactionRepository.calculateAverageAmountByProviderAndDateRange("STRIPE", startDate.atStartOfDay(), endDate.atTime(23, 59, 59)))
            .thenReturn(new BigDecimal("100.00"));

        // Act
        FinancialReportService.ProviderSummary summary = 
            financialReportService.getProviderSummary("STRIPE", startDate, endDate);

        // Assert
        assertNotNull(summary);
        assertEquals("STRIPE", summary.getProvider());
        assertEquals(new BigDecimal("5000.00"), summary.getTotalAmount());
        assertEquals(50L, summary.getTransactionCount());
        assertEquals(new BigDecimal("100.00"), summary.getAverageAmount());
    }

    @Test
    void testGetAllProviderSummaries() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // Mock data for each provider
        when(transactionRepository.calculateTotalAmountByProviderAndDateRange(eq("STRIPE"), any(), any()))
            .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.countTransactionsByProviderAndDateRange(eq("STRIPE"), any(), any()))
            .thenReturn(50L);
        when(transactionRepository.calculateAverageAmountByProviderAndDateRange(eq("STRIPE"), any(), any()))
            .thenReturn(new BigDecimal("100.00"));
        
        when(transactionRepository.calculateTotalAmountByProviderAndDateRange(eq("BKASH"), any(), any()))
            .thenReturn(new BigDecimal("3000.00"));
        when(transactionRepository.countTransactionsByProviderAndDateRange(eq("BKASH"), any(), any()))
            .thenReturn(30L);
        when(transactionRepository.calculateAverageAmountByProviderAndDateRange(eq("BKASH"), any(), any()))
            .thenReturn(new BigDecimal("100.00"));
        
        when(transactionRepository.calculateTotalAmountByProviderAndDateRange(eq("NAGAD"), any(), any()))
            .thenReturn(new BigDecimal("2000.00"));
        when(transactionRepository.countTransactionsByProviderAndDateRange(eq("NAGAD"), any(), any()))
            .thenReturn(20L);
        when(transactionRepository.calculateAverageAmountByProviderAndDateRange(eq("NAGAD"), any(), any()))
            .thenReturn(new BigDecimal("100.00"));

        // Act
        List<FinancialReportService.ProviderSummary> summaries = 
            financialReportService.getAllProviderSummaries(startDate, endDate);

        // Assert
        assertNotNull(summaries);
        assertEquals(3, summaries.size());
        
        FinancialReportService.ProviderSummary stripeSummary = summaries.stream()
            .filter(s -> "STRIPE".equals(s.getProvider()))
            .findFirst().orElse(null);
        assertNotNull(stripeSummary);
        assertEquals(new BigDecimal("5000.00"), stripeSummary.getTotalAmount());
    }

    @Test
    void testScheduledMonthlyReportGeneration() {
        // This test verifies that the scheduled method exists and can be called
        // The actual scheduling is tested through integration tests
        assertDoesNotThrow(() -> {
            financialReportService.generateMonthlyReport();
        });
    }

    @Test
    void testScheduledYearlyReportGeneration() {
        // This test verifies that the scheduled method exists and can be called
        // The actual scheduling is tested through integration tests
        assertDoesNotThrow(() -> {
            financialReportService.generateYearlyReport();
        });
    }

    @Test
    void testGenerateReportId() {
        // Test monthly report ID generation
        String monthlyId = financialReportService.generateReportId("MONTHLY", LocalDate.of(2024, 1, 15));
        assertEquals("monthly_2024_01", monthlyId);

        // Test yearly report ID generation
        String yearlyId = financialReportService.generateReportId("YEARLY", LocalDate.of(2024, 6, 15));
        assertEquals("yearly_2024", yearlyId);
    }

    @Test
    void testFinancialMetricsConstructor() {
        // Test FinancialMetrics data class
        FinancialReportService.FinancialMetrics metrics = 
            new FinancialReportService.FinancialMetrics(
                new BigDecimal("1000.00"), new BigDecimal("200.00"), 
                new BigDecimal("800.00"), 10L, new BigDecimal("100.00")
            );
        
        assertEquals(new BigDecimal("1000.00"), metrics.getTotalRevenue());
        assertEquals(new BigDecimal("200.00"), metrics.getTotalExpenses());
        assertEquals(new BigDecimal("800.00"), metrics.getNetIncome());
        assertEquals(10L, metrics.getTransactionCount());
        assertEquals(new BigDecimal("100.00"), metrics.getAverageTransactionAmount());
    }

    @Test
    void testProviderSummaryConstructor() {
        // Test ProviderSummary data class
        FinancialReportService.ProviderSummary summary = 
            new FinancialReportService.ProviderSummary(
                "TEST_PROVIDER", new BigDecimal("500.00"), 5L, new BigDecimal("100.00")
            );
        
        assertEquals("TEST_PROVIDER", summary.getProvider());
        assertEquals(new BigDecimal("500.00"), summary.getTotalAmount());
        assertEquals(5L, summary.getTransactionCount());
        assertEquals(new BigDecimal("100.00"), summary.getAverageAmount());
    }
}
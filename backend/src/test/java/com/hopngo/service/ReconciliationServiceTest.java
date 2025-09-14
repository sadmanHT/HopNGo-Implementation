package com.hopngo.service;

import com.hopngo.entity.ReconciliationJob;
import com.hopngo.entity.ReconciliationDiscrepancy;
import com.hopngo.entity.Transaction;
import com.hopngo.repository.ReconciliationJobRepository;
import com.hopngo.repository.ReconciliationDiscrepancyRepository;
import com.hopngo.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private ReconciliationJobRepository reconciliationJobRepository;

    @Mock
    private ReconciliationDiscrepancyRepository discrepancyRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentProviderService paymentProviderService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SupportTicketService supportTicketService;

    @InjectMocks
    private ReconciliationService reconciliationService;

    private Transaction internalTransaction;
    private PaymentProviderService.ProviderTransaction providerTransaction;
    private ReconciliationJob reconciliationJob;

    @BeforeEach
    void setUp() {
        // Create internal transaction
        internalTransaction = new Transaction();
        ReflectionTestUtils.setField(internalTransaction, "id", 1L);
        ReflectionTestUtils.setField(internalTransaction, "transactionId", "txn_123");
        ReflectionTestUtils.setField(internalTransaction, "amount", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(internalTransaction, "status", "COMPLETED");
        ReflectionTestUtils.setField(internalTransaction, "paymentProvider", "STRIPE");
        ReflectionTestUtils.setField(internalTransaction, "createdAt", LocalDateTime.now());

        // Create provider transaction
        providerTransaction = new PaymentProviderService.ProviderTransaction(
            "txn_123", new BigDecimal("100.00"), "succeeded", LocalDateTime.now()
        );

        // Create reconciliation job
        reconciliationJob = new ReconciliationJob();
        ReflectionTestUtils.setField(reconciliationJob, "id", 1L);
        ReflectionTestUtils.setField(reconciliationJob, "jobId", "job_123");
        ReflectionTestUtils.setField(reconciliationJob, "provider", "STRIPE");
        ReflectionTestUtils.setField(reconciliationJob, "status", "PENDING");
        ReflectionTestUtils.setField(reconciliationJob, "reconciliationDate", LocalDate.now());
    }

    @Test
    void testPerformDailyReconciliation_Success() {
        // Arrange
        when(reconciliationJobRepository.save(any(ReconciliationJob.class))).thenReturn(reconciliationJob);
        when(paymentProviderService.fetchTransactionsByDate(eq("STRIPE"), any(LocalDate.class)))
            .thenReturn(Arrays.asList(providerTransaction));
        when(transactionRepository.findByPaymentProviderAndCreatedAtBetween(eq("STRIPE"), any(), any()))
            .thenReturn(Arrays.asList(internalTransaction));
        when(discrepancyRepository.save(any(ReconciliationDiscrepancy.class))).thenReturn(new ReconciliationDiscrepancy());

        // Act
        reconciliationService.performDailyReconciliation();

        // Assert
        verify(reconciliationJobRepository, times(3)).save(any(ReconciliationJob.class)); // 3 providers
        verify(paymentProviderService).fetchTransactionsByDate(eq("STRIPE"), any());
        verify(paymentProviderService).fetchTransactionsByDate(eq("BKASH"), any());
        verify(paymentProviderService).fetchTransactionsByDate(eq("NAGAD"), any());
        verify(notificationService, times(3)).sendReconciliationSummary(any(), anyInt(), anyInt());
    }

    @Test
    void testReconcileProvider_NoDiscrepancies() {
        // Arrange
        when(paymentProviderService.fetchTransactionsByDate(eq("STRIPE"), any(LocalDate.class)))
            .thenReturn(Arrays.asList(providerTransaction));
        when(transactionRepository.findByPaymentProviderAndCreatedAtBetween(eq("STRIPE"), any(), any()))
            .thenReturn(Arrays.asList(internalTransaction));

        // Act
        int discrepancies = reconciliationService.reconcileProvider("STRIPE", LocalDate.now(), "job_123");

        // Assert
        assertEquals(0, discrepancies);
        verify(discrepancyRepository, never()).save(any());
    }

    @Test
    void testReconcileProvider_MissingInternalTransaction() {
        // Arrange
        when(paymentProviderService.fetchTransactionsByDate(eq("STRIPE"), any(LocalDate.class)))
            .thenReturn(Arrays.asList(providerTransaction));
        when(transactionRepository.findByPaymentProviderAndCreatedAtBetween(eq("STRIPE"), any(), any()))
            .thenReturn(Arrays.asList()); // No internal transactions
        when(discrepancyRepository.save(any(ReconciliationDiscrepancy.class))).thenReturn(new ReconciliationDiscrepancy());

        // Act
        int discrepancies = reconciliationService.reconcileProvider("STRIPE", LocalDate.now(), "job_123");

        // Assert
        assertEquals(1, discrepancies);
        verify(discrepancyRepository).save(argThat(discrepancy -> 
            "MISSING_INTERNAL".equals(ReflectionTestUtils.getField(discrepancy, "discrepancyType"))
        ));
    }

    @Test
    void testReconcileProvider_MissingProviderTransaction() {
        // Arrange
        when(paymentProviderService.fetchTransactionsByDate(eq("STRIPE"), any(LocalDate.class)))
            .thenReturn(Arrays.asList()); // No provider transactions
        when(transactionRepository.findByPaymentProviderAndCreatedAtBetween(eq("STRIPE"), any(), any()))
            .thenReturn(Arrays.asList(internalTransaction));
        when(discrepancyRepository.save(any(ReconciliationDiscrepancy.class))).thenReturn(new ReconciliationDiscrepancy());

        // Act
        int discrepancies = reconciliationService.reconcileProvider("STRIPE", LocalDate.now(), "job_123");

        // Assert
        assertEquals(1, discrepancies);
        verify(discrepancyRepository).save(argThat(discrepancy -> 
            "MISSING_PROVIDER".equals(ReflectionTestUtils.getField(discrepancy, "discrepancyType"))
        ));
    }

    @Test
    void testReconcileProvider_AmountMismatch() {
        // Arrange
        PaymentProviderService.ProviderTransaction mismatchTransaction = 
            new PaymentProviderService.ProviderTransaction(
                "txn_123", new BigDecimal("150.00"), "succeeded", LocalDateTime.now()
            );
        
        when(paymentProviderService.fetchTransactionsByDate(eq("STRIPE"), any(LocalDate.class)))
            .thenReturn(Arrays.asList(mismatchTransaction));
        when(transactionRepository.findByPaymentProviderAndCreatedAtBetween(eq("STRIPE"), any(), any()))
            .thenReturn(Arrays.asList(internalTransaction));
        when(discrepancyRepository.save(any(ReconciliationDiscrepancy.class))).thenReturn(new ReconciliationDiscrepancy());

        // Act
        int discrepancies = reconciliationService.reconcileProvider("STRIPE", LocalDate.now(), "job_123");

        // Assert
        assertEquals(1, discrepancies);
        verify(discrepancyRepository).save(argThat(discrepancy -> 
            "AMOUNT_MISMATCH".equals(ReflectionTestUtils.getField(discrepancy, "discrepancyType"))
        ));
    }

    @Test
    void testReconcileProvider_StatusMismatch() {
        // Arrange
        PaymentProviderService.ProviderTransaction statusMismatchTransaction = 
            new PaymentProviderService.ProviderTransaction(
                "txn_123", new BigDecimal("100.00"), "failed", LocalDateTime.now()
            );
        
        when(paymentProviderService.fetchTransactionsByDate(eq("STRIPE"), any(LocalDate.class)))
            .thenReturn(Arrays.asList(statusMismatchTransaction));
        when(transactionRepository.findByPaymentProviderAndCreatedAtBetween(eq("STRIPE"), any(), any()))
            .thenReturn(Arrays.asList(internalTransaction));
        when(discrepancyRepository.save(any(ReconciliationDiscrepancy.class))).thenReturn(new ReconciliationDiscrepancy());

        // Act
        int discrepancies = reconciliationService.reconcileProvider("STRIPE", LocalDate.now(), "job_123");

        // Assert
        assertEquals(1, discrepancies);
        verify(discrepancyRepository).save(argThat(discrepancy -> 
            "STATUS_MISMATCH".equals(ReflectionTestUtils.getField(discrepancy, "discrepancyType"))
        ));
    }

    @Test
    void testDetermineSeverity_High() {
        // Test high severity for large amount mismatch
        String severity = reconciliationService.determineSeverity(
            "AMOUNT_MISMATCH", new BigDecimal("1500.00"), new BigDecimal("100.00")
        );
        assertEquals("HIGH", severity);

        // Test high severity for missing high-value transaction
        severity = reconciliationService.determineSeverity(
            "MISSING_INTERNAL", new BigDecimal("2000.00"), null
        );
        assertEquals("HIGH", severity);
    }

    @Test
    void testDetermineSeverity_Medium() {
        // Test medium severity for moderate amount mismatch
        String severity = reconciliationService.determineSeverity(
            "AMOUNT_MISMATCH", new BigDecimal("600.00"), new BigDecimal("100.00")
        );
        assertEquals("MEDIUM", severity);

        // Test medium severity for status mismatch
        severity = reconciliationService.determineSeverity(
            "STATUS_MISMATCH", new BigDecimal("100.00"), new BigDecimal("100.00")
        );
        assertEquals("MEDIUM", severity);
    }

    @Test
    void testDetermineSeverity_Low() {
        // Test low severity for small amount mismatch
        String severity = reconciliationService.determineSeverity(
            "AMOUNT_MISMATCH", new BigDecimal("105.00"), new BigDecimal("100.00")
        );
        assertEquals("LOW", severity);

        // Test low severity for missing low-value transaction
        severity = reconciliationService.determineSeverity(
            "MISSING_PROVIDER", new BigDecimal("50.00"), null
        );
        assertEquals("LOW", severity);
    }

    @Test
    void testCreateDiscrepancy() {
        // Arrange
        ReconciliationDiscrepancy mockDiscrepancy = new ReconciliationDiscrepancy();
        when(discrepancyRepository.save(any(ReconciliationDiscrepancy.class))).thenReturn(mockDiscrepancy);

        // Act
        reconciliationService.createDiscrepancy(
            "job_123", "txn_123", "AMOUNT_MISMATCH", "HIGH", 
            "Amount mismatch: expected 100.00, found 150.00"
        );

        // Assert
        verify(discrepancyRepository).save(argThat(discrepancy -> {
            return "job_123".equals(ReflectionTestUtils.getField(discrepancy, "jobId")) &&
                   "txn_123".equals(ReflectionTestUtils.getField(discrepancy, "transactionId")) &&
                   "AMOUNT_MISMATCH".equals(ReflectionTestUtils.getField(discrepancy, "discrepancyType")) &&
                   "HIGH".equals(ReflectionTestUtils.getField(discrepancy, "severity"));
        }));
        verify(supportTicketService).createReconciliationDiscrepancyTicket(any(), eq("HIGH"));
    }

    @Test
    void testManualReconciliation() {
        // Arrange
        when(reconciliationJobRepository.save(any(ReconciliationJob.class))).thenReturn(reconciliationJob);
        when(paymentProviderService.fetchTransactionsByDateRange(eq("STRIPE"), any(), any()))
            .thenReturn(Arrays.asList(providerTransaction));
        when(transactionRepository.findByPaymentProviderAndCreatedAtBetween(eq("STRIPE"), any(), any()))
            .thenReturn(Arrays.asList(internalTransaction));

        // Act
        String jobId = reconciliationService.performManualReconciliation(
            "STRIPE", LocalDate.now().minusDays(1), LocalDate.now()
        );

        // Assert
        assertNotNull(jobId);
        assertTrue(jobId.startsWith("manual_"));
        verify(reconciliationJobRepository).save(any(ReconciliationJob.class));
        verify(paymentProviderService).fetchTransactionsByDateRange(eq("STRIPE"), any(), any());
    }

    @Test
    void testGetReconciliationSummary() {
        // Arrange
        when(reconciliationJobRepository.findByJobId("job_123")).thenReturn(Optional.of(reconciliationJob));
        when(discrepancyRepository.countByJobId("job_123")).thenReturn(5L);
        when(discrepancyRepository.countByJobIdAndSeverity("job_123", "HIGH")).thenReturn(2L);
        when(discrepancyRepository.countByJobIdAndResolved("job_123", false)).thenReturn(3L);

        // Act
        ReconciliationService.ReconciliationSummary summary = 
            reconciliationService.getReconciliationSummary("job_123");

        // Assert
        assertNotNull(summary);
        assertEquals("job_123", summary.getJobId());
        assertEquals(5, summary.getTotalDiscrepancies());
        assertEquals(2, summary.getHighSeverityCount());
        assertEquals(3, summary.getUnresolvedCount());
    }

    @Test
    void testScheduledReconciliation() {
        // This test verifies that the scheduled method exists and can be called
        // The actual scheduling is tested through integration tests
        assertDoesNotThrow(() -> {
            reconciliationService.performDailyReconciliation();
        });
    }

    @Test
    void testProviderTransactionConstructor() {
        // Test ProviderTransaction data class
        PaymentProviderService.ProviderTransaction transaction = 
            new PaymentProviderService.ProviderTransaction(
                "test_id", new BigDecimal("99.99"), "completed", LocalDateTime.now()
            );
        
        assertEquals("test_id", transaction.getId());
        assertEquals(new BigDecimal("99.99"), transaction.getAmount());
        assertEquals("completed", transaction.getStatus());
        assertNotNull(transaction.getTimestamp());
    }
}
package com.hopngo.service;

import com.hopngo.entity.LedgerEntry;
import com.hopngo.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerVerificationServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LedgerVerificationService ledgerVerificationService;

    private LedgerEntry debitEntry;
    private LedgerEntry creditEntry;
    private LedgerEntry orphanedEntry;

    @BeforeEach
    void setUp() {
        // Create balanced debit/credit entries
        debitEntry = new LedgerEntry();
        ReflectionTestUtils.setField(debitEntry, "id", 1L);
        ReflectionTestUtils.setField(debitEntry, "transactionId", "txn_123");
        ReflectionTestUtils.setField(debitEntry, "accountType", "CASH");
        ReflectionTestUtils.setField(debitEntry, "entryType", "DEBIT");
        ReflectionTestUtils.setField(debitEntry, "amount", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(debitEntry, "createdAt", LocalDateTime.now());

        creditEntry = new LedgerEntry();
        ReflectionTestUtils.setField(creditEntry, "id", 2L);
        ReflectionTestUtils.setField(creditEntry, "transactionId", "txn_123");
        ReflectionTestUtils.setField(creditEntry, "accountType", "REVENUE");
        ReflectionTestUtils.setField(creditEntry, "entryType", "CREDIT");
        ReflectionTestUtils.setField(creditEntry, "amount", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(creditEntry, "createdAt", LocalDateTime.now());

        // Create orphaned entry (no matching debit/credit)
        orphanedEntry = new LedgerEntry();
        ReflectionTestUtils.setField(orphanedEntry, "id", 3L);
        ReflectionTestUtils.setField(orphanedEntry, "transactionId", "txn_orphan");
        ReflectionTestUtils.setField(orphanedEntry, "accountType", "CASH");
        ReflectionTestUtils.setField(orphanedEntry, "entryType", "DEBIT");
        ReflectionTestUtils.setField(orphanedEntry, "amount", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(orphanedEntry, "createdAt", LocalDateTime.now());
    }

    @Test
    void testVerifyAccountBalances_Success() {
        // Arrange
        when(ledgerEntryRepository.findAll()).thenReturn(Arrays.asList(debitEntry, creditEntry));
        when(ledgerEntryRepository.calculateAccountBalance("CASH")).thenReturn(new BigDecimal("100.00"));
        when(ledgerEntryRepository.calculateAccountBalance("REVENUE")).thenReturn(new BigDecimal("-100.00"));

        // Act
        LedgerVerificationService.VerificationResult result = ledgerVerificationService.verifyAccountBalances();

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("Account balances verified successfully", result.getMessage());
        assertNotNull(result.getDetails());
        verify(notificationService, never()).sendLedgerVerificationAlert(any(), any());
    }

    @Test
    void testVerifyAccountBalances_Failure() {
        // Arrange
        when(ledgerEntryRepository.findAll()).thenReturn(Arrays.asList(debitEntry, creditEntry, orphanedEntry));
        when(ledgerEntryRepository.calculateAccountBalance("CASH")).thenReturn(new BigDecimal("150.00"));
        when(ledgerEntryRepository.calculateAccountBalance("REVENUE")).thenReturn(new BigDecimal("-100.00"));

        // Act
        LedgerVerificationService.VerificationResult result = ledgerVerificationService.verifyAccountBalances();

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Account balance verification failed"));
        verify(notificationService).sendLedgerVerificationAlert(eq("Balance Verification Failed"), any());
    }

    @Test
    void testCheckOrphanedEntries_Found() {
        // Arrange
        when(ledgerEntryRepository.findOrphanedEntries()).thenReturn(Arrays.asList(orphanedEntry));

        // Act
        LedgerVerificationService.VerificationResult result = ledgerVerificationService.checkOrphanedEntries();

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Found 1 orphaned entries"));
        verify(notificationService).sendLedgerVerificationAlert(eq("Orphaned Entries Found"), any());
    }

    @Test
    void testCheckOrphanedEntries_None() {
        // Arrange
        when(ledgerEntryRepository.findOrphanedEntries()).thenReturn(Arrays.asList());

        // Act
        LedgerVerificationService.VerificationResult result = ledgerVerificationService.checkOrphanedEntries();

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("No orphaned entries found", result.getMessage());
        verify(notificationService, never()).sendLedgerVerificationAlert(any(), any());
    }

    @Test
    void testVerifyTransactionIntegrity_Success() {
        // Arrange
        when(ledgerEntryRepository.findUnbalancedTransactions()).thenReturn(Arrays.asList());

        // Act
        LedgerVerificationService.VerificationResult result = ledgerVerificationService.verifyTransactionIntegrity();

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("All transactions are properly balanced", result.getMessage());
        verify(notificationService, never()).sendLedgerVerificationAlert(any(), any());
    }

    @Test
    void testVerifyTransactionIntegrity_Failure() {
        // Arrange
        when(ledgerEntryRepository.findUnbalancedTransactions()).thenReturn(Arrays.asList("txn_unbalanced"));

        // Act
        LedgerVerificationService.VerificationResult result = ledgerVerificationService.verifyTransactionIntegrity();

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Found 1 unbalanced transactions"));
        verify(notificationService).sendLedgerVerificationAlert(eq("Transaction Integrity Issues"), any());
    }

    @Test
    void testGenerateBalanceSummary() {
        // Arrange
        when(ledgerEntryRepository.getAccountBalanceSummary()).thenReturn(
            Map.of(
                "CASH", new BigDecimal("1000.00"),
                "REVENUE", new BigDecimal("-800.00"),
                "EXPENSES", new BigDecimal("200.00")
            )
        );

        // Act
        Map<String, BigDecimal> summary = ledgerVerificationService.generateBalanceSummary();

        // Assert
        assertNotNull(summary);
        assertEquals(3, summary.size());
        assertEquals(new BigDecimal("1000.00"), summary.get("CASH"));
        assertEquals(new BigDecimal("-800.00"), summary.get("REVENUE"));
        assertEquals(new BigDecimal("200.00"), summary.get("EXPENSES"));
    }

    @Test
    void testPerformNightlyVerification_AllSuccess() {
        // Arrange
        when(ledgerEntryRepository.findAll()).thenReturn(Arrays.asList(debitEntry, creditEntry));
        when(ledgerEntryRepository.calculateAccountBalance(any())).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.findOrphanedEntries()).thenReturn(Arrays.asList());
        when(ledgerEntryRepository.findUnbalancedTransactions()).thenReturn(Arrays.asList());
        when(ledgerEntryRepository.getAccountBalanceSummary()).thenReturn(Map.of());

        // Act
        ledgerVerificationService.performNightlyVerification();

        // Assert
        verify(notificationService).sendLedgerVerificationAlert(eq("Nightly Verification Completed"), any());
    }

    @Test
    void testPerformNightlyVerification_WithFailures() {
        // Arrange
        when(ledgerEntryRepository.findAll()).thenReturn(Arrays.asList(debitEntry, creditEntry, orphanedEntry));
        when(ledgerEntryRepository.calculateAccountBalance(any())).thenReturn(new BigDecimal("50.00"));
        when(ledgerEntryRepository.findOrphanedEntries()).thenReturn(Arrays.asList(orphanedEntry));
        when(ledgerEntryRepository.findUnbalancedTransactions()).thenReturn(Arrays.asList("txn_unbalanced"));
        when(ledgerEntryRepository.getAccountBalanceSummary()).thenReturn(Map.of());

        // Act
        ledgerVerificationService.performNightlyVerification();

        // Assert
        verify(notificationService, atLeast(3)).sendLedgerVerificationAlert(any(), any());
    }

    @Test
    void testVerificationResult_Constructor() {
        // Test success result
        LedgerVerificationService.VerificationResult successResult = 
            new LedgerVerificationService.VerificationResult(true, "Success", Map.of("key", "value"));
        
        assertTrue(successResult.isSuccess());
        assertEquals("Success", successResult.getMessage());
        assertEquals(Map.of("key", "value"), successResult.getDetails());

        // Test failure result
        LedgerVerificationService.VerificationResult failureResult = 
            new LedgerVerificationService.VerificationResult(false, "Failed", null);
        
        assertFalse(failureResult.isSuccess());
        assertEquals("Failed", failureResult.getMessage());
        assertNull(failureResult.getDetails());
    }

    @Test
    void testScheduledVerification() {
        // This test verifies that the scheduled method exists and can be called
        // The actual scheduling is tested through integration tests
        assertDoesNotThrow(() -> {
            ledgerVerificationService.performNightlyVerification();
        });
    }
}
package com.hopngo.service;

import com.hopngo.entity.Dispute;
import com.hopngo.entity.LedgerEntry;
import com.hopngo.entity.Transaction;
import com.hopngo.repository.DisputeRepository;
import com.hopngo.repository.LedgerEntryRepository;
import com.hopngo.repository.TransactionRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SupportTicketService supportTicketService;

    @InjectMocks
    private DisputeService disputeService;

    private Transaction transaction;
    private Dispute dispute;
    private LedgerEntry ledgerEntry;

    @BeforeEach
    void setUp() {
        // Create transaction
        transaction = new Transaction();
        ReflectionTestUtils.setField(transaction, "id", 1L);
        ReflectionTestUtils.setField(transaction, "transactionId", "txn_123");
        ReflectionTestUtils.setField(transaction, "amount", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(transaction, "status", "COMPLETED");
        ReflectionTestUtils.setField(transaction, "paymentProvider", "STRIPE");
        ReflectionTestUtils.setField(transaction, "createdAt", LocalDateTime.now());

        // Create dispute
        dispute = new Dispute();
        ReflectionTestUtils.setField(dispute, "id", 1L);
        ReflectionTestUtils.setField(dispute, "disputeId", "dispute_123");
        ReflectionTestUtils.setField(dispute, "providerDisputeId", "dp_stripe_123");
        ReflectionTestUtils.setField(dispute, "transactionId", "txn_123");
        ReflectionTestUtils.setField(dispute, "amount", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(dispute, "status", "OPEN");
        ReflectionTestUtils.setField(dispute, "type", "CHARGEBACK");
        ReflectionTestUtils.setField(dispute, "reason", "FRAUDULENT");
        ReflectionTestUtils.setField(dispute, "paymentProvider", "STRIPE");
        ReflectionTestUtils.setField(dispute, "createdAt", LocalDateTime.now());

        // Create ledger entry
        ledgerEntry = new LedgerEntry();
        ReflectionTestUtils.setField(ledgerEntry, "id", 1L);
        ReflectionTestUtils.setField(ledgerEntry, "transactionId", "txn_123");
        ReflectionTestUtils.setField(ledgerEntry, "accountType", "FROZEN_FUNDS");
        ReflectionTestUtils.setField(ledgerEntry, "entryType", "DEBIT");
        ReflectionTestUtils.setField(ledgerEntry, "amount", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(ledgerEntry, "createdAt", LocalDateTime.now());
    }

    @Test
    void testHandleStripeDisputeCreated() {
        // Arrange
        when(transactionRepository.findByTransactionId("txn_123")).thenReturn(Optional.of(transaction));
        when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(ledgerEntry);

        // Act
        disputeService.handleStripeDisputeCreated(
            "dp_stripe_123", "txn_123", new BigDecimal("100.00"), 
            "chargeback", "fraudulent", LocalDateTime.now()
        );

        // Assert
        verify(disputeRepository).save(argThat(d -> 
            "STRIPE".equals(ReflectionTestUtils.getField(d, "paymentProvider")) &&
            "OPEN".equals(ReflectionTestUtils.getField(d, "status")) &&
            "CHARGEBACK".equals(ReflectionTestUtils.getField(d, "type"))
        ));
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class)); // Freeze funds (debit/credit)
        verify(notificationService).sendDisputeAlert(any(), eq("CREATED"));
    }

    @Test
    void testHandleStripeDisputeCreated_TransactionNotFound() {
        // Arrange
        when(transactionRepository.findByTransactionId("txn_123")).thenReturn(Optional.empty());
        when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);

        // Act
        disputeService.handleStripeDisputeCreated(
            "dp_stripe_123", "txn_123", new BigDecimal("100.00"), 
            "chargeback", "fraudulent", LocalDateTime.now()
        );

        // Assert
        verify(disputeRepository).save(any(Dispute.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class)); // No funds to freeze
        verify(supportTicketService).createDisputeTicket(any(), eq("Missing transaction for dispute"));
    }

    @Test
    void testHandleStripeDisputeCreated_HighValueDispute() {
        // Arrange
        BigDecimal highAmount = new BigDecimal("1500.00");
        when(transactionRepository.findByTransactionId("txn_123")).thenReturn(Optional.of(transaction));
        when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(ledgerEntry);

        // Act
        disputeService.handleStripeDisputeCreated(
            "dp_stripe_123", "txn_123", highAmount, 
            "chargeback", "fraudulent", LocalDateTime.now()
        );

        // Assert
        verify(supportTicketService).createDisputeTicket(any(), contains("High-value dispute"));
        verify(notificationService).sendDisputeAlert(any(), eq("CREATED"));
    }

    @Test
    void testHandleStripeDisputeUpdated() {
        // Arrange
        when(disputeRepository.findByProviderDisputeId("dp_stripe_123")).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);

        // Act
        disputeService.handleStripeDisputeUpdated(
            "dp_stripe_123", "under_review", "Additional evidence required", LocalDateTime.now()
        );

        // Assert
        verify(disputeRepository).save(argThat(d -> 
            "UNDER_REVIEW".equals(ReflectionTestUtils.getField(d, "status"))
        ));
        verify(notificationService).sendDisputeAlert(any(), eq("UPDATED"));
    }

    @Test
    void testHandleStripeDisputeClosed_Won() {
        // Arrange
        when(disputeRepository.findByProviderDisputeId("dp_stripe_123")).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(ledgerEntry);

        // Act
        disputeService.handleStripeDisputeClosed(
            "dp_stripe_123", "won", "Dispute resolved in merchant's favor", LocalDateTime.now()
        );

        // Assert
        verify(disputeRepository).save(argThat(d -> 
            "WON".equals(ReflectionTestUtils.getField(d, "status"))
        ));
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class)); // Unfreeze funds
        verify(notificationService).sendDisputeAlert(any(), eq("CLOSED"));
    }

    @Test
    void testHandleStripeDisputeClosed_Lost() {
        // Arrange
        when(disputeRepository.findByProviderDisputeId("dp_stripe_123")).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(ledgerEntry);

        // Act
        disputeService.handleStripeDisputeClosed(
            "dp_stripe_123", "lost", "Dispute resolved in customer's favor", LocalDateTime.now()
        );

        // Assert
        verify(disputeRepository).save(argThat(d -> 
            "LOST".equals(ReflectionTestUtils.getField(d, "status"))
        ));
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class)); // Process chargeback
        verify(notificationService).sendDisputeAlert(any(), eq("CLOSED"));
    }

    @Test
    void testHandleBkashDispute() {
        // Arrange
        when(transactionRepository.findByTransactionId("txn_123")).thenReturn(Optional.of(transaction));
        when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(ledgerEntry);

        // Act
        disputeService.handleBkashDispute(
            "dp_bkash_123", "txn_123", new BigDecimal("100.00"), 
            "complaint", "service_issue", "open", LocalDateTime.now()
        );

        // Assert
        verify(disputeRepository).save(argThat(d -> 
            "BKASH".equals(ReflectionTestUtils.getField(d, "paymentProvider")) &&
            "COMPLAINT".equals(ReflectionTestUtils.getField(d, "type"))
        ));
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
        verify(notificationService).sendDisputeAlert(any(), eq("CREATED"));
    }

    @Test
    void testHandleNagadDispute() {
        // Arrange
        when(transactionRepository.findByTransactionId("txn_123")).thenReturn(Optional.of(transaction));
        when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(ledgerEntry);

        // Act
        disputeService.handleNagadDispute(
            "dp_nagad_123", "txn_123", new BigDecimal("100.00"), 
            "refund_request", "product_not_received", "pending", LocalDateTime.now()
        );

        // Assert
        verify(disputeRepository).save(argThat(d -> 
            "NAGAD".equals(ReflectionTestUtils.getField(d, "paymentProvider")) &&
            "REFUND_REQUEST".equals(ReflectionTestUtils.getField(d, "type"))
        ));
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
        verify(notificationService).sendDisputeAlert(any(), eq("CREATED"));
    }

    @Test
    void testFreezeFunds() {
        // Arrange
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(ledgerEntry);

        // Act
        disputeService.freezeFunds("txn_123", new BigDecimal("100.00"), "Dispute freeze");

        // Assert
        verify(ledgerEntryRepository, times(2)).save(argThat(entry -> {
            String accountType = (String) ReflectionTestUtils.getField(entry, "accountType");
            String entryType = (String) ReflectionTestUtils.getField(entry, "entryType");
            return ("CASH".equals(accountType) && "CREDIT".equals(entryType)) ||
                   ("FROZEN_FUNDS".equals(accountType) && "DEBIT".equals(entryType));
        }));
    }

    @Test
    void testUnfreezeFunds() {
        // Arrange
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(ledgerEntry);

        // Act
        disputeService.unfreezeFunds("txn_123", new BigDecimal("100.00"), "Dispute won - unfreeze");

        // Assert
        verify(ledgerEntryRepository, times(2)).save(argThat(entry -> {
            String accountType = (String) ReflectionTestUtils.getField(entry, "accountType");
            String entryType = (String) ReflectionTestUtils.getField(entry, "entryType");
            return ("FROZEN_FUNDS".equals(accountType) && "CREDIT".equals(entryType)) ||
                   ("CASH".equals(accountType) && "DEBIT".equals(entryType));
        }));
    }

    @Test
    void testProcessChargeback() {
        // Arrange
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(ledgerEntry);

        // Act
        disputeService.processChargeback("txn_123", new BigDecimal("100.00"), "Chargeback processed");

        // Assert
        verify(ledgerEntryRepository, times(2)).save(argThat(entry -> {
            String accountType = (String) ReflectionTestUtils.getField(entry, "accountType");
            String entryType = (String) ReflectionTestUtils.getField(entry, "entryType");
            return ("FROZEN_FUNDS".equals(accountType) && "CREDIT".equals(entryType)) ||
                   ("CHARGEBACK_LOSS".equals(accountType) && "DEBIT".equals(entryType));
        }));
    }

    @Test
    void testMapStripeStatus() {
        assertEquals("OPEN", disputeService.mapStripeStatus("warning_needs_response"));
        assertEquals("UNDER_REVIEW", disputeService.mapStripeStatus("under_review"));
        assertEquals("WON", disputeService.mapStripeStatus("won"));
        assertEquals("LOST", disputeService.mapStripeStatus("lost"));
        assertEquals("OPEN", disputeService.mapStripeStatus("unknown_status"));
    }

    @Test
    void testMapStripeReason() {
        assertEquals("FRAUDULENT", disputeService.mapStripeReason("fraudulent"));
        assertEquals("SUBSCRIPTION_CANCELED", disputeService.mapStripeReason("subscription_canceled"));
        assertEquals("PRODUCT_UNACCEPTABLE", disputeService.mapStripeReason("product_unacceptable"));
        assertEquals("PRODUCT_NOT_RECEIVED", disputeService.mapStripeReason("product_not_received"));
        assertEquals("UNRECOGNIZED", disputeService.mapStripeReason("unrecognized"));
        assertEquals("DUPLICATE", disputeService.mapStripeReason("duplicate"));
        assertEquals("CREDIT_NOT_PROCESSED", disputeService.mapStripeReason("credit_not_processed"));
        assertEquals("GENERAL", disputeService.mapStripeReason("general"));
        assertEquals("OTHER", disputeService.mapStripeReason("unknown_reason"));
    }

    @Test
    void testGetActiveDisputes() {
        // Arrange
        when(disputeRepository.findByStatusIn(Arrays.asList("OPEN", "UNDER_REVIEW")))
            .thenReturn(Arrays.asList(dispute));

        // Act
        List<Dispute> activeDisputes = disputeService.getActiveDisputes();

        // Assert
        assertEquals(1, activeDisputes.size());
        assertEquals(dispute, activeDisputes.get(0));
    }

    @Test
    void testGetDisputesByProvider() {
        // Arrange
        when(disputeRepository.findByPaymentProvider("STRIPE"))
            .thenReturn(Arrays.asList(dispute));

        // Act
        List<Dispute> disputes = disputeService.getDisputesByProvider("STRIPE");

        // Assert
        assertEquals(1, disputes.size());
        assertEquals(dispute, disputes.get(0));
    }

    @Test
    void testGetDisputeById() {
        // Arrange
        when(disputeRepository.findByDisputeId("dispute_123"))
            .thenReturn(Optional.of(dispute));

        // Act
        Optional<Dispute> result = disputeService.getDisputeById("dispute_123");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(dispute, result.get());
    }

    @Test
    void testGenerateDisputeId() {
        // Act
        String disputeId1 = disputeService.generateDisputeId();
        String disputeId2 = disputeService.generateDisputeId();

        // Assert
        assertNotNull(disputeId1);
        assertNotNull(disputeId2);
        assertTrue(disputeId1.startsWith("dispute_"));
        assertTrue(disputeId2.startsWith("dispute_"));
        assertNotEquals(disputeId1, disputeId2); // Should be unique
    }
}
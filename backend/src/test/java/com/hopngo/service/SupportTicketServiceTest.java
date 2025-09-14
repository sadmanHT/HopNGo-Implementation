package com.hopngo.service;

import com.hopngo.entity.SupportTicket;
import com.hopngo.repository.SupportTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SupportTicketService supportTicketService;

    private SupportTicket sampleTicket;

    @BeforeEach
    void setUp() {
        sampleTicket = new SupportTicket(
            "TKT-001",
            "Test Issue",
            "This is a test issue description",
            "HIGH",
            "system",
            "OPEN",
            "user123",
            "admin456",
            LocalDateTime.now(),
            LocalDateTime.now(),
            List.of("Initial comment")
        );
    }

    @Test
    void testCreateTicket_Success() {
        // Arrange
        String subject = "Payment Processing Issue";
        String description = "Users unable to process payments";
        String priority = "HIGH";
        String category = "payment";
        String createdBy = "user123";

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.createTicket(subject, description, priority, category, createdBy);

        // Assert
        assertNotNull(result);
        assertEquals("TKT-001", result.getTicketId());
        verify(supportTicketRepository).save(any(SupportTicket.class));
        verify(notificationService).sendSupportTicketAlert(
            anyString(), eq(subject), eq(description), eq(priority), eq(category), any(LocalDateTime.class)
        );
    }

    @Test
    void testCreateTicket_AutomaticTicket() {
        // Arrange
        String subject = "Automated System Alert";
        String description = "System detected an anomaly";
        String priority = "MEDIUM";
        String category = "system";

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.createAutomaticTicket(subject, description, priority, category);

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(argThat(ticket -> 
            "SYSTEM".equals(ticket.getCreatedBy()) &&
            "OPEN".equals(ticket.getStatus())
        ));
        verify(notificationService).sendSupportTicketAlert(
            anyString(), eq(subject), eq(description), eq(priority), eq(category), any(LocalDateTime.class)
        );
    }

    @Test
    void testCreateReconciliationTicket_HighSeverity() {
        // Arrange
        String reconciliationId = "REC-001";
        String provider = "stripe";
        List<String> discrepancies = List.of(
            "Missing transaction: TXN-123 ($500.00)",
            "Amount mismatch: TXN-456 (Expected: $100.00, Found: $95.00)"
        );
        String severity = "HIGH";

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.createReconciliationTicket(
            reconciliationId, provider, discrepancies, severity
        );

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(argThat(ticket -> 
            ticket.getSubject().contains("Reconciliation Discrepancy") &&
            ticket.getSubject().contains(provider.toUpperCase()) &&
            ticket.getDescription().contains(reconciliationId) &&
            ticket.getDescription().contains("Missing transaction: TXN-123") &&
            "HIGH".equals(ticket.getPriority()) &&
            "reconciliation".equals(ticket.getCategory())
        ));
    }

    @Test
    void testCreateReconciliationTicket_LowSeverity() {
        // Arrange
        String reconciliationId = "REC-002";
        String provider = "bkash";
        List<String> discrepancies = List.of("Status mismatch: TXN-789");
        String severity = "LOW";

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.createReconciliationTicket(
            reconciliationId, provider, discrepancies, severity
        );

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(argThat(ticket -> 
            "LOW".equals(ticket.getPriority())
        ));
    }

    @Test
    void testCreateDisputeTicket_HighValueDispute() {
        // Arrange
        String disputeId = "DP-001";
        String provider = "stripe";
        String transactionId = "TXN-001";
        BigDecimal amount = new BigDecimal("1500.00");
        String disputeType = "chargeback";
        String reason = "fraudulent";

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.createDisputeTicket(
            disputeId, provider, transactionId, amount, disputeType, reason
        );

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(argThat(ticket -> 
            ticket.getSubject().contains("Dispute Alert") &&
            ticket.getSubject().contains(provider.toUpperCase()) &&
            ticket.getDescription().contains(disputeId) &&
            ticket.getDescription().contains(transactionId) &&
            ticket.getDescription().contains("$1,500.00") &&
            "HIGH".equals(ticket.getPriority()) &&
            "dispute".equals(ticket.getCategory())
        ));
    }

    @Test
    void testCreateDisputeTicket_LowValueDispute() {
        // Arrange
        String disputeId = "DP-002";
        String provider = "nagad";
        String transactionId = "TXN-002";
        BigDecimal amount = new BigDecimal("50.00");
        String disputeType = "complaint";
        String reason = "service_issue";

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.createDisputeTicket(
            disputeId, provider, transactionId, amount, disputeType, reason
        );

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(argThat(ticket -> 
            "MEDIUM".equals(ticket.getPriority())
        ));
    }

    @Test
    void testCreateLedgerVerificationTicket() {
        // Arrange
        String verificationId = "VER-001";
        List<String> issues = List.of(
            "Account balance mismatch: Expected 1000.00, Found 950.00",
            "Orphaned entry found: TXN-123"
        );

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.createLedgerVerificationTicket(verificationId, issues);

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(argThat(ticket -> 
            ticket.getSubject().contains("Ledger Verification Failure") &&
            ticket.getDescription().contains(verificationId) &&
            ticket.getDescription().contains("Account balance mismatch") &&
            ticket.getDescription().contains("Orphaned entry found") &&
            "HIGH".equals(ticket.getPriority()) &&
            "ledger".equals(ticket.getCategory())
        ));
    }

    @Test
    void testCreateFinancialReportTicket() {
        // Arrange
        String reportId = "RPT-001";
        String reportType = "monthly";
        String period = "2024-01";
        String errorMessage = "Database connection timeout";

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.createFinancialReportTicket(
            reportId, reportType, period, errorMessage
        );

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(argThat(ticket -> 
            ticket.getSubject().contains("Financial Report Generation Failed") &&
            ticket.getSubject().contains(reportType.toUpperCase()) &&
            ticket.getDescription().contains(reportId) &&
            ticket.getDescription().contains(period) &&
            ticket.getDescription().contains(errorMessage) &&
            "HIGH".equals(ticket.getPriority()) &&
            "financial_report".equals(ticket.getCategory())
        ));
    }

    @Test
    void testUpdateTicketStatus_Success() {
        // Arrange
        String ticketId = "TKT-001";
        String newStatus = "IN_PROGRESS";
        String updatedBy = "admin123";

        when(supportTicketRepository.findByTicketId(ticketId)).thenReturn(Optional.of(sampleTicket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.updateTicketStatus(ticketId, newStatus, updatedBy);

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(argThat(ticket -> 
            newStatus.equals(ticket.getStatus()) &&
            updatedBy.equals(ticket.getAssignedTo())
        ));
    }

    @Test
    void testUpdateTicketStatus_TicketNotFound() {
        // Arrange
        String ticketId = "TKT-999";
        String newStatus = "IN_PROGRESS";
        String updatedBy = "admin123";

        when(supportTicketRepository.findByTicketId(ticketId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            supportTicketService.updateTicketStatus(ticketId, newStatus, updatedBy)
        );
        verify(supportTicketRepository, never()).save(any(SupportTicket.class));
    }

    @Test
    void testAddComment_Success() {
        // Arrange
        String ticketId = "TKT-001";
        String comment = "This is a new comment";
        String addedBy = "admin123";

        when(supportTicketRepository.findByTicketId(ticketId)).thenReturn(Optional.of(sampleTicket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.addComment(ticketId, comment, addedBy);

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(argThat(ticket -> {
            List<String> comments = ticket.getComments();
            return comments.size() > 1 && 
                   comments.get(comments.size() - 1).contains(comment) &&
                   comments.get(comments.size() - 1).contains(addedBy);
        }));
    }

    @Test
    void testAddComment_TicketNotFound() {
        // Arrange
        String ticketId = "TKT-999";
        String comment = "This is a new comment";
        String addedBy = "admin123";

        when(supportTicketRepository.findByTicketId(ticketId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            supportTicketService.addComment(ticketId, comment, addedBy)
        );
        verify(supportTicketRepository, never()).save(any(SupportTicket.class));
    }

    @Test
    void testAssignTicket_Success() {
        // Arrange
        String ticketId = "TKT-001";
        String assignedTo = "admin456";
        String assignedBy = "manager789";

        when(supportTicketRepository.findByTicketId(ticketId)).thenReturn(Optional.of(sampleTicket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.assignTicket(ticketId, assignedTo, assignedBy);

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(argThat(ticket -> 
            assignedTo.equals(ticket.getAssignedTo()) &&
            "IN_PROGRESS".equals(ticket.getStatus())
        ));
    }

    @Test
    void testAssignTicket_TicketNotFound() {
        // Arrange
        String ticketId = "TKT-999";
        String assignedTo = "admin456";
        String assignedBy = "manager789";

        when(supportTicketRepository.findByTicketId(ticketId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            supportTicketService.assignTicket(ticketId, assignedTo, assignedBy)
        );
        verify(supportTicketRepository, never()).save(any(SupportTicket.class));
    }

    @Test
    void testGetTicketById_Success() {
        // Arrange
        String ticketId = "TKT-001";
        when(supportTicketRepository.findByTicketId(ticketId)).thenReturn(Optional.of(sampleTicket));

        // Act
        Optional<SupportTicket> result = supportTicketService.getTicketById(ticketId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(ticketId, result.get().getTicketId());
        verify(supportTicketRepository).findByTicketId(ticketId);
    }

    @Test
    void testGetTicketById_NotFound() {
        // Arrange
        String ticketId = "TKT-999";
        when(supportTicketRepository.findByTicketId(ticketId)).thenReturn(Optional.empty());

        // Act
        Optional<SupportTicket> result = supportTicketService.getTicketById(ticketId);

        // Assert
        assertFalse(result.isPresent());
        verify(supportTicketRepository).findByTicketId(ticketId);
    }

    @Test
    void testGetTicketsByStatus() {
        // Arrange
        String status = "OPEN";
        List<SupportTicket> tickets = List.of(sampleTicket);
        when(supportTicketRepository.findByStatus(status)).thenReturn(tickets);

        // Act
        List<SupportTicket> result = supportTicketService.getTicketsByStatus(status);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleTicket.getTicketId(), result.get(0).getTicketId());
        verify(supportTicketRepository).findByStatus(status);
    }

    @Test
    void testGetTicketsByPriority() {
        // Arrange
        String priority = "HIGH";
        List<SupportTicket> tickets = List.of(sampleTicket);
        when(supportTicketRepository.findByPriority(priority)).thenReturn(tickets);

        // Act
        List<SupportTicket> result = supportTicketService.getTicketsByPriority(priority);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleTicket.getTicketId(), result.get(0).getTicketId());
        verify(supportTicketRepository).findByPriority(priority);
    }

    @Test
    void testGetTicketsByCategory() {
        // Arrange
        String category = "system";
        List<SupportTicket> tickets = List.of(sampleTicket);
        when(supportTicketRepository.findByCategory(category)).thenReturn(tickets);

        // Act
        List<SupportTicket> result = supportTicketService.getTicketsByCategory(category);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleTicket.getTicketId(), result.get(0).getTicketId());
        verify(supportTicketRepository).findByCategory(category);
    }

    @Test
    void testGetTicketsByAssignee() {
        // Arrange
        String assignedTo = "admin456";
        List<SupportTicket> tickets = List.of(sampleTicket);
        when(supportTicketRepository.findByAssignedTo(assignedTo)).thenReturn(tickets);

        // Act
        List<SupportTicket> result = supportTicketService.getTicketsByAssignee(assignedTo);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleTicket.getTicketId(), result.get(0).getTicketId());
        verify(supportTicketRepository).findByAssignedTo(assignedTo);
    }

    @Test
    void testGetAllTickets() {
        // Arrange
        List<SupportTicket> tickets = List.of(sampleTicket);
        when(supportTicketRepository.findAll()).thenReturn(tickets);

        // Act
        List<SupportTicket> result = supportTicketService.getAllTickets();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleTicket.getTicketId(), result.get(0).getTicketId());
        verify(supportTicketRepository).findAll();
    }

    @Test
    void testGenerateTicketId() {
        // Act
        String ticketId1 = supportTicketService.generateTicketId();
        String ticketId2 = supportTicketService.generateTicketId();

        // Assert
        assertNotNull(ticketId1);
        assertNotNull(ticketId2);
        assertTrue(ticketId1.startsWith("TKT-"));
        assertTrue(ticketId2.startsWith("TKT-"));
        assertNotEquals(ticketId1, ticketId2); // Should be unique
    }

    @Test
    void testFormatCurrency() {
        // Test indirectly through dispute ticket creation
        BigDecimal amount = new BigDecimal("1234.56");
        
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        supportTicketService.createDisputeTicket(
            "DP-001", "stripe", "TXN-001", amount, "chargeback", "fraudulent"
        );

        verify(supportTicketRepository).save(argThat(ticket -> 
            ticket.getDescription().contains("$1,234.56")
        ));
    }

    @Test
    void testDeterminePriorityFromSeverity_High() {
        // Test indirectly through reconciliation ticket creation
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        supportTicketService.createReconciliationTicket(
            "REC-001", "stripe", List.of("Test discrepancy"), "HIGH"
        );

        verify(supportTicketRepository).save(argThat(ticket -> 
            "HIGH".equals(ticket.getPriority())
        ));
    }

    @Test
    void testDeterminePriorityFromSeverity_Medium() {
        // Test indirectly through reconciliation ticket creation
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        supportTicketService.createReconciliationTicket(
            "REC-001", "stripe", List.of("Test discrepancy"), "MEDIUM"
        );

        verify(supportTicketRepository).save(argThat(ticket -> 
            "MEDIUM".equals(ticket.getPriority())
        ));
    }

    @Test
    void testDeterminePriorityFromSeverity_Low() {
        // Test indirectly through reconciliation ticket creation
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        supportTicketService.createReconciliationTicket(
            "REC-001", "stripe", List.of("Test discrepancy"), "LOW"
        );

        verify(supportTicketRepository).save(argThat(ticket -> 
            "LOW".equals(ticket.getPriority())
        ));
    }

    @Test
    void testDeterminePriorityFromAmount_HighValue() {
        // Test indirectly through dispute ticket creation
        BigDecimal highAmount = new BigDecimal("2000.00");
        
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        supportTicketService.createDisputeTicket(
            "DP-001", "stripe", "TXN-001", highAmount, "chargeback", "fraudulent"
        );

        verify(supportTicketRepository).save(argThat(ticket -> 
            "HIGH".equals(ticket.getPriority())
        ));
    }

    @Test
    void testDeterminePriorityFromAmount_MediumValue() {
        // Test indirectly through dispute ticket creation
        BigDecimal mediumAmount = new BigDecimal("500.00");
        
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        supportTicketService.createDisputeTicket(
            "DP-001", "stripe", "TXN-001", mediumAmount, "chargeback", "fraudulent"
        );

        verify(supportTicketRepository).save(argThat(ticket -> 
            "MEDIUM".equals(ticket.getPriority())
        ));
    }

    @Test
    void testSupportTicketDataClass() {
        // Test the SupportTicket data class
        String ticketId = "TKT-TEST";
        String subject = "Test Subject";
        String description = "Test Description";
        String priority = "HIGH";
        String category = "test";
        String status = "OPEN";
        String createdBy = "user123";
        String assignedTo = "admin456";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        List<String> comments = List.of("Test comment");

        SupportTicket ticket = new SupportTicket(
            ticketId, subject, description, priority, category, status,
            createdBy, assignedTo, createdAt, updatedAt, comments
        );

        // Assert all fields
        assertEquals(ticketId, ticket.getTicketId());
        assertEquals(subject, ticket.getSubject());
        assertEquals(description, ticket.getDescription());
        assertEquals(priority, ticket.getPriority());
        assertEquals(category, ticket.getCategory());
        assertEquals(status, ticket.getStatus());
        assertEquals(createdBy, ticket.getCreatedBy());
        assertEquals(assignedTo, ticket.getAssignedTo());
        assertEquals(createdAt, ticket.getCreatedAt());
        assertEquals(updatedAt, ticket.getUpdatedAt());
        assertEquals(comments, ticket.getComments());
    }

    @Test
    void testCreateTicket_WithNullValues() {
        // Arrange
        String subject = "Test Subject";
        String description = "Test Description";
        String priority = "MEDIUM";
        String category = "test";
        String createdBy = null; // Null value

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.createTicket(subject, description, priority, category, createdBy);

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(any(SupportTicket.class));
    }

    @Test
    void testCreateTicket_WithEmptyValues() {
        // Arrange
        String subject = "";
        String description = "";
        String priority = "LOW";
        String category = "test";
        String createdBy = "user123";

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(sampleTicket);

        // Act
        SupportTicket result = supportTicketService.createTicket(subject, description, priority, category, createdBy);

        // Assert
        assertNotNull(result);
        verify(supportTicketRepository).save(any(SupportTicket.class));
    }
}
package com.hopngo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Set up configuration properties
        ReflectionTestUtils.setField(notificationService, "fromEmail", "noreply@hopngo.com");
        ReflectionTestUtils.setField(notificationService, "adminEmails", List.of("admin@hopngo.com", "finance@hopngo.com"));
        ReflectionTestUtils.setField(notificationService, "supportEmail", "support@hopngo.com");
        ReflectionTestUtils.setField(notificationService, "financeEmail", "finance@hopngo.com");
    }

    @Test
    void testSendLedgerVerificationAlert_Success() {
        // Arrange
        String verificationId = "VER-001";
        List<String> issues = List.of(
            "Account balance mismatch: Expected 1000.00, Found 950.00",
            "Orphaned entry found: TXN-123"
        );
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendLedgerVerificationAlert(verificationId, issues, timestamp);

        // Assert
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendReconciliationAlert_HighSeverity() {
        // Arrange
        String reconciliationId = "REC-001";
        String provider = "stripe";
        List<String> discrepancies = List.of(
            "Missing transaction: TXN-456 (Amount: $500.00)",
            "Amount mismatch: TXN-789 (Expected: $100.00, Found: $95.00)"
        );
        String severity = "HIGH";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendReconciliationAlert(reconciliationId, provider, discrepancies, severity, timestamp);

        // Assert
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendReconciliationAlert_LowSeverity() {
        // Arrange
        String reconciliationId = "REC-002";
        String provider = "bkash";
        List<String> discrepancies = List.of(
            "Status mismatch: TXN-101 (Expected: completed, Found: pending)"
        );
        String severity = "LOW";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendReconciliationAlert(reconciliationId, provider, discrepancies, severity, timestamp);

        // Assert
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class)); // Only to finance team for low severity
    }

    @Test
    void testSendDisputeAlert_HighValueDispute() {
        // Arrange
        String disputeId = "DP-001";
        String provider = "stripe";
        String transactionId = "TXN-001";
        BigDecimal amount = new BigDecimal("1500.00");
        String disputeType = "chargeback";
        String reason = "fraudulent";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendDisputeAlert(disputeId, provider, transactionId, amount, disputeType, reason, timestamp);

        // Assert
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class)); // To both admin and finance
    }

    @Test
    void testSendDisputeAlert_LowValueDispute() {
        // Arrange
        String disputeId = "DP-002";
        String provider = "nagad";
        String transactionId = "TXN-002";
        BigDecimal amount = new BigDecimal("50.00");
        String disputeType = "complaint";
        String reason = "service_issue";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendDisputeAlert(disputeId, provider, transactionId, amount, disputeType, reason, timestamp);

        // Assert
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class)); // Only to finance for low value
    }

    @Test
    void testSendSupportTicketAlert_HighPriority() {
        // Arrange
        String ticketId = "TKT-001";
        String subject = "Critical System Issue";
        String description = "Payment processing system is down";
        String priority = "HIGH";
        String category = "system";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendSupportTicketAlert(ticketId, subject, description, priority, category, timestamp);

        // Assert
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class)); // To both admin and support
    }

    @Test
    void testSendSupportTicketAlert_LowPriority() {
        // Arrange
        String ticketId = "TKT-002";
        String subject = "Minor UI Issue";
        String description = "Button alignment issue on mobile";
        String priority = "LOW";
        String category = "ui";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendSupportTicketAlert(ticketId, subject, description, priority, category, timestamp);

        // Assert
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class)); // Only to support for low priority
    }

    @Test
    void testSendFinancialReportAlert_Success() {
        // Arrange
        String reportId = "RPT-001";
        String reportType = "monthly";
        String period = "2024-01";
        String status = "completed";
        String downloadUrl = "https://hopngo.com/reports/RPT-001";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendFinancialReportAlert(reportId, reportType, period, status, downloadUrl, timestamp);

        // Assert
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendFinancialReportAlert_Failed() {
        // Arrange
        String reportId = "RPT-002";
        String reportType = "yearly";
        String period = "2023";
        String status = "failed";
        String downloadUrl = null;
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendFinancialReportAlert(reportId, reportType, period, status, downloadUrl, timestamp);

        // Assert
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendEmail_Success() {
        // Arrange
        String to = "test@example.com";
        String subject = "Test Subject";
        String body = "Test email body";

        // Act
        notificationService.sendEmail(to, subject, body);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendEmail_Exception() {
        // Arrange
        String to = "test@example.com";
        String subject = "Test Subject";
        String body = "Test email body";
        
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        assertDoesNotThrow(() -> notificationService.sendEmail(to, subject, body));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendEmailToMultipleRecipients_Success() {
        // Arrange
        List<String> recipients = List.of("admin@hopngo.com", "finance@hopngo.com");
        String subject = "Test Subject";
        String body = "Test email body";

        // Act
        notificationService.sendEmailToMultipleRecipients(recipients, subject, body);

        // Assert
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendEmailToMultipleRecipients_EmptyList() {
        // Arrange
        List<String> recipients = List.of();
        String subject = "Test Subject";
        String body = "Test email body";

        // Act
        notificationService.sendEmailToMultipleRecipients(recipients, subject, body);

        // Assert
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendEmailToMultipleRecipients_NullList() {
        // Arrange
        List<String> recipients = null;
        String subject = "Test Subject";
        String body = "Test email body";

        // Act
        notificationService.sendEmailToMultipleRecipients(recipients, subject, body);

        // Assert
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testFormatCurrency() {
        // Test the private method through reflection or by testing methods that use it
        BigDecimal amount1 = new BigDecimal("1000.00");
        BigDecimal amount2 = new BigDecimal("1000.5");
        BigDecimal amount3 = new BigDecimal("1000");

        // Since formatCurrency is private, we test it indirectly through dispute alerts
        notificationService.sendDisputeAlert(
            "DP-001", "stripe", "TXN-001", amount1, "chargeback", "fraudulent", LocalDateTime.now()
        );

        verify(mailSender, atLeastOnce()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testGetSeverityBasedRecipients_HighSeverity() {
        // Test indirectly through reconciliation alert
        notificationService.sendReconciliationAlert(
            "REC-001", "stripe", List.of("Test discrepancy"), "HIGH", LocalDateTime.now()
        );

        // Should send to both admin and finance (2 emails)
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testGetSeverityBasedRecipients_MediumSeverity() {
        // Test indirectly through reconciliation alert
        notificationService.sendReconciliationAlert(
            "REC-001", "stripe", List.of("Test discrepancy"), "MEDIUM", LocalDateTime.now()
        );

        // Should send to both admin and finance (2 emails)
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testGetSeverityBasedRecipients_LowSeverity() {
        // Test indirectly through reconciliation alert
        notificationService.sendReconciliationAlert(
            "REC-001", "stripe", List.of("Test discrepancy"), "LOW", LocalDateTime.now()
        );

        // Should send only to finance (1 email)
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testGetPriorityBasedRecipients_HighPriority() {
        // Test indirectly through support ticket alert
        notificationService.sendSupportTicketAlert(
            "TKT-001", "Test Subject", "Test Description", "HIGH", "system", LocalDateTime.now()
        );

        // Should send to both admin and support (2 emails)
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testGetPriorityBasedRecipients_MediumPriority() {
        // Test indirectly through support ticket alert
        notificationService.sendSupportTicketAlert(
            "TKT-001", "Test Subject", "Test Description", "MEDIUM", "system", LocalDateTime.now()
        );

        // Should send to both admin and support (2 emails)
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testGetPriorityBasedRecipients_LowPriority() {
        // Test indirectly through support ticket alert
        notificationService.sendSupportTicketAlert(
            "TKT-001", "Test Subject", "Test Description", "LOW", "system", LocalDateTime.now()
        );

        // Should send only to support (1 email)
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testEmailContentFormatting_LedgerVerification() {
        // Arrange
        String verificationId = "VER-001";
        List<String> issues = List.of(
            "Account balance mismatch",
            "Orphaned entry found"
        );
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendLedgerVerificationAlert(verificationId, issues, timestamp);

        // Assert - Verify that email was sent with proper formatting
        verify(mailSender, times(2)).send(argThat(message -> {
            SimpleMailMessage mail = (SimpleMailMessage) message;
            return mail.getSubject().contains("Ledger Verification Alert") &&
                   mail.getText().contains(verificationId) &&
                   mail.getText().contains("Account balance mismatch") &&
                   mail.getText().contains("Orphaned entry found");
        }));
    }

    @Test
    void testEmailContentFormatting_Reconciliation() {
        // Arrange
        String reconciliationId = "REC-001";
        String provider = "stripe";
        List<String> discrepancies = List.of("Missing transaction: TXN-123");
        String severity = "HIGH";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendReconciliationAlert(reconciliationId, provider, discrepancies, severity, timestamp);

        // Assert - Verify email content formatting
        verify(mailSender, times(2)).send(argThat(message -> {
            SimpleMailMessage mail = (SimpleMailMessage) message;
            return mail.getSubject().contains("Reconciliation Alert") &&
                   mail.getText().contains(reconciliationId) &&
                   mail.getText().contains(provider.toUpperCase()) &&
                   mail.getText().contains("Missing transaction: TXN-123") &&
                   mail.getText().contains(severity);
        }));
    }

    @Test
    void testEmailContentFormatting_Dispute() {
        // Arrange
        String disputeId = "DP-001";
        String provider = "stripe";
        String transactionId = "TXN-001";
        BigDecimal amount = new BigDecimal("1000.00");
        String disputeType = "chargeback";
        String reason = "fraudulent";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendDisputeAlert(disputeId, provider, transactionId, amount, disputeType, reason, timestamp);

        // Assert - Verify email content formatting
        verify(mailSender, times(2)).send(argThat(message -> {
            SimpleMailMessage mail = (SimpleMailMessage) message;
            return mail.getSubject().contains("Dispute Alert") &&
                   mail.getText().contains(disputeId) &&
                   mail.getText().contains(provider.toUpperCase()) &&
                   mail.getText().contains(transactionId) &&
                   mail.getText().contains("$1,000.00") &&
                   mail.getText().contains(disputeType) &&
                   mail.getText().contains(reason);
        }));
    }

    @Test
    void testEmailContentFormatting_SupportTicket() {
        // Arrange
        String ticketId = "TKT-001";
        String subject = "Test Issue";
        String description = "Detailed description of the issue";
        String priority = "HIGH";
        String category = "system";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendSupportTicketAlert(ticketId, subject, description, priority, category, timestamp);

        // Assert - Verify email content formatting
        verify(mailSender, times(2)).send(argThat(message -> {
            SimpleMailMessage mail = (SimpleMailMessage) message;
            return mail.getSubject().contains("Support Ticket Alert") &&
                   mail.getText().contains(ticketId) &&
                   mail.getText().contains(subject) &&
                   mail.getText().contains(description) &&
                   mail.getText().contains(priority) &&
                   mail.getText().contains(category);
        }));
    }

    @Test
    void testEmailContentFormatting_FinancialReport() {
        // Arrange
        String reportId = "RPT-001";
        String reportType = "monthly";
        String period = "2024-01";
        String status = "completed";
        String downloadUrl = "https://hopngo.com/reports/RPT-001";
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        notificationService.sendFinancialReportAlert(reportId, reportType, period, status, downloadUrl, timestamp);

        // Assert - Verify email content formatting
        verify(mailSender, times(2)).send(argThat(message -> {
            SimpleMailMessage mail = (SimpleMailMessage) message;
            return mail.getSubject().contains("Financial Report Alert") &&
                   mail.getText().contains(reportId) &&
                   mail.getText().contains(reportType.toUpperCase()) &&
                   mail.getText().contains(period) &&
                   mail.getText().contains(status.toUpperCase()) &&
                   mail.getText().contains(downloadUrl);
        }));
    }
}
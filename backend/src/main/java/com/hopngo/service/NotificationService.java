package com.hopngo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.admin.email:admin@hopngo.com}")
    private String adminEmail;

    @Value("${app.finance.email:finance@hopngo.com}")
    private String financeEmail;

    @Value("${app.support.email:support@hopngo.com}")
    private String supportEmail;

    @Value("${app.notifications.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${spring.application.name:HopNGo}")
    private String applicationName;

    /**
     * Send ledger verification alert to administrators
     */
    public void sendLedgerVerificationAlert(String subject, String message) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled - would send ledger alert: {}", subject);
            return;
        }

        String fullSubject = String.format("[%s] Ledger Alert: %s", applicationName, subject);
        String fullMessage = buildLedgerAlertMessage(message);

        sendEmailToAdmins(fullSubject, fullMessage);
        logger.warn("Ledger verification alert sent: {}", subject);
    }

    /**
     * Send reconciliation discrepancy alert
     */
    public void sendReconciliationAlert(String provider, int discrepancyCount, String details) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled - would send reconciliation alert for {}", provider);
            return;
        }

        String subject = String.format("[%s] Reconciliation Alert: %d discrepancies found for %s", 
            applicationName, discrepancyCount, provider);
        String message = buildReconciliationAlertMessage(provider, discrepancyCount, details);

        sendEmailToFinanceTeam(subject, message);
        logger.warn("Reconciliation alert sent for {}: {} discrepancies", provider, discrepancyCount);
    }

    /**
     * Send dispute notification
     */
    public void sendDisputeNotification(String disputeId, String provider, String amount, String reason) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled - would send dispute notification for {}", disputeId);
            return;
        }

        String subject = String.format("[%s] New Dispute: %s (%s)", applicationName, disputeId, provider);
        String message = buildDisputeNotificationMessage(disputeId, provider, amount, reason);

        sendEmailToFinanceTeam(subject, message);
        logger.warn("Dispute notification sent: {} for {}", disputeId, amount);
    }

    /**
     * Send support ticket creation notification
     */
    public void sendSupportTicketNotification(String ticketId, String type, String description) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled - would send support ticket notification for {}", ticketId);
            return;
        }

        String subject = String.format("[%s] Auto-Generated Ticket: %s", applicationName, ticketId);
        String message = buildSupportTicketMessage(ticketId, type, description);

        sendEmailToSupportTeam(subject, message);
        logger.info("Support ticket notification sent: {}", ticketId);
    }

    /**
     * Send financial report generation notification
     */
    public void sendReportGenerationNotification(String reportType, String period, boolean success, String details) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled - would send report notification for {} {}", reportType, period);
            return;
        }

        String status = success ? "Generated Successfully" : "Generation Failed";
        String subject = String.format("[%s] Financial Report %s: %s %s", 
            applicationName, status, reportType, period);
        String message = buildReportNotificationMessage(reportType, period, success, details);

        sendEmailToFinanceTeam(subject, message);
        logger.info("Report generation notification sent: {} {} - {}", reportType, period, status);
    }

    /**
     * Send email to administrators
     */
    private void sendEmailToAdmins(String subject, String message) {
        sendEmail(List.of(adminEmail, financeEmail), subject, message);
    }

    /**
     * Send email to finance team
     */
    private void sendEmailToFinanceTeam(String subject, String message) {
        sendEmail(List.of(financeEmail, adminEmail), subject, message);
    }

    /**
     * Send email to support team
     */
    private void sendEmailToSupportTeam(String subject, String message) {
        sendEmail(List.of(supportEmail, adminEmail), subject, message);
    }

    /**
     * Send email to specified recipients
     */
    private void sendEmail(List<String> recipients, String subject, String message) {
        if (mailSender == null) {
            logger.warn("Mail sender not configured - logging email instead");
            logger.warn("EMAIL - To: {}, Subject: {}, Message: {}", recipients, subject, message);
            return;
        }

        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(recipients.toArray(new String[0]));
            email.setSubject(subject);
            email.setText(message);
            email.setFrom("noreply@hopngo.com");

            mailSender.send(email);
            logger.info("Email sent successfully to: {}", recipients);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", recipients, e);
            // Log the message content for debugging
            logger.error("Failed email content - Subject: {}, Message: {}", subject, message);
        }
    }

    /**
     * Build ledger alert message
     */
    private String buildLedgerAlertMessage(String details) {
        return String.format(
            "Ledger Verification Alert\n\n" +
            "Time: %s\n" +
            "Application: %s\n\n" +
            "Details: %s\n\n" +
            "Please investigate immediately and ensure data integrity.\n\n" +
            "This is an automated alert from the HopNGo financial system.",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            applicationName,
            details
        );
    }

    /**
     * Build reconciliation alert message
     */
    private String buildReconciliationAlertMessage(String provider, int discrepancyCount, String details) {
        return String.format(
            "Reconciliation Discrepancy Alert\n\n" +
            "Time: %s\n" +
            "Provider: %s\n" +
            "Discrepancies Found: %d\n\n" +
            "Details: %s\n\n" +
            "Please review the reconciliation report and resolve any issues.\n\n" +
            "This is an automated alert from the HopNGo financial system.",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            provider,
            discrepancyCount,
            details
        );
    }

    /**
     * Build dispute notification message
     */
    private String buildDisputeNotificationMessage(String disputeId, String provider, String amount, String reason) {
        return String.format(
            "New Payment Dispute Notification\n\n" +
            "Time: %s\n" +
            "Dispute ID: %s\n" +
            "Provider: %s\n" +
            "Amount: %s\n" +
            "Reason: %s\n\n" +
            "Funds have been automatically frozen pending resolution.\n" +
            "Please review the dispute in the admin console and take appropriate action.\n\n" +
            "This is an automated notification from the HopNGo financial system.",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            disputeId,
            provider,
            amount,
            reason
        );
    }

    /**
     * Build support ticket message
     */
    private String buildSupportTicketMessage(String ticketId, String type, String description) {
        return String.format(
            "Auto-Generated Support Ticket\n\n" +
            "Time: %s\n" +
            "Ticket ID: %s\n" +
            "Type: %s\n\n" +
            "Description: %s\n\n" +
            "This ticket was automatically generated by the financial system.\n" +
            "Please investigate and resolve the underlying issue.\n\n" +
            "This is an automated notification from the HopNGo financial system.",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            ticketId,
            type,
            description
        );
    }

    /**
     * Build report notification message
     */
    private String buildReportNotificationMessage(String reportType, String period, boolean success, String details) {
        String status = success ? "has been generated successfully" : "generation failed";
        
        return String.format(
            "Financial Report Notification\n\n" +
            "Time: %s\n" +
            "Report Type: %s\n" +
            "Period: %s\n" +
            "Status: %s\n\n" +
            "Details: %s\n\n" +
            "%s\n\n" +
            "This is an automated notification from the HopNGo financial system.",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            reportType,
            period,
            status,
            details,
            success ? "The report is available for download in the admin console." : "Please check the system logs for error details."
        );
    }

    /**
     * Test notification system
     */
    public void sendTestNotification() {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled - test notification skipped");
            return;
        }

        String subject = String.format("[%s] Test Notification", applicationName);
        String message = String.format(
            "Test Notification\n\n" +
            "Time: %s\n" +
            "Application: %s\n\n" +
            "This is a test notification to verify the email system is working correctly.\n\n" +
            "If you receive this message, the notification system is functioning properly.",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            applicationName
        );

        sendEmailToAdmins(subject, message);
        logger.info("Test notification sent");
    }
}
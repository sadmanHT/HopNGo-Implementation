package com.hopngo.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.mail.from:noreply@hopngo.com}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    public void sendKycApprovalEmail(String userEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(userEmail);
            message.setSubject("KYC Verification Approved - HopNGo");
            
            String emailBody = String.format(
                "Dear %s,\n\n" +
                "Congratulations! Your KYC verification has been approved.\n\n" +
                "You can now:\n" +
                "- Create listings in the marketplace\n" +
                "- Offer services to other users\n" +
                "- Display your verified provider badge\n\n" +
                "Visit your dashboard: %s/dashboard\n\n" +
                "Thank you for being part of the HopNGo community!\n\n" +
                "Best regards,\n" +
                "The HopNGo Team",
                userName, frontendUrl
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            
            logger.info("KYC approval email sent successfully to: {}", userEmail);
        } catch (Exception e) {
            logger.error("Failed to send KYC approval email to: {}", userEmail, e);
        }
    }
    
    public void sendKycRejectionEmail(String userEmail, String userName, String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(userEmail);
            message.setSubject("KYC Verification Update - HopNGo");
            
            String emailBody = String.format(
                "Dear %s,\n\n" +
                "We have reviewed your KYC verification request.\n\n" +
                "Unfortunately, we were unable to approve your verification at this time.\n\n" +
                "Reason: %s\n\n" +
                "You can resubmit your verification with updated documents at: %s/provider/verification\n\n" +
                "If you have any questions, please contact our support team.\n\n" +
                "Best regards,\n" +
                "The HopNGo Team",
                userName, reason != null ? reason : "Please ensure all documents are clear and valid", frontendUrl
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            
            logger.info("KYC rejection email sent successfully to: {}", userEmail);
        } catch (Exception e) {
            logger.error("Failed to send KYC rejection email to: {}", userEmail, e);
        }
    }
}
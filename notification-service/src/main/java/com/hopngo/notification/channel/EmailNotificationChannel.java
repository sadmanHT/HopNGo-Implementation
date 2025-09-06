package com.hopngo.notification.channel;

import com.hopngo.notification.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

@Component
public class EmailNotificationChannel implements NotificationChannel {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationChannel.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Override
    public boolean supports(String channel) {
        return "EMAIL".equalsIgnoreCase(channel);
    }
    
    @Override
    public void send(Notification notification) throws Exception {
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required for email notifications");
        }
        
        try {
            if (notification.getTemplateName() != null && !notification.getTemplateName().trim().isEmpty()) {
                sendHtmlEmail(notification);
            } else {
                sendSimpleEmail(notification);
            }
            
            logger.info("Email sent successfully to: {} for notification: {}", 
                       notification.getRecipientEmail(), notification.getId());
            
        } catch (MailException | MessagingException e) {
            logger.error("Failed to send email to: {} for notification: {}", 
                        notification.getRecipientEmail(), notification.getId(), e);
            throw new Exception("Failed to send email: " + e.getMessage(), e);
        }
    }
    
    private void sendSimpleEmail(Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(notification.getRecipientEmail());
        message.setSubject(notification.getSubject() != null ? notification.getSubject() : "HopNGo Notification");
        message.setText(notification.getContent());
        message.setFrom("noreply@hopngo.com");
        
        mailSender.send(message);
    }
    
    private void sendHtmlEmail(Notification notification) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        helper.setTo(notification.getRecipientEmail());
        helper.setSubject(notification.getSubject() != null ? notification.getSubject() : "HopNGo Notification");
        helper.setFrom("noreply@hopngo.com");
        
        // Process template with variables
        // Convert Map<String, String> to Map<String, Object> for template processing
        Map<String, Object> templateVariables = new HashMap<>();
        if (notification.getVariables() != null) {
            notification.getVariables().forEach((key, value) -> 
                templateVariables.put(key, value)
            );
        }
        String htmlContent = processTemplate(notification.getTemplateName(), templateVariables);
        helper.setText(htmlContent, true);
        
        mailSender.send(mimeMessage);
    }
    
    private String processTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }
            
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            logger.error("Failed to process email template: {}", templateName, e);
            // Fallback to simple text
            return "Notification from HopNGo. Please check your account for details.";
        }
    }
    
    @Override
    public String getChannelName() {
        return "EMAIL";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Simple availability check
            return mailSender != null;
        } catch (Exception e) {
            logger.warn("Email channel is not available: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public int getMaxRetries() {
        return 3;
    }
    
    @Override
    public long getRetryDelayMs() {
        return 5000; // 5 seconds
    }
}
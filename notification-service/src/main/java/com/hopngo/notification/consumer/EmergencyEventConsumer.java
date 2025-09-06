package com.hopngo.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.notification.dto.EmergencyEvent;
import com.hopngo.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class EmergencyEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(EmergencyEventConsumer.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Bean
    public Consumer<Message<String>> emergencyEventsConsumer() {
        return message -> {
            try {
                String payload = message.getPayload();
                String routingKey = (String) message.getHeaders().get("amqp_receivedRoutingKey");
                
                logger.info("Received emergency event with routing key: {}, payload: {}", routingKey, payload);
                
                EmergencyEvent event = objectMapper.readValue(payload, EmergencyEvent.class);
                
                // Process emergency event
                switch (routingKey) {
                    case "emergency.triggered":
                        handleEmergencyTriggered(event);
                        break;
                    default:
                        logger.warn("Unknown emergency event type: {}", routingKey);
                }
                
            } catch (Exception e) {
                logger.error("Error processing emergency event: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process emergency event", e);
            }
        };
    }
    
    /**
     * Handle emergency triggered event
     */
    private void handleEmergencyTriggered(EmergencyEvent event) {
        logger.info("Processing emergency triggered for user: {} at location: {},{}", 
                event.getUserId(), 
                event.getLocation() != null ? event.getLocation().getLat() : "unknown",
                event.getLocation() != null ? event.getLocation().getLng() : "unknown");
        
        try {
            // Send email notifications to all emergency contacts
            for (EmergencyEvent.EmergencyContact contact : event.getContacts()) {
                sendEmergencyEmail(event, contact);
                sendEmergencySMS(event, contact);
            }
            
            logger.info("Emergency notifications sent successfully for user: {}", event.getUserId());
            
        } catch (Exception e) {
            logger.error("Failed to send emergency notifications for user {}: {}", event.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to send emergency notifications", e);
        }
    }
    
    /**
     * Send emergency email notification
     */
    private void sendEmergencyEmail(EmergencyEvent event, EmergencyEvent.EmergencyContact contact) {
        try {
            String subject = "🚨 EMERGENCY ALERT - " + event.getUserName() + " needs help";
            
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("EMERGENCY ALERT\n\n");
            emailBody.append("This is an automated emergency notification from HopNGo.\n\n");
            emailBody.append("User: ").append(event.getUserName()).append(" (").append(event.getUserId()).append(")\n");
            emailBody.append("Contact: ").append(contact.getName()).append(" (").append(contact.getRelation()).append(")\n\n");
            
            if (event.getLocation() != null) {
                emailBody.append("Last Known Location:\n");
                emailBody.append("Latitude: ").append(event.getLocation().getLat()).append("\n");
                emailBody.append("Longitude: ").append(event.getLocation().getLng()).append("\n");
                emailBody.append("Google Maps: https://maps.google.com/maps?q=")
                        .append(event.getLocation().getLat()).append(",")
                        .append(event.getLocation().getLng()).append("\n\n");
            }
            
            if (event.getNote() != null && !event.getNote().trim().isEmpty()) {
                emailBody.append("Additional Information: ").append(event.getNote()).append("\n\n");
            }
            
            emailBody.append("Time: ").append(event.getTriggeredAt()).append("\n\n");
            emailBody.append("Please contact ").append(event.getUserName()).append(" immediately or call emergency services if needed.\n\n");
            emailBody.append("This message was sent automatically by the HopNGo emergency system.");
            
            // Use a dummy email for the contact since we don't have their email in the system
            String contactEmail = contact.getName().toLowerCase().replaceAll("\\s+", ".") + "@example.com";
            
            notificationService.sendEmail(
                    contactEmail,
                    subject,
                    emailBody.toString()
            );
            
            logger.info("Emergency email sent to contact: {} ({})", contact.getName(), contactEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send emergency email to contact {}: {}", contact.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Send emergency SMS notification (stub implementation)
     */
    private void sendEmergencySMS(EmergencyEvent event, EmergencyEvent.EmergencyContact contact) {
        try {
            StringBuilder smsMessage = new StringBuilder();
            smsMessage.append("🚨 EMERGENCY: ").append(event.getUserName()).append(" needs help! ");
            
            if (event.getLocation() != null) {
                smsMessage.append("Location: https://maps.google.com/maps?q=")
                        .append(event.getLocation().getLat()).append(",")
                        .append(event.getLocation().getLng()).append(" ");
            }
            
            if (event.getNote() != null && !event.getNote().trim().isEmpty()) {
                smsMessage.append("Note: ").append(event.getNote()).append(" ");
            }
            
            smsMessage.append("Time: ").append(event.getTriggeredAt()).append(". Please contact them immediately!");
            
            // Log SMS stub (since SMS is not actually implemented)
            logger.info("[SMS STUB] Emergency SMS to {} ({}): {}", 
                    contact.getName(), 
                    contact.getPhone(), 
                    smsMessage.toString());
            
        } catch (Exception e) {
            logger.error("Failed to send emergency SMS to contact {}: {}", contact.getName(), e.getMessage(), e);
        }
    }
}
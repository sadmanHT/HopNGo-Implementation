package com.hopngo.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.notification.channel.NotificationChannel;
import com.hopngo.notification.dto.BookingEvent;
import com.hopngo.notification.dto.ChatEvent;
import com.hopngo.notification.dto.PaymentEvent;
import com.hopngo.notification.entity.Notification;
import com.hopngo.notification.entity.NotificationStatus;
import com.hopngo.notification.entity.NotificationType;
import com.hopngo.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private List<NotificationChannel> notificationChannels;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Booking event handlers
    @Async
    public void sendBookingConfirmedNotification(BookingEvent event) {
        Map<String, Object> variables = createBookingVariables(event);
        
        Notification notification = createNotification(
            event.getUserId(),
            event.getUserEmail(),
            null, // phone not available in booking event
            NotificationType.BOOKING_CONFIRMED,
            "EMAIL",
            "booking-confirmed",
            "Booking Confirmed - " + event.getPropertyName(),
            "Your booking has been confirmed!",
            variables,
            event.getEventId(),
            event.getEventType()
        );
        
        processNotification(notification);
    }
    
    @Async
    public void sendBookingCancelledNotification(BookingEvent event) {
        Map<String, Object> variables = createBookingVariables(event);
        
        Notification notification = createNotification(
            event.getUserId(),
            event.getUserEmail(),
            null,
            NotificationType.BOOKING_CANCELLED,
            "EMAIL",
            "booking-cancelled",
            "Booking Cancelled - " + event.getPropertyName(),
            "Your booking has been cancelled.",
            variables,
            event.getEventId(),
            event.getEventType()
        );
        
        processNotification(notification);
    }
    
    @Async
    public void sendBookingReminderNotification(BookingEvent event) {
        Map<String, Object> variables = createBookingVariables(event);
        
        Notification notification = createNotification(
            event.getUserId(),
            event.getUserEmail(),
            null,
            NotificationType.BOOKING_REMINDER,
            "EMAIL",
            "booking-reminder",
            "Booking Reminder - " + event.getPropertyName(),
            "Don't forget about your upcoming booking!",
            variables,
            event.getEventId(),
            event.getEventType()
        );
        
        processNotification(notification);
    }
    
    // Payment event handlers
    @Async
    public void sendPaymentSucceededNotification(PaymentEvent event) {
        Map<String, Object> variables = createPaymentVariables(event);
        
        Notification notification = createNotification(
            event.getUserId(),
            event.getUserEmail(),
            null,
            NotificationType.PAYMENT_RECEIPT,
            "EMAIL",
            "payment-receipt",
            "Payment Receipt - Order #" + event.getOrderId(),
            "Your payment has been processed successfully.",
            variables,
            event.getEventId(),
            event.getEventType()
        );
        
        processNotification(notification);
    }
    
    @Async
    public void sendPaymentFailedNotification(PaymentEvent event) {
        Map<String, Object> variables = createPaymentVariables(event);
        
        Notification notification = createNotification(
            event.getUserId(),
            event.getUserEmail(),
            null,
            NotificationType.PAYMENT_FAILED,
            "EMAIL",
            "payment-failed",
            "Payment Failed - Order #" + event.getOrderId(),
            "Your payment could not be processed.",
            variables,
            event.getEventId(),
            event.getEventType()
        );
        
        processNotification(notification);
    }
    
    // Chat event handlers
    @Async
    public void sendChatMessageNotification(ChatEvent event) {
        // Send to all recipients
        if (event.getRecipientEmails() != null) {
            for (int i = 0; i < event.getRecipientEmails().size(); i++) {
                String recipientId = i < event.getRecipientIds().size() ? event.getRecipientIds().get(i) : null;
                String recipientEmail = event.getRecipientEmails().get(i);
                
                Map<String, Object> variables = createChatVariables(event);
                
                Notification notification = createNotification(
                    recipientId,
                    recipientEmail,
                    null,
                    NotificationType.CHAT_MESSAGE,
                    "EMAIL",
                    "chat-message",
                    "New Message in " + event.getConversationTitle(),
                    "You have a new message.",
                    variables,
                    event.getEventId(),
                    event.getEventType()
                );
                
                processNotification(notification);
            }
        }
    }
    
    @Async
    public void sendChatMentionNotification(ChatEvent event) {
        if (event.getMentionedUserEmails() != null) {
            for (int i = 0; i < event.getMentionedUserEmails().size(); i++) {
                String userId = i < event.getMentionedUserIds().size() ? event.getMentionedUserIds().get(i) : null;
                String userEmail = event.getMentionedUserEmails().get(i);
                
                Map<String, Object> variables = createChatVariables(event);
                
                Notification notification = createNotification(
                    userId,
                    userEmail,
                    null,
                    NotificationType.CHAT_MENTION,
                    "EMAIL",
                    "chat-mention",
                    "You were mentioned in " + event.getConversationTitle(),
                    "You were mentioned in a conversation.",
                    variables,
                    event.getEventId(),
                    event.getEventType()
                );
                
                processNotification(notification);
            }
        }
    }
    
    @Async
    public void sendConversationCreatedNotification(ChatEvent event) {
        sendChatMessageNotification(event); // Reuse chat message logic
    }
    
    @Async
    public void sendConversationJoinedNotification(ChatEvent event) {
        sendChatMessageNotification(event); // Reuse chat message logic
    }
    
    @Async
    public void sendUrgentChatNotification(ChatEvent event) {
        // Send via multiple channels for urgent messages
        if (event.getRecipientEmails() != null) {
            for (int i = 0; i < event.getRecipientEmails().size(); i++) {
                String recipientId = i < event.getRecipientIds().size() ? event.getRecipientIds().get(i) : null;
                String recipientEmail = event.getRecipientEmails().get(i);
                
                Map<String, Object> variables = createChatVariables(event);
                
                // Send email
                Notification emailNotification = createNotification(
                    recipientId,
                    recipientEmail,
                    null,
                    NotificationType.CHAT_URGENT,
                    "EMAIL",
                    "chat-urgent",
                    "URGENT: " + event.getConversationTitle(),
                    "You have an urgent message.",
                    variables,
                    event.getEventId(),
                    event.getEventType()
                );
                
                // Send push notification
                Notification pushNotification = createNotification(
                    recipientId,
                    recipientEmail,
                    null,
                    NotificationType.CHAT_URGENT,
                    "PUSH",
                    null,
                    "URGENT: " + event.getConversationTitle(),
                    event.getMessageContent(),
                    variables,
                    event.getEventId(),
                    event.getEventType()
                );
                
                processNotification(emailNotification);
                processNotification(pushNotification);
            }
        }
    }
    
    // Test notification method
    public void sendTestNotification(String recipientEmail, String message) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("message", message);
        variables.put("timestamp", LocalDateTime.now().toString());
        
        Notification notification = createNotification(
            "test-user",
            recipientEmail,
            null,
            NotificationType.SYSTEM_TEST,
            "EMAIL",
            "test-notification",
            "Test Notification from HopNGo",
            message,
            variables,
            "test-" + System.currentTimeMillis(),
            "test.notification"
        );
        
        processNotification(notification);
    }
    
    private Notification createNotification(String recipientId, String recipientEmail, String recipientPhone,
                                          NotificationType type, String channel, String templateName,
                                          String subject, String content, Map<String, Object> variables,
                                          String eventId, String eventType) {
        
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setRecipientEmail(recipientEmail);
        notification.setRecipientPhone(recipientPhone);
        notification.setType(type);
        notification.setChannel(channel);
        notification.setTemplateName(templateName);
        notification.setSubject(subject);
        notification.setContent(content);
        notification.setVariables(variables);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setEventId(eventId);
        notification.setEventType(eventType);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
    
    private void processNotification(Notification notification) {
        try {
            notification.setStatus(NotificationStatus.PROCESSING);
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            NotificationChannel channel = findChannel(notification.getChannel());
            if (channel == null) {
                throw new Exception("No channel found for: " + notification.getChannel());
            }
            
            if (!channel.isAvailable()) {
                throw new Exception("Channel is not available: " + notification.getChannel());
            }
            
            channel.send(notification);
            
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            logger.info("Notification sent successfully: {}", notification.getId());
            
        } catch (Exception e) {
            logger.error("Failed to send notification: {}", notification.getId(), e);
            
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setUpdatedAt(LocalDateTime.now());
            
            // Check if we should retry
            NotificationChannel channel = findChannel(notification.getChannel());
            if (channel != null && notification.getRetryCount() < channel.getMaxRetries()) {
                notification.scheduleRetry(channel.getRetryDelayMs());
                notification.setStatus(NotificationStatus.RETRY);
            }
            
            notificationRepository.save(notification);
        }
    }
    
    private NotificationChannel findChannel(String channelName) {
        return notificationChannels.stream()
            .filter(channel -> channel.supports(channelName))
            .findFirst()
            .orElse(null);
    }
    
    private Map<String, Object> createBookingVariables(BookingEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("bookingId", event.getBookingId());
        variables.put("userName", event.getUserName());
        variables.put("propertyName", event.getPropertyName());
        variables.put("propertyAddress", event.getPropertyAddress());
        variables.put("checkInDate", event.getCheckInDate());
        variables.put("checkOutDate", event.getCheckOutDate());
        variables.put("totalAmount", event.getTotalAmount());
        variables.put("currency", event.getCurrency());
        variables.put("status", event.getStatus());
        if (event.getCancellationReason() != null) {
            variables.put("cancellationReason", event.getCancellationReason());
        }
        return variables;
    }
    
    private Map<String, Object> createPaymentVariables(PaymentEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("paymentId", event.getPaymentId());
        variables.put("orderId", event.getOrderId());
        variables.put("userName", event.getUserName());
        variables.put("amount", event.getAmount());
        variables.put("currency", event.getCurrency());
        variables.put("paymentMethod", event.getPaymentMethod());
        variables.put("transactionId", event.getTransactionId());
        if (event.getFailureReason() != null) {
            variables.put("failureReason", event.getFailureReason());
        }
        if (event.getOrderItems() != null) {
            variables.put("orderItems", event.getOrderItems());
        }
        return variables;
    }
    
    private Map<String, Object> createChatVariables(ChatEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("messageId", event.getMessageId());
        variables.put("conversationId", event.getConversationId());
        variables.put("conversationTitle", event.getConversationTitle());
        variables.put("senderName", event.getSenderName());
        variables.put("messageContent", event.getMessageContent());
        variables.put("messageType", event.getMessageType());
        variables.put("isUrgent", event.getIsUrgent());
        return variables;
    }
}
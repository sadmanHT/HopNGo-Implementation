package com.hopngo.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.notification.channel.NotificationChannel;
import com.hopngo.notification.dto.BookingEvent;
import com.hopngo.notification.dto.ChatEvent;
import com.hopngo.notification.dto.PaymentEvent;
import com.hopngo.notification.dto.PushNotificationRequest;
import com.hopngo.notification.entity.Notification;
import com.hopngo.notification.entity.NotificationStatus;
import com.hopngo.notification.entity.NotificationType;
import com.hopngo.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rabbitmq.client.Channel;
import jakarta.validation.ValidationException;
import org.springframework.amqp.core.AmqpTemplate;

import java.math.BigDecimal;
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
    
    @Autowired
    private WebPushService webPushService;
    
    @Autowired(required = false)
    private FirebaseMessagingService firebaseMessagingService;
    
    // Booking event handlers with enhanced error handling
    @Async
    @RabbitListener(
        queues = "booking.notifications",
        errorHandler = "rabbitListenerErrorHandler"
    )
    public void handleBookingEvent(BookingEvent bookingEvent, 
                                 @Header Map<String, Object> headers,
                                 Channel channel,
                                 @Header("amqp_deliveryTag") long deliveryTag) {
        
        logger.info("Received booking event: {} with delivery tag: {}", bookingEvent, deliveryTag);
        
        try {
            validateBookingEvent(bookingEvent);
            
            String subject = "Booking Confirmation - " + bookingEvent.getBookingId();
            String message = String.format(
                "Your booking has been confirmed!\n\n" +
                "Booking ID: %s\n" +
                "User: %s\n" +
                "Amount: $%.2f\n" +
                "Status: %s\n" +
                "Created: %s",
                bookingEvent.getBookingId(),
                bookingEvent.getUserId(),
                bookingEvent.getTotalAmount(),
                bookingEvent.getStatus(),
                bookingEvent.getTimestamp()
            );
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("bookingId", bookingEvent.getBookingId());
            variables.put("userId", bookingEvent.getUserId());
            variables.put("amount", bookingEvent.getTotalAmount());
            variables.put("status", bookingEvent.getStatus());
            variables.put("createdAt", bookingEvent.getTimestamp());
            variables.put("deliveryTag", deliveryTag);
            variables.put("retryCount", headers.getOrDefault("x-retry-count", 0));
            
            Notification notification = createNotification(
                bookingEvent.getUserId(),
                bookingEvent.getUserEmail(),
                null,
                NotificationType.BOOKING_CONFIRMED,
                "EMAIL",
                "booking-confirmation",
                subject,
                message,
                variables,
                bookingEvent.getBookingId(),
                "booking.confirmation"
            );
            
            try {
                processNotification(notification);
            } catch (Exception e) {
                logger.error("Failed to process booking notification", e);
            }
            
            logger.info("Successfully processed booking notification for booking: {}", bookingEvent.getBookingId());
            
        } catch (ValidationException e) {
            logger.error("Validation failed for booking event: {} - {}", bookingEvent.getBookingId(), e.getMessage());
            // Don't retry validation errors - send to DLQ immediately
            throw new AmqpRejectAndDontRequeueException("Validation failed", e);
        } catch (Exception e) {
            logger.error("Failed to process booking notification for booking: {} (attempt: {})", 
                        bookingEvent.getBookingId(), headers.getOrDefault("x-retry-count", 0), e);
            throw e; // Will be retried by RabbitMQ
        }
    }
    
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
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process booking confirmed notification", e);
        }
        
        // Send web push notification
        try {
            webPushService.sendBookingNotification(
                event.getUserId(),
                event.getBookingId(),
                event.getPropertyName()
            );
        } catch (Exception e) {
            logger.error("Failed to send web push notification for booking confirmation", e);
        }
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
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process booking cancelled notification", e);
        }
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
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process booking reminder notification", e);
        }
    }
    
    @Async
    public void sendBookingUpdateNotification(BookingEvent event) {
        Map<String, Object> variables = createBookingVariables(event);
        
        Notification notification = createNotification(
            event.getUserId(),
            event.getUserEmail(),
            null,
            NotificationType.BOOKING_UPDATED,
            "EMAIL",
            "booking-updated",
            "Booking Updated - " + event.getPropertyName(),
            "Your booking has been updated.",
            variables,
            event.getEventId(),
            event.getEventType()
        );
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process booking update notification", e);
        }
    }
    
    // Payment event handlers with enhanced error handling
    @Async
    @RabbitListener(
        queues = "payment.notifications",
        errorHandler = "rabbitListenerErrorHandler"
    )
    public void handlePaymentEvent(PaymentEvent paymentEvent,
                                 @Header Map<String, Object> headers,
                                 Channel channel,
                                 @Header("amqp_deliveryTag") long deliveryTag) {
        
        logger.info("Received payment event: {} with delivery tag: {}", paymentEvent, deliveryTag);
        
        try {
            validatePaymentEvent(paymentEvent);
            
            String subject = "Payment " + paymentEvent.getStatus() + " - " + paymentEvent.getPaymentId();
            String message = String.format(
                "Payment Update:\n\n" +
                "Payment ID: %s\n" +
                "Amount: $%.2f\n" +
                "Status: %s\n" +
                "Method: %s\n" +
                "Processed: %s",
                paymentEvent.getPaymentId(),
                paymentEvent.getAmount(),
                paymentEvent.getStatus(),
                paymentEvent.getPaymentMethod(),
                paymentEvent.getTimestamp()
            );
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("paymentId", paymentEvent.getPaymentId());
            variables.put("amount", paymentEvent.getAmount());
            variables.put("status", paymentEvent.getStatus());
            variables.put("paymentMethod", paymentEvent.getPaymentMethod());
            variables.put("processedAt", paymentEvent.getTimestamp());
            variables.put("deliveryTag", deliveryTag);
            variables.put("retryCount", headers.getOrDefault("x-retry-count", 0));
            
            Notification notification = createNotification(
                paymentEvent.getUserId(),
                paymentEvent.getUserEmail(),
                null,
                NotificationType.PAYMENT_RECEIPT,
                "EMAIL",
                "payment-confirmation",
                subject,
                message,
                variables,
                paymentEvent.getPaymentId(),
                "payment.confirmation"
            );
            
            try {
                processNotification(notification);
            } catch (Exception e) {
                logger.error("Failed to process payment notification", e);
            }
            
            logger.info("Successfully processed payment notification for payment: {}", paymentEvent.getPaymentId());
            
        } catch (ValidationException e) {
            logger.error("Validation failed for payment event: {} - {}", paymentEvent.getPaymentId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Validation failed", e);
        } catch (Exception e) {
            logger.error("Failed to process payment notification for payment: {} (attempt: {})", 
                        paymentEvent.getPaymentId(), headers.getOrDefault("x-retry-count", 0), e);
            throw e;
        }
    }
    
    // Payment event handlers
    @Async
    public void sendPaymentReceiptNotification(PaymentEvent event) {
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
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process payment receipt notification", e);
        }
    }
    
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
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process payment succeeded notification", e);
        }
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
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process payment failed notification", e);
        }
    }
    
    @Async
    public void sendPaymentPendingNotification(PaymentEvent event) {
        Map<String, Object> variables = createPaymentVariables(event);
        
        Notification notification = createNotification(
            event.getUserId(),
            event.getUserEmail(),
            null,
            NotificationType.PAYMENT_PENDING,
            "EMAIL",
            "payment-pending",
            "Payment Pending - Order #" + event.getOrderId(),
            "Your payment is being processed.",
            variables,
            event.getEventId(),
            event.getEventType()
        );        
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process payment pending notification", e);
        }
    }
    
    @Async
    public void sendPaymentRefundNotification(PaymentEvent event) {
        Map<String, Object> variables = createPaymentVariables(event);
        
        Notification notification = createNotification(
            event.getUserId(),
            event.getUserEmail(),
            null,
            NotificationType.PAYMENT_REFUNDED,
            "EMAIL",
            "payment-refunded",
            "Payment Refunded - Order #" + event.getOrderId(),
            "Your payment has been refunded.",
            variables,
            event.getEventId(),
            event.getEventType()
        );        
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process payment refund notification", e);
        }
    }
    
    @Async
    public void sendPaymentCancelledNotification(PaymentEvent event) {
        Map<String, Object> variables = createPaymentVariables(event);
        
        Notification notification = createNotification(
            event.getUserId(),
            event.getUserEmail(),
            null,
            NotificationType.PAYMENT_CANCELLED,
            "EMAIL",
            "payment-cancelled",
            "Payment Cancelled - Order #" + event.getOrderId(),
            "Your payment has been cancelled.",
            variables,
            event.getEventId(),
            event.getEventType()
        );
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process payment cancelled notification", e);
        }
    }
    
    // Chat event handlers with enhanced error handling
    @Async
    @RabbitListener(
        queues = "chat.notifications",
        errorHandler = "rabbitListenerErrorHandler"
    )
    public void handleChatEvent(ChatEvent chatEvent,
                              @Header Map<String, Object> headers,
                              Channel channel,
                              @Header("amqp_deliveryTag") long deliveryTag) {
        
        logger.info("Received chat event: {} with delivery tag: {}", chatEvent, deliveryTag);
        
        try {
            validateChatEvent(chatEvent);
            
            String subject = "New Message from " + chatEvent.getSenderName();
            String message = String.format(
                "You have a new message!\n\n" +
                "From: %s\n" +
                "Message: %s\n" +
                "Chat ID: %s\n" +
                "Sent: %s",
                chatEvent.getSenderName(),
                chatEvent.getMessageContent(),
                chatEvent.getConversationId(),
                chatEvent.getTimestamp()
            );
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("chatId", chatEvent.getConversationId());
            variables.put("senderId", chatEvent.getSenderId());
            variables.put("senderName", chatEvent.getSenderName());
            variables.put("message", chatEvent.getMessageContent());
            variables.put("timestamp", chatEvent.getTimestamp());
            variables.put("deliveryTag", deliveryTag);
            variables.put("retryCount", headers.getOrDefault("x-retry-count", 0));
            
            Notification notification = createNotification(
                chatEvent.getRecipientIds() != null && !chatEvent.getRecipientIds().isEmpty() ? chatEvent.getRecipientIds().get(0) : null,
                chatEvent.getRecipientEmails() != null && !chatEvent.getRecipientEmails().isEmpty() ? chatEvent.getRecipientEmails().get(0) : null,
                null,
                NotificationType.CHAT_MESSAGE,
                "EMAIL",
                "chat-message",
                subject,
                message,
                variables,
                chatEvent.getConversationId(),
                "chat.message"
            );
            
            try {
                processNotification(notification);
            } catch (Exception e) {
                logger.error("Failed to process chat notification", e);
            }
            
            logger.info("Successfully processed chat notification for chat: {}", chatEvent.getConversationId());
            
        } catch (ValidationException e) {
            logger.error("Validation failed for chat event: {} - {}", chatEvent.getConversationId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Validation failed", e);
        } catch (Exception e) {
            logger.error("Failed to process chat notification for chat: {} (attempt: {})", 
                        chatEvent.getConversationId(), headers.getOrDefault("x-retry-count", 0), e);
            throw e;
        }
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
                
                try {
                    processNotification(notification);
                } catch (Exception e) {
                    logger.error("Failed to process chat message notification", e);
                }
                
                // Send web push notification
                try {
                    webPushService.sendChatNotification(
                        recipientId,
                        event.getSenderName(),
                        event.getMessageContent(),
                        event.getConversationId()
                    );
                } catch (Exception e) {
                    logger.error("Failed to send web push notification for chat message", e);
                }
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
                
                try {
                    processNotification(notification);
                } catch (Exception e) {
                    logger.error("Failed to process chat mention notification", e);
                }
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
                
                try {
                    processNotification(emailNotification);
                } catch (Exception e) {
                    logger.error("Failed to process email notification", e);
                }
                
                try {
                    processNotification(pushNotification);
                } catch (Exception e) {
                    logger.error("Failed to process push notification", e);
                }
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
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process test notification", e);
        }
    }
    
    public void sendMultiChannelNotification(String recipientId, String recipientEmail, String subject, String message, Map<String, String> pushTokens) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("message", message);
        variables.put("timestamp", LocalDateTime.now().toString());
        
        // Send email notification
        Notification notification = createNotification(
            recipientId,
            recipientEmail,
            null,
            NotificationType.SYSTEM_NOTIFICATION,
            "EMAIL",
            "multi-channel-notification",
            subject,
            message,
            variables,
            "multi-" + System.currentTimeMillis(),
            "multi.channel"
        );
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process multi-channel notification", e);
        }
        
        // Send push notifications if tokens are provided and FCM is available
        if (pushTokens != null && !pushTokens.isEmpty() && firebaseMessagingService != null) {
            sendPushNotifications(recipientId, subject, message, pushTokens);
        }
    }
    
    public void sendEmail(String recipientEmail, String subject, String content) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("content", content);
        
        Notification notification = createNotification(
            "emergency-user",
            recipientEmail,
            null,
            NotificationType.SYSTEM_NOTIFICATION,
            "EMAIL",
            "emergency-notification",
            subject,
            content,
            variables,
            "emergency-event-" + System.currentTimeMillis(),
            "emergency"
        );
        
        try {
            processNotification(notification);
        } catch (Exception e) {
            logger.error("Failed to process emergency notification", e);
        }
    }
    
    public Notification createNotification(String recipientId, String recipientEmail, String recipientPhone,
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
        // Convert Map<String, Object> to Map<String, String>
        Map<String, String> stringVariables = new HashMap<>();
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                stringVariables.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
            }
        }
        notification.setVariables(stringVariables);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setEventId(eventId);
        notification.setEventType(eventType);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
    
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public void processNotification(Notification notification) throws Exception {
        try {
            notification.setStatus(NotificationStatus.PROCESSING);
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            NotificationChannel channel = findChannel(notification.getChannel());
            if (channel == null) {
                throw new RuntimeException("No channel found for: " + notification.getChannel());
            }
            
            if (!channel.isAvailable()) {
                throw new RuntimeException("Channel is not available: " + notification.getChannel());
            }
            
            sendNotificationWithRetry(notification, channel);
            
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            logger.info("Notification sent successfully: {}", notification.getId());
            
        } catch (Exception e) {
            logger.error("Failed to send notification after retries: {}", notification.getId(), e);
            throw e; // Re-throw to trigger @Recover method
        }
    }
    
    @Retryable(
        value = {Exception.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 2000, multiplier = 1.5, maxDelay = 30000)
    )
    private void sendNotificationWithRetry(Notification notification, NotificationChannel channel) throws Exception {
        try {
            channel.send(notification);
            logger.debug("Notification sent via channel: {} for notification: {}", channel.getClass().getSimpleName(), notification.getId());
        } catch (Exception e) {
            logger.warn("Attempt failed for notification: {} via channel: {}, error: {}", 
                notification.getId(), channel.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
    
    private void sendPushNotifications(String recipientId, String subject, String message, Map<String, String> pushTokens) {
        for (Map.Entry<String, String> entry : pushTokens.entrySet()) {
            String platform = entry.getKey(); // "android" or "ios"
            String token = entry.getValue();
            
            try {
                PushNotificationRequest pushRequest = PushNotificationRequest.builder()
                    .token(token)
                    .title(subject)
                    .body(message)
                    .priority("high")
                    .data(Map.of(
                        "userId", recipientId,
                        "timestamp", LocalDateTime.now().toString()
                    ))
                    .build();
                    
                firebaseMessagingService.sendNotification(pushRequest);
                logger.info("Push notification sent successfully to {} device for user: {}", platform, recipientId);
            } catch (Exception e) {
                logger.error("Failed to send push notification to {} device for user: {}", platform, recipientId, e);
                // Don't throw exception here to avoid failing the entire notification process
            }
        }
    }
    
    // Validation methods for event processing
    private void validateBookingEvent(BookingEvent event) {
        if (event == null) {
            throw new ValidationException("Booking event cannot be null");
        }
        if (event.getBookingId() == null || event.getBookingId().trim().isEmpty()) {
            throw new ValidationException("Booking ID cannot be null or empty");
        }
        if (event.getUserId() == null || event.getUserId().trim().isEmpty()) {
            throw new ValidationException("User ID cannot be null or empty");
        }
        if (event.getUserEmail() == null || event.getUserEmail().trim().isEmpty()) {
            throw new ValidationException("User email cannot be null or empty");
        }
    }
    
    private void validateChatEvent(ChatEvent event) {
        if (event == null) {
            throw new ValidationException("Chat event cannot be null");
        }
        if (event.getConversationId() == null || event.getConversationId().trim().isEmpty()) {
            throw new ValidationException("Conversation ID cannot be null or empty");
        }
        if (event.getRecipientIds() == null || event.getRecipientIds().isEmpty()) {
            throw new ValidationException("Recipient IDs cannot be null or empty");
        }
        if (event.getMessageContent() == null || event.getMessageContent().trim().isEmpty()) {
            throw new ValidationException("Message content cannot be null or empty");
        }
    }
    
    private void validatePaymentEvent(PaymentEvent event) {
        if (event == null) {
            throw new ValidationException("Payment event cannot be null");
        }
        if (event.getPaymentId() == null || event.getPaymentId().trim().isEmpty()) {
            throw new ValidationException("Payment ID cannot be null or empty");
        }
        if (event.getUserId() == null || event.getUserId().trim().isEmpty()) {
            throw new ValidationException("User ID cannot be null or empty");
        }
        if (event.getAmount() == null || event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment amount must be greater than zero");
        }
    }
    
    @Recover
    private void recoverFromNotificationFailure(Exception ex, Notification notification) {
        logger.error("All retry attempts exhausted for notification: {}, marking as failed", notification.getId(), ex);
        
        notification.setStatus(NotificationStatus.FAILED);
        notification.setErrorMessage(ex.getMessage());
        notification.setUpdatedAt(LocalDateTime.now());
        
        // Schedule for later retry if within retry limits
        if (notification.getRetryCount() < 10) { // Max 10 total retries across all attempts
            notification.setNextRetryAt(LocalDateTime.now().plusMinutes(30)); // Retry in 30 minutes
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setStatus(NotificationStatus.RETRY);
            logger.info("Scheduling notification {} for retry attempt {} in 30 minutes", 
                notification.getId(), notification.getRetryCount());
        }
        
        notificationRepository.save(notification);
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
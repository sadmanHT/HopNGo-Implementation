package com.hopngo.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.notification.dto.PaymentEvent;
import com.hopngo.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class PaymentEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Bean
    public Consumer<Message<String>> paymentEventsConsumer() {
        return message -> {
            try {
                String payload = message.getPayload();
                String routingKey = (String) message.getHeaders().get("amqp_receivedRoutingKey");
                
                logger.info("Received payment event with routing key: {}, payload: {}", routingKey, payload);
                
                PaymentEvent event = objectMapper.readValue(payload, PaymentEvent.class);
                
                // Process different types of payment events
                switch (routingKey) {
                    case "payment.succeeded":
                        handlePaymentSucceeded(event);
                        break;
                    case "payment.failed":
                        handlePaymentFailed(event);
                        break;
                    case "payment.refunded":
                        handlePaymentRefunded(event);
                        break;
                    case "payment.cancelled":
                        handlePaymentCancelled(event);
                        break;
                    case "payment.pending":
                        handlePaymentPending(event);
                        break;
                    default:
                        logger.warn("Unknown payment event type: {}", routingKey);
                }
                
            } catch (Exception e) {
                logger.error("Error processing payment event: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process payment event", e);
            }
        };
    }
    
    private void handlePaymentSucceeded(PaymentEvent event) {
        logger.info("Processing payment succeeded event for payment: {}", event.getPaymentId());
        
        try {
            notificationService.sendPaymentReceiptNotification(event);
            logger.info("Successfully processed payment receipt notification for payment: {}", event.getPaymentId());
        } catch (Exception e) {
            logger.error("Failed to send payment receipt notification for payment: {}", event.getPaymentId(), e);
            throw e;
        }
    }
    
    private void handlePaymentFailed(PaymentEvent event) {
        logger.info("Processing payment failed event for payment: {}", event.getPaymentId());
        
        try {
            notificationService.sendPaymentFailedNotification(event);
            logger.info("Successfully processed payment failed notification for payment: {}", event.getPaymentId());
        } catch (Exception e) {
            logger.error("Failed to send payment failed notification for payment: {}", event.getPaymentId(), e);
            throw e;
        }
    }
    
    private void handlePaymentRefunded(PaymentEvent event) {
        logger.info("Processing payment refunded event for payment: {}", event.getPaymentId());
        
        try {
            notificationService.sendPaymentRefundNotification(event);
            logger.info("Successfully processed payment refund notification for payment: {}", event.getPaymentId());
        } catch (Exception e) {
            logger.error("Failed to send payment refund notification for payment: {}", event.getPaymentId(), e);
            throw e;
        }
    }
    
    private void handlePaymentCancelled(PaymentEvent event) {
        logger.info("Processing payment cancelled event for payment: {}", event.getPaymentId());
        
        try {
            notificationService.sendPaymentCancelledNotification(event);
            logger.info("Successfully processed payment cancelled notification for payment: {}", event.getPaymentId());
        } catch (Exception e) {
            logger.error("Failed to send payment cancelled notification for payment: {}", event.getPaymentId(), e);
            throw e;
        }
    }
    
    private void handlePaymentPending(PaymentEvent event) {
        logger.info("Processing payment pending event for payment: {}", event.getPaymentId());
        
        try {
            notificationService.sendPaymentPendingNotification(event);
            logger.info("Successfully processed payment pending notification for payment: {}", event.getPaymentId());
        } catch (Exception e) {
            logger.error("Failed to send payment pending notification for payment: {}", event.getPaymentId(), e);
            throw e;
        }
    }
}
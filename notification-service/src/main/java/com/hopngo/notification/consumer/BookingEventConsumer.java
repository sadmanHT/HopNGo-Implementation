package com.hopngo.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.notification.dto.BookingEvent;
import com.hopngo.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class BookingEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingEventConsumer.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Bean
    public Consumer<Message<String>> bookingEventsConsumer() {
        return message -> {
            try {
                String payload = message.getPayload();
                String routingKey = (String) message.getHeaders().get("amqp_receivedRoutingKey");
                
                logger.info("Received booking event with routing key: {}, payload: {}", routingKey, payload);
                
                BookingEvent event = objectMapper.readValue(payload, BookingEvent.class);
                
                // Process different types of booking events
                switch (routingKey) {
                    case "booking.confirmed":
                        handleBookingConfirmed(event);
                        break;
                    case "booking.cancelled":
                        handleBookingCancelled(event);
                        break;
                    case "booking.reminder":
                        handleBookingReminder(event);
                        break;
                    case "booking.updated":
                        handleBookingUpdated(event);
                        break;
                    default:
                        logger.warn("Unknown booking event type: {}", routingKey);
                }
                
            } catch (Exception e) {
                logger.error("Error processing booking event: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process booking event", e);
            }
        };
    }
    
    private void handleBookingConfirmed(BookingEvent event) {
        logger.info("Processing booking confirmed event for booking: {}", event.getBookingId());
        
        try {
            notificationService.sendBookingConfirmationNotification(event);
            logger.info("Successfully processed booking confirmed notification for booking: {}", event.getBookingId());
        } catch (Exception e) {
            logger.error("Failed to send booking confirmation notification for booking: {}", event.getBookingId(), e);
            throw e;
        }
    }
    
    private void handleBookingCancelled(BookingEvent event) {
        logger.info("Processing booking cancelled event for booking: {}", event.getBookingId());
        
        try {
            notificationService.sendBookingCancellationNotification(event);
            logger.info("Successfully processed booking cancelled notification for booking: {}", event.getBookingId());
        } catch (Exception e) {
            logger.error("Failed to send booking cancellation notification for booking: {}", event.getBookingId(), e);
            throw e;
        }
    }
    
    private void handleBookingReminder(BookingEvent event) {
        logger.info("Processing booking reminder event for booking: {}", event.getBookingId());
        
        try {
            notificationService.sendBookingReminderNotification(event);
            logger.info("Successfully processed booking reminder notification for booking: {}", event.getBookingId());
        } catch (Exception e) {
            logger.error("Failed to send booking reminder notification for booking: {}", event.getBookingId(), e);
            throw e;
        }
    }
    
    private void handleBookingUpdated(BookingEvent event) {
        logger.info("Processing booking updated event for booking: {}", event.getBookingId());
        
        try {
            notificationService.sendBookingUpdateNotification(event);
            logger.info("Successfully processed booking updated notification for booking: {}", event.getBookingId());
        } catch (Exception e) {
            logger.error("Failed to send booking update notification for booking: {}", event.getBookingId(), e);
            throw e;
        }
    }
}
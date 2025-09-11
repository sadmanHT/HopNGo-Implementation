package com.hopngo.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.analytics.dto.DomainEvent;
import com.hopngo.analytics.dto.EventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service to consume domain events from RabbitMQ and convert them to analytics events
 */
@Service
public class DomainEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(DomainEventConsumer.class);
    
    private final EventService eventService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public DomainEventConsumer(EventService eventService, ObjectMapper objectMapper) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Consume booking domain events
     */
    @RabbitListener(queues = "booking.events.queue", ackMode = "MANUAL")
    public void handleBookingEvent(String eventPayload, 
             @Header Map<String, Object> headers,
             Message message,
             com.rabbitmq.client.Channel channel) {
        
        logger.debug("Received booking event: {}", eventPayload);
        
        try {
            // Parse the domain event
            DomainEvent domainEvent = objectMapper.readValue(eventPayload, DomainEvent.class);
            
            // Convert to analytics event
            EventRequest analyticsEvent = domainEvent.toAnalyticsEvent();
            
            // Process the analytics event
            String clientIp = extractClientIp(headers);
            eventService.processEvent(analyticsEvent, clientIp);
            
            logger.info("Successfully processed booking event: {}", domainEvent.getEventId());
            
            // Manually acknowledge the message
            try {
                long deliveryTag = message.getMessageProperties().getDeliveryTag();
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                logger.error("Failed to acknowledge booking event: {}", domainEvent.getEventId(), e);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process booking event: {}", eventPayload, e);
            
            // Reject and requeue the message (up to retry limit)
            try {
                long deliveryTag = message.getMessageProperties().getDeliveryTag();
                boolean requeue = shouldRequeue(headers);
                channel.basicNack(deliveryTag, false, requeue);
            } catch (Exception ackException) {
                logger.error("Failed to nack booking event", ackException);
            }
        }
    }
    
    /**
     * Consume payment domain events
     */
    @RabbitListener(queues = "payment.events.queue", ackMode = "MANUAL")
    public void handlePaymentEvent(String eventPayload, 
             @Header Map<String, Object> headers,
             Message message,
             com.rabbitmq.client.Channel channel) {
        
        logger.debug("Received payment event: {}", eventPayload);
        
        try {
            // Parse the domain event
            DomainEvent domainEvent = objectMapper.readValue(eventPayload, DomainEvent.class);
            
            // Convert to analytics event
            EventRequest analyticsEvent = domainEvent.toAnalyticsEvent();
            
            // Process the analytics event
            String clientIp = extractClientIp(headers);
            eventService.processEvent(analyticsEvent, clientIp);
            
            logger.info("Successfully processed payment event: {}", domainEvent.getEventId());
            
            // Manually acknowledge the message
            try {
                long deliveryTag = message.getMessageProperties().getDeliveryTag();
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                logger.error("Failed to acknowledge payment event: {}", domainEvent.getEventId(), e);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process payment event: {}", eventPayload, e);
            
            // Reject and requeue the message (up to retry limit)
            try {
                long deliveryTag = message.getMessageProperties().getDeliveryTag();
                boolean requeue = shouldRequeue(headers);
                channel.basicNack(deliveryTag, false, requeue);
            } catch (Exception ackException) {
                logger.error("Failed to nack payment event", ackException);
            }
        }
    }
    
    /**
     * Consume user domain events
     */
    @RabbitListener(queues = "user.events.queue", ackMode = "MANUAL")
    public void handleUserEvent(String eventPayload, 
             @Header Map<String, Object> headers,
             Message message,
             com.rabbitmq.client.Channel channel) {
        
        logger.debug("Received user event: {}", eventPayload);
        
        try {
            // Parse the domain event
            DomainEvent domainEvent = objectMapper.readValue(eventPayload, DomainEvent.class);
            
            // Convert to analytics event
            EventRequest analyticsEvent = domainEvent.toAnalyticsEvent();
            
            // Process the analytics event
            String clientIp = extractClientIp(headers);
            eventService.processEvent(analyticsEvent, clientIp);
            
            logger.info("Successfully processed user event: {}", domainEvent.getEventId());
            
            // Manually acknowledge the message
            try {
                long deliveryTag = message.getMessageProperties().getDeliveryTag();
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                logger.error("Failed to acknowledge user event: {}", domainEvent.getEventId(), e);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process user event: {}", eventPayload, e);
            
            // Reject and requeue the message (up to retry limit)
            try {
                long deliveryTag = message.getMessageProperties().getDeliveryTag();
                boolean requeue = shouldRequeue(headers);
                channel.basicNack(deliveryTag, false, requeue);
            } catch (Exception ackException) {
                logger.error("Failed to nack user event", ackException);
            }
        }
    }
    
    /**
     * Consume chat domain events
     */
    @RabbitListener(queues = "chat.events.queue", ackMode = "MANUAL")
    public void handleChatEvent(String eventPayload, 
             @Header Map<String, Object> headers,
             Message message,
             com.rabbitmq.client.Channel channel) {
        
        logger.debug("Received chat event: {}", eventPayload);
        
        try {
            // Parse the domain event
            DomainEvent domainEvent = objectMapper.readValue(eventPayload, DomainEvent.class);
            
            // Convert to analytics event
            EventRequest analyticsEvent = domainEvent.toAnalyticsEvent();
            
            // Process the analytics event
            String clientIp = extractClientIp(headers);
            eventService.processEvent(analyticsEvent, clientIp);
            
            logger.info("Successfully processed chat event: {}", domainEvent.getEventId());
            
            // Manually acknowledge the message
            try {
                long deliveryTag = message.getMessageProperties().getDeliveryTag();
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                logger.error("Failed to acknowledge chat event: {}", domainEvent.getEventId(), e);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process chat event: {}", eventPayload, e);
            
            // Reject and requeue the message (up to retry limit)
            try {
                long deliveryTag = message.getMessageProperties().getDeliveryTag();
                boolean requeue = shouldRequeue(headers);
                channel.basicNack(deliveryTag, false, requeue);
            } catch (Exception ackException) {
                logger.error("Failed to nack chat event", ackException);
            }
        }
    }
    
    /**
     * Extract client IP from message headers
     */
    private String extractClientIp(Map<String, Object> headers) {
        if (headers == null) {
            return "unknown";
        }
        
        // Try to get IP from various header fields
        Object clientIp = headers.get("x-forwarded-for");
        if (clientIp == null) {
            clientIp = headers.get("x-real-ip");
        }
        if (clientIp == null) {
            clientIp = headers.get("client-ip");
        }
        if (clientIp == null) {
            clientIp = headers.get("remote-addr");
        }
        
        return clientIp != null ? clientIp.toString() : "unknown";
    }
    
    /**
     * Determine if message should be requeued based on retry count
     */
    private boolean shouldRequeue(Map<String, Object> headers) {
        if (headers == null) {
            return true;
        }
        
        Object retryCount = headers.get("x-retry-count");
        if (retryCount instanceof Number) {
            int count = ((Number) retryCount).intValue();
            return count < 3; // Max 3 retries
        }
        
        return true; // Default to requeue if no retry count header
    }
}
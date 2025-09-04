package com.hopngo.booking.service;

import com.hopngo.booking.entity.OutboxEvent;
import com.hopngo.booking.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);
    
    private final OutboxEventRepository outboxEventRepository;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                          StreamBridge streamBridge,
                          ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
    }
    
    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();
        
        if (pendingEvents.isEmpty()) {
            return;
        }
        
        logger.info("Publishing {} pending outbox events", pendingEvents.size());
        
        for (OutboxEvent event : pendingEvents) {
            try {
                publishEvent(event);
                markEventAsProcessed(event);
                logger.debug("Successfully published event: {} for aggregate: {}", 
                           event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                markEventAsFailed(event);
                logger.error("Failed to publish event: {} for aggregate: {}. Error: {}", 
                           event.getEventType(), event.getAggregateId(), e.getMessage(), e);
            }
        }
    }
    
    private void publishEvent(OutboxEvent event) {
        try {
            // Create the message payload
            EventMessage message = new EventMessage(
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.getEventData().toString(),
                event.getCreatedAt()
            );
            
            // Determine the routing key based on event type
            String routingKey = determineRoutingKey(event.getEventType());
            
            // Send to RabbitMQ via Spring Cloud Stream
            boolean sent = streamBridge.send(routingKey, message);
            
            if (!sent) {
                throw new RuntimeException("Failed to send message to stream bridge");
            }
            
        } catch (Exception e) {
            logger.error("Error publishing event to RabbitMQ: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private String determineRoutingKey(String eventType) {
        // Map event types to routing keys/bindings
        if (eventType.startsWith("vendor.")) {
            return "vendor-events-out-0";
        } else if (eventType.startsWith("listing.")) {
            return "listing-events-out-0";
        } else if (eventType.startsWith("booking.")) {
            return "booking-events-out-0";
        } else if (eventType.startsWith("review.")) {
            return "review-events-out-0";
        }
        
        // Default routing key
        return "booking-events-out-0";
    }
    
    @Transactional
    public void markEventAsProcessed(OutboxEvent event) {
        event.setStatus(OutboxEvent.OutboxStatus.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        outboxEventRepository.save(event);
    }

    @Transactional
    public void markEventAsFailed(OutboxEvent event) {
        event.setStatus(OutboxEvent.OutboxStatus.FAILED);
        outboxEventRepository.save(event);
    }
    
    @Scheduled(fixedDelay = 3600000) // Run every hour
    @Transactional
    public void retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxEventRepository.findFailedEvents();
        
        if (failedEvents.isEmpty()) {
            return;
        }
        
        logger.info("Retrying {} failed outbox events", failedEvents.size());
        
        for (OutboxEvent event : failedEvents) {
            // Reset status to pending for retry
            event.setStatus(OutboxEvent.OutboxStatus.PENDING);
            outboxEventRepository.save(event);
        }
    }
    
    @Scheduled(fixedDelay = 86400000) // Run daily
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7); // Keep events for 7 days
        outboxEventRepository.deleteOldProcessedEvents(cutoff);
        
        logger.info("Cleaned up old processed outbox events");
    }
    
    @Transactional(readOnly = true)
    public long getPendingEventCount() {
        return outboxEventRepository.countPendingEvents();
    }
    
    @Transactional(readOnly = true)
    public long getFailedEventCount() {
        return outboxEventRepository.countFailedEvents();
    }
}

// Event message class for RabbitMQ
class EventMessage {
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String eventData;
    private LocalDateTime timestamp;
    
    public EventMessage() {}
    
    public EventMessage(String aggregateType, String aggregateId, String eventType, 
                       String eventData, LocalDateTime timestamp) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
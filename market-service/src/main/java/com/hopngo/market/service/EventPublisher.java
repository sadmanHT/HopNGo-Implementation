package com.hopngo.market.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
    
    private final StreamBridge streamBridge;
    
    public EventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }
    
    public void publishContentFlaggedEvent(String contentType, String contentId, String reporterId, String reason) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("contentType", contentType);
            eventData.put("contentId", contentId);
            eventData.put("reporterId", reporterId);
            eventData.put("reason", reason);
            eventData.put("timestamp", LocalDateTime.now().toString());
            eventData.put("service", "market-service");
            
            boolean sent = streamBridge.send("contentFlagged-out-0", eventData);
            
            if (sent) {
                logger.info("Content flagged event published: {} {} by {}", contentType, contentId, reporterId);
            } else {
                logger.error("Failed to publish content flagged event: {} {}", contentType, contentId);
            }
            
        } catch (Exception e) {
            logger.error("Error publishing content flagged event for {} {}", contentType, contentId, e);
        }
    }
    
    public void publishContentModerationEvent(String contentType, String contentId, String action, String reason) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("contentType", contentType);
            eventData.put("contentId", contentId);
            eventData.put("action", action); // APPROVED, REJECTED, REMOVED
            eventData.put("reason", reason);
            eventData.put("timestamp", LocalDateTime.now().toString());
            eventData.put("service", "market-service");
            
            boolean sent = streamBridge.send("contentModeration-out-0", eventData);
            
            if (sent) {
                logger.info("Content moderation event published: {} {} - {}", contentType, contentId, action);
            } else {
                logger.error("Failed to publish content moderation event: {} {}", contentType, contentId);
            }
            
        } catch (Exception e) {
            logger.error("Error publishing content moderation event for {} {}", contentType, contentId, e);
        }
    }
}
package com.hopngo.social.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
    
    @Autowired
    private StreamBridge streamBridge;
    
    public void publishContentFlaggedEvent(String contentType, String contentId, String reporterId, String reason) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("contentType", contentType);
        eventData.put("contentId", contentId);
        eventData.put("reporterUserId", reporterId);
        eventData.put("reason", reason);
        eventData.put("priority", "MEDIUM");
        eventData.put("timestamp", Instant.now().toEpochMilli());
        
        try {
            streamBridge.send("contentFlagged-out-0", eventData);
            logger.info("Published content.flagged event for {} with ID: {}", contentType, contentId);
        } catch (Exception e) {
            logger.error("Failed to publish content.flagged event for {} with ID: {}", contentType, contentId, e);
        }
    }
    
    public void publishContentModerationEvent(String contentType, String contentId, String decision, String reason) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("contentType", contentType);
        eventData.put("contentId", contentId);
        eventData.put("decision", decision);
        eventData.put("reason", reason);
        eventData.put("timestamp", Instant.now().toEpochMilli());
        
        try {
            streamBridge.send("contentModeration-out-0", eventData);
            logger.info("Published content.moderation event for {} with ID: {} - Decision: {}", contentType, contentId, decision);
        } catch (Exception e) {
            logger.error("Failed to publish content.moderation event for {} with ID: {}", contentType, contentId, e);
        }
    }
    
    public void publishPostImpressionEvent(String postId, String userId) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", "post_impression");
        eventData.put("postId", postId);
        eventData.put("userId", userId);
        eventData.put("timestamp", Instant.now().toEpochMilli());
        
        try {
            streamBridge.send("analytics-out-0", eventData);
            logger.debug("Published post impression event for post: {} by user: {}", postId, userId);
        } catch (Exception e) {
            logger.error("Failed to publish post impression event for post: {} by user: {}", postId, userId, e);
        }
    }
    
    public void publishPostDetailViewEvent(String postId, String userId) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", "post_detail_view");
        eventData.put("postId", postId);
        eventData.put("userId", userId);
        eventData.put("timestamp", Instant.now().toEpochMilli());
        
        try {
            streamBridge.send("analytics-out-0", eventData);
            logger.debug("Published post detail view event for post: {} by user: {}", postId, userId);
        } catch (Exception e) {
            logger.error("Failed to publish post detail view event for post: {} by user: {}", postId, userId, e);
        }
    }
}
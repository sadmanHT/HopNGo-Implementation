package com.hopngo.admin.messaging;

import com.hopngo.admin.entity.ModerationItem;
import com.hopngo.admin.repository.ModerationItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class ContentFlaggedEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentFlaggedEventConsumer.class);
    
    @Autowired
    private ModerationItemRepository moderationItemRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @RabbitListener(queues = "${app.rabbitmq.queues.content-flagged:content.flagged}")
    public void handleContentFlaggedEvent(String message) {
        try {
            logger.info("Received content flagged event: {}", message);
            
            // Parse the event message
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            
            // Extract event details
            String eventType = (String) eventData.get("eventType");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) eventData.get("payload");
            
            if (!"CONTENT_FLAGGED".equals(eventType) || payload == null) {
                logger.warn("Invalid event format or type: {}", eventType);
                return;
            }
            
            // Extract payload details
            String contentType = (String) payload.get("contentType");
            String contentId = (String) payload.get("contentId");
            String reportedBy = (String) payload.get("reportedBy");
            String reason = (String) payload.get("reason");
            String priority = (String) payload.get("priority");
            @SuppressWarnings("unchecked")
            Map<String, Object> contentDetails = (Map<String, Object>) payload.get("contentDetails");
            
            // Validate required fields
            if (contentType == null || contentId == null || reportedBy == null || reason == null) {
                logger.error("Missing required fields in content flagged event: {}", payload);
                return;
            }
            
            // Check if moderation item already exists
            if (moderationItemRepository.existsByReferenceId(contentId)) {
                logger.info("Moderation item already exists for content: {}", contentId);
                return;
            }
            
            // Create moderation item
            ModerationItem moderationItem = new ModerationItem();
            moderationItem.setType(mapContentTypeToModerationItemType(contentType));
            moderationItem.setReferenceId(contentId);
            moderationItem.setReportedBy(reportedBy);
            moderationItem.setReason(reason);
            moderationItem.setStatus(ModerationItem.ModerationStatus.OPEN);
            moderationItem.setPriority(mapPriorityToModerationItemPriority(priority));
            moderationItem.setCreatedAt(Instant.now());
            moderationItem.setUpdatedAt(Instant.now());
            
            // Set content details if available
            if (contentDetails != null) {
                try {
                    String contentDetailsJson = objectMapper.writeValueAsString(contentDetails);
                    moderationItem.setContentDetails(contentDetailsJson);
                } catch (Exception e) {
                    logger.warn("Failed to serialize content details: {}", e.getMessage());
                }
            }
            
            // Save moderation item
            moderationItemRepository.save(moderationItem);
            
            logger.info("Created moderation item {} for content {} reported by {}", 
                       moderationItem.getId(), contentId, reportedBy);
            
        } catch (Exception e) {
            logger.error("Failed to process content flagged event: {}", e.getMessage(), e);
            // In a production environment, you might want to:
            // 1. Send to a dead letter queue
            // 2. Implement retry logic
            // 3. Send alerts to monitoring systems
        }
    }
    
    private ModerationItem.ModerationItemType mapContentTypeToModerationItemType(String contentType) {
        if (contentType == null) {
            return ModerationItem.ModerationItemType.POST; // Default fallback
        }
        
        switch (contentType.toUpperCase()) {
            case "POST":
            case "SOCIAL_POST":
                return ModerationItem.ModerationItemType.POST;
            case "COMMENT":
            case "SOCIAL_COMMENT":
                return ModerationItem.ModerationItemType.COMMENT;
            case "LISTING":
            case "MARKET_LISTING":
                return ModerationItem.ModerationItemType.LISTING;
            case "USER":
            case "USER_PROFILE":
                return ModerationItem.ModerationItemType.USER;
            default:
                logger.warn("Unknown content type: {}, defaulting to POST", contentType);
                return ModerationItem.ModerationItemType.POST;
        }
    }
    
    private ModerationItem.Priority mapPriorityToModerationItemPriority(String priority) {
        if (priority == null) {
            return ModerationItem.Priority.MEDIUM; // Default fallback
        }
        
        switch (priority.toUpperCase()) {
            case "HIGH":
            case "URGENT":
                return ModerationItem.Priority.HIGH;
            case "MEDIUM":
            case "NORMAL":
                return ModerationItem.Priority.MEDIUM;
            case "LOW":
                return ModerationItem.Priority.LOW;
            default:
                logger.warn("Unknown priority: {}, defaulting to MEDIUM", priority);
                return ModerationItem.Priority.MEDIUM;
        }
    }
}
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
public class ReviewFlaggedEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewFlaggedEventConsumer.class);
    
    @Autowired
    private ModerationItemRepository moderationItemRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @RabbitListener(queues = "${app.rabbitmq.queues.review-flagged:review.flagged}")
    public void handleReviewEvent(String message) {
        try {
            logger.info("Received review event: {}", message);
            
            // Parse the event message
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            
            // Extract event details
            String eventType = (String) eventData.get("eventType");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) eventData.get("payload");
            
            if (payload == null) {
                logger.warn("Invalid event format - missing payload: {}", eventType);
                return;
            }
            
            if ("REVIEW_FLAGGED".equals(eventType)) {
                handleReviewFlaggedEvent(payload);
            } else if ("REVIEW_FLAG_RESOLVED".equals(eventType)) {
                handleReviewFlagResolvedEvent(payload);
            } else {
                logger.warn("Unknown event type: {}", eventType);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process review event: {}", e.getMessage(), e);
        }
    }
    
    private void handleReviewFlaggedEvent(Map<String, Object> payload) {
        try {
            // Extract payload details
            Object reviewIdObj = payload.get("reviewId");
            String reportedBy = (String) payload.get("reportedBy");
            String reason = (String) payload.get("reason");
            String priority = (String) payload.get("priority");
            @SuppressWarnings("unchecked")
            Map<String, Object> reviewDetails = (Map<String, Object>) payload.get("reviewDetails");
            
            // Validate required fields
            if (reviewIdObj == null || reportedBy == null || reason == null) {
                logger.error("Missing required fields in review flagged event: {}", payload);
                return;
            }
            
            Long reviewId = convertToLong(reviewIdObj);
            if (reviewId == null) {
                logger.error("Invalid reviewId format: {}", reviewIdObj);
                return;
            }
            
            // Check if moderation item already exists
            ModerationItem existingItem = moderationItemRepository.findByTypeAndRefId(
                ModerationItem.ModerationItemType.REVIEW, reviewId);
            
            if (existingItem != null) {
                logger.info("Moderation item already exists for review: {}", reviewId);
                return;
            }
            
            // Create moderation item
            ModerationItem moderationItem = new ModerationItem();
            moderationItem.setType(ModerationItem.ModerationItemType.REVIEW);
            moderationItem.setRefId(reviewId);
            moderationItem.setReporterUserId(Long.valueOf(reportedBy));
            moderationItem.setReason(reason);
            moderationItem.setStatus(ModerationItem.ModerationStatus.OPEN);
            moderationItem.setPriority(mapPriorityToModerationItemPriority(priority));
            moderationItem.setCreatedAt(Instant.now());
            moderationItem.setUpdatedAt(Instant.now());
            
            // Set review details if available
            if (reviewDetails != null) {
                try {
                    String reviewDetailsJson = objectMapper.writeValueAsString(reviewDetails);
                    moderationItem.setContentDetails(reviewDetailsJson);
                } catch (Exception e) {
                    logger.warn("Failed to serialize review details: {}", e.getMessage());
                }
            }
            
            // Save moderation item
            moderationItemRepository.save(moderationItem);
            
            logger.info("Created moderation item {} for review {} reported by {}", 
                       moderationItem.getId(), reviewId, reportedBy);
            
        } catch (Exception e) {
            logger.error("Failed to process review flagged event: {}", e.getMessage(), e);
        }
    }
    
    private void handleReviewFlagResolvedEvent(Map<String, Object> payload) {
        try {
            // Extract payload details
            Object reviewIdObj = payload.get("reviewId");
            String resolvedBy = (String) payload.get("resolvedBy");
            String resolution = (String) payload.get("resolution");
            String decisionNote = (String) payload.get("decisionNote");
            
            // Validate required fields
            if (reviewIdObj == null || resolvedBy == null || resolution == null) {
                logger.error("Missing required fields in review flag resolved event: {}", payload);
                return;
            }
            
            Long reviewId = convertToLong(reviewIdObj);
            if (reviewId == null) {
                logger.error("Invalid reviewId format: {}", reviewIdObj);
                return;
            }
            
            // Find existing moderation item
            ModerationItem moderationItem = moderationItemRepository.findByTypeAndRefId(
                ModerationItem.ModerationItemType.REVIEW, reviewId);
            
            if (moderationItem == null) {
                logger.warn("No moderation item found for review: {}", reviewId);
                return;
            }
            
            // Update moderation item status based on resolution
            ModerationItem.ModerationStatus newStatus = mapResolutionToStatus(resolution);
            moderationItem.setStatus(newStatus);
            moderationItem.setAssigneeUserId(Long.valueOf(resolvedBy));
            moderationItem.setDecisionNote(decisionNote);
            moderationItem.setUpdatedAt(Instant.now());
            
            // Save updated moderation item
            moderationItemRepository.save(moderationItem);
            
            logger.info("Updated moderation item {} for review {} with resolution: {}", 
                       moderationItem.getId(), reviewId, resolution);
            
        } catch (Exception e) {
            logger.error("Failed to process review flag resolved event: {}", e.getMessage(), e);
        }
    }
    
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Long) {
            return (Long) value;
        }
        
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        
        if (value instanceof String) {
            try {
                return Long.valueOf((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
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
            case "CRITICAL":
                return ModerationItem.Priority.CRITICAL;
            default:
                logger.warn("Unknown priority: {}, defaulting to MEDIUM", priority);
                return ModerationItem.Priority.MEDIUM;
        }
    }
    
    private ModerationItem.ModerationStatus mapResolutionToStatus(String resolution) {
        if (resolution == null) {
            return ModerationItem.ModerationStatus.PENDING;
        }
        
        switch (resolution.toUpperCase()) {
            case "RESOLVED":
            case "APPROVED":
                return ModerationItem.ModerationStatus.APPROVED;
            case "DISMISSED":
            case "REJECTED":
                return ModerationItem.ModerationStatus.REJECTED;
            case "REMOVED":
                return ModerationItem.ModerationStatus.REMOVED;
            default:
                logger.warn("Unknown resolution: {}, defaulting to PENDING", resolution);
                return ModerationItem.ModerationStatus.PENDING;
        }
    }
}
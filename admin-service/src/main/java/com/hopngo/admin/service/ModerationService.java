package com.hopngo.admin.service;

import com.hopngo.admin.dto.ModerationDecisionRequest;
import com.hopngo.admin.dto.ModerationItemResponse;
import com.hopngo.admin.entity.ModerationItem;
import com.hopngo.admin.entity.ModerationItem.ModerationStatus;
import com.hopngo.admin.entity.ModerationItem.ModerationItemType;
import com.hopngo.admin.repository.ModerationItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ModerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModerationService.class);
    
    private final ModerationItemRepository moderationItemRepository;
    private final AdminAuditService auditService;
    private final IntegrationService integrationService;
    
    public ModerationService(ModerationItemRepository moderationItemRepository,
                           AdminAuditService auditService,
                           IntegrationService integrationService) {
        this.moderationItemRepository = moderationItemRepository;
        this.auditService = auditService;
        this.integrationService = integrationService;
    }
    
    @Transactional(readOnly = true)
    public Page<ModerationItemResponse> getModerationItems(ModerationStatus status,
                                                         ModerationItemType type,
                                                         String assigneeUserId,
                                                         Pageable pageable) {
        Page<ModerationItem> items;
        Long assigneeUserIdLong = assigneeUserId != null ? Long.valueOf(assigneeUserId) : null;
        
        if (status != null && type != null && assigneeUserIdLong != null) {
            items = moderationItemRepository.findByStatusAndTypeAndAssigneeUserId(status, type, assigneeUserIdLong, pageable);
        } else if (status != null && type != null) {
            items = moderationItemRepository.findByStatusAndType(status, type, pageable);
        } else if (status != null) {
            items = moderationItemRepository.findByStatus(status, pageable);
        } else if (type != null) {
            items = moderationItemRepository.findByType(type, pageable);
        } else if (assigneeUserIdLong != null) {
            items = moderationItemRepository.findByAssigneeUserId(assigneeUserIdLong, pageable);
        } else {
            items = moderationItemRepository.findAll(pageable);
        }
        
        return items.map(ModerationItemResponse::new);
    }
    
    public ModerationItemResponse approveModerationItem(Long itemId, 
                                                       ModerationDecisionRequest request, 
                                                       String adminUserId) {
        ModerationItem item = getModerationItemById(itemId);
        
        if (item.getStatus() != ModerationStatus.OPEN) {
            throw new IllegalStateException("Only open items can be approved");
        }
        
        item.setStatus(ModerationStatus.APPROVED);
        item.setDecisionNote(request.getDecisionNote());
        item.setAssigneeUserId(Long.valueOf(adminUserId));
        item.setUpdatedAt(Instant.now());
        
        ModerationItem savedItem = moderationItemRepository.save(item);
        
        // Log audit
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("itemId", itemId);
        auditDetails.put("previousStatus", ModerationStatus.OPEN.name());
        auditDetails.put("newStatus", ModerationStatus.APPROVED.name());
        auditDetails.put("decisionNote", request.getDecisionNote());
        auditDetails.put("itemType", item.getType().name());
        auditDetails.put("refId", item.getRefId());
        
        auditService.logAction(Long.valueOf(adminUserId), "APPROVE_MODERATION", "MODERATION_ITEM", itemId, auditDetails);
        
        logger.info("Moderation item {} approved by admin {}", itemId, adminUserId);
        
        return new ModerationItemResponse(savedItem);
    }
    
    public ModerationItemResponse rejectModerationItem(Long itemId, 
                                                      ModerationDecisionRequest request, 
                                                      String adminUserId) {
        ModerationItem item = getModerationItemById(itemId);
        
        if (item.getStatus() != ModerationStatus.OPEN) {
            throw new IllegalStateException("Only open items can be rejected");
        }
        
        item.setStatus(ModerationStatus.REJECTED);
        item.setDecisionNote(request.getDecisionNote());
        item.setAssigneeUserId(Long.valueOf(adminUserId));
        item.setUpdatedAt(Instant.now());
        
        ModerationItem savedItem = moderationItemRepository.save(item);
        
        // Log audit
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("itemId", itemId);
        auditDetails.put("previousStatus", ModerationStatus.OPEN.name());
        auditDetails.put("newStatus", ModerationStatus.REJECTED.name());
        auditDetails.put("decisionNote", request.getDecisionNote());
        auditDetails.put("itemType", item.getType().name());
        auditDetails.put("refId", item.getRefId());
        
        auditService.logAction(Long.valueOf(adminUserId), "REJECT_MODERATION", "MODERATION_ITEM", itemId, auditDetails);
        
        logger.info("Moderation item {} rejected by admin {}", itemId, adminUserId);
        
        return new ModerationItemResponse(savedItem);
    }
    
    public ModerationItemResponse removeModerationItem(Long itemId, 
                                                      ModerationDecisionRequest request, 
                                                      String adminUserId) {
        ModerationItem item = getModerationItemById(itemId);
        
        if (item.getStatus() != ModerationStatus.OPEN) {
            throw new IllegalStateException("Only open items can be removed");
        }
        
        // Call integration service to remove the actual content
        try {
            switch (item.getType()) {
                case POST:
                    integrationService.removeSocialPost(item.getRefId(), request.getDecisionNote());
                    break;
                case COMMENT:
                    integrationService.removeSocialComment(item.getRefId(), request.getDecisionNote());
                    break;
                case LISTING:
                    integrationService.removeMarketListing(item.getRefId(), request.getDecisionNote());
                    break;
                case TRIP:
                    integrationService.removeBookingTrip(item.getRefId(), request.getDecisionNote());
                    break;
                default:
                    // Fallback to generic method for other types
                    boolean removed = integrationService.removeContent(item.getType(), item.getRefId());
                    if (!removed) {
                        throw new RuntimeException("Failed to remove content from source service");
                    }
            }
        } catch (Exception e) {
            logger.error("Failed to remove content for item {}: {}", itemId, e.getMessage());
            throw new RuntimeException("Failed to remove content from source service", e);
        }
        
        item.setStatus(ModerationStatus.REMOVED);
        item.setDecisionNote(request.getDecisionNote());
        item.setAssigneeUserId(Long.valueOf(adminUserId));
        item.setUpdatedAt(Instant.now());

        ModerationItem savedItem = moderationItemRepository.save(item);

        // Log audit
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("itemId", itemId);
        auditDetails.put("previousStatus", ModerationStatus.OPEN.name());
        auditDetails.put("newStatus", ModerationStatus.REMOVED.name());
        auditDetails.put("decisionNote", request.getDecisionNote());
        auditDetails.put("itemType", item.getType().name());
        auditDetails.put("refId", item.getRefId());
        auditDetails.put("contentRemoved", true);
        
        auditService.logAction(Long.valueOf(adminUserId), "REMOVE_CONTENT", "MODERATION_ITEM", itemId, auditDetails);
        
        logger.info("Moderation item {} removed by admin {}", itemId, adminUserId);
        
        return new ModerationItemResponse(savedItem);
    }
    
    public void banUser(String userId, ModerationDecisionRequest request, String adminUserId) {
        // Call integration service to ban the user
        boolean banned = integrationService.banUser(userId, request.getDecisionNote());
        
        if (!banned) {
            throw new RuntimeException("Failed to ban user in auth service");
        }
        
        // Log audit
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("bannedUserId", userId);
        auditDetails.put("reason", request.getDecisionNote());
        auditDetails.put("banTimestamp", Instant.now());
        
        auditService.logAction(Long.valueOf(adminUserId), "BAN_USER", "USER", Long.valueOf(userId), auditDetails);
        
        logger.info("User {} banned by admin {}", userId, adminUserId);
    }
    
    public ModerationItem createModerationItem(ModerationItemType type, String refId, String reason, String reporterUserId) {
        // Check if item already exists
        ModerationItem existing = moderationItemRepository.findByTypeAndRefId(type, Long.valueOf(refId));
        if (existing != null) {
            logger.warn("Moderation item already exists for type {} and refId {}", type, refId);
            return existing;
        }
        
        ModerationItem item = new ModerationItem();
        item.setType(type);
        item.setRefId(Long.valueOf(refId));
        item.setStatus(ModerationStatus.OPEN);
        item.setReason(reason);
        item.setReporterUserId(Long.valueOf(reporterUserId));
        item.setCreatedAt(Instant.now());
        item.setUpdatedAt(Instant.now());
        
        ModerationItem savedItem = moderationItemRepository.save(item);
        
        logger.info("Created moderation item {} for type {} and refId {}", savedItem.getId(), type, refId);
        
        return savedItem;
    }
    
    private ModerationItem getModerationItemById(Long itemId) {
        return moderationItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Moderation item not found: " + itemId));
    }
}
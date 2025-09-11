package com.hopngo.admin.service;

import com.hopngo.admin.dto.AdminAuditResponse;
import com.hopngo.admin.entity.AdminAudit;
import com.hopngo.admin.repository.AdminAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminAuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminAuditService.class);
    
    private final AdminAuditRepository auditRepository;
    
    public AdminAuditService(AdminAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }
    
    public void logAction(Long actorUserId, String action, String targetType, Long targetId, Map<String, Object> details) {
        AdminAudit audit = new AdminAudit();
        audit.setActorUserId(actorUserId);
        audit.setAction(action);
        audit.setTargetType(targetType);
        audit.setTargetId(targetId);
        audit.setDetails(details);
        audit.setCreatedAt(Instant.now());
        
        auditRepository.save(audit);
        
        logger.debug("Logged admin action: {} by {} on {}:{}", action, actorUserId, targetType, targetId);
    }
    
    public void logAction(String actorUserId, String action, String targetType, String targetId, Map<String, Object> details) {
        logAction(Long.valueOf(actorUserId), action, targetType, Long.valueOf(targetId), details);
    }
    
    @Transactional(readOnly = true)
    public Page<AdminAuditResponse> getAuditLog(String actorUserId,
                                              String targetType,
                                              String targetId,
                                              String action,
                                              Instant startDate,
                                              Instant endDate,
                                              Pageable pageable) {
        Page<AdminAudit> audits;
        
        Long actorUserIdLong = actorUserId != null ? Long.valueOf(actorUserId) : null;
        Long targetIdLong = targetId != null ? Long.valueOf(targetId) : null;
        
        if (actorUserIdLong != null || targetType != null || targetIdLong != null || action != null || startDate != null || endDate != null) {
            audits = auditRepository.findByFilters(actorUserIdLong, targetType, targetIdLong, action, startDate, endDate, pageable);
        } else {
            audits = auditRepository.findAll(pageable);
        }
        
        return audits.map(AdminAuditResponse::new);
    }
    
    @Transactional(readOnly = true)
    public List<AdminAuditResponse> getRecentAuditsByTarget(String targetType, String targetId, int limit) {
        Long targetIdLong = Long.valueOf(targetId);
        List<AdminAudit> audits = auditRepository.findRecentByTarget(targetType, targetIdLong, limit);
        return audits.stream()
                .map(AdminAuditResponse::new)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public long countAuditsByActor(String actorUserId, Instant startDate, Instant endDate) {
        Long actorUserIdLong = Long.valueOf(actorUserId);
        return auditRepository.countByActorUserIdAndCreatedAtBetween(actorUserIdLong, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public long countAuditsByAction(String action, Instant startDate, Instant endDate) {
        return auditRepository.countByActionAndCreatedAtBetween(action, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public long countAuditsByTarget(String targetType, String targetId, Instant startDate, Instant endDate) {
        Long targetIdLong = Long.valueOf(targetId);
        return auditRepository.countByTargetTypeAndTargetIdAndCreatedAtBetween(targetType, targetIdLong, startDate, endDate);
    }
}
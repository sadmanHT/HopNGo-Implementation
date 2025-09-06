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
    
    public void logAction(String actorUserId, String action, String targetType, String targetId, Map<String, Object> details) {
        AdminAudit audit = new AdminAudit();
        audit.setActorUserId(actorUserId);
        audit.setAction(action);
        audit.setTargetType(targetType);
        audit.setTargetId(targetId);
        audit.setDetails(details);
        audit.setCreatedAt(LocalDateTime.now());
        
        auditRepository.save(audit);
        
        logger.debug("Logged admin action: {} by {} on {}:{}", action, actorUserId, targetType, targetId);
    }
    
    @Transactional(readOnly = true)
    public Page<AdminAuditResponse> getAuditLog(String actorUserId,
                                              String targetType,
                                              String targetId,
                                              String action,
                                              LocalDateTime startDate,
                                              LocalDateTime endDate,
                                              Pageable pageable) {
        Page<AdminAudit> audits;
        
        if (actorUserId != null || targetType != null || targetId != null || action != null || startDate != null || endDate != null) {
            audits = auditRepository.findByFilters(actorUserId, targetType, targetId, action, startDate, endDate, pageable);
        } else {
            audits = auditRepository.findAll(pageable);
        }
        
        return audits.map(AdminAuditResponse::new);
    }
    
    @Transactional(readOnly = true)
    public List<AdminAuditResponse> getRecentAuditsByTarget(String targetType, String targetId, int limit) {
        List<AdminAudit> audits = auditRepository.findRecentByTarget(targetType, targetId, limit);
        return audits.stream()
                .map(AdminAuditResponse::new)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public long countAuditsByActor(String actorUserId, LocalDateTime startDate, LocalDateTime endDate) {
        return auditRepository.countByActorUserIdAndCreatedAtBetween(actorUserId, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public long countAuditsByAction(String action, LocalDateTime startDate, LocalDateTime endDate) {
        return auditRepository.countByActionAndCreatedAtBetween(action, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public long countAuditsByTarget(String targetType, String targetId, LocalDateTime startDate, LocalDateTime endDate) {
        return auditRepository.countByTargetTypeAndTargetIdAndCreatedAtBetween(targetType, targetId, startDate, endDate);
    }
}
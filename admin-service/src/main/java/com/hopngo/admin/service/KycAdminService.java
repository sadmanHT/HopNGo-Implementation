package com.hopngo.admin.service;

import com.hopngo.admin.dto.KycDecisionRequest;
import com.hopngo.admin.dto.KycRequestDto;
import com.hopngo.admin.entity.AdminAudit;
import com.hopngo.admin.repository.AdminAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KycAdminService {
    
    private static final Logger logger = LoggerFactory.getLogger(KycAdminService.class);
    
    private final RestTemplate restTemplate;
    private final AdminAuditRepository adminAuditRepository;
    
    @Value("${integration.auth-service.url}")
    private String authServiceUrl;
    
    public KycAdminService(RestTemplate restTemplate, AdminAuditRepository adminAuditRepository) {
        this.restTemplate = restTemplate;
        this.adminAuditRepository = adminAuditRepository;
    }
    
    /**
     * Get all pending KYC requests with pagination
     */
    public Page<KycRequestDto> getPendingKycRequests(Pageable pageable) {
        try {
            String url = authServiceUrl + "/internal/kyc/pending?page=" + pageable.getPageNumber() + 
                        "&size=" + pageable.getPageSize();
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
                
                List<KycRequestDto> kycRequests = content.stream()
                    .map(this::mapToKycRequestDto)
                    .toList();
                
                int totalElements = ((Number) body.get("totalElements")).intValue();
                
                return new PageImpl<>(kycRequests, pageable, totalElements);
            }
            
            logger.warn("Failed to fetch pending KYC requests: {}", response.getStatusCode());
            return Page.empty(pageable);
            
        } catch (Exception e) {
            logger.error("Error fetching pending KYC requests", e);
            return Page.empty(pageable);
        }
    }
    
    /**
     * Get KYC request by ID
     */
    public KycRequestDto getKycRequestById(Long requestId) {
        try {
            String url = authServiceUrl + "/internal/kyc/" + requestId;
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return mapToKycRequestDto(response.getBody());
            }
            
            logger.warn("Failed to fetch KYC request {}: {}", requestId, response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            logger.error("Error fetching KYC request {}", requestId, e);
            return null;
        }
    }
    
    /**
     * Process KYC decision (approve/reject)
     */
    public boolean processKycDecision(Long requestId, KycDecisionRequest decision, Long adminUserId) {
        try {
            String url = authServiceUrl + "/internal/kyc/" + requestId + "/decision";
            
            HttpHeaders headers = createServiceHeaders();
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("decision", decision.getDecision());
            requestBody.put("rejectionReason", decision.getRejectionReason());
            requestBody.put("adminNotes", decision.getAdminNotes());
            requestBody.put("adminUserId", adminUserId);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Void.class
            );
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            
            if (success) {
                // Log audit trail
                logKycDecisionAudit(adminUserId, requestId, decision);
                logger.info("Successfully processed KYC decision for request {} by admin {}", requestId, adminUserId);
            } else {
                logger.warn("Failed to process KYC decision for request {}: {}", requestId, response.getStatusCode());
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error processing KYC decision for request {}", requestId, e);
            return false;
        }
    }
    
    /**
     * Get KYC statistics
     */
    public Map<String, Object> getKycStatistics() {
        try {
            String url = authServiceUrl + "/internal/kyc/statistics";
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            
            logger.warn("Failed to fetch KYC statistics: {}", response.getStatusCode());
            return Map.of();
            
        } catch (Exception e) {
            logger.error("Error fetching KYC statistics", e);
            return Map.of();
        }
    }
    
    /**
     * Search KYC requests by user email or name
     */
    public Page<KycRequestDto> searchKycRequests(String query, Pageable pageable) {
        try {
            String url = authServiceUrl + "/internal/kyc/search?query=" + query + 
                        "&page=" + pageable.getPageNumber() + 
                        "&size=" + pageable.getPageSize();
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
                
                List<KycRequestDto> kycRequests = content.stream()
                    .map(this::mapToKycRequestDto)
                    .toList();
                
                int totalElements = ((Number) body.get("totalElements")).intValue();
                
                return new PageImpl<>(kycRequests, pageable, totalElements);
            }
            
            logger.warn("Failed to search KYC requests: {}", response.getStatusCode());
            return Page.empty(pageable);
            
        } catch (Exception e) {
            logger.error("Error searching KYC requests with query: {}", query, e);
            return Page.empty(pageable);
        }
    }
    
    // Helper methods
    
    private HttpHeaders createServiceHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // TODO: Add service-to-service authentication header
        return headers;
    }
    
    private KycRequestDto mapToKycRequestDto(Map<String, Object> data) {
        KycRequestDto dto = new KycRequestDto();
        
        dto.setId(((Number) data.get("id")).longValue());
        dto.setUserId(((Number) data.get("userId")).longValue());
        dto.setStatus((String) data.get("status"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) data.get("fields");
        dto.setFields(fields);
        
        // Parse timestamps
        if (data.get("createdAt") != null) {
            dto.setCreatedAt(java.time.Instant.parse((String) data.get("createdAt")));
        }
        if (data.get("updatedAt") != null) {
            dto.setUpdatedAt(java.time.Instant.parse((String) data.get("updatedAt")));
        }
        
        // Additional user info if available
        dto.setUserEmail((String) data.get("userEmail"));
        dto.setUserName((String) data.get("userName"));
        dto.setVerificationStatus((Boolean) data.get("verificationStatus"));
        
        return dto;
    }
    
    private void logKycDecisionAudit(Long adminUserId, Long requestId, KycDecisionRequest decision) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("decision", decision.getDecision());
            auditDetails.put("rejectionReason", decision.getRejectionReason());
            auditDetails.put("adminNotes", decision.getAdminNotes());
            auditDetails.put("timestamp", java.time.Instant.now().toString());
            
            AdminAudit audit = new AdminAudit(
                adminUserId,
                "KYC_DECISION",
                "KYC_REQUEST",
                requestId,
                auditDetails
            );
            
            adminAuditRepository.save(audit);
            
        } catch (Exception e) {
            logger.error("Failed to log KYC decision audit for request {}", requestId, e);
        }
    }
}
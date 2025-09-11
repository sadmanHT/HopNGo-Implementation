package com.hopngo.admin.service;

import com.hopngo.admin.dto.AdminAuditResponse;
import com.hopngo.admin.entity.AdminAudit;
import com.hopngo.admin.repository.AdminAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    @Mock
    private AdminAuditRepository adminAuditRepository;

    @InjectMocks
    private AdminAuditService adminAuditService;

    private AdminAudit testAudit;

    @BeforeEach
    void setUp() {
        testAudit = new AdminAudit();
        testAudit.setId(1L);
        testAudit.setActorUserId(123L);
        testAudit.setAction("APPROVE_MODERATION_ITEM");
        testAudit.setTargetType("ModerationItem");
        testAudit.setTargetId(456L);
        Map<String, Object> details = new HashMap<>();
        details.put("itemId", 123L);
        details.put("status", "APPROVED");
        testAudit.setDetails(details);
        testAudit.setCreatedAt(java.time.Instant.now());
    }

    @Test
    void logAction_ShouldSaveAuditRecord() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("itemId", 123L);
        details.put("status", "APPROVED");
        
        when(adminAuditRepository.save(any(AdminAudit.class))).thenReturn(testAudit);

        // When
        adminAuditService.logAction(123L, "APPROVE_MODERATION_ITEM", "MODERATION_ITEM", 456L, details);

        // Then
        verify(adminAuditRepository).save(argThat(audit -> 
            audit.getActorUserId().equals(123L) &&
            audit.getAction().equals("APPROVE_MODERATION_ITEM") &&
            audit.getTargetType().equals("MODERATION_ITEM") &&
            audit.getTargetId().equals(456L) &&
            audit.getDetails() != null &&
            audit.getDetails().containsKey("itemId") &&
            audit.getDetails().containsValue("APPROVED")
        ));
    }

    @Test
    void logAction_WithNullDetails_ShouldSaveAuditRecordWithEmptyDetails() {
        // Given
        when(adminAuditRepository.save(any(AdminAudit.class))).thenReturn(testAudit);

        // When
        adminAuditService.logAction(456L, "BAN_USER", "USER", 456L, null);

        // Then
        verify(adminAuditRepository).save(argThat(audit -> 
            audit.getActorUserId().equals(456L) &&
            audit.getAction().equals("BAN_USER") &&
            audit.getTargetType().equals("USER") &&
            audit.getTargetId().equals(456L) &&
            audit.getDetails() == null
        ));
    }

    @Test
    void testGetUserAuditHistory() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        Page<AdminAudit> page = new PageImpl<>(List.of(testAudit));
        when(adminAuditRepository.findByFilters(
            123L, null, null, null, null, null, pageable))
            .thenReturn(page);

        // When
        Page<AdminAuditResponse> result = adminAuditService.getAuditLog(
            "123", null, null, null, null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(adminAuditRepository).findByFilters(
            123L, null, null, null, null, null, pageable);
    }

    @Test
    void testGetActionHistory() {
        // Given
        Pageable pageable = PageRequest.of(0, 100);
        Page<AdminAudit> page = new PageImpl<>(List.of(testAudit));
        when(adminAuditRepository.findByFilters(
            null, null, null, "APPROVE_MODERATION_ITEM", null, null, pageable))
            .thenReturn(page);

        // When
        Page<AdminAuditResponse> result = adminAuditService.getAuditLog(
            null, null, null, "APPROVE_MODERATION_ITEM", null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        AdminAuditResponse response = result.getContent().get(0);
        assertThat(response.getAction()).isEqualTo("APPROVE_MODERATION_ITEM");
        verify(adminAuditRepository).findByFilters(
            null, null, null, "APPROVE_MODERATION_ITEM", null, null, pageable);
    }

    @Test
    void getAuditEntries_WithFilters_ShouldCallRepositoryWithCorrectParameters() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        Page<AdminAudit> page = new PageImpl<>(List.of(testAudit));
        
        when(adminAuditRepository.findByFilters(
            123L, null, null, "APPROVE_MODERATION_ITEM", 
            startDate.atZone(java.time.ZoneId.systemDefault()).toInstant(), 
            endDate.atZone(java.time.ZoneId.systemDefault()).toInstant(), pageable))
            .thenReturn(page);

        // When
        Page<AdminAuditResponse> result = adminAuditService.getAuditLog(
            "123", null, null, "APPROVE_MODERATION_ITEM", 
            startDate.atZone(java.time.ZoneId.systemDefault()).toInstant(), 
            endDate.atZone(java.time.ZoneId.systemDefault()).toInstant(), pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(adminAuditRepository).findByFilters(
            123L, null, null, "APPROVE_MODERATION_ITEM", 
            startDate.atZone(java.time.ZoneId.systemDefault()).toInstant(), 
            endDate.atZone(java.time.ZoneId.systemDefault()).toInstant(), pageable);
    }

    @Test
    void getAuditEntries_WithUserIdOnly_ShouldCallRepositoryWithUserIdFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AdminAudit> page = new PageImpl<>(List.of(testAudit));
        
        when(adminAuditRepository.findByFilters(
            123L, null, null, null, null, null, pageable))
            .thenReturn(page);

        // When
        Page<AdminAuditResponse> result = adminAuditService.getAuditLog(
            "123", null, null, null, null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(adminAuditRepository).findByFilters(
            123L, null, null, null, null, null, pageable);
    }

    @Test
    void getAuditEntries_WithActionOnly_ShouldCallRepositoryWithActionFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AdminAudit> page = new PageImpl<>(List.of(testAudit));
        
        when(adminAuditRepository.findByFilters(
            null, null, null, "APPROVE_MODERATION_ITEM", null, null, pageable))
            .thenReturn(page);

        // When
        Page<AdminAuditResponse> result = adminAuditService.getAuditLog(
            null, null, null, "APPROVE_MODERATION_ITEM", null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(adminAuditRepository).findByFilters(
            null, null, null, "APPROVE_MODERATION_ITEM", null, null, pageable);
    }

    @Test
    void getAuditEntries_WithDateRangeOnly_ShouldCallRepositoryWithDateFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        Page<AdminAudit> page = new PageImpl<>(List.of(testAudit));
        
        when(adminAuditRepository.findByFilters(
            null, null, null, null, 
            startDate.atZone(java.time.ZoneId.systemDefault()).toInstant(),
            endDate.atZone(java.time.ZoneId.systemDefault()).toInstant(), pageable))
            .thenReturn(page);

        // When
        Page<AdminAuditResponse> result = adminAuditService.getAuditLog(
            null, null, null, null,
            startDate.atZone(java.time.ZoneId.systemDefault()).toInstant(),
            endDate.atZone(java.time.ZoneId.systemDefault()).toInstant(), pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(adminAuditRepository).findByFilters(
            null, null, null, null,
            startDate.atZone(java.time.ZoneId.systemDefault()).toInstant(),
            endDate.atZone(java.time.ZoneId.systemDefault()).toInstant(), pageable);
    }
}
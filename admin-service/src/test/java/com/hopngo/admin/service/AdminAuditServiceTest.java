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
        testAudit.setUserId("admin123");
        testAudit.setAction("APPROVE_MODERATION_ITEM");
        testAudit.setResourceType("ModerationItem");
        testAudit.setResourceId("item123");
        testAudit.setDetails("{\"itemId\":123,\"status\":\"APPROVED\"}");
        testAudit.setTimestamp(LocalDateTime.now());
    }

    @Test
    void logAction_ShouldSaveAuditRecord() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("itemId", 123L);
        details.put("status", "APPROVED");
        
        when(adminAuditRepository.save(any(AdminAudit.class))).thenReturn(testAudit);

        // When
        adminAuditService.logAction("admin123", "APPROVE_MODERATION_ITEM", details);

        // Then
        verify(adminAuditRepository).save(argThat(audit -> 
            audit.getUserId().equals("admin123") &&
            audit.getAction().equals("APPROVE_MODERATION_ITEM") &&
            audit.getDetails().contains("itemId") &&
            audit.getDetails().contains("APPROVED")
        ));
    }

    @Test
    void logAction_WithNullDetails_ShouldSaveAuditRecordWithEmptyDetails() {
        // Given
        when(adminAuditRepository.save(any(AdminAudit.class))).thenReturn(testAudit);

        // When
        adminAuditService.logAction("admin123", "BAN_USER", null);

        // Then
        verify(adminAuditRepository).save(argThat(audit -> 
            audit.getUserId().equals("admin123") &&
            audit.getAction().equals("BAN_USER") &&
            audit.getDetails().equals("{}")
        ));
    }

    @Test
    void getAuditEntries_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AdminAudit> page = new PageImpl<>(List.of(testAudit));
        when(adminAuditRepository.findAllByOrderByTimestampDesc(pageable)).thenReturn(page);

        // When
        Page<AdminAuditResponse> result = adminAuditService.getAuditEntries(
            null, null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        AdminAuditResponse response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo("admin123");
        assertThat(response.getAction()).isEqualTo("APPROVE_MODERATION_ITEM");
        assertThat(response.getResourceType()).isEqualTo("ModerationItem");
        assertThat(response.getResourceId()).isEqualTo("item123");
    }

    @Test
    void getAuditEntries_WithFilters_ShouldCallRepositoryWithCorrectParameters() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        Page<AdminAudit> page = new PageImpl<>(List.of(testAudit));
        
        when(adminAuditRepository.findByUserIdAndActionAndTimestampBetweenOrderByTimestampDesc(
            "admin123", "APPROVE_MODERATION_ITEM", startDate, endDate, pageable))
            .thenReturn(page);

        // When
        Page<AdminAuditResponse> result = adminAuditService.getAuditEntries(
            "admin123", "APPROVE_MODERATION_ITEM", startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(adminAuditRepository).findByUserIdAndActionAndTimestampBetweenOrderByTimestampDesc(
            "admin123", "APPROVE_MODERATION_ITEM", startDate, endDate, pageable);
    }

    @Test
    void getAuditEntries_WithUserIdOnly_ShouldCallRepositoryWithUserIdFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AdminAudit> page = new PageImpl<>(List.of(testAudit));
        
        when(adminAuditRepository.findByUserIdOrderByTimestampDesc("admin123", pageable))
            .thenReturn(page);

        // When
        Page<AdminAuditResponse> result = adminAuditService.getAuditEntries(
            "admin123", null, null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(adminAuditRepository).findByUserIdOrderByTimestampDesc("admin123", pageable);
    }

    @Test
    void getAuditEntries_WithActionOnly_ShouldCallRepositoryWithActionFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AdminAudit> page = new PageImpl<>(List.of(testAudit));
        
        when(adminAuditRepository.findByActionOrderByTimestampDesc("APPROVE_MODERATION_ITEM", pageable))
            .thenReturn(page);

        // When
        Page<AdminAuditResponse> result = adminAuditService.getAuditEntries(
            null, "APPROVE_MODERATION_ITEM", null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(adminAuditRepository).findByActionOrderByTimestampDesc("APPROVE_MODERATION_ITEM", pageable);
    }

    @Test
    void getAuditEntries_WithDateRangeOnly_ShouldCallRepositoryWithDateFilter() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        Page<AdminAudit> page = new PageImpl<>(List.of(testAudit));
        
        when(adminAuditRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable))
            .thenReturn(page);

        // When
        Page<AdminAuditResponse> result = adminAuditService.getAuditEntries(
            null, null, startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(adminAuditRepository).findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable);
    }
}
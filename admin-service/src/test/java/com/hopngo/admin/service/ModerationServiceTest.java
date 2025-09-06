package com.hopngo.admin.service;

import com.hopngo.admin.dto.ModerationDecisionRequest;
import com.hopngo.admin.dto.ModerationItemResponse;
import com.hopngo.admin.entity.ModerationItem;
import com.hopngo.admin.entity.ModerationItem.ModerationStatus;
import com.hopngo.admin.entity.ModerationItem.ModerationItemType;
import com.hopngo.admin.repository.ModerationItemRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private ModerationItemRepository moderationItemRepository;

    @Mock
    private AdminAuditService auditService;

    @Mock
    private IntegrationService integrationService;

    @InjectMocks
    private ModerationService moderationService;

    private ModerationItem testItem;
    private ModerationDecisionRequest testRequest;

    @BeforeEach
    void setUp() {
        testItem = new ModerationItem();
        testItem.setId(1L);
        testItem.setType(ModerationItemType.POST);
        testItem.setRefId("post123");
        testItem.setStatus(ModerationStatus.OPEN);
        testItem.setReason("Inappropriate content");
        testItem.setReporterUserId("reporter123");
        testItem.setCreatedAt(LocalDateTime.now());

        testRequest = new ModerationDecisionRequest();
        testRequest.setDecisionNote("Test decision note");
    }

    @Test
    void getModerationItems_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ModerationItem> page = new PageImpl<>(List.of(testItem));
        when(moderationItemRepository.findAll(any(Pageable.class))).thenReturn(page);

        // When
        Page<ModerationItemResponse> result = moderationService.getModerationItems(
            null, null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getType()).isEqualTo(ModerationItemType.POST);
    }

    @Test
    void approveModerationItem_ShouldUpdateStatusToApproved() {
        // Given
        when(moderationItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(moderationItemRepository.save(any(ModerationItem.class))).thenReturn(testItem);

        // When
        ModerationItemResponse result = moderationService.approveModerationItem(
            1L, testRequest, "admin123");

        // Then
        assertThat(result.getStatus()).isEqualTo(ModerationStatus.APPROVED);
        verify(moderationItemRepository).save(argThat(item -> 
            item.getStatus() == ModerationStatus.APPROVED &&
            item.getDecisionNote().equals("Test decision note") &&
            item.getAssigneeUserId().equals("admin123")
        ));
        verify(auditService).logAction(eq("admin123"), eq("APPROVE_MODERATION_ITEM"), any());
    }

    @Test
    void approveModerationItem_WhenNotPending_ShouldThrowException() {
        // Given
        testItem.setStatus(ModerationStatus.APPROVED);
        when(moderationItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        // When & Then
        assertThatThrownBy(() -> moderationService.approveModerationItem(1L, testRequest, "admin123"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Only pending items can be approved");
    }

    @Test
    void rejectModerationItem_ShouldUpdateStatusToRejected() {
        // Given
        when(moderationItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(moderationItemRepository.save(any(ModerationItem.class))).thenReturn(testItem);

        // When
        ModerationItemResponse result = moderationService.rejectModerationItem(
            1L, testRequest, "admin123");

        // Then
        assertThat(result.getStatus()).isEqualTo(ModerationStatus.REJECTED);
        verify(moderationItemRepository).save(argThat(item -> 
            item.getStatus() == ModerationStatus.REJECTED
        ));
        verify(auditService).logAction(eq("admin123"), eq("REJECT_MODERATION_ITEM"), any());
    }

    @Test
    void removeModerationItem_ShouldCallIntegrationServiceAndUpdateStatus() {
        // Given
        when(moderationItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(moderationItemRepository.save(any(ModerationItem.class))).thenReturn(testItem);
        doNothing().when(integrationService).removeSocialPost(anyString(), anyString());

        // When
        ModerationItemResponse result = moderationService.removeModerationItem(
            1L, testRequest, "admin123");

        // Then
        assertThat(result.getStatus()).isEqualTo(ModerationStatus.REMOVED);
        verify(integrationService).removeSocialPost("post123", "Test decision note");
        verify(moderationItemRepository).save(argThat(item -> 
            item.getStatus() == ModerationStatus.REMOVED
        ));
        verify(auditService).logAction(eq("admin123"), eq("REMOVE_MODERATION_ITEM"), any());
    }

    @Test
    void removeModerationItem_WhenIntegrationFails_ShouldThrowException() {
        // Given
        when(moderationItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        doThrow(new RuntimeException("Integration failed"))
            .when(integrationService).removeSocialPost(anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> moderationService.removeModerationItem(1L, testRequest, "admin123"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to remove content from source service");
    }

    @Test
    void banUser_ShouldCallIntegrationServiceAndLogAudit() {
        // Given
        when(integrationService.banUser("user123", "Test decision note")).thenReturn(true);

        // When
        moderationService.banUser("user123", testRequest, "admin123");

        // Then
        verify(integrationService).banUser("user123", "Test decision note");
        verify(auditService).logAction(eq("admin123"), eq("BAN_USER"), any());
    }

    @Test
    void banUser_WhenIntegrationFails_ShouldThrowException() {
        // Given
        when(integrationService.banUser("user123", "Test decision note")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> moderationService.banUser("user123", testRequest, "admin123"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to ban user");
    }

    @Test
    void createModerationItem_ShouldSaveNewItem() {
        // Given
        when(moderationItemRepository.save(any(ModerationItem.class))).thenReturn(testItem);

        // When
        ModerationItem result = moderationService.createModerationItem(
            ModerationItemType.POST, "post123", "Inappropriate content", "reporter123");

        // Then
        assertThat(result.getType()).isEqualTo(ModerationItemType.POST);
        assertThat(result.getRefId()).isEqualTo("post123");
        assertThat(result.getStatus()).isEqualTo(ModerationStatus.OPEN);
        verify(moderationItemRepository).save(any(ModerationItem.class));
    }
}
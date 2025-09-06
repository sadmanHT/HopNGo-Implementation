package com.hopngo.admin.messaging;

import com.hopngo.admin.entity.ModerationItem;
import com.hopngo.admin.entity.ModerationItem.ModerationItemType;
import com.hopngo.admin.repository.ModerationItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentFlaggedEventConsumerTest {

    @Mock
    private ModerationItemRepository moderationItemRepository;

    @InjectMocks
    private ContentFlaggedEventConsumer contentFlaggedEventConsumer;

    private Map<String, Object> validEventData;

    @BeforeEach
    void setUp() {
        validEventData = new HashMap<>();
        validEventData.put("contentType", "POST");
        validEventData.put("contentId", "post123");
        validEventData.put("reason", "Inappropriate content");
        validEventData.put("reporterUserId", "user456");
        validEventData.put("priority", "HIGH");
        validEventData.put("timestamp", System.currentTimeMillis());
    }

    @Test
    void handleContentFlaggedEvent_WithValidData_ShouldCreateModerationItem() {
        // Given
        when(moderationItemRepository.findByTypeAndRefId(any(), anyString()))
            .thenReturn(Optional.empty());
        when(moderationItemRepository.save(any(ModerationItem.class)))
            .thenReturn(new ModerationItem());

        // When
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);

        // Then
        verify(moderationItemRepository).save(argThat(item -> 
            item.getType() == ModerationItemType.POST &&
            item.getRefId().equals("post123") &&
            item.getReason().equals("Inappropriate content") &&
            item.getReporterUserId().equals("user456")
        ));
    }

    @Test
    void handleContentFlaggedEvent_WithExistingItem_ShouldNotCreateDuplicate() {
        // Given
        ModerationItem existingItem = new ModerationItem();
        when(moderationItemRepository.findByTypeAndRefId(any(), anyString()))
            .thenReturn(Optional.of(existingItem));

        // When
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);

        // Then
        verify(moderationItemRepository, never()).save(any(ModerationItem.class));
    }

    @Test
    void handleContentFlaggedEvent_WithCommentType_ShouldCreateCommentModerationItem() {
        // Given
        validEventData.put("contentType", "COMMENT");
        validEventData.put("contentId", "comment123");
        
        when(moderationItemRepository.findByTypeAndRefId(any(), anyString()))
            .thenReturn(Optional.empty());
        when(moderationItemRepository.save(any(ModerationItem.class)))
            .thenReturn(new ModerationItem());

        // When
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);

        // Then
        verify(moderationItemRepository).save(argThat(item -> 
            item.getType() == ModerationItemType.COMMENT &&
            item.getRefId().equals("comment123")
        ));
    }

    @Test
    void handleContentFlaggedEvent_WithListingType_ShouldCreateListingModerationItem() {
        // Given
        validEventData.put("contentType", "LISTING");
        validEventData.put("contentId", "listing123");
        
        when(moderationItemRepository.findByTypeAndRefId(any(), anyString()))
            .thenReturn(Optional.empty());
        when(moderationItemRepository.save(any(ModerationItem.class)))
            .thenReturn(new ModerationItem());

        // When
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);

        // Then
        verify(moderationItemRepository).save(argThat(item -> 
            item.getType() == ModerationItemType.LISTING &&
            item.getRefId().equals("listing123")
        ));
    }

    @Test
    void handleContentFlaggedEvent_WithTripType_ShouldCreateTripModerationItem() {
        // Given
        validEventData.put("contentType", "TRIP");
        validEventData.put("contentId", "trip123");
        
        when(moderationItemRepository.findByTypeAndRefId(any(), anyString()))
            .thenReturn(Optional.empty());
        when(moderationItemRepository.save(any(ModerationItem.class)))
            .thenReturn(new ModerationItem());

        // When
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);

        // Then
        verify(moderationItemRepository).save(argThat(item -> 
            item.getType() == ModerationItemType.USER &&
            item.getRefId().equals("trip123")
        ));
    }

    @Test
    void handleContentFlaggedEvent_WithMissingContentType_ShouldNotCreateItem() {
        // Given
        validEventData.remove("contentType");

        // When
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);

        // Then
        verify(moderationItemRepository, never()).save(any(ModerationItem.class));
    }

    @Test
    void handleContentFlaggedEvent_WithMissingContentId_ShouldNotCreateItem() {
        // Given
        validEventData.remove("contentId");

        // When
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);

        // Then
        verify(moderationItemRepository, never()).save(any(ModerationItem.class));
    }

    @Test
    void handleContentFlaggedEvent_WithMissingReason_ShouldNotCreateItem() {
        // Given
        validEventData.remove("reason");

        // When
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);

        // Then
        verify(moderationItemRepository, never()).save(any(ModerationItem.class));
    }

    @Test
    void handleContentFlaggedEvent_WithUnknownContentType_ShouldNotCreateItem() {
        // Given
        validEventData.put("contentType", "UNKNOWN_TYPE");

        // When
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);

        // Then
        verify(moderationItemRepository, never()).save(any(ModerationItem.class));
    }

    @Test
    void handleContentFlaggedEvent_WithLowPriority_ShouldCreateItemWithCorrectPriority() {
        // Given
        validEventData.put("priority", "LOW");
        
        when(moderationItemRepository.findByTypeAndRefId(any(), anyString()))
            .thenReturn(Optional.empty());
        when(moderationItemRepository.save(any(ModerationItem.class)))
            .thenReturn(new ModerationItem());

        // When
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);

        // Then
        verify(moderationItemRepository).save(argThat(item -> 
            item.getPriority() == ModerationItem.Priority.LOW
        ));
    }

    @Test
    void handleContentFlaggedEvent_WithInvalidPriority_ShouldDefaultToMedium() {
        // Given
        validEventData.put("priority", "INVALID_PRIORITY");
        
        when(moderationItemRepository.findByTypeAndRefId(any(), anyString()))
            .thenReturn(Optional.empty());
        when(moderationItemRepository.save(any(ModerationItem.class)))
            .thenReturn(new ModerationItem());

        // When
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);

        // Then
        verify(moderationItemRepository).save(argThat(item -> 
            item.getPriority() == ModerationItem.Priority.MEDIUM
        ));
    }

    @Test
    void handleContentFlaggedEvent_WhenRepositoryThrowsException_ShouldHandleGracefully() {
        // Given
        when(moderationItemRepository.findByTypeAndRefId(any(), anyString()))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then (should not throw exception)
        contentFlaggedEventConsumer.handleContentFlaggedEvent(validEventData);
        
        verify(moderationItemRepository, never()).save(any(ModerationItem.class));
    }
}
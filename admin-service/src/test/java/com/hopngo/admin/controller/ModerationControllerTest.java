package com.hopngo.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.admin.dto.ModerationDecisionRequest;
import com.hopngo.admin.dto.ModerationItemResponse;
import com.hopngo.admin.entity.ModerationItem.ModerationStatus;
import com.hopngo.admin.entity.ModerationItem.ModerationItemType;
import com.hopngo.admin.service.ModerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModerationController.class)
class ModerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModerationService moderationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getModerationItems_ShouldReturnPagedResults() throws Exception {
        // Given
        ModerationItemResponse item = createModerationItemResponse();
        Page<ModerationItemResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);
        
        when(moderationService.getModerationItems(any(), any(), any(), any()))
            .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/moderation")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveModerationItem_ShouldReturnApprovedItem() throws Exception {
        // Given
        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecisionNote("Content is appropriate");
        
        ModerationItemResponse response = createModerationItemResponse();
        response.setStatus(ModerationStatus.APPROVED);
        
        when(moderationService.approveModerationItem(eq(1L), any(), anyString()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/moderation/1/approve")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectModerationItem_ShouldReturnRejectedItem() throws Exception {
        // Given
        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecisionNote("Content violates guidelines");
        
        ModerationItemResponse response = createModerationItemResponse();
        response.setStatus(ModerationStatus.REJECTED);
        
        when(moderationService.rejectModerationItem(eq(1L), any(), anyString()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/moderation/1/reject")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void removeModerationItem_ShouldReturnRemovedItem() throws Exception {
        // Given
        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecisionNote("Content removed for policy violation");
        
        ModerationItemResponse response = createModerationItemResponse();
        response.setStatus(ModerationStatus.REMOVED);
        
        when(moderationService.removeModerationItem(eq(1L), any(), anyString()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/moderation/1/remove")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMOVED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void banUser_ShouldReturnSuccessMessage() throws Exception {
        // Given
        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecisionNote("User banned for repeated violations");

        // When & Then
        mockMvc.perform(post("/api/v1/admin/moderation/users/user123/ban")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User banned successfully"));
    }

    @Test
    void getModerationItems_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/moderation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getModerationItems_WithUserRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/moderation"))
                .andExpect(status().isForbidden());
    }

    private ModerationItemResponse createModerationItemResponse() {
        ModerationItemResponse response = new ModerationItemResponse();
        response.setId(1L);
        response.setType(ModerationItemType.POST);
        response.setRefId("post123");
        response.setStatus(ModerationStatus.OPEN);
        response.setReason("Inappropriate content reported");
        response.setReporterUserId("reporter123");
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
}
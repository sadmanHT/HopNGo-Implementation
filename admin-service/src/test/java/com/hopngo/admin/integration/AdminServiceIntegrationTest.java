package com.hopngo.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.admin.entity.ModerationItem;
import com.hopngo.admin.entity.ModerationItem.ModerationItemType;
import com.hopngo.admin.repository.ModerationItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@Transactional
class AdminServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ModerationItemRepository moderationItemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ModerationItem testModerationItem;

    @BeforeEach
    void setUp() {
        moderationItemRepository.deleteAll();
        
        testModerationItem = new ModerationItem();
        testModerationItem.setType(ModerationItemType.POST);
        testModerationItem.setRefId(Long.valueOf("123"));
        testModerationItem.setReason("Inappropriate content");
        testModerationItem.setReporterUserId(Long.valueOf("456"));
        testModerationItem.setPriority(ModerationItem.Priority.HIGH);
        testModerationItem.setStatus(ModerationItem.ModerationStatus.OPEN);
        testModerationItem.setCreatedAt(Instant.now());
        
        moderationItemRepository.save(testModerationItem);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getModerationItems_WithAdminRole_ShouldReturnItems() throws Exception {
        mockMvc.perform(get("/api/v1/admin/moderation")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type", is("POST")))
                .andExpect(jsonPath("$.content[0].refId", is("post123")))
                .andExpect(jsonPath("$.content[0].reason", is("Inappropriate content")))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getModerationItems_WithUserRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/moderation"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getModerationItems_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/moderation"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveModerationItem_WithValidId_ShouldUpdateStatus() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("adminNotes", "Content is acceptable");

        mockMvc.perform(post("/api/v1/admin/moderation/{id}/approve", testModerationItem.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.adminNotes", is("Content is acceptable")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectModerationItem_WithValidId_ShouldUpdateStatus() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("adminNotes", "Content violates guidelines");

        mockMvc.perform(post("/api/v1/admin/moderation/{id}/reject", testModerationItem.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.adminNotes", is("Content violates guidelines")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void removeModerationItem_WithValidId_ShouldUpdateStatus() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("reason", "Spam content");

        mockMvc.perform(post("/api/v1/admin/moderation/{id}/remove", testModerationItem.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REMOVED")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void banUser_WithValidRequest_ShouldReturnSuccess() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", "user123");
        request.put("reason", "Multiple violations");
        request.put("duration", 7);

        mockMvc.perform(post("/api/v1/admin/users/ban")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("banned")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getModerationItems_WithStatusFilter_ShouldReturnFilteredResults() throws Exception {
        // Create additional items with different statuses
        ModerationItem approvedItem = new ModerationItem();
        approvedItem.setType(ModerationItemType.COMMENT);
        approvedItem.setRefId(Long.valueOf("123"));
        approvedItem.setReason("Test reason");
        approvedItem.setReporterUserId(Long.valueOf("789"));
        approvedItem.setPriority(ModerationItem.Priority.MEDIUM);
        approvedItem.setStatus(ModerationItem.ModerationStatus.APPROVED);
        approvedItem.setCreatedAt(Instant.now());
        moderationItemRepository.save(approvedItem);

        mockMvc.perform(get("/api/v1/admin/moderation")
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getModerationItems_WithTypeFilter_ShouldReturnFilteredResults() throws Exception {
        // Create additional item with different type
        ModerationItem commentItem = new ModerationItem();
        commentItem.setType(ModerationItemType.COMMENT);
        commentItem.setRefId(Long.valueOf("456"));
        commentItem.setReason("Test reason");
        commentItem.setReporterUserId(Long.valueOf("789"));
        commentItem.setPriority(ModerationItem.Priority.LOW);
        commentItem.setStatus(ModerationItem.ModerationStatus.OPEN);
        commentItem.setCreatedAt(Instant.now());
        moderationItemRepository.save(commentItem);

        mockMvc.perform(get("/api/v1/admin/moderation")
                .param("type", "POST")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type", is("POST")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getModerationItems_WithPriorityFilter_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(get("/api/v1/admin/moderation")
                .param("priority", "HIGH")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].priority", is("HIGH")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveModerationItem_WithInvalidId_ShouldReturnNotFound() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("adminNotes", "Test notes");

        mockMvc.perform(post("/api/v1/admin/moderation/999/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectModerationItem_WithInvalidId_ShouldReturnNotFound() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("adminNotes", "Test notes");

        mockMvc.perform(post("/api/v1/admin/moderation/999/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void removeModerationItem_WithInvalidId_ShouldReturnNotFound() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("reason", "Test reason");

        mockMvc.perform(post("/api/v1/admin/moderation/999/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void banUser_WithMissingUserId_ShouldReturnBadRequest() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("reason", "Test reason");
        request.put("duration", 7);

        mockMvc.perform(post("/api/v1/admin/users/ban")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void banUser_WithMissingReason_ShouldReturnBadRequest() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", "user123");
        request.put("duration", 7);

        mockMvc.perform(post("/api/v1/admin/users/ban")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
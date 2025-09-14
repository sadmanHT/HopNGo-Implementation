package com.hopngo.controller;

import com.hopngo.entity.TaxDocument;
import com.hopngo.service.TaxDocumentationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TaxDocumentController
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest(TaxDocumentController.class)
class TaxDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaxDocumentationService taxDocumentationService;

    @Autowired
    private ObjectMapper objectMapper;

    private TaxDocument mockTaxDocument;
    private List<TaxDocument> mockTaxDocuments;

    @BeforeEach
    void setUp() {
        mockTaxDocument = createMockTaxDocument();
        mockTaxDocuments = Arrays.asList(mockTaxDocument);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetTaxDocuments_Success() throws Exception {
        // Arrange
        when(taxDocumentationService.getTaxDocumentsByJurisdiction("BD"))
            .thenReturn(mockTaxDocuments);
        
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents")
                .param("jurisdiction", "BD")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].jurisdiction").value("BD"))
                .andExpect(jsonPath("$[0].taxYear").value(2023))
                .andExpect(jsonPath("$[0].documentType").value("INCOME_STATEMENT"));
        
        verify(taxDocumentationService).getTaxDocumentsByJurisdiction("BD");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetTaxDocuments_ByYear() throws Exception {
        // Arrange
        when(taxDocumentationService.getTaxDocumentsByYear(2023))
            .thenReturn(mockTaxDocuments);
        
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents")
                .param("year", "2023")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpected(jsonPath("$[0].taxYear").value(2023));
        
        verify(taxDocumentationService).getTaxDocumentsByYear(2023);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetTaxDocuments_ByJurisdictionAndYear() throws Exception {
        // Arrange
        when(taxDocumentationService.getTaxDocumentsByJurisdiction("BD"))
            .thenReturn(mockTaxDocuments);
        when(taxDocumentationService.getTaxDocumentsByYear(2023))
            .thenReturn(mockTaxDocuments);
        
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents")
                .param("jurisdiction", "BD")
                .param("year", "2023")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        
        verify(taxDocumentationService).getTaxDocumentsByJurisdiction("BD");
        verify(taxDocumentationService).getTaxDocumentsByYear(2023);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetTaxDocuments_AccessDenied() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetTaxDocumentById_Success() throws Exception {
        // Arrange
        when(taxDocumentationService.getTaxDocumentById(1L))
            .thenReturn(Optional.of(mockTaxDocument));
        
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.jurisdiction").value("BD"))
                .andExpect(jsonPath("$.taxYear").value(2023));
        
        verify(taxDocumentationService).getTaxDocumentById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetTaxDocumentById_NotFound() throws Exception {
        // Arrange
        when(taxDocumentationService.getTaxDocumentById(999L))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        
        verify(taxDocumentationService).getTaxDocumentById(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateTaxDocuments_Success() throws Exception {
        // Arrange
        when(taxDocumentationService.generateTaxDocumentsForJurisdiction("BD", 2023))
            .thenReturn(mockTaxDocuments);
        
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/generate")
                .param("jurisdiction", "BD")
                .param("year", "2023")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].jurisdiction").value("BD"))
                .andExpect(jsonPath("$[0].taxYear").value(2023));
        
        verify(taxDocumentationService).generateTaxDocumentsForJurisdiction("BD", 2023);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateTaxDocuments_InvalidJurisdiction() throws Exception {
        // Arrange
        when(taxDocumentationService.generateTaxDocumentsForJurisdiction("INVALID", 2023))
            .thenThrow(new IllegalArgumentException("Unsupported jurisdiction: INVALID"));
        
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/generate")
                .param("jurisdiction", "INVALID")
                .param("year", "2023")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        
        verify(taxDocumentationService).generateTaxDocumentsForJurisdiction("INVALID", 2023);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateTaxDocuments_ServiceException() throws Exception {
        // Arrange
        when(taxDocumentationService.generateTaxDocumentsForJurisdiction("BD", 2023))
            .thenThrow(new RuntimeException("Service error"));
        
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/generate")
                .param("jurisdiction", "BD")
                .param("year", "2023")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        
        verify(taxDocumentationService).generateTaxDocumentsForJurisdiction("BD", 2023);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGenerateTaxDocuments_AccessDenied() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/generate")
                .param("jurisdiction", "BD")
                .param("year", "2023")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDownloadTaxDocument_Success() throws Exception {
        // Arrange
        byte[] documentContent = "Tax document content".getBytes();
        when(taxDocumentationService.exportTaxDocumentContent(1L))
            .thenReturn(documentContent);
        when(taxDocumentationService.getTaxDocumentById(1L))
            .thenReturn(Optional.of(mockTaxDocument));
        
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents/1/download")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", 
                    "attachment; filename=\"BD_INCOME_STATEMENT_2023.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(documentContent));
        
        verify(taxDocumentationService).exportTaxDocumentContent(1L);
        verify(taxDocumentationService).getTaxDocumentById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDownloadTaxDocument_NotFound() throws Exception {
        // Arrange
        when(taxDocumentationService.exportTaxDocumentContent(999L))
            .thenThrow(new IllegalArgumentException("Tax document not found: 999"));
        
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents/999/download")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        
        verify(taxDocumentationService).exportTaxDocumentContent(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRegenerateTaxDocument_Success() throws Exception {
        // Arrange
        when(taxDocumentationService.regenerateTaxDocument(1L))
            .thenReturn(mockTaxDocument);
        
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/1/regenerate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.jurisdiction").value("BD"))
                .andExpect(jsonPath("$.taxYear").value(2023));
        
        verify(taxDocumentationService).regenerateTaxDocument(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRegenerateTaxDocument_NotFound() throws Exception {
        // Arrange
        when(taxDocumentationService.regenerateTaxDocument(999L))
            .thenThrow(new IllegalArgumentException("Tax document not found: 999"));
        
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/999/regenerate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        
        verify(taxDocumentationService).regenerateTaxDocument(999L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testRegenerateTaxDocument_AccessDenied() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/1/regenerate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testApproveTaxDocument_Success() throws Exception {
        // Arrange
        TaxDocument approvedDocument = createMockTaxDocument();
        approvedDocument.setStatus(TaxDocument.DocumentStatus.APPROVED);
        
        when(taxDocumentationService.getTaxDocumentById(1L))
            .thenReturn(Optional.of(mockTaxDocument))
            .thenReturn(Optional.of(approvedDocument));
        
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/1/approve")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"));
        
        verify(taxDocumentationService, times(2)).getTaxDocumentById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testApproveTaxDocument_NotFound() throws Exception {
        // Arrange
        when(taxDocumentationService.getTaxDocumentById(999L))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/999/approve")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        
        verify(taxDocumentationService).getTaxDocumentById(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetPendingTaxDocuments_Success() throws Exception {
        // Arrange
        when(taxDocumentationService.getPendingTaxDocuments())
            .thenReturn(mockTaxDocuments);
        
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents/pending")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
        
        verify(taxDocumentationService).getPendingTaxDocuments();
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetPendingTaxDocuments_AccessDenied() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents/pending")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetTaxDocumentStatistics_Success() throws Exception {
        // Arrange
        when(taxDocumentationService.getTaxDocumentsByJurisdiction(anyString()))
            .thenReturn(mockTaxDocuments);
        when(taxDocumentationService.getPendingTaxDocuments())
            .thenReturn(mockTaxDocuments);
        
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocuments").exists())
                .andExpect(jsonPath("$.pendingDocuments").exists())
                .andExpect(jsonPath("$.completedDocuments").exists())
                .andExpect(jsonPath("$.documentsByJurisdiction").exists())
                .andExpect(jsonPath("$.documentsByType").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetTaxDocumentStatistics_AccessDenied() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testHealthCheck_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("TaxDocumentController"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetTaxDocuments_EmptyResult() throws Exception {
        // Arrange
        when(taxDocumentationService.getTaxDocumentsByJurisdiction("BD"))
            .thenReturn(Collections.emptyList());
        
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents")
                .param("jurisdiction", "BD")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        
        verify(taxDocumentationService).getTaxDocumentsByJurisdiction("BD");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateTaxDocuments_MissingParameters() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/generate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateTaxDocuments_InvalidYear() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/generate")
                .param("jurisdiction", "BD")
                .param("year", "invalid")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDownloadTaxDocument_ServiceException() throws Exception {
        // Arrange
        when(taxDocumentationService.exportTaxDocumentContent(1L))
            .thenThrow(new RuntimeException("Export failed"));
        
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents/1/download")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        
        verify(taxDocumentationService).exportTaxDocumentContent(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRegenerateTaxDocument_ServiceException() throws Exception {
        // Arrange
        when(taxDocumentationService.regenerateTaxDocument(1L))
            .thenThrow(new RuntimeException("Regeneration failed"));
        
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/1/regenerate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        
        verify(taxDocumentationService).regenerateTaxDocument(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testApproveTaxDocument_AlreadyApproved() throws Exception {
        // Arrange
        TaxDocument approvedDocument = createMockTaxDocument();
        approvedDocument.setStatus(TaxDocument.DocumentStatus.APPROVED);
        
        when(taxDocumentationService.getTaxDocumentById(1L))
            .thenReturn(Optional.of(approvedDocument));
        
        // Act & Assert
        mockMvc.perform(post("/api/tax-documents/1/approve")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        
        verify(taxDocumentationService).getTaxDocumentById(1L);
    }

    @Test
    void testUnauthenticatedAccess() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCorsHeaders() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/tax-documents/health")
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    // ==================== HELPER METHODS ====================

    private TaxDocument createMockTaxDocument() {
        TaxDocument document = new TaxDocument();
        document.setId(1L);
        document.setDocumentType(TaxDocument.DocumentType.INCOME_STATEMENT);
        document.setJurisdiction("BD");
        document.setTaxYear(2023);
        document.setStartDate(LocalDate.of(2023, 1, 1));
        document.setEndDate(LocalDate.of(2023, 12, 31));
        document.setStatus(TaxDocument.DocumentStatus.COMPLETED);
        document.setGrossRevenue(new BigDecimal("100000.00"));
        document.setNetRevenue(new BigDecimal("80000.00"));
        document.setTaxableIncome(new BigDecimal("70000.00"));
        document.setEstimatedTaxLiability(new BigDecimal("7000.00"));
        document.setVatCollected(new BigDecimal("12000.00"));
        document.setVatPaid(new BigDecimal("4500.00"));
        document.setNetVatLiability(new BigDecimal("7500.00"));
        document.setWithholdingTaxAmount(new BigDecimal("5000.00"));
        document.setDocumentContent("Sample tax document content".getBytes());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return document;
    }
}
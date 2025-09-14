package com.hopngo.controller;

import com.hopngo.entity.FinancialReport;
import com.hopngo.service.FinancialReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FinancialReportController.class)
class FinancialReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinancialReportService financialReportService;

    @Autowired
    private ObjectMapper objectMapper;

    private FinancialReport monthlyReport;
    private FinancialReport yearlyReport;
    private FinancialReportService.ProviderSummary providerSummary;

    @BeforeEach
    void setUp() {
        // Create monthly report
        monthlyReport = new FinancialReport();
        ReflectionTestUtils.setField(monthlyReport, "id", 1L);
        ReflectionTestUtils.setField(monthlyReport, "reportId", "monthly_2024_01");
        ReflectionTestUtils.setField(monthlyReport, "type", "MONTHLY");
        ReflectionTestUtils.setField(monthlyReport, "status", "COMPLETED");
        ReflectionTestUtils.setField(monthlyReport, "periodStart", LocalDate.of(2024, 1, 1));
        ReflectionTestUtils.setField(monthlyReport, "periodEnd", LocalDate.of(2024, 1, 31));
        ReflectionTestUtils.setField(monthlyReport, "totalRevenue", new BigDecimal("10000.00"));
        ReflectionTestUtils.setField(monthlyReport, "totalExpenses", new BigDecimal("2000.00"));
        ReflectionTestUtils.setField(monthlyReport, "netIncome", new BigDecimal("8000.00"));
        ReflectionTestUtils.setField(monthlyReport, "createdAt", LocalDateTime.now());

        // Create yearly report
        yearlyReport = new FinancialReport();
        ReflectionTestUtils.setField(yearlyReport, "id", 2L);
        ReflectionTestUtils.setField(yearlyReport, "reportId", "yearly_2024");
        ReflectionTestUtils.setField(yearlyReport, "type", "YEARLY");
        ReflectionTestUtils.setField(yearlyReport, "status", "COMPLETED");
        ReflectionTestUtils.setField(yearlyReport, "periodStart", LocalDate.of(2024, 1, 1));
        ReflectionTestUtils.setField(yearlyReport, "periodEnd", LocalDate.of(2024, 12, 31));
        ReflectionTestUtils.setField(yearlyReport, "totalRevenue", new BigDecimal("120000.00"));
        ReflectionTestUtils.setField(yearlyReport, "totalExpenses", new BigDecimal("24000.00"));
        ReflectionTestUtils.setField(yearlyReport, "netIncome", new BigDecimal("96000.00"));
        ReflectionTestUtils.setField(yearlyReport, "createdAt", LocalDateTime.now());

        // Create provider summary
        providerSummary = new FinancialReportService.ProviderSummary(
            "STRIPE", new BigDecimal("5000.00"), 50L, new BigDecimal("100.00")
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetReports_Success() throws Exception {
        // Arrange
        when(financialReportService.getReports(any(), any(), any(), any()))
            .thenReturn(Arrays.asList(monthlyReport, yearlyReport));

        // Act & Assert
        mockMvc.perform(get("/api/financial-reports")
                .param("type", "MONTHLY")
                .param("status", "COMPLETED")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].reportId").value("monthly_2024_01"))
            .andExpect(jsonPath("$[0].type").value("MONTHLY"))
            .andExpect(jsonPath("$[0].totalRevenue").value(10000.00))
            .andExpect(jsonPath("$[1].reportId").value("yearly_2024"))
            .andExpect(jsonPath("$[1].type").value("YEARLY"));

        verify(financialReportService).getReports("MONTHLY", "COMPLETED", 
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetReports_NoFilters() throws Exception {
        // Arrange
        when(financialReportService.getReports(null, null, null, null))
            .thenReturn(Arrays.asList(monthlyReport));

        // Act & Assert
        mockMvc.perform(get("/api/financial-reports"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));

        verify(financialReportService).getReports(null, null, null, null);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetReports_Forbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/financial-reports"))
            .andExpect(status().isForbidden());

        verify(financialReportService, never()).getReports(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetReportById_Success() throws Exception {
        // Arrange
        when(financialReportService.getReportById("monthly_2024_01"))
            .thenReturn(Optional.of(monthlyReport));

        // Act & Assert
        mockMvc.perform(get("/api/financial-reports/monthly_2024_01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportId").value("monthly_2024_01"))
            .andExpect(jsonPath("$.type").value("MONTHLY"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.totalRevenue").value(10000.00))
            .andExpect(jsonPath("$.totalExpenses").value(2000.00))
            .andExpect(jsonPath("$.netIncome").value(8000.00));

        verify(financialReportService).getReportById("monthly_2024_01");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetReportById_NotFound() throws Exception {
        // Arrange
        when(financialReportService.getReportById("nonexistent"))
            .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/financial-reports/nonexistent"))
            .andExpect(status().isNotFound());

        verify(financialReportService).getReportById("nonexistent");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetPendingReports_Success() throws Exception {
        // Arrange
        FinancialReport pendingReport = new FinancialReport();
        ReflectionTestUtils.setField(pendingReport, "reportId", "monthly_2024_02");
        ReflectionTestUtils.setField(pendingReport, "status", "PENDING");
        
        when(financialReportService.getPendingReports())
            .thenReturn(Arrays.asList(pendingReport));

        // Act & Assert
        mockMvc.perform(get("/api/financial-reports/pending"))
            .andExpected(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].reportId").value("monthly_2024_02"))
            .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(financialReportService).getPendingReports();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetReportStatistics_Success() throws Exception {
        // Arrange
        when(financialReportService.getReportStatistics())
            .thenReturn(Map.of(
                "totalReports", 10L,
                "completedReports", 8L,
                "pendingReports", 1L,
                "failedReports", 1L
            ));

        // Act & Assert
        mockMvc.perform(get("/api/financial-reports/statistics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalReports").value(10))
            .andExpect(jsonPath("$.completedReports").value(8))
            .andExpect(jsonPath("$.pendingReports").value(1))
            .andExpect(jsonPath("$.failedReports").value(1));

        verify(financialReportService).getReportStatistics();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateMonthlyReport_Success() throws Exception {
        // Arrange
        doNothing().when(financialReportService).generateMonthlyReport(any(LocalDate.class));

        // Act & Assert
        mockMvc.perform(post("/api/financial-reports/generate/monthly")
                .with(csrf())
                .param("date", "2024-01-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Monthly report generation initiated"));

        verify(financialReportService).generateMonthlyReport(LocalDate.of(2024, 1, 15));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateMonthlyReport_CurrentMonth() throws Exception {
        // Arrange
        doNothing().when(financialReportService).generateMonthlyReport(any(LocalDate.class));

        // Act & Assert
        mockMvc.perform(post("/api/financial-reports/generate/monthly")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Monthly report generation initiated"));

        verify(financialReportService).generateMonthlyReport(any(LocalDate.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateYearlyReport_Success() throws Exception {
        // Arrange
        doNothing().when(financialReportService).generateYearlyReport(anyInt());

        // Act & Assert
        mockMvc.perform(post("/api/financial-reports/generate/yearly")
                .with(csrf())
                .param("year", "2024"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Yearly report generation initiated"));

        verify(financialReportService).generateYearlyReport(2024);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateYearlyReport_CurrentYear() throws Exception {
        // Arrange
        doNothing().when(financialReportService).generateYearlyReport(anyInt());

        // Act & Assert
        mockMvc.perform(post("/api/financial-reports/generate/yearly")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Yearly report generation initiated"));

        verify(financialReportService).generateYearlyReport(anyInt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testExportCsv_Success() throws Exception {
        // Arrange
        byte[] csvData = "Report ID,Type,Total Revenue\nmonthly_2024_01,MONTHLY,10000.00".getBytes();
        when(financialReportService.exportToCsv("monthly_2024_01")).thenReturn(csvData);

        // Act & Assert
        mockMvc.perform(get("/api/financial-reports/monthly_2024_01/export/csv"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/csv"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"financial_report_monthly_2024_01.csv\""))
            .andExpect(content().bytes(csvData));

        verify(financialReportService).exportToCsv("monthly_2024_01");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testExportPdf_Success() throws Exception {
        // Arrange
        byte[] pdfData = "%PDF-1.4\n1 0 obj\n<<\n/Type /Catalog".getBytes();
        when(financialReportService.exportToPdf("monthly_2024_01")).thenReturn(pdfData);

        // Act & Assert
        mockMvc.perform(get("/api/financial-reports/monthly_2024_01/export/pdf"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"financial_report_monthly_2024_01.pdf\""))
            .andExpect(content().bytes(pdfData));

        verify(financialReportService).exportToPdf("monthly_2024_01");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testExportCsv_ReportNotFound() throws Exception {
        // Arrange
        when(financialReportService.exportToCsv("nonexistent"))
            .thenThrow(new RuntimeException("Report not found"));

        // Act & Assert
        mockMvc.perform(get("/api/financial-reports/nonexistent/export/csv"))
            .andExpect(status().isNotFound());

        verify(financialReportService).exportToCsv("nonexistent");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetProviderSummaries_Success() throws Exception {
        // Arrange
        when(financialReportService.getAllProviderSummaries(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Arrays.asList(providerSummary));

        // Act & Assert
        mockMvc.perform(get("/api/financial-reports/provider-summaries")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].provider").value("STRIPE"))
            .andExpect(jsonPath("$[0].totalAmount").value(5000.00))
            .andExpect(jsonPath("$[0].transactionCount").value(50))
            .andExpect(jsonPath("$[0].averageAmount").value(100.00));

        verify(financialReportService).getAllProviderSummaries(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetProviderSummary_Success() throws Exception {
        // Arrange
        when(financialReportService.getProviderSummary(eq("STRIPE"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(providerSummary);

        // Act & Assert
        mockMvc.perform(get("/api/financial-reports/provider-summaries/STRIPE")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provider").value("STRIPE"))
            .andExpect(jsonPath("$.totalAmount").value(5000.00))
            .andExpect(jsonPath("$.transactionCount").value(50))
            .andExpect(jsonPath("$.averageAmount").value(100.00));

        verify(financialReportService).getProviderSummary(
            "STRIPE", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetProviderSummaries_InvalidDateRange() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/financial-reports/provider-summaries")
                .param("startDate", "2024-01-31")
                .param("endDate", "2024-01-01")) // End before start
            .andExpect(status().isBadRequest());

        verify(financialReportService, never()).getAllProviderSummaries(any(), any());
    }

    @Test
    void testGetReports_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/financial-reports"))
            .andExpect(status().isUnauthorized());

        verify(financialReportService, never()).getReports(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateMonthlyReport_InvalidDate() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/financial-reports/generate/monthly")
                .with(csrf())
                .param("date", "invalid-date"))
            .andExpect(status().isBadRequest());

        verify(financialReportService, never()).generateMonthlyReport(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateYearlyReport_InvalidYear() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/financial-reports/generate/yearly")
                .with(csrf())
                .param("year", "invalid-year"))
            .andExpect(status().isBadRequest());

        verify(financialReportService, never()).generateYearlyReport(anyInt());
    }
}
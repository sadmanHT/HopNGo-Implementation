package com.hopngo.controller;

import com.hopngo.entity.FinancialReport;
import com.hopngo.service.FinancialReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/financial-reports")
@CrossOrigin(origins = "*")
public class FinancialReportController {

    private static final Logger logger = LoggerFactory.getLogger(FinancialReportController.class);

    @Autowired
    private FinancialReportService financialReportService;

    /**
     * Get all financial reports with optional filtering
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<List<FinancialReport>> getFinancialReports(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        try {
            List<FinancialReport> reports;
            
            if (year != null) {
                reports = financialReportService.getReportsByPeriod(year, month);
            } else {
                // Get reports for current year if no year specified
                int currentYear = LocalDate.now().getYear();
                reports = financialReportService.getReportsByPeriod(currentYear, month);
            }
            
            return ResponseEntity.ok(reports);
            
        } catch (Exception e) {
            logger.error("Error retrieving financial reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get specific financial report by ID
     */
    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<FinancialReport> getFinancialReport(@PathVariable Long reportId) {
        try {
            Optional<FinancialReport> report = financialReportService.getReportById(reportId);
            
            if (report.isPresent()) {
                return ResponseEntity.ok(report.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving financial report: {}", reportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate monthly financial report
     */
    @PostMapping("/generate/monthly")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<FinancialReport> generateMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {
        
        try {
            // Validate month
            if (month < 1 || month > 12) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate year
            int currentYear = LocalDate.now().getYear();
            if (year > currentYear) {
                return ResponseEntity.badRequest().build();
            }
            
            FinancialReport report = financialReportService.generateMonthlyReport(year, month);
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            logger.error("Error generating monthly financial report for {}-{:02d}", year, month, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate yearly financial report
     */
    @PostMapping("/generate/yearly")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<FinancialReport> generateYearlyReport(@RequestParam int year) {
        try {
            // Validate year
            int currentYear = LocalDate.now().getYear();
            if (year > currentYear) {
                return ResponseEntity.badRequest().build();
            }
            
            FinancialReport report = financialReportService.generateYearlyReport(year);
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            logger.error("Error generating yearly financial report for {}", year, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export financial report as CSV
     */
    @GetMapping("/{reportId}/export/csv")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<byte[]> exportReportAsCSV(@PathVariable Long reportId) {
        try {
            Optional<FinancialReport> reportOpt = financialReportService.getReportById(reportId);
            
            if (!reportOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            FinancialReport report = reportOpt.get();
            byte[] csvData = financialReportService.exportReportAsCSV(reportId);
            
            // Generate filename
            String filename = generateReportFilename(report, "csv");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(csvData.length);
            
            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
            
        } catch (IOException e) {
            logger.error("Error exporting financial report as CSV: {}", reportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Error processing CSV export request: {}", reportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export financial report as PDF
     */
    @GetMapping("/{reportId}/export/pdf")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<byte[]> exportReportAsPDF(@PathVariable Long reportId) {
        try {
            Optional<FinancialReport> reportOpt = financialReportService.getReportById(reportId);
            
            if (!reportOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            FinancialReport report = reportOpt.get();
            byte[] pdfData = financialReportService.exportReportAsPDF(reportId);
            
            // Generate filename
            String filename = generateReportFilename(report, "pdf");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);
            
            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);
            
        } catch (IOException e) {
            logger.error("Error exporting financial report as PDF: {}", reportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Error processing PDF export request: {}", reportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get provider-specific financial summary
     */
    @GetMapping("/provider/{providerId}/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE') or (hasRole('PROVIDER') and @securityService.isProviderOwner(#providerId))")
    public ResponseEntity<FinancialReportService.ProviderFinancialSummary> getProviderFinancialSummary(
            @PathVariable Long providerId,
            @RequestParam int year,
            @RequestParam int month) {
        
        try {
            // Validate month
            if (month < 1 || month > 12) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate year
            int currentYear = LocalDate.now().getYear();
            if (year > currentYear) {
                return ResponseEntity.badRequest().build();
            }
            
            FinancialReportService.ProviderFinancialSummary summary = 
                financialReportService.getProviderFinancialSummary(providerId, year, month);
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            logger.error("Error retrieving provider financial summary for provider: {} period: {}-{:02d}", 
                providerId, year, month, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pending financial reports
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<List<FinancialReport>> getPendingReports() {
        try {
            List<FinancialReport> pendingReports = financialReportService.getPendingReports();
            return ResponseEntity.ok(pendingReports);
            
        } catch (Exception e) {
            logger.error("Error retrieving pending financial reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get financial report statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<FinancialReportStatistics> getFinancialReportStatistics(
            @RequestParam(required = false) Integer year) {
        
        try {
            int targetYear = year != null ? year : LocalDate.now().getYear();
            
            List<FinancialReport> yearlyReports = financialReportService.getReportsByPeriod(targetYear, null);
            
            FinancialReportStatistics stats = new FinancialReportStatistics();
            stats.year = targetYear;
            stats.totalReports = yearlyReports.size();
            stats.completedReports = (int) yearlyReports.stream()
                .filter(r -> r.getStatus() == FinancialReport.ReportStatus.COMPLETED)
                .count();
            stats.pendingReports = (int) yearlyReports.stream()
                .filter(r -> r.getStatus() == FinancialReport.ReportStatus.GENERATING)
                .count();
            stats.failedReports = (int) yearlyReports.stream()
                .filter(r -> r.getStatus() == FinancialReport.ReportStatus.FAILED)
                .count();
            
            // Calculate totals from completed reports
            yearlyReports.stream()
                .filter(r -> r.getStatus() == FinancialReport.ReportStatus.COMPLETED)
                .forEach(report -> {
                    if (report.getTotalRevenue() != null) {
                        stats.totalRevenue = stats.totalRevenue.add(report.getTotalRevenue());
                    }
                    if (report.getPlatformFees() != null) {
                        stats.totalPlatformFees = stats.totalPlatformFees.add(report.getPlatformFees());
                    }
                    if (report.getProviderEarnings() != null) {
                        stats.totalProviderEarnings = stats.totalProviderEarnings.add(report.getProviderEarnings());
                    }
                    if (report.getTotalTransactions() != null) {
                        stats.totalTransactions += report.getTotalTransactions();
                    }
                });
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error retrieving financial report statistics for year: {}", year, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Financial report service is healthy");
    }

    // Helper Methods

    private String generateReportFilename(FinancialReport report, String extension) {
        StringBuilder filename = new StringBuilder();
        filename.append("hopngo_financial_report_");
        filename.append(report.getReportType().toString().toLowerCase());
        filename.append("_");
        filename.append(report.getYear());
        
        if (report.getMonth() != null) {
            filename.append(String.format("_%02d", report.getMonth()));
        }
        
        filename.append("_");
        filename.append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        filename.append(".");
        filename.append(extension);
        
        return filename.toString();
    }

    // Data Classes

    public static class FinancialReportStatistics {
        public int year;
        public int totalReports;
        public int completedReports;
        public int pendingReports;
        public int failedReports;
        public java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;
        public java.math.BigDecimal totalPlatformFees = java.math.BigDecimal.ZERO;
        public java.math.BigDecimal totalProviderEarnings = java.math.BigDecimal.ZERO;
        public long totalTransactions = 0L;
    }

    /**
     * Exception handler for validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(IllegalArgumentException e) {
        logger.warn("Validation error in financial report controller: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }

    /**
     * Exception handler for general errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralError(Exception e) {
        logger.error("Unexpected error in financial report controller", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    public static class ErrorResponse {
        public String code;
        public String message;
        public String timestamp;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
            this.timestamp = LocalDate.now().toString();
        }
    }
}
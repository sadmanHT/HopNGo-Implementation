package com.hopngo.service;

import com.hopngo.entity.*;
import com.hopngo.entity.Transaction.TransactionStatus;
import com.hopngo.entity.Dispute.DisputeStatus;
import com.hopngo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.hopngo.util.FinancialCalculationUtil;

@Service
@Transactional
public class FinancialReportService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialReportService.class);

    @Autowired
    private FinancialReportRepository financialReportRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SupportTicketService supportTicketService;

    /**
     * Scheduled job to generate monthly financial reports
     * Runs on the 1st day of each month at 6:00 AM
     */
    @Scheduled(cron = "0 0 6 1 * ?")
    public void generateMonthlyReports() {
        logger.info("Starting scheduled monthly financial report generation");
        
        try {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            generateMonthlyReport(lastMonth.getYear(), lastMonth.getMonthValue());
            
            logger.info("Monthly financial report generation completed successfully");
            
        } catch (Exception e) {
            logger.error("Error generating monthly financial reports", e);
            
            // Create support ticket for failed report generation
            supportTicketService.createReportGenerationTicket(
                "MONTHLY_REPORT_GENERATION_FAILED",
                "Monthly",
                "Error: " + e.getMessage()
            );
        }
    }

    /**
     * Scheduled job to generate yearly financial reports
     * Runs on January 15th at 8:00 AM
     */
    @Scheduled(cron = "0 0 8 15 1 ?")
    public void generateYearlyReports() {
        logger.info("Starting scheduled yearly financial report generation");
        
        try {
            int lastYear = LocalDate.now().getYear() - 1;
            generateYearlyReport(lastYear);
            
            logger.info("Yearly financial report generation completed successfully");
            
        } catch (Exception e) {
            logger.error("Error generating yearly financial reports", e);
            
            // Create support ticket for failed report generation
            supportTicketService.createReportGenerationTicket(
                "YEARLY_REPORT_GENERATION_FAILED",
                "Yearly",
                "Error: " + e.getMessage()
            );
        }
    }

    /**
     * Generate monthly financial report
     */
    public FinancialReport generateMonthlyReport(int year, int month) {
        logger.info("Generating monthly financial report for {}-{:02d}", year, month);
        
        try {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startLocalDate = yearMonth.atDay(1);
            LocalDate endLocalDate = yearMonth.atEndOfMonth();
            LocalDateTime startDate = FinancialCalculationUtil.getStartOfDay(startLocalDate);
            LocalDateTime endDate = FinancialCalculationUtil.getEndOfDay(endLocalDate);
            
            // Check if report already exists
            Optional<FinancialReport> existingReport = financialReportRepository
                .findMonthlyReportByYearMonthAndType(year, month, FinancialReport.ReportType.REVENUE);
            
            if (existingReport.isPresent() && 
                existingReport.get().getStatus() == FinancialReport.ReportStatus.COMPLETED) {
                logger.info("Monthly report already exists for {}-{:02d}", year, month);
                return existingReport.get();
            }
            
            // Create new report
            FinancialReport report = new FinancialReport();
            report.setReportType(FinancialReport.ReportType.REVENUE);
            report.setPeriodType(FinancialReport.PeriodType.MONTHLY);
            report.setYear(year);
            report.setMonth(month);
            report.setPeriodStart(startDate.toLocalDate());
            report.setPeriodEnd(endDate.toLocalDate());
            report.setStatus(FinancialReport.ReportStatus.GENERATING);
            report.setCreatedAt(FinancialCalculationUtil.getCurrentUtcTime());
            
            report = financialReportRepository.save(report);
            
            // Calculate financial metrics
            MonthlyFinancialData data = calculateMonthlyFinancialData(startDate, endDate);
            
            // Update report with calculated data
            report.setTotalRevenue(data.totalRevenue);
            report.setProviderEarnings(data.providerEarnings);
            report.setPlatformFees(data.platformFees);
            report.setPlatformFees(data.processingFees);
            report.setTotalTransactions(data.totalTransactions);
            report.setSuccessfulTransactions(data.successfulTransactions);
            report.setFailedTransactions(data.failedTransactions);
            report.setTotalRefunds(data.refundAmount);
            report.setRefundedTransactions(data.refundCount.longValue());
            report.setDisputeAmount(data.disputeAmount);
            report.setTotalDisputes(data.disputeCount.longValue());
            report.setTaxWithheld(data.taxAmount);
            report.setNetRevenue(data.netRevenue);
            report.setStatus(FinancialReport.ReportStatus.COMPLETED);
            report.setGenerationCompletedAt(FinancialCalculationUtil.getCurrentUtcTime());
            
            report = financialReportRepository.save(report);
            
            // Send notification
            notificationService.sendReportGenerationNotification(
                "Monthly",
                String.format("%d-%02d", year, month),
                true,
                String.format("Total Revenue: %s", data.totalRevenue)
            );
            
            logger.info("Monthly financial report generated successfully: {}", report.getId());
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating monthly financial report for {}-{:02d}", year, month, e);
            throw new RuntimeException("Failed to generate monthly financial report", e);
        }
    }

    /**
     * Generate yearly financial report
     */
    public FinancialReport generateYearlyReport(int year) {
        logger.info("Generating yearly financial report for {}", year);
        
        try {
            LocalDate startLocalDate = LocalDate.of(year, 1, 1);
            LocalDate endLocalDate = LocalDate.of(year, 12, 31);
            LocalDateTime startDate = FinancialCalculationUtil.getStartOfDay(startLocalDate);
            LocalDateTime endDate = FinancialCalculationUtil.getEndOfDay(endLocalDate);
            
            // Check if report already exists
            Optional<FinancialReport> existingReport = financialReportRepository
                .findYearlyReportByYearAndType(year, FinancialReport.ReportType.TAX);
            
            if (existingReport.isPresent() && 
                existingReport.get().getStatus() == FinancialReport.ReportStatus.COMPLETED) {
                logger.info("Yearly report already exists for {}", year);
                return existingReport.get();
            }
            
            // Create new report
            FinancialReport report = new FinancialReport();
            report.setReportType(FinancialReport.ReportType.TAX);
            report.setPeriodType(FinancialReport.PeriodType.YEARLY);
            report.setYear(year);
            report.setPeriodStart(startDate.toLocalDate());
            report.setPeriodEnd(endDate.toLocalDate());
            report.setStatus(FinancialReport.ReportStatus.GENERATING);
            report.setCreatedAt(LocalDateTime.now());
            
            report = financialReportRepository.save(report);
            
            // Calculate yearly financial metrics
            YearlyFinancialData data = calculateYearlyFinancialData(startDate, endDate);
            
            // Update report with calculated data
            report.setTotalRevenue(data.totalRevenue);
            report.setProviderEarnings(data.providerEarnings);
            report.setPlatformFees(data.platformFees);
            report.setPlatformFees(data.processingFees);
            report.setTotalTransactions(data.totalTransactions);
            report.setSuccessfulTransactions(data.successfulTransactions);
            report.setFailedTransactions(data.failedTransactions);
            report.setTotalRefunds(data.refundAmount);
            report.setRefundedTransactions(data.refundCount.longValue());
            report.setDisputeAmount(data.disputeAmount);
            report.setTotalDisputes(data.disputeCount.longValue());
            report.setTaxWithheld(data.taxAmount);
            report.setNetRevenue(data.netRevenue);
            report.setStatus(FinancialReport.ReportStatus.COMPLETED);
            report.setGenerationCompletedAt(LocalDateTime.now());
            
            report = financialReportRepository.save(report);
            
            // Send notification
            notificationService.sendReportGenerationNotification(
                "Yearly",
                String.valueOf(year),
                true,
                String.format("Total Revenue: %s", data.totalRevenue)
            );
            
            logger.info("Yearly financial report generated successfully: {}", report.getId());
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating yearly financial report for {}", year, e);
            throw new RuntimeException("Failed to generate yearly financial report", e);
        }
    }

    /**
     * Export financial report as CSV
     */
    public byte[] exportReportAsCSV(Long reportId) throws IOException {
        logger.info("Exporting financial report as CSV: {}", reportId);
        
        Optional<FinancialReport> reportOpt = financialReportRepository.findById(reportId);
        if (!reportOpt.isPresent()) {
            throw new IllegalArgumentException("Financial report not found: " + reportId);
        }
        
        FinancialReport report = reportOpt.get();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);
        
        // Write CSV header
        writer.println("Financial Report CSV Export");
        writer.println("Report ID," + report.getId());
        writer.println("Report Type," + report.getReportType());
        writer.println("Period," + report.getPeriodType());
        writer.println("Year," + report.getYear());
        if (report.getMonth() != null) {
            writer.println("Month," + report.getMonth());
        }
        writer.println("Start Date," + report.getPeriodStart());
        writer.println("End Date," + report.getPeriodEnd());
        writer.println("Generated At," + report.getCreatedAt());
        writer.println("");
        
        // Write financial data
        writer.println("Metric,Amount,Currency");
        writer.println("Total Revenue," + report.getTotalRevenue() + ",USD");
        writer.println("Provider Earnings," + report.getProviderEarnings() + ",USD");
        writer.println("Platform Fees," + report.getPlatformFees() + ",USD");
        writer.println("Processing Fees," + report.getPlatformFees() + ",USD");
        writer.println("Refund Amount," + report.getTotalRefunds() + ",USD");
        writer.println("Dispute Amount," + report.getDisputeAmount() + ",USD");
        writer.println("Tax Amount," + report.getTaxWithheld() + ",USD");
        writer.println("Net Revenue," + report.getNetRevenue() + ",USD");
        writer.println("");
        
        // Write transaction statistics
        writer.println("Transaction Statistics,Count");
        writer.println("Total Transactions," + report.getTotalTransactions());
        writer.println("Successful Transactions," + report.getSuccessfulTransactions());
        writer.println("Failed Transactions," + report.getFailedTransactions());
        writer.println("Refund Count," + report.getRefundedTransactions());
        writer.println("Dispute Count," + report.getTotalDisputes());
        
        writer.flush();
        writer.close();
        
        return outputStream.toByteArray();
    }

    /**
     * Export financial report as PDF (simplified text-based PDF)
     */
    public byte[] exportReportAsPDF(Long reportId) throws IOException {
        logger.info("Exporting financial report as PDF: {}", reportId);
        
        Optional<FinancialReport> reportOpt = financialReportRepository.findById(reportId);
        if (!reportOpt.isPresent()) {
            throw new IllegalArgumentException("Financial report not found: " + reportId);
        }
        
        FinancialReport report = reportOpt.get();
        
        // For now, return a text-based PDF content
        // In a real implementation, you would use a PDF library like iText or Apache PDFBox
        StringBuilder pdfContent = new StringBuilder();
        pdfContent.append("HOPNGO FINANCIAL REPORT\n\n");
        pdfContent.append("Report ID: ").append(report.getId()).append("\n");
        pdfContent.append("Report Type: ").append(report.getReportType()).append("\n");
        pdfContent.append("Period: ").append(report.getPeriodType()).append("\n");
        pdfContent.append("Year: ").append(report.getYear()).append("\n");
        if (report.getMonth() != null) {
            pdfContent.append("Month: ").append(report.getMonth()).append("\n");
        }
        pdfContent.append("Period: ").append(report.getPeriodStart()).append(" to ").append(report.getPeriodEnd()).append("\n");
        pdfContent.append("Generated: ").append(report.getCreatedAt()).append("\n\n");
        
        pdfContent.append("FINANCIAL SUMMARY\n");
        pdfContent.append("================\n");
        pdfContent.append("Total Revenue: $").append(report.getTotalRevenue()).append("\n");
        pdfContent.append("Provider Earnings: $").append(report.getProviderEarnings()).append("\n");
        pdfContent.append("Platform Fees: $").append(report.getPlatformFees()).append("\n");
        pdfContent.append("Processing Fees: $").append(report.getPlatformFees()).append("\n");
        pdfContent.append("Refund Amount: $").append(report.getTotalRefunds()).append("\n");
        pdfContent.append("Dispute Amount: $").append(report.getDisputeAmount()).append("\n");
        pdfContent.append("Tax Amount: $").append(report.getTaxWithheld()).append("\n");
        pdfContent.append("Net Revenue: $").append(report.getNetRevenue()).append("\n\n");
        
        pdfContent.append("TRANSACTION STATISTICS\n");
        pdfContent.append("====================\n");
        pdfContent.append("Total Transactions: ").append(report.getTotalTransactions()).append("\n");
        pdfContent.append("Successful Transactions: ").append(report.getSuccessfulTransactions()).append("\n");
        pdfContent.append("Failed Transactions: ").append(report.getFailedTransactions()).append("\n");
        pdfContent.append("Refund Count: ").append(report.getRefundedTransactions()).append("\n");
        pdfContent.append("Dispute Count: ").append(report.getTotalDisputes()).append("\n");
        
        return pdfContent.toString().getBytes();
    }

    /**
     * Get provider-specific financial report
     */
    public ProviderFinancialSummary getProviderFinancialSummary(Long providerId, int year, int month) {
        logger.info("Generating provider financial summary for provider: {} period: {}-{:02d}", 
            providerId, year, month);
        
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        // Get provider transactions for the period using existing methods
        ProviderFinancialSummary summary = new ProviderFinancialSummary();
        summary.providerId = providerId;
        summary.year = year;
        summary.month = month;
        summary.startDate = startDate.toLocalDate();
        summary.endDate = endDate.toLocalDate();
        
        // Use existing repository methods
        summary.totalEarnings = transactionRepository.sumAmountByStatusAndDateRange(
            TransactionStatus.SUCCESS, startDate, endDate) != null ? 
            transactionRepository.sumAmountByStatusAndDateRange(TransactionStatus.SUCCESS, startDate, endDate) : BigDecimal.ZERO;
        summary.totalTransactions = transactionRepository.countByStatusAndDateRange(
            TransactionStatus.SUCCESS, startDate, endDate) != null ?
            transactionRepository.countByStatusAndDateRange(TransactionStatus.SUCCESS, startDate, endDate) : 0L;
        summary.successfulTransactions = summary.totalTransactions;
        summary.platformFees = summary.totalEarnings.multiply(BigDecimal.valueOf(0.03)).setScale(2, RoundingMode.HALF_UP);
        summary.processingFees = summary.totalEarnings.multiply(BigDecimal.valueOf(0.025)).setScale(2, RoundingMode.HALF_UP);
        
        // Calculate net earnings
        summary.netEarnings = summary.totalEarnings
            .subtract(summary.platformFees)
            .subtract(summary.processingFees);
        
        return summary;
    }

    // Helper Methods

    private MonthlyFinancialData calculateMonthlyFinancialData(LocalDateTime startDate, LocalDateTime endDate) {
        MonthlyFinancialData data = new MonthlyFinancialData();
        
        // Get transaction summaries using existing methods
        data.totalRevenue = transactionRepository.sumAmountByStatusAndDateRange(
            TransactionStatus.SUCCESS, startDate, endDate);
        if (data.totalRevenue == null) data.totalRevenue = BigDecimal.ZERO;
        
        data.totalTransactions = transactionRepository.countByStatusAndDateRange(
            TransactionStatus.SUCCESS, startDate, endDate);
        if (data.totalTransactions == null) data.totalTransactions = 0L;
        
        data.successfulTransactions = data.totalTransactions;
        
        Long failedCount = transactionRepository.countByStatusAndDateRange(
            TransactionStatus.FAILED, startDate, endDate);
        data.failedTransactions = failedCount != null ? failedCount : 0L;
        
        data.processingFees = data.totalRevenue.multiply(BigDecimal.valueOf(0.025)).setScale(2, RoundingMode.HALF_UP);
        
        // Calculate platform fees (assume 3% of total revenue)
        data.platformFees = data.totalRevenue.multiply(BigDecimal.valueOf(0.03)).setScale(2, RoundingMode.HALF_UP);
        
        // Calculate provider earnings (total revenue - platform fees - processing fees)
        data.providerEarnings = data.totalRevenue
            .subtract(data.platformFees)
            .subtract(data.processingFees);
        
        // Get refund data using existing methods
        data.refundAmount = transactionRepository.sumRefundAmountByDateRange(
            Transaction.TransactionType.REFUND, TransactionStatus.SUCCESS, startDate, endDate);
        if (data.refundAmount == null) data.refundAmount = BigDecimal.ZERO;
        
        Long refundCountLong = transactionRepository.countRefundsByDateRange(
            Transaction.TransactionType.REFUND, TransactionStatus.SUCCESS, startDate, endDate);
        data.refundCount = refundCountLong != null ? refundCountLong.intValue() : 0;
        
        // Get dispute data using existing methods
        data.disputeAmount = disputeRepository.sumDisputedAmountByStatusAndDateRange(
            DisputeStatus.LOST, startDate, endDate);
        if (data.disputeAmount == null) data.disputeAmount = BigDecimal.ZERO;
        
        Long disputeCountLong = disputeRepository.countByStatusAndDateRange(
            DisputeStatus.LOST, startDate, endDate);
        data.disputeCount = disputeCountLong != null ? disputeCountLong.intValue() : 0;
        
        // Calculate tax amount (assume 10% of platform fees)
        data.taxAmount = data.platformFees.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
        
        // Calculate net revenue
        data.netRevenue = data.totalRevenue
            .subtract(data.refundAmount)
            .subtract(data.disputeAmount)
            .subtract(data.taxAmount);
        
        return data;
    }

    private YearlyFinancialData calculateYearlyFinancialData(LocalDateTime startDate, LocalDateTime endDate) {
        YearlyFinancialData data = new YearlyFinancialData();
        
        // Get yearly transaction summaries using existing methods
        data.totalRevenue = transactionRepository.sumAmountByStatusAndDateRange(
            TransactionStatus.SUCCESS, startDate, endDate);
        if (data.totalRevenue == null) data.totalRevenue = BigDecimal.ZERO;
        
        data.totalTransactions = transactionRepository.countByStatusAndDateRange(
            TransactionStatus.SUCCESS, startDate, endDate);
        if (data.totalTransactions == null) data.totalTransactions = 0L;
        
        data.successfulTransactions = data.totalTransactions;
        
        Long failedCount = transactionRepository.countByStatusAndDateRange(
            TransactionStatus.FAILED, startDate, endDate);
        data.failedTransactions = failedCount != null ? failedCount : 0L;
        
        data.processingFees = data.totalRevenue.multiply(BigDecimal.valueOf(0.025)).setScale(2, RoundingMode.HALF_UP);
        
        // Calculate platform fees (assume 3% of total revenue)
        data.platformFees = data.totalRevenue.multiply(BigDecimal.valueOf(0.03)).setScale(2, RoundingMode.HALF_UP);
        
        // Calculate provider earnings
        data.providerEarnings = data.totalRevenue
            .subtract(data.platformFees)
            .subtract(data.processingFees);
        
        // Get yearly refund data using existing methods
        data.refundAmount = transactionRepository.sumRefundAmountByDateRange(
            Transaction.TransactionType.REFUND, TransactionStatus.SUCCESS, startDate, endDate);
        if (data.refundAmount == null) data.refundAmount = BigDecimal.ZERO;
        
        Long refundCountLong = transactionRepository.countRefundsByDateRange(
            Transaction.TransactionType.REFUND, TransactionStatus.SUCCESS, startDate, endDate);
        data.refundCount = refundCountLong != null ? refundCountLong.intValue() : 0;
        
        // Get yearly dispute data using existing methods
        data.disputeAmount = disputeRepository.sumDisputedAmountByStatusAndDateRange(
            DisputeStatus.LOST, startDate, endDate);
        if (data.disputeAmount == null) data.disputeAmount = BigDecimal.ZERO;
        
        Long disputeCountLong = disputeRepository.countByStatusAndDateRange(
            DisputeStatus.LOST, startDate, endDate);
        data.disputeCount = disputeCountLong != null ? disputeCountLong.intValue() : 0;
        
        // Calculate tax amount (assume 15% of platform fees for yearly)
        data.taxAmount = data.platformFees.multiply(BigDecimal.valueOf(0.15)).setScale(2, RoundingMode.HALF_UP);
        
        // Calculate net revenue
        data.netRevenue = data.totalRevenue
            .subtract(data.refundAmount)
            .subtract(data.disputeAmount)
            .subtract(data.taxAmount);
        
        return data;
    }

    // Public API Methods

    public List<FinancialReport> getReportsByPeriod(int year, Integer month) {
        if (month != null) {
            return financialReportRepository.findReportsByYearAndMonth(year, month);
        } else {
            return financialReportRepository.findReportsByYear(year);
        }
    }

    public Optional<FinancialReport> getReportById(Long reportId) {
        return financialReportRepository.findById(reportId);
    }

    public List<FinancialReport> getPendingReports() {
        return financialReportRepository.findByStatus(FinancialReport.ReportStatus.GENERATING);
    }

    // Data Classes

    private static class MonthlyFinancialData {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal providerEarnings = BigDecimal.ZERO;
        BigDecimal platformFees = BigDecimal.ZERO;
        BigDecimal processingFees = BigDecimal.ZERO;
        Long totalTransactions = 0L;
        Long successfulTransactions = 0L;
        Long failedTransactions = 0L;
        BigDecimal refundAmount = BigDecimal.ZERO;
        Integer refundCount = 0;
        BigDecimal disputeAmount = BigDecimal.ZERO;
        Integer disputeCount = 0;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;
    }

    private static class YearlyFinancialData {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal providerEarnings = BigDecimal.ZERO;
        BigDecimal platformFees = BigDecimal.ZERO;
        BigDecimal processingFees = BigDecimal.ZERO;
        Long totalTransactions = 0L;
        Long successfulTransactions = 0L;
        Long failedTransactions = 0L;
        BigDecimal refundAmount = BigDecimal.ZERO;
        Integer refundCount = 0;
        BigDecimal disputeAmount = BigDecimal.ZERO;
        Integer disputeCount = 0;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;
    }

    public static class ProviderFinancialSummary {
        public Long providerId;
        public int year;
        public int month;
        public LocalDate startDate;
        public LocalDate endDate;
        public BigDecimal totalEarnings = BigDecimal.ZERO;
        public BigDecimal netEarnings = BigDecimal.ZERO;
        public BigDecimal platformFees = BigDecimal.ZERO;
        public BigDecimal processingFees = BigDecimal.ZERO;
        public Long totalTransactions = 0L;
        public Long successfulTransactions = 0L;
    }
}
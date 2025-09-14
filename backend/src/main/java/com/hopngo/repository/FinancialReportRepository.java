package com.hopngo.repository;

import com.hopngo.entity.FinancialReport;
import com.hopngo.entity.FinancialReport.PeriodType;
import com.hopngo.entity.FinancialReport.ReportStatus;
import com.hopngo.entity.FinancialReport.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, Long> {
    
    Optional<FinancialReport> findByReportId(String reportId);
    
    List<FinancialReport> findByReportType(ReportType reportType);
    
    List<FinancialReport> findByStatus(ReportStatus status);
    
    List<FinancialReport> findByPeriodType(PeriodType periodType);
    
    Page<FinancialReport> findByReportTypeAndStatus(ReportType reportType, ReportStatus status, Pageable pageable);
    
    List<FinancialReport> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.periodStart = :periodStart AND fr.periodEnd = :periodEnd " +
           "AND fr.reportType = :reportType")
    Optional<FinancialReport> findByPeriodAndType(@Param("periodStart") LocalDate periodStart,
                                                 @Param("periodEnd") LocalDate periodEnd,
                                                 @Param("reportType") ReportType reportType);
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.year = :year AND fr.periodType = 'YEARLY' " +
           "AND fr.reportType = :reportType")
    Optional<FinancialReport> findYearlyReportByYearAndType(@Param("year") Integer year,
                                                           @Param("reportType") ReportType reportType);
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.year = :year AND fr.month = :month " +
           "AND fr.periodType = 'MONTHLY' AND fr.reportType = :reportType")
    Optional<FinancialReport> findMonthlyReportByYearMonthAndType(@Param("year") Integer year,
                                                                 @Param("month") Integer month,
                                                                 @Param("reportType") ReportType reportType);
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.year = :year AND fr.quarter = :quarter " +
           "AND fr.periodType = 'QUARTERLY' AND fr.reportType = :reportType")
    Optional<FinancialReport> findQuarterlyReportByYearQuarterAndType(@Param("year") Integer year,
                                                                     @Param("quarter") Integer quarter,
                                                                     @Param("reportType") ReportType reportType);
    
    // Status-based queries
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.status = 'GENERATING' " +
           "AND fr.generationStartedAt <= :cutoffTime")
    List<FinancialReport> findStaleGeneratingReports(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.status = 'COMPLETED' " +
           "ORDER BY fr.generationCompletedAt DESC")
    Page<FinancialReport> findCompletedReportsOrderByCompletedAt(Pageable pageable);
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.status = 'FAILED' " +
           "ORDER BY fr.createdAt DESC")
    List<FinancialReport> findFailedReports();
    
    // Recent reports
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.createdAt >= :fromDate " +
           "ORDER BY fr.createdAt DESC")
    List<FinancialReport> findRecentReports(@Param("fromDate") LocalDateTime fromDate);
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.generationCompletedAt >= :fromDate " +
           "AND fr.status = 'COMPLETED' ORDER BY fr.generationCompletedAt DESC")
    List<FinancialReport> findRecentlyCompletedReports(@Param("fromDate") LocalDateTime fromDate);
    
    // Period-based queries
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.periodStart >= :startDate " +
           "AND fr.periodEnd <= :endDate ORDER BY fr.periodStart DESC")
    List<FinancialReport> findReportsByPeriodRange(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.year = :year ORDER BY fr.month ASC, fr.quarter ASC")
    List<FinancialReport> findReportsByYear(@Param("year") Integer year);
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.year = :year AND fr.month = :month")
    List<FinancialReport> findReportsByYearAndMonth(@Param("year") Integer year,
                                                   @Param("month") Integer month);
    
    // Statistics queries
    @Query("SELECT COUNT(fr) FROM FinancialReport fr WHERE fr.status = :status " +
           "AND fr.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndDateRange(@Param("status") ReportStatus status,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT fr.reportType, COUNT(fr) as reportCount " +
           "FROM FinancialReport fr WHERE fr.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY fr.reportType ORDER BY reportCount DESC")
    List<Object[]> getReportCountByType(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT fr.periodType, COUNT(fr) as reportCount " +
           "FROM FinancialReport fr WHERE fr.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY fr.periodType ORDER BY reportCount DESC")
    List<Object[]> getReportCountByPeriodType(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    // Generation performance queries
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, fr.generationStartedAt, fr.generationCompletedAt)) " +
           "FROM FinancialReport fr WHERE fr.status = 'COMPLETED' " +
           "AND fr.generationStartedAt IS NOT NULL AND fr.generationCompletedAt IS NOT NULL " +
           "AND fr.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageGenerationTimeInSeconds(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT fr.reportType, AVG(TIMESTAMPDIFF(SECOND, fr.generationStartedAt, fr.generationCompletedAt)) as avgTime " +
           "FROM FinancialReport fr WHERE fr.status = 'COMPLETED' " +
           "AND fr.generationStartedAt IS NOT NULL AND fr.generationCompletedAt IS NOT NULL " +
           "AND fr.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY fr.reportType")
    List<Object[]> getAverageGenerationTimeByType(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
    
    // File availability queries
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.csvFilePath IS NOT NULL " +
           "AND fr.status = 'COMPLETED'")
    List<FinancialReport> findReportsWithCsvFiles();
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.pdfFilePath IS NOT NULL " +
           "AND fr.status = 'COMPLETED'")
    List<FinancialReport> findReportsWithPdfFiles();
    
    @Query("SELECT fr FROM FinancialReport fr WHERE (fr.csvFilePath IS NULL OR fr.pdfFilePath IS NULL) " +
           "AND fr.status = 'COMPLETED'")
    List<FinancialReport> findCompletedReportsWithMissingFiles();
    
    // Latest reports by type and period
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.reportType = :reportType " +
           "AND fr.periodType = :periodType AND fr.status = 'COMPLETED' " +
           "ORDER BY fr.periodStart DESC")
    List<FinancialReport> findLatestReportsByTypeAndPeriod(@Param("reportType") ReportType reportType,
                                                          @Param("periodType") PeriodType periodType,
                                                          Pageable pageable);
    
    // Monthly aggregation
    @Query("SELECT YEAR(fr.createdAt) as year, MONTH(fr.createdAt) as month, fr.reportType, " +
           "COUNT(fr) as reportCount, " +
           "COUNT(CASE WHEN fr.status = 'COMPLETED' THEN 1 END) as completedCount, " +
           "COUNT(CASE WHEN fr.status = 'FAILED' THEN 1 END) as failedCount " +
           "FROM FinancialReport fr " +
           "GROUP BY YEAR(fr.createdAt), MONTH(fr.createdAt), fr.reportType " +
           "ORDER BY YEAR(fr.createdAt) DESC, MONTH(fr.createdAt) DESC")
    List<Object[]> getMonthlyReportGenerationSummary();
    
    // Tax-specific queries
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.reportType = 'TAX' " +
           "AND fr.year = :year ORDER BY fr.createdAt DESC")
    List<FinancialReport> findTaxReportsByYear(@Param("year") Integer year);
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.reportType = 'TAX' " +
           "AND fr.status = 'COMPLETED' ORDER BY fr.year DESC, fr.createdAt DESC")
    List<FinancialReport> findCompletedTaxReports();
    
    // Provider-specific reports
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.reportType = 'PROVIDER' " +
           "AND fr.status = 'COMPLETED' ORDER BY fr.periodStart DESC")
    List<FinancialReport> findCompletedProviderReports();
    
    // Comprehensive reports
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.reportType = 'COMPREHENSIVE' " +
           "AND fr.status = 'COMPLETED' ORDER BY fr.periodStart DESC")
    List<FinancialReport> findCompletedComprehensiveReports();
    
    // Reports requiring regeneration (failed or old)
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.status = 'FAILED' " +
           "OR (fr.status = 'GENERATING' AND fr.generationStartedAt <= :cutoffTime)")
    List<FinancialReport> findReportsRequiringRegeneration(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // User-generated reports tracking
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.generatedBy = :userId " +
           "ORDER BY fr.createdAt DESC")
    List<FinancialReport> findReportsByUser(@Param("userId") String userId);
    
    @Query("SELECT fr.generatedBy, COUNT(fr) as reportCount " +
           "FROM FinancialReport fr WHERE fr.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY fr.generatedBy ORDER BY reportCount DESC")
    List<Object[]> getReportGenerationStatsByUser(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
}
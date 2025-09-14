package com.hopngo.repository;

import com.hopngo.entity.TaxDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TaxDocument entity operations
 */
@Repository
public interface TaxDocumentRepository extends JpaRepository<TaxDocument, Long> {

    /**
     * Find tax documents by jurisdiction
     */
    List<TaxDocument> findByJurisdiction(String jurisdiction);

    /**
     * Find tax documents by tax year
     */
    List<TaxDocument> findByTaxYear(Integer taxYear);

    /**
     * Find tax documents by jurisdiction and tax year
     */
    List<TaxDocument> findByJurisdictionAndTaxYear(String jurisdiction, Integer taxYear);

    /**
     * Find tax documents by document type
     */
    List<TaxDocument> findByDocumentType(TaxDocument.DocumentType documentType);

    /**
     * Find tax documents by status
     */
    List<TaxDocument> findByStatus(TaxDocument.DocumentStatus status);

    /**
     * Find tax documents by jurisdiction, tax year, and document type
     */
    Optional<TaxDocument> findByJurisdictionAndTaxYearAndDocumentType(
        String jurisdiction, 
        Integer taxYear, 
        TaxDocument.DocumentType documentType
    );

    /**
     * Find tax documents created within a date range
     */
    List<TaxDocument> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find tax documents by tax year range
     */
    List<TaxDocument> findByTaxYearBetween(Integer startYear, Integer endYear);

    /**
     * Find pending tax documents (draft or failed status)
     */
    @Query("SELECT td FROM TaxDocument td WHERE td.status IN ('DRAFT', 'FAILED') ORDER BY td.createdAt DESC")
    List<TaxDocument> findPendingDocuments();

    /**
     * Find completed tax documents for a jurisdiction
     */
    @Query("SELECT td FROM TaxDocument td WHERE td.jurisdiction = :jurisdiction AND td.status = 'COMPLETED' ORDER BY td.taxYear DESC, td.documentType")
    List<TaxDocument> findCompletedByJurisdiction(@Param("jurisdiction") String jurisdiction);

    /**
     * Find approved tax documents
     */
    List<TaxDocument> findByStatusOrderByTaxYearDescCreatedAtDesc(TaxDocument.DocumentStatus status);

    /**
     * Find tax documents that need approval
     */
    @Query("SELECT td FROM TaxDocument td WHERE td.status = 'COMPLETED' AND td.approvedBy IS NULL ORDER BY td.createdAt ASC")
    List<TaxDocument> findDocumentsNeedingApproval();

    /**
     * Find tax documents by approved by user
     */
    List<TaxDocument> findByApprovedBy(String approvedBy);

    /**
     * Find tax documents approved within a date range
     */
    List<TaxDocument> findByApprovedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count tax documents by jurisdiction and year
     */
    @Query("SELECT COUNT(td) FROM TaxDocument td WHERE td.jurisdiction = :jurisdiction AND td.taxYear = :taxYear")
    Long countByJurisdictionAndTaxYear(@Param("jurisdiction") String jurisdiction, @Param("taxYear") Integer taxYear);

    /**
     * Count tax documents by status
     */
    Long countByStatus(TaxDocument.DocumentStatus status);

    /**
     * Find latest tax documents by jurisdiction
     */
    @Query("SELECT td FROM TaxDocument td WHERE td.jurisdiction = :jurisdiction AND td.taxYear = (SELECT MAX(td2.taxYear) FROM TaxDocument td2 WHERE td2.jurisdiction = :jurisdiction)")
    List<TaxDocument> findLatestByJurisdiction(@Param("jurisdiction") String jurisdiction);

    /**
     * Find tax documents with content size greater than specified bytes
     */
    @Query("SELECT td FROM TaxDocument td WHERE td.fileSize > :minSize ORDER BY td.fileSize DESC")
    List<TaxDocument> findLargeDocuments(@Param("minSize") Long minSize);

    /**
     * Find tax documents without content
     */
    @Query("SELECT td FROM TaxDocument td WHERE td.documentContent IS NULL OR td.fileSize = 0 OR td.fileSize IS NULL")
    List<TaxDocument> findDocumentsWithoutContent();

    /**
     * Find tax documents by date range (start and end dates)
     */
    @Query("SELECT td FROM TaxDocument td WHERE td.startDate >= :startDate AND td.endDate <= :endDate")
    List<TaxDocument> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find tax documents with financial data
     */
    @Query("SELECT td FROM TaxDocument td WHERE td.grossRevenue IS NOT NULL OR td.netRevenue IS NOT NULL OR td.taxableIncome IS NOT NULL")
    List<TaxDocument> findDocumentsWithFinancialData();

    /**
     * Find tax documents by jurisdiction and status with pagination support
     */
    List<TaxDocument> findByJurisdictionAndStatusOrderByTaxYearDescCreatedAtDesc(
        String jurisdiction, 
        TaxDocument.DocumentStatus status
    );

    /**
     * Find tax documents created by system vs manual
     */
    List<TaxDocument> findByGeneratedBy(String generatedBy);

    /**
     * Find tax documents that are overdue for approval (completed more than X days ago)
     */
    @Query("SELECT td FROM TaxDocument td WHERE td.status = 'COMPLETED' AND td.approvedBy IS NULL AND td.createdAt < :cutoffDate ORDER BY td.createdAt ASC")
    List<TaxDocument> findOverdueForApproval(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find tax documents summary by jurisdiction and year
     */
    @Query("SELECT td.jurisdiction, td.taxYear, td.documentType, COUNT(td), td.status " +
           "FROM TaxDocument td " +
           "WHERE td.jurisdiction = :jurisdiction AND td.taxYear = :taxYear " +
           "GROUP BY td.jurisdiction, td.taxYear, td.documentType, td.status " +
           "ORDER BY td.documentType")
    List<Object[]> findDocumentSummaryByJurisdictionAndYear(
        @Param("jurisdiction") String jurisdiction, 
        @Param("taxYear") Integer taxYear
    );

    /**
     * Find tax documents with notes containing specific text
     */
    @Query("SELECT td FROM TaxDocument td WHERE td.notes LIKE %:searchText% ORDER BY td.createdAt DESC")
    List<TaxDocument> findByNotesContaining(@Param("searchText") String searchText);

    /**
     * Delete old draft documents (cleanup)
     */
    @Query("DELETE FROM TaxDocument td WHERE td.status = 'DRAFT' AND td.createdAt < :cutoffDate")
    void deleteOldDrafts(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find documents that can be archived (old submitted documents)
     */
    @Query("SELECT td FROM TaxDocument td WHERE td.status = 'SUBMITTED' AND td.createdAt < :cutoffDate ORDER BY td.createdAt ASC")
    List<TaxDocument> findDocumentsForArchival(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Check if tax documents exist for a specific jurisdiction and year
     */
    boolean existsByJurisdictionAndTaxYear(String jurisdiction, Integer taxYear);

    /**
     * Check if a specific document type exists for jurisdiction and year
     */
    boolean existsByJurisdictionAndTaxYearAndDocumentType(
        String jurisdiction, 
        Integer taxYear, 
        TaxDocument.DocumentType documentType
    );

    /**
     * Find the most recent tax document for each jurisdiction
     */
    @Query("SELECT td FROM TaxDocument td WHERE (td.jurisdiction, td.taxYear, td.createdAt) IN " +
           "(SELECT td2.jurisdiction, MAX(td2.taxYear), MAX(td2.createdAt) " +
           "FROM TaxDocument td2 " +
           "GROUP BY td2.jurisdiction) " +
           "ORDER BY td.jurisdiction")
    List<TaxDocument> findMostRecentByJurisdiction();

    /**
     * Find tax documents with specific financial thresholds
     */
    @Query("SELECT td FROM TaxDocument td WHERE " +
           "(td.grossRevenue IS NOT NULL AND td.grossRevenue >= :minRevenue) OR " +
           "(td.taxableIncome IS NOT NULL AND td.taxableIncome >= :minTaxableIncome)")
    List<TaxDocument> findDocumentsAboveThresholds(
        @Param("minRevenue") java.math.BigDecimal minRevenue,
        @Param("minTaxableIncome") java.math.BigDecimal minTaxableIncome
    );

    /**
     * Get document statistics by year
     */
    @Query("SELECT td.taxYear, COUNT(td), " +
           "SUM(CASE WHEN td.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN td.status = 'APPROVED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN td.status = 'SUBMITTED' THEN 1 ELSE 0 END) " +
           "FROM TaxDocument td " +
           "GROUP BY td.taxYear " +
           "ORDER BY td.taxYear DESC")
    List<Object[]> getDocumentStatisticsByYear();
}
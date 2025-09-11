package com.hopngo.support.repository;

import com.hopngo.support.entity.CannedReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CannedReplyRepository extends JpaRepository<CannedReply, Long> {

    // Find by category
    List<CannedReply> findByCategoryOrderByTitleAsc(String category);
    
    Page<CannedReply> findByCategoryOrderByTitleAsc(String category, Pageable pageable);
    
    // Find by category (case insensitive)
    @Query("SELECT cr FROM CannedReply cr WHERE LOWER(cr.category) = LOWER(:category) ORDER BY cr.title ASC")
    List<CannedReply> findByCategoryIgnoreCaseOrderByTitleAsc(@Param("category") String category);
    
    @Query("SELECT cr FROM CannedReply cr WHERE LOWER(cr.category) = LOWER(:category) ORDER BY cr.title ASC")
    Page<CannedReply> findByCategoryIgnoreCaseOrderByTitleAsc(@Param("category") String category, Pageable pageable);
    
    // Find by created by
    List<CannedReply> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    
    Page<CannedReply> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);
    
    // Search by title
    @Query("SELECT cr FROM CannedReply cr WHERE LOWER(cr.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY cr.title ASC")
    List<CannedReply> searchByTitle(@Param("query") String query);
    
    @Query("SELECT cr FROM CannedReply cr WHERE LOWER(cr.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY cr.title ASC")
    Page<CannedReply> searchByTitle(@Param("query") String query, Pageable pageable);
    
    // Search by title or body
    @Query("SELECT cr FROM CannedReply cr WHERE LOWER(cr.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(cr.body) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY cr.title ASC")
    List<CannedReply> searchByTitleOrBody(@Param("query") String query);
    
    @Query("SELECT cr FROM CannedReply cr WHERE LOWER(cr.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(cr.body) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY cr.title ASC")
    Page<CannedReply> searchByTitleOrBody(@Param("query") String query, Pageable pageable);
    
    // Find by title (exact match)
    Optional<CannedReply> findByTitle(String title);
    
    // Find by title (case insensitive)
    @Query("SELECT cr FROM CannedReply cr WHERE LOWER(cr.title) = LOWER(:title)")
    Optional<CannedReply> findByTitleIgnoreCase(@Param("title") String title);
    
    // Find all ordered by category and title
    @Query("SELECT cr FROM CannedReply cr ORDER BY cr.category ASC, cr.title ASC")
    List<CannedReply> findAllOrderByCategoryAndTitle();
    
    @Query("SELECT cr FROM CannedReply cr ORDER BY cr.category ASC, cr.title ASC")
    Page<CannedReply> findAllOrderByCategoryAndTitle(Pageable pageable);
    
    // Find recent canned replies
    List<CannedReply> findByCreatedAtAfterOrderByCreatedAtDesc(Instant since);
    
    // Find updated canned replies
    List<CannedReply> findByUpdatedAtAfterOrderByUpdatedAtDesc(Instant since);
    
    // Get distinct categories
    @Query("SELECT DISTINCT cr.category FROM CannedReply cr WHERE cr.category IS NOT NULL ORDER BY cr.category ASC")
    List<String> findDistinctCategories();
    
    // Count by category
    long countByCategory(String category);
    
    // Count by created by
    long countByCreatedBy(String createdBy);
    
    // Find system created canned replies
    @Query("SELECT cr FROM CannedReply cr WHERE LOWER(cr.createdBy) = 'system' ORDER BY cr.category ASC, cr.title ASC")
    List<CannedReply> findSystemCannedReplies();
    
    // Find user created canned replies
    @Query("SELECT cr FROM CannedReply cr WHERE LOWER(cr.createdBy) != 'system' OR cr.createdBy IS NULL ORDER BY cr.createdAt DESC")
    List<CannedReply> findUserCannedReplies();
    
    @Query("SELECT cr FROM CannedReply cr WHERE LOWER(cr.createdBy) != 'system' OR cr.createdBy IS NULL ORDER BY cr.createdAt DESC")
    Page<CannedReply> findUserCannedReplies(Pageable pageable);
    
    // Check if title exists (for validation)
    boolean existsByTitle(String title);
    
    // Check if title exists (case insensitive)
    @Query("SELECT COUNT(cr) > 0 FROM CannedReply cr WHERE LOWER(cr.title) = LOWER(:title)")
    boolean existsByTitleIgnoreCase(@Param("title") String title);
    
    // Check if title exists excluding specific ID (for updates)
    @Query("SELECT COUNT(cr) > 0 FROM CannedReply cr WHERE LOWER(cr.title) = LOWER(:title) AND cr.id != :excludeId")
    boolean existsByTitleIgnoreCaseAndIdNot(@Param("title") String title, @Param("excludeId") Long excludeId);
    
    // Get category statistics
    @Query("SELECT cr.category, COUNT(cr) FROM CannedReply cr GROUP BY cr.category ORDER BY COUNT(cr) DESC")
    List<Object[]> getCategoryStatistics();
    
    // Find most recently used canned replies (this would require usage tracking)
    @Query("SELECT cr FROM CannedReply cr ORDER BY cr.updatedAt DESC")
    List<CannedReply> findMostRecentlyUpdated(Pageable pageable);
}
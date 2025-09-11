package com.hopngo.support.repository;

import com.hopngo.support.entity.HelpArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface HelpArticleRepository extends JpaRepository<HelpArticle, Long> {

    // Find by slug
    Optional<HelpArticle> findBySlug(String slug);
    
    // Find published articles
    List<HelpArticle> findByPublishedTrueOrderByCreatedAtDesc();
    
    Page<HelpArticle> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);
    
    // Find draft articles
    List<HelpArticle> findByPublishedFalseOrderByCreatedAtDesc();
    
    Page<HelpArticle> findByPublishedFalseOrderByCreatedAtDesc(Pageable pageable);
    
    // Find by author
    List<HelpArticle> findByAuthorIdOrderByCreatedAtDesc(String authorId);
    
    Page<HelpArticle> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);
    
    // Search published articles by title
    @Query("SELECT ha FROM HelpArticle ha WHERE ha.published = true AND LOWER(ha.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY ha.viewCount DESC, ha.createdAt DESC")
    List<HelpArticle> searchPublishedByTitle(@Param("query") String query);
    
    @Query("SELECT ha FROM HelpArticle ha WHERE ha.published = true AND LOWER(ha.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY ha.viewCount DESC, ha.createdAt DESC")
    Page<HelpArticle> searchPublishedByTitle(@Param("query") String query, Pageable pageable);
    
    // Search published articles by title or content
    @Query("SELECT ha FROM HelpArticle ha WHERE ha.published = true AND (LOWER(ha.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(ha.bodyMd) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY ha.viewCount DESC, ha.createdAt DESC")
    List<HelpArticle> searchPublishedByTitleOrContent(@Param("query") String query);
    
    @Query("SELECT ha FROM HelpArticle ha WHERE ha.published = true AND (LOWER(ha.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(ha.bodyMd) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY ha.viewCount DESC, ha.createdAt DESC")
    Page<HelpArticle> searchPublishedByTitleOrContent(@Param("query") String query, Pageable pageable);
    
    // Full-text search using PostgreSQL's text search
    @Query(value = "SELECT * FROM help_articles WHERE published = true AND (to_tsvector('english', title) @@ plainto_tsquery('english', :query) OR to_tsvector('english', body_md) @@ plainto_tsquery('english', :query)) ORDER BY view_count DESC, created_at DESC", nativeQuery = true)
    List<HelpArticle> fullTextSearchPublished(@Param("query") String query);
    
    @Query(value = "SELECT * FROM help_articles WHERE published = true AND (to_tsvector('english', title) @@ plainto_tsquery('english', :query) OR to_tsvector('english', body_md) @@ plainto_tsquery('english', :query)) ORDER BY view_count DESC, created_at DESC", 
           countQuery = "SELECT COUNT(*) FROM help_articles WHERE published = true AND (to_tsvector('english', title) @@ plainto_tsquery('english', :query) OR to_tsvector('english', body_md) @@ plainto_tsquery('english', :query))",
           nativeQuery = true)
    Page<HelpArticle> fullTextSearchPublished(@Param("query") String query, Pageable pageable);
    
    // Find by tag
    @Query("SELECT ha FROM HelpArticle ha WHERE ha.published = true AND :tag = ANY(ha.tags) ORDER BY ha.viewCount DESC, ha.createdAt DESC")
    List<HelpArticle> findPublishedByTag(@Param("tag") String tag);
    
    @Query("SELECT ha FROM HelpArticle ha WHERE ha.published = true AND :tag = ANY(ha.tags) ORDER BY ha.viewCount DESC, ha.createdAt DESC")
    Page<HelpArticle> findPublishedByTag(@Param("tag") String tag, Pageable pageable);
    
    // Find by multiple tags (articles containing any of the tags)
    @Query("SELECT DISTINCT ha FROM HelpArticle ha WHERE ha.published = true AND ha.tags && CAST(:tags AS text[]) ORDER BY ha.viewCount DESC, ha.createdAt DESC")
    List<HelpArticle> findPublishedByAnyTag(@Param("tags") String[] tags);
    
    @Query("SELECT DISTINCT ha FROM HelpArticle ha WHERE ha.published = true AND ha.tags && CAST(:tags AS text[]) ORDER BY ha.viewCount DESC, ha.createdAt DESC")
    Page<HelpArticle> findPublishedByAnyTag(@Param("tags") String[] tags, Pageable pageable);
    
    // Find most popular articles (by view count)
    @Query("SELECT ha FROM HelpArticle ha WHERE ha.published = true ORDER BY ha.viewCount DESC, ha.createdAt DESC")
    List<HelpArticle> findMostPopular(Pageable pageable);
    
    // Find recent articles
    List<HelpArticle> findByPublishedTrueAndCreatedAtAfterOrderByCreatedAtDesc(Instant since);
    
    // Find recently updated articles
    List<HelpArticle> findByPublishedTrueAndUpdatedAtAfterOrderByUpdatedAtDesc(Instant since);
    
    // Get all distinct tags from published articles
    @Query(value = "SELECT DISTINCT unnest(tags) as tag FROM help_articles WHERE published = true AND tags IS NOT NULL ORDER BY tag", nativeQuery = true)
    List<String> findAllPublishedTags();
    
    // Get tag statistics
    @Query(value = "SELECT unnest(tags) as tag, COUNT(*) as count FROM help_articles WHERE published = true AND tags IS NOT NULL GROUP BY tag ORDER BY count DESC, tag", nativeQuery = true)
    List<Object[]> getTagStatistics();
    
    // Check if slug exists
    boolean existsBySlug(String slug);
    
    // Check if slug exists excluding specific ID (for updates)
    @Query("SELECT COUNT(ha) > 0 FROM HelpArticle ha WHERE ha.slug = :slug AND ha.id != :excludeId")
    boolean existsBySlugAndIdNot(@Param("slug") String slug, @Param("excludeId") Long excludeId);
    
    // Count published articles
    long countByPublishedTrue();
    
    // Count draft articles
    long countByPublishedFalse();
    
    // Count articles by author
    long countByAuthorId(String authorId);
    
    // Count articles by tag
    @Query("SELECT COUNT(ha) FROM HelpArticle ha WHERE ha.published = true AND :tag = ANY(ha.tags)")
    long countByTag(@Param("tag") String tag);
    
    // Increment view count
    @Modifying
    @Query("UPDATE HelpArticle ha SET ha.viewCount = ha.viewCount + 1 WHERE ha.id = :id")
    void incrementViewCount(@Param("id") Long id);
    
    // Find articles with high view counts (for trending)
    @Query("SELECT ha FROM HelpArticle ha WHERE ha.published = true AND ha.viewCount >= :minViews ORDER BY ha.viewCount DESC, ha.updatedAt DESC")
    List<HelpArticle> findTrendingArticles(@Param("minViews") Integer minViews, Pageable pageable);
    
    // Find related articles by tags (excluding current article)
    @Query("SELECT DISTINCT ha FROM HelpArticle ha WHERE ha.published = true AND ha.id != :excludeId AND ha.tags && CAST(:tags AS text[]) ORDER BY ha.viewCount DESC")
    List<HelpArticle> findRelatedArticles(@Param("excludeId") Long excludeId, @Param("tags") String[] tags, Pageable pageable);
    
    // Search with tag filter
    @Query("SELECT ha FROM HelpArticle ha WHERE ha.published = true AND :tag = ANY(ha.tags) AND (LOWER(ha.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(ha.bodyMd) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY ha.viewCount DESC, ha.createdAt DESC")
    List<HelpArticle> searchPublishedByTagAndQuery(@Param("tag") String tag, @Param("query") String query);
    
    @Query("SELECT ha FROM HelpArticle ha WHERE ha.published = true AND :tag = ANY(ha.tags) AND (LOWER(ha.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(ha.bodyMd) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY ha.viewCount DESC, ha.createdAt DESC")
    Page<HelpArticle> searchPublishedByTagAndQuery(@Param("tag") String tag, @Param("query") String query, Pageable pageable);
}
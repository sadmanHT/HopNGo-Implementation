package com.hopngo.analytics.repository;

import com.hopngo.analytics.entity.ItemPopularity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemPopularityRepository extends JpaRepository<ItemPopularity, Long> {

    /**
     * Find item popularity by item ID and type
     */
    Optional<ItemPopularity> findByItemIdAndItemType(String itemId, ItemPopularity.ItemType itemType);

    /**
     * Find most popular items by type
     */
    @Query("SELECT i FROM ItemPopularity i WHERE i.itemType = :itemType ORDER BY i.popularityScore DESC")
    List<ItemPopularity> findMostPopularByType(@Param("itemType") ItemPopularity.ItemType itemType, 
                                              org.springframework.data.domain.Pageable pageable);

    /**
     * Find trending items (high recency score)
     */
    @Query("SELECT i FROM ItemPopularity i WHERE i.itemType = :itemType ORDER BY i.recencyScore DESC")
    List<ItemPopularity> findTrendingByType(@Param("itemType") ItemPopularity.ItemType itemType,
                                           org.springframework.data.domain.Pageable pageable);

    /**
     * Find items with most likes
     */
    @Query("SELECT i FROM ItemPopularity i WHERE i.itemType = :itemType ORDER BY i.likes DESC")
    List<ItemPopularity> findMostLikedByType(@Param("itemType") ItemPopularity.ItemType itemType,
                                            org.springframework.data.domain.Pageable pageable);

    /**
     * Find items with most bookmarks
     */
    @Query("SELECT i FROM ItemPopularity i WHERE i.itemType = :itemType ORDER BY i.bookmarks DESC")
    List<ItemPopularity> findMostBookmarkedByType(@Param("itemType") ItemPopularity.ItemType itemType,
                                                 org.springframework.data.domain.Pageable pageable);

    /**
     * Increment likes for an item
     */
    @Modifying
    @Query("UPDATE ItemPopularity i SET i.likes = i.likes + 1 WHERE i.itemId = :itemId AND i.itemType = :itemType")
    int incrementLikes(@Param("itemId") String itemId, @Param("itemType") ItemPopularity.ItemType itemType);

    /**
     * Increment bookmarks for an item
     */
    @Modifying
    @Query("UPDATE ItemPopularity i SET i.bookmarks = i.bookmarks + 1 WHERE i.itemId = :itemId AND i.itemType = :itemType")
    int incrementBookmarks(@Param("itemId") String itemId, @Param("itemType") ItemPopularity.ItemType itemType);

    /**
     * Increment views for an item
     */
    @Modifying
    @Query("UPDATE ItemPopularity i SET i.views = i.views + 1 WHERE i.itemId = :itemId AND i.itemType = :itemType")
    int incrementViews(@Param("itemId") String itemId, @Param("itemType") ItemPopularity.ItemType itemType);

    /**
     * Update popularity score for an item
     */
    @Modifying
    @Query("UPDATE ItemPopularity i SET i.popularityScore = :score WHERE i.itemId = :itemId AND i.itemType = :itemType")
    int updatePopularityScore(@Param("itemId") String itemId, 
                             @Param("itemType") ItemPopularity.ItemType itemType,
                             @Param("score") java.math.BigDecimal score);

    /**
     * Update recency score for an item
     */
    @Modifying
    @Query("UPDATE ItemPopularity i SET i.recencyScore = :score WHERE i.itemId = :itemId AND i.itemType = :itemType")
    int updateRecencyScore(@Param("itemId") String itemId,
                          @Param("itemType") ItemPopularity.ItemType itemType,
                          @Param("score") java.math.BigDecimal score);

    /**
     * Find items that need recency score update (older than specified hours)
     */
    @Query("SELECT i FROM ItemPopularity i WHERE i.updatedAt < :cutoffTime")
    List<ItemPopularity> findItemsNeedingRecencyUpdate(@Param("cutoffTime") java.time.OffsetDateTime cutoffTime);

    /**
     * Get similar items based on engagement patterns
     */
    @Query("SELECT i FROM ItemPopularity i WHERE i.itemType = :itemType " +
           "AND i.itemId != :excludeItemId " +
           "AND ABS(i.likes - :likes) <= :threshold " +
           "AND ABS(i.bookmarks - :bookmarks) <= :threshold " +
           "ORDER BY ABS(i.likes - :likes) + ABS(i.bookmarks - :bookmarks)")
    List<ItemPopularity> findSimilarItems(@Param("itemType") ItemPopularity.ItemType itemType,
                                         @Param("excludeItemId") String excludeItemId,
                                         @Param("likes") Integer likes,
                                         @Param("bookmarks") Integer bookmarks,
                                         @Param("threshold") Integer threshold,
                                         org.springframework.data.domain.Pageable pageable);

    /**
     * Check if item popularity exists
     */
    boolean existsByItemIdAndItemType(String itemId, ItemPopularity.ItemType itemType);

    /**
     * Delete item popularity by item ID and type
     */
    void deleteByItemIdAndItemType(String itemId, ItemPopularity.ItemType itemType);

    /**
     * Find all items by type
     */
    List<ItemPopularity> findByItemType(ItemPopularity.ItemType itemType);
}
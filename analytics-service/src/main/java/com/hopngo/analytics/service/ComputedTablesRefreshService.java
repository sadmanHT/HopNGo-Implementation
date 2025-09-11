package com.hopngo.analytics.service;

import com.hopngo.analytics.entity.ItemPopularity;
import com.hopngo.analytics.entity.UserContentStats;
import com.hopngo.analytics.repository.EventRepository;
import com.hopngo.analytics.repository.ItemPopularityRepository;
import com.hopngo.analytics.repository.UserContentStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ComputedTablesRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(ComputedTablesRefreshService.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserContentStatsRepository userContentStatsRepository;

    @Autowired
    private ItemPopularityRepository itemPopularityRepository;

    /**
     * Refresh computed tables nightly at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void refreshComputedTables() {
        logger.info("Starting nightly refresh of computed tables");
        
        try {
            refreshUserContentStats();
            refreshItemPopularity();
            updateRecencyScores();
            
            logger.info("Successfully completed nightly refresh of computed tables");
        } catch (Exception e) {
            logger.error("Error during nightly refresh of computed tables", e);
            throw e;
        }
    }

    /**
     * Refresh user content statistics from events
     */
    @Transactional
    public void refreshUserContentStats() {
        logger.info("Refreshing user content stats");
        
        // Get all user stats from events
        List<Object[]> userStats = eventRepository.getUserContentStats();
        
        for (Object[] stats : userStats) {
            String userId = (String) stats[0];
            if (userId == null) continue;
            
            Integer contentCreated = ((Number) stats[1]).intValue();  // contentCreated column
            Integer contentLiked = ((Number) stats[2]).intValue();    // contentLiked column
            Integer contentBookmarked = ((Number) stats[3]).intValue(); // contentBookmarked column
            Integer contentShared = ((Number) stats[4]).intValue();   // contentShared column
            Integer contentViewed = ((Number) stats[5]).intValue();   // contentViewed column
            
            Optional<UserContentStats> existingStats = userContentStatsRepository.findByUserId(userId);
            
            UserContentStats userContentStats;
            if (existingStats.isPresent()) {
                userContentStats = existingStats.get();
            } else {
                userContentStats = new UserContentStats(userId);
            }
            
            userContentStats.setPostsCount(contentCreated);
            userContentStats.setLikesGiven(contentLiked);
            userContentStats.setLikesReceived(0); // This would need separate calculation
            userContentStats.setBookmarksCount(contentBookmarked);
            userContentStats.setFollowsCount(0); // This would need separate calculation
            
            userContentStatsRepository.save(userContentStats);
        }
        
        logger.info("Refreshed stats for {} users", userStats.size());
    }

    /**
     * Refresh item popularity from events
     */
    @Transactional
    public void refreshItemPopularity() {
        logger.info("Refreshing item popularity");
        
        // Get item stats from events
        List<Object[]> itemStats = eventRepository.getItemPopularityStats();
        
        for (Object[] stats : itemStats) {
            String itemId = (String) stats[0];
            String itemTypeStr = (String) stats[1];
            
            if (itemId == null || itemTypeStr == null) continue;
            
            ItemPopularity.ItemType itemType;
            try {
                itemType = ItemPopularity.ItemType.valueOf(itemTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown item type: {}", itemTypeStr);
                continue;
            }
            
            Integer likes = ((Number) stats[2]).intValue();     // likes column
            Integer bookmarks = ((Number) stats[3]).intValue(); // bookmarks column
            Integer views = ((Number) stats[4]).intValue();     // views column
            
            Optional<ItemPopularity> existingPopularity = itemPopularityRepository
                .findByItemIdAndItemType(itemId, itemType);
            
            ItemPopularity itemPopularity;
            if (existingPopularity.isPresent()) {
                itemPopularity = existingPopularity.get();
            } else {
                itemPopularity = new ItemPopularity(itemId, itemType);
            }
            
            itemPopularity.setLikes(likes);
            itemPopularity.setBookmarks(bookmarks);
            itemPopularity.setViews(views);
            
            // Calculate recency score based on creation time
            itemPopularity.calculateRecencyScore();
            
            // Calculate popularity score
            itemPopularity.calculatePopularityScore();
            
            itemPopularityRepository.save(itemPopularity);
        }
        
        logger.info("Refreshed popularity for {} items", itemStats.size());
    }

    /**
     * Update recency scores for all items
     */
    @Transactional
    public void updateRecencyScores() {
        logger.info("Updating recency scores");
        
        // Update recency scores for items that haven't been updated in the last hour
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusHours(1);
        List<ItemPopularity> itemsToUpdate = itemPopularityRepository.findItemsNeedingRecencyUpdate(cutoffTime);
        
        for (ItemPopularity item : itemsToUpdate) {
            item.calculateRecencyScore();
            item.calculatePopularityScore();
            itemPopularityRepository.save(item);
        }
        
        logger.info("Updated recency scores for {} items", itemsToUpdate.size());
    }

    /**
     * Manual refresh trigger for testing/admin purposes
     */
    public void manualRefresh() {
        logger.info("Manual refresh triggered");
        refreshComputedTables();
    }

    /**
     * Refresh stats for a specific user
     */
    @Transactional
    public void refreshUserStats(String userId) {
        logger.info("Refreshing stats for user: {}", userId);
        
        List<Object[]> statsResults = eventRepository.getUserContentStatsByUserId(userId);
        
        if (statsResults != null && !statsResults.isEmpty()) {
            Object[] stats = statsResults.get(0);
            Integer contentCreated = ((Number) stats[1]).intValue();  // contentCreated column
            Integer contentLiked = ((Number) stats[2]).intValue();    // contentLiked column
            Integer contentBookmarked = ((Number) stats[3]).intValue(); // contentBookmarked column
            Integer contentShared = ((Number) stats[4]).intValue();   // contentShared column
            Integer contentViewed = ((Number) stats[5]).intValue();   // contentViewed column
            
            Optional<UserContentStats> existingStats = userContentStatsRepository.findByUserId(userId);
            
            UserContentStats userContentStats;
            if (existingStats.isPresent()) {
                userContentStats = existingStats.get();
            } else {
                userContentStats = new UserContentStats(userId);
            }
            
            userContentStats.setPostsCount(contentCreated);
            userContentStats.setLikesGiven(contentLiked);
            userContentStats.setLikesReceived(0); // This would need separate calculation
            userContentStats.setBookmarksCount(contentBookmarked);
            userContentStats.setFollowsCount(0); // This would need separate calculation
            
            userContentStatsRepository.save(userContentStats);
        }
    }

    /**
     * Refresh popularity for a specific item
     */
    @Transactional
    public void refreshItemPopularity(String itemId, ItemPopularity.ItemType itemType) {
        logger.info("Refreshing popularity for item: {} ({})", itemId, itemType);
        
        List<Object[]> statsResults = eventRepository.getItemPopularityStatsByItem(itemId, itemType.name());
        
        if (statsResults != null && !statsResults.isEmpty()) {
            Object[] stats = statsResults.get(0);
            Integer likes = ((Number) stats[2]).intValue();  // likes column
            Integer bookmarks = ((Number) stats[3]).intValue();  // bookmarks column
            Integer views = ((Number) stats[4]).intValue();  // views column
            
            Optional<ItemPopularity> existingPopularity = itemPopularityRepository
                .findByItemIdAndItemType(itemId, itemType);
            
            ItemPopularity itemPopularity;
            if (existingPopularity.isPresent()) {
                itemPopularity = existingPopularity.get();
            } else {
                itemPopularity = new ItemPopularity(itemId, itemType);
            }
            
            itemPopularity.setLikes(likes);
            itemPopularity.setBookmarks(bookmarks);
            itemPopularity.setViews(views);
            itemPopularity.calculateRecencyScore();
            itemPopularity.calculatePopularityScore();
            
            itemPopularityRepository.save(itemPopularity);
        }
    }
}
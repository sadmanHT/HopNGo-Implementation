package com.hopngo.analytics.repository;

import com.hopngo.analytics.entity.UserContentStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserContentStatsRepository extends JpaRepository<UserContentStats, Long> {

    /**
     * Find user content stats by user ID
     */
    Optional<UserContentStats> findByUserId(String userId);

    /**
     * Find users with most posts
     */
    @Query("SELECT u FROM UserContentStats u ORDER BY u.postsCount DESC")
    List<UserContentStats> findTopContentCreators(org.springframework.data.domain.Pageable pageable);

    /**
     * Find users with most likes received
     */
    @Query("SELECT u FROM UserContentStats u ORDER BY u.likesReceived DESC")
    List<UserContentStats> findMostLikedUsers(org.springframework.data.domain.Pageable pageable);

    /**
     * Find users with most engagement (likes given + bookmarks + follows)
     */
    @Query("SELECT u FROM UserContentStats u ORDER BY (u.likesGiven + u.bookmarksCount + u.followsCount) DESC")
    List<UserContentStats> findMostEngagedUsers(org.springframework.data.domain.Pageable pageable);

    /**
     * Increment posts count for a user
     */
    @Modifying
    @Query("UPDATE UserContentStats u SET u.postsCount = u.postsCount + 1 WHERE u.userId = :userId")
    int incrementPostsCount(@Param("userId") String userId);

    /**
     * Increment likes given for a user
     */
    @Modifying
    @Query("UPDATE UserContentStats u SET u.likesGiven = u.likesGiven + 1 WHERE u.userId = :userId")
    int incrementLikesGiven(@Param("userId") String userId);

    /**
     * Increment likes received for a user
     */
    @Modifying
    @Query("UPDATE UserContentStats u SET u.likesReceived = u.likesReceived + 1 WHERE u.userId = :userId")
    int incrementLikesReceived(@Param("userId") String userId);

    /**
     * Increment bookmarks count for a user
     */
    @Modifying
    @Query("UPDATE UserContentStats u SET u.bookmarksCount = u.bookmarksCount + 1 WHERE u.userId = :userId")
    int incrementBookmarksCount(@Param("userId") String userId);

    /**
     * Increment follows count for a user
     */
    @Modifying
    @Query("UPDATE UserContentStats u SET u.followsCount = u.followsCount + 1 WHERE u.userId = :userId")
    int incrementFollowsCount(@Param("userId") String userId);

    /**
     * Get users with similar engagement patterns (for collaborative filtering)
     */
    @Query("SELECT u FROM UserContentStats u WHERE u.userId != :userId " +
           "AND ABS(u.likesGiven - :likesGiven) <= :threshold " +
           "AND ABS(u.bookmarksCount - :bookmarksCount) <= :threshold " +
           "ORDER BY ABS(u.likesGiven - :likesGiven) + ABS(u.bookmarksCount - :bookmarksCount)")
    List<UserContentStats> findSimilarUsers(@Param("userId") String userId,
                                           @Param("likesGiven") Integer likesGiven,
                                           @Param("bookmarksCount") Integer bookmarksCount,
                                           @Param("threshold") Integer threshold,
                                           org.springframework.data.domain.Pageable pageable);

    /**
     * Check if user stats exist
     */
    boolean existsByUserId(String userId);

    /**
     * Delete user stats by user ID
     */
    void deleteByUserId(String userId);
}
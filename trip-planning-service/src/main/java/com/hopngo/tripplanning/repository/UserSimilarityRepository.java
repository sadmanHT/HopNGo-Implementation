package com.hopngo.tripplanning.repository;

import com.hopngo.tripplanning.entity.UserSimilarity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSimilarityRepository extends JpaRepository<UserSimilarity, UUID> {

    /**
     * Find similarity between two specific users (bidirectional)
     */
    @Query("SELECT us FROM UserSimilarity us WHERE " +
           "(us.userId1 = :userId1 AND us.userId2 = :userId2) OR " +
           "(us.userId1 = :userId2 AND us.userId2 = :userId1)")
    Optional<UserSimilarity> findSimilarityBetweenUsers(
            @Param("userId1") String userId1, 
            @Param("userId2") String userId2);

    /**
     * Find most similar users to a given user
     */
    @Query("SELECT us FROM UserSimilarity us WHERE " +
           "(us.userId1 = :userId OR us.userId2 = :userId) " +
           "ORDER BY us.similarityScore DESC")
    List<UserSimilarity> findMostSimilarUsers(@Param("userId") String userId, Pageable pageable);

    /**
     * Find users similar to a given user with minimum similarity threshold
     */
    @Query("SELECT us FROM UserSimilarity us WHERE " +
           "(us.userId1 = :userId OR us.userId2 = :userId) " +
           "AND us.similarityScore >= :minSimilarity " +
           "ORDER BY us.similarityScore DESC")
    List<UserSimilarity> findSimilarUsersAboveThreshold(
            @Param("userId") String userId, 
            @Param("minSimilarity") BigDecimal minSimilarity);

    /**
     * Get the other user ID from a similarity record
     */
    @Query("SELECT CASE WHEN us.userId1 = :userId THEN us.userId2 ELSE us.userId1 END " +
           "FROM UserSimilarity us WHERE " +
           "(us.userId1 = :userId OR us.userId2 = :userId) " +
           "AND us.similarityScore >= :minSimilarity " +
           "ORDER BY us.similarityScore DESC")
    List<String> findSimilarUserIds(
            @Param("userId") String userId, 
            @Param("minSimilarity") BigDecimal minSimilarity);

    /**
     * Find all similarities for a user
     */
    @Query("SELECT us FROM UserSimilarity us WHERE us.userId1 = :userId OR us.userId2 = :userId")
    List<UserSimilarity> findAllSimilaritiesForUser(@Param("userId") String userId);

    /**
     * Count similarities for a user
     */
    @Query("SELECT COUNT(us) FROM UserSimilarity us WHERE us.userId1 = :userId OR us.userId2 = :userId")
    long countSimilaritiesForUser(@Param("userId") String userId);

    /**
     * Find similarities calculated before a certain date (for cleanup)
     */
    List<UserSimilarity> findByCalculatedAtBefore(Instant cutoffDate);

    /**
     * Delete similarities calculated before a certain date
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSimilarity us WHERE us.calculatedAt < :cutoffDate")
    int deleteByCalculatedAtBefore(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Delete all similarities for a specific user
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSimilarity us WHERE us.userId1 = :userId OR us.userId2 = :userId")
    int deleteAllSimilaritiesForUser(@Param("userId") String userId);

    /**
     * Update or insert similarity score between two users
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserSimilarity us SET us.similarityScore = :score " +
           "WHERE (us.userId1 = :userId1 AND us.userId2 = :userId2) OR " +
           "(us.userId1 = :userId2 AND us.userId2 = :userId1)")
    int updateSimilarityScore(
            @Param("userId1") String userId1, 
            @Param("userId2") String userId2, 
            @Param("score") BigDecimal score);

    /**
     * Find top N most similar user pairs globally
     */
    @Query("SELECT us FROM UserSimilarity us ORDER BY us.similarityScore DESC")
    List<UserSimilarity> findTopSimilarPairs(Pageable pageable);

    /**
     * Find average similarity score for a user
     */
    @Query("SELECT AVG(us.similarityScore) FROM UserSimilarity us WHERE us.userId1 = :userId OR us.userId2 = :userId")
    Optional<BigDecimal> findAverageSimilarityForUser(@Param("userId") String userId);

    /**
     * Check if similarity exists between two users
     */
    @Query("SELECT COUNT(us) > 0 FROM UserSimilarity us WHERE " +
           "(us.userId1 = :userId1 AND us.userId2 = :userId2) OR " +
           "(us.userId1 = :userId2 AND us.userId2 = :userId1)")
    boolean existsSimilarityBetweenUsers(
            @Param("userId1") String userId1, 
            @Param("userId2") String userId2);
}
package com.hopngo.tripplanning.repository;

import com.hopngo.tripplanning.entity.UserPreference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    /**
     * Find all preferences for a specific user
     */
    List<UserPreference> findByUserId(String userId);

    /**
     * Find preferences by user ID and type
     */
    List<UserPreference> findByUserIdAndPreferenceType(String userId, String preferenceType);

    /**
     * Find a specific preference by user ID, type, and value
     */
    Optional<UserPreference> findByUserIdAndPreferenceTypeAndPreferenceValue(
            String userId, String preferenceType, String preferenceValue);

    /**
     * Find all users who have a specific preference type and value
     */
    @Query("SELECT DISTINCT up.userId FROM UserPreference up WHERE up.preferenceType = :preferenceType AND up.preferenceValue = :preferenceValue")
    List<String> findUserIdsByPreferenceTypeAndValue(
            @Param("preferenceType") String preferenceType, 
            @Param("preferenceValue") String preferenceValue);

    /**
     * Find users with similar preferences to a given user
     */
    @Query("SELECT DISTINCT up2.userId FROM UserPreference up1 " +
           "JOIN UserPreference up2 ON up1.preferenceType = up2.preferenceType " +
           "AND up1.preferenceValue = up2.preferenceValue " +
           "WHERE up1.userId = :userId AND up2.userId != :userId")
    List<String> findUsersWithSimilarPreferences(@Param("userId") String userId);

    /**
     * Count preferences by type for a user
     */
    long countByUserIdAndPreferenceType(String userId, String preferenceType);

    /**
     * Delete all preferences for a user
     */
    void deleteByUserId(String userId);

    /**
     * Delete specific preference by user ID and type
     */
    void deleteByUserIdAndPreferenceType(String userId, String preferenceType);

    /**
     * Find all distinct preference types
     */
    @Query("SELECT DISTINCT up.preferenceType FROM UserPreference up ORDER BY up.preferenceType")
    List<String> findAllPreferenceTypes();

    /**
     * Find all distinct preference values for a given type
     */
    @Query("SELECT DISTINCT up.preferenceValue FROM UserPreference up WHERE up.preferenceType = :preferenceType ORDER BY up.preferenceValue")
    List<String> findPreferenceValuesByType(@Param("preferenceType") String preferenceType);

    /**
     * Find preferences with pagination
     */
    Page<UserPreference> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find top preferences by weight for a user
     */
    @Query("SELECT up FROM UserPreference up WHERE up.userId = :userId ORDER BY up.weight DESC")
    List<UserPreference> findTopPreferencesByWeight(@Param("userId") String userId, Pageable pageable);
}
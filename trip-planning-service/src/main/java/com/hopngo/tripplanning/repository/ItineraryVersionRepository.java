package com.hopngo.tripplanning.repository;

import com.hopngo.tripplanning.entity.ItineraryVersion;
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
public interface ItineraryVersionRepository extends JpaRepository<ItineraryVersion, UUID> {

    /**
     * Find all versions for a specific itinerary ordered by version number descending
     */
    List<ItineraryVersion> findByItineraryIdOrderByVersionDesc(UUID itineraryId);

    /**
     * Find all versions for a specific itinerary with pagination
     */
    Page<ItineraryVersion> findByItineraryIdOrderByVersionDesc(UUID itineraryId, Pageable pageable);

    /**
     * Find a specific version of an itinerary
     */
    Optional<ItineraryVersion> findByItineraryIdAndVersion(UUID itineraryId, Integer version);

    /**
     * Find the latest version number for an itinerary
     */
    @Query("SELECT MAX(v.version) FROM ItineraryVersion v WHERE v.itinerary.id = :itineraryId")
    Optional<Integer> findMaxVersionByItineraryId(@Param("itineraryId") UUID itineraryId);

    /**
     * Find the latest version for an itinerary
     */
    @Query("SELECT v FROM ItineraryVersion v WHERE v.itinerary.id = :itineraryId " +
           "AND v.version = (SELECT MAX(v2.version) FROM ItineraryVersion v2 WHERE v2.itinerary.id = :itineraryId)")
    Optional<ItineraryVersion> findLatestVersionByItineraryId(@Param("itineraryId") UUID itineraryId);

    /**
     * Count total versions for an itinerary
     */
    long countByItineraryId(UUID itineraryId);

    /**
     * Find versions created by a specific user
     */
    List<ItineraryVersion> findByAuthorUserIdOrderByCreatedAtDesc(String authorUserId);

    /**
     * Find versions for an itinerary created by a specific user
     */
    List<ItineraryVersion> findByItineraryIdAndAuthorUserIdOrderByVersionDesc(UUID itineraryId, String authorUserId);

    /**
     * Check if a version exists for an itinerary
     */
    boolean existsByItineraryIdAndVersion(UUID itineraryId, Integer version);

    /**
     * Delete all versions for an itinerary
     */
    void deleteByItineraryId(UUID itineraryId);

    /**
     * Find recent versions across all itineraries (for admin/monitoring)
     */
    @Query("SELECT v FROM ItineraryVersion v ORDER BY v.createdAt DESC")
    Page<ItineraryVersion> findRecentVersions(Pageable pageable);
}
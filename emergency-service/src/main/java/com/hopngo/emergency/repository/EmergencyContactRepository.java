package com.hopngo.emergency.repository;

import com.hopngo.emergency.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {
    
    /**
     * Find all emergency contacts for a specific user
     */
    List<EmergencyContact> findByUserIdOrderByIsPrimaryDescCreatedAtAsc(String userId);
    
    /**
     * Find a specific contact by ID and user ID (for security)
     */
    Optional<EmergencyContact> findByIdAndUserId(Long id, String userId);
    
    /**
     * Find primary contacts for a user
     */
    List<EmergencyContact> findByUserIdAndIsPrimaryTrue(String userId);
    
    /**
     * Count contacts for a user
     */
    long countByUserId(String userId);
    
    /**
     * Check if user has any primary contacts
     */
    @Query("SELECT COUNT(ec) > 0 FROM EmergencyContact ec WHERE ec.userId = :userId AND ec.isPrimary = true")
    boolean hasPrimaryContact(@Param("userId") String userId);
    
    /**
     * Delete contact by ID and user ID (for security)
     */
    void deleteByIdAndUserId(Long id, String userId);
}
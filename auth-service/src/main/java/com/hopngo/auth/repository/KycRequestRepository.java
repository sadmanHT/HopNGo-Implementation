package com.hopngo.auth.repository;

import com.hopngo.auth.entity.KycRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycRequestRepository extends JpaRepository<KycRequest, Long> {
    
    /**
     * Find KYC request by user ID
     */
    Optional<KycRequest> findByUserId(Long userId);
    
    /**
     * Find all KYC requests by status
     */
    List<KycRequest> findByStatus(KycRequest.KycStatus status);
    
    /**
     * Find KYC requests by status with pagination
     */
    Page<KycRequest> findByStatus(KycRequest.KycStatus status, Pageable pageable);
    
    /**
     * Find all pending KYC requests
     */
    @Query("SELECT k FROM KycRequest k WHERE k.status = 'PENDING' ORDER BY k.createdAt ASC")
    List<KycRequest> findAllPendingRequests();
    
    /**
     * Find pending KYC requests with pagination
     */
    @Query("SELECT k FROM KycRequest k WHERE k.status = 'PENDING' ORDER BY k.createdAt ASC")
    Page<KycRequest> findAllPendingRequests(Pageable pageable);
    
    /**
     * Check if user has any KYC request
     */
    boolean existsByUserId(Long userId);
    
    /**
     * Check if user has pending KYC request
     */
    @Query("SELECT COUNT(k) > 0 FROM KycRequest k WHERE k.userId = :userId AND k.status = 'PENDING'")
    boolean existsPendingRequestByUserId(@Param("userId") Long userId);
    
    /**
     * Find latest KYC request by user ID
     */
    @Query("SELECT k FROM KycRequest k WHERE k.userId = :userId ORDER BY k.createdAt DESC LIMIT 1")
    Optional<KycRequest> findLatestByUserId(@Param("userId") Long userId);
    
    /**
     * Find KYC requests by status ordered by creation date
     */
    @Query("SELECT k FROM KycRequest k WHERE k.status = :status ORDER BY k.createdAt ASC")
    Page<KycRequest> findByStatusOrderByCreatedAtAsc(@Param("status") KycRequest.KycStatus status, Pageable pageable);
    
    /**
     * Count requests by status
     */
    long countByStatus(KycRequest.KycStatus status);
}
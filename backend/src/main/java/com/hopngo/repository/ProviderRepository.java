package com.hopngo.repository;

import com.hopngo.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Provider entity
 */
@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    
    // Placeholder methods - these will need to be updated when Provider entity is created
    
    /**
     * Find provider by email
     */
    @Query("SELECT p FROM Provider p WHERE p.email = :email")
    Optional<Provider> findByEmail(@Param("email") String email);
    
    /**
     * Find active providers
     */
    @Query("SELECT p FROM Provider p WHERE p.isActive = true")
    List<Provider> findActiveProviders();
    
    /**
     * Find providers by service type
     */
    @Query("SELECT p FROM Provider p WHERE p.serviceType = :serviceType")
    List<Provider> findByServiceType(@Param("serviceType") String serviceType);
    
    /**
     * Find providers with earnings above threshold
     */
    @Query("SELECT p FROM Provider p WHERE p.totalEarnings >= :threshold")
    List<Provider> findProvidersWithEarningsAbove(@Param("threshold") BigDecimal threshold);
    
    /**
     * Find providers created between dates
     */
    @Query("SELECT p FROM Provider p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Provider> findProvidersCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count active providers
     */
    @Query("SELECT COUNT(p) FROM Provider p WHERE p.isActive = true")
    long countActiveProviders();
    
    /**
     * Find top earning providers
     */
    @Query("SELECT p FROM Provider p ORDER BY p.totalEarnings DESC")
    List<Provider> findTopEarningProviders();
}
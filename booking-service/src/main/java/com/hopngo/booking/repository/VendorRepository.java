package com.hopngo.booking.repository;

import com.hopngo.booking.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    
    Optional<Vendor> findByUserId(String userId);
    
    boolean existsByUserId(String userId);
    
    List<Vendor> findByStatus(Vendor.VendorStatus status);
    
    @Query("SELECT v FROM Vendor v WHERE v.status = :status AND " +
           "(:businessName IS NULL OR LOWER(v.businessName) LIKE LOWER(CONCAT('%', :businessName, '%')))")
    List<Vendor> findByStatusAndBusinessNameContaining(
        @Param("status") Vendor.VendorStatus status,
        @Param("businessName") String businessName
    );
    
    @Query("SELECT v FROM Vendor v WHERE v.latitude IS NOT NULL AND v.longitude IS NOT NULL AND " +
           "v.status = 'ACTIVE' AND " +
           "(6371 * acos(cos(radians(:lat)) * cos(radians(v.latitude)) * " +
           "cos(radians(v.longitude) - radians(:lng)) + sin(radians(:lat)) * " +
           "sin(radians(v.latitude)))) <= :radiusKm")
    List<Vendor> findActiveVendorsWithinRadius(
        @Param("lat") Double latitude,
        @Param("lng") Double longitude,
        @Param("radiusKm") Double radiusKm
    );
    
    @Query("SELECT COUNT(v) FROM Vendor v WHERE v.status = 'ACTIVE'")
    long countActiveVendors();
}
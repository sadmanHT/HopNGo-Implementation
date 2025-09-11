package com.hopngo.market.repository;

import com.hopngo.market.entity.Refund;
import com.hopngo.market.entity.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {
    
    Optional<Refund> findByRefundReference(String refundReference);
    
    Optional<Refund> findByProviderRefundId(String providerRefundId);
    
    List<Refund> findByBookingId(UUID bookingId);
    
    List<Refund> findByPaymentId(UUID paymentId);
    
    List<Refund> findByStatus(RefundStatus status);
    
    Page<Refund> findByStatus(RefundStatus status, Pageable pageable);
    
    @Query("SELECT r FROM Refund r WHERE r.payment.order.userId = :userId")
    Page<Refund> findByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT r FROM Refund r WHERE r.payment.order.userId = :userId AND r.status = :status")
    List<Refund> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") RefundStatus status);
    
    @Query("SELECT r FROM Refund r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<Refund> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT r FROM Refund r WHERE r.status = :status AND r.createdAt < :cutoffDate")
    List<Refund> findStaleRefunds(@Param("status") RefundStatus status, 
                                 @Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(r) FROM Refund r WHERE r.payment.provider = :provider AND r.status = :status")
    long countByProviderAndStatus(@Param("provider") String provider, @Param("status") RefundStatus status);
    
    boolean existsByBookingId(UUID bookingId);
    
    boolean existsByPaymentIdAndStatus(UUID paymentId, RefundStatus status);
}
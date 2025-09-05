package com.hopngo.booking.repository;

import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    
    List<Booking> findByUserId(String userId);
    
    List<Booking> findByUserIdAndStatus(String userId, BookingStatus status);
    
    List<Booking> findByListingId(UUID listingId);
    
    List<Booking> findByVendorId(UUID vendorId);
    
    List<Booking> findByVendorIdAndStatus(UUID vendorId, BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.vendor.userId = :vendorUserId")
    List<Booking> findByVendorUserId(@Param("vendorUserId") String vendorUserId);
    
    Optional<Booking> findByBookingReference(String bookingReference);
    
    @Query("SELECT b FROM Booking b WHERE b.userId = :userId AND " +
           "b.status IN ('CONFIRMED', 'COMPLETED') AND " +
           "b.endDate < :currentDate AND " +
           "NOT EXISTS (SELECT r FROM Review r WHERE r.booking = b)")
    List<Booking> findBookingsEligibleForReview(
        @Param("userId") String userId,
        @Param("currentDate") LocalDate currentDate
    );
    
    @Query("SELECT b FROM Booking b WHERE b.listing.id = :listingId AND " +
           "b.status IN ('PENDING', 'CONFIRMED') AND " +
           "((b.startDate BETWEEN :startDate AND :endDate) OR " +
           "(b.endDate BETWEEN :startDate AND :endDate) OR " +
           "(b.startDate <= :startDate AND b.endDate >= :endDate))")
    List<Booking> findConflictingBookings(
        @Param("listingId") UUID listingId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT b FROM Booking b WHERE b.userId = :userId AND " +
           "b.listing.id = :listingId AND " +
           "b.status IN ('PENDING', 'CONFIRMED') AND " +
           "((b.startDate BETWEEN :startDate AND :endDate) OR " +
           "(b.endDate BETWEEN :startDate AND :endDate) OR " +
           "(b.startDate <= :startDate AND b.endDate >= :endDate))")
    List<Booking> findUserConflictingBookings(
        @Param("userId") String userId,
        @Param("listingId") UUID listingId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND " +
           "b.createdAt < :cutoffTime")
    List<Booking> findExpiredPendingBookings(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);
    
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND " +
           "b.endDate < :currentDate")
    List<Booking> findCompletedBookings(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.userId = :userId AND " +
           "b.status = :status")
    long countByUserIdAndStatus(
        @Param("userId") String userId,
        @Param("status") BookingStatus status
    );
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.vendor.id = :vendorId AND " +
           "b.status = :status")
    long countByVendorIdAndStatus(
        @Param("vendorId") UUID vendorId,
        @Param("status") BookingStatus status
    );
    
    @Query("SELECT b FROM Booking b WHERE b.userId = :userId " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
    
    @Query("SELECT b FROM Booking b WHERE b.vendor.id = :vendorId " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByVendorIdOrderByCreatedAtDesc(@Param("vendorId") UUID vendorId);
    
    @Query("SELECT SUM(b.totalAmount) FROM Booking b WHERE b.vendor.id = :vendorId AND " +
           "b.status = 'CONFIRMED' AND " +
           "b.createdAt BETWEEN :startDate AND :endDate")
    java.math.BigDecimal calculateVendorRevenue(
        @Param("vendorId") UUID vendorId,
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );
    
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND " +
           "b.startDate = :date")
    List<Booking> findBookingsStartingOnDate(
        @Param("status") BookingStatus status,
        @Param("date") LocalDate date
    );
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.vendor.id = :vendorId AND b.status = 'COMPLETED'")
    long countCompletedBookingsByVendor(@Param("vendorId") UUID vendorId);
    
    long countByVendorId(UUID vendorId);
    
    List<Booking> findByStatus(BookingStatus status);
    
    long countByUserId(String userId);
}
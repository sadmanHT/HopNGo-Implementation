package com.hopngo.booking.repository;

import com.hopngo.booking.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    
    Optional<Inventory> findByListingIdAndDate(UUID listingId, LocalDate date);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.listing.id = :listingId AND i.date = :date")
    Optional<Inventory> findByListingIdAndDateForUpdate(
        @Param("listingId") UUID listingId, 
        @Param("date") LocalDate date
    );
    
    List<Inventory> findByListingIdAndDateBetween(
        UUID listingId, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.listing.id = :listingId AND " +
           "i.date BETWEEN :startDate AND :endDate ORDER BY i.date")
    List<Inventory> findByListingIdAndDateBetweenForUpdate(
        @Param("listingId") UUID listingId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT i FROM Inventory i WHERE i.listing.id = :listingId AND " +
           "i.date BETWEEN :startDate AND :endDate AND " +
           "(i.availableQuantity - i.reservedQuantity) >= :requiredQuantity")
    List<Inventory> findAvailableInventory(
        @Param("listingId") UUID listingId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("requiredQuantity") Integer requiredQuantity
    );
    
    @Query("SELECT CASE WHEN COUNT(i) = :expectedDays THEN true ELSE false END " +
           "FROM Inventory i WHERE i.listing.id = :listingId AND " +
           "i.date BETWEEN :startDate AND :endDate AND " +
           "(i.availableQuantity - i.reservedQuantity) >= :requiredQuantity")
    boolean isAvailableForPeriod(
        @Param("listingId") UUID listingId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("requiredQuantity") Integer requiredQuantity,
        @Param("expectedDays") long expectedDays
    );
    
    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity + :quantity " +
           "WHERE i.listing.id = :listingId AND i.date BETWEEN :startDate AND :endDate")
    int reserveInventory(
        @Param("listingId") UUID listingId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("quantity") Integer quantity
    );
    
    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity - :quantity " +
           "WHERE i.listing.id = :listingId AND i.date BETWEEN :startDate AND :endDate " +
           "AND i.reservedQuantity >= :quantity")
    int releaseInventory(
        @Param("listingId") UUID listingId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("quantity") Integer quantity
    );
    
    @Query("SELECT i FROM Inventory i WHERE i.listing.id IN :listingIds AND " +
           "i.date BETWEEN :startDate AND :endDate AND " +
           "(i.availableQuantity - i.reservedQuantity) > 0")
    List<Inventory> findAvailableInventoryForListings(
        @Param("listingIds") List<UUID> listingIds,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT DISTINCT i.listing.id FROM Inventory i WHERE " +
           "i.date BETWEEN :startDate AND :endDate AND " +
           "(i.availableQuantity - i.reservedQuantity) >= :requiredQuantity " +
           "GROUP BY i.listing.id " +
           "HAVING COUNT(i) = :expectedDays")
    List<UUID> findAvailableListingIds(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("requiredQuantity") Integer requiredQuantity,
        @Param("expectedDays") long expectedDays
    );
    
    void deleteByListingId(UUID listingId);
    
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.listing.id = :listingId AND " +
           "i.date BETWEEN :startDate AND :endDate")
    long countInventoryForPeriod(
        @Param("listingId") UUID listingId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
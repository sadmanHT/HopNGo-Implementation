package com.hopngo.market.repository;

import com.hopngo.market.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    
    // Find items by order
    List<OrderItem> findByOrder_IdOrderByCreatedAtAsc(UUID orderId);
    
    // Find items by product
    Page<OrderItem> findByProduct_IdOrderByCreatedAtDesc(UUID productId, Pageable pageable);
    
    // Find rental items
    @Query("SELECT oi FROM OrderItem oi WHERE oi.rentalDays IS NOT NULL AND oi.rentalDays > 0 ORDER BY oi.createdAt DESC")
    Page<OrderItem> findByIsRentalTrueOrderByCreatedAtDesc(Pageable pageable);
    
    // Find purchase items
    @Query("SELECT oi FROM OrderItem oi WHERE oi.rentalDays IS NULL OR oi.rentalDays = 0 ORDER BY oi.createdAt DESC")
    Page<OrderItem> findByIsRentalFalseOrderByCreatedAtDesc(Pageable pageable);
    
    // Find rental items by date range
    @Query("SELECT oi FROM OrderItem oi WHERE (oi.rentalDays IS NOT NULL AND oi.rentalDays > 0) AND " +
           "(:startDate IS NULL OR oi.rentalStartDate >= :startDate) AND " +
           "(:endDate IS NULL OR oi.rentalEndDate <= :endDate) " +
           "ORDER BY oi.rentalStartDate ASC")
    Page<OrderItem> findRentalItemsByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );
    
    // Find active rental items (currently rented)
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE " +
           "(oi.rentalDays IS NOT NULL AND oi.rentalDays > 0) AND " +
           "o.status IN ('PAID', 'SHIPPED', 'DELIVERED') AND " +
           "oi.rentalStartDate <= CURRENT_DATE AND " +
           "oi.rentalEndDate >= CURRENT_DATE " +
           "ORDER BY oi.rentalEndDate ASC")
    List<OrderItem> findActiveRentalItems();
    
    // Find overdue rental items
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE " +
           "(oi.rentalDays IS NOT NULL AND oi.rentalDays > 0) AND " +
           "o.status = 'DELIVERED' AND " +
           "oi.rentalEndDate < CURRENT_DATE " +
           "ORDER BY oi.rentalEndDate ASC")
    List<OrderItem> findOverdueRentalItems();
    
    // Find items by product and rental status
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId AND " +
           "CASE WHEN :isRental = true THEN (oi.rentalDays IS NOT NULL AND oi.rentalDays > 0) " +
           "ELSE (oi.rentalDays IS NULL OR oi.rentalDays = 0) END " +
           "ORDER BY oi.createdAt DESC")
    Page<OrderItem> findByProductIdAndIsRental(
        @Param("productId") UUID productId,
        @Param("isRental") Boolean isRental,
        Pageable pageable
    );
    
    // Calculate total quantity sold for a product
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi JOIN oi.order o WHERE " +
           "oi.product.id = :productId AND (oi.rentalDays IS NULL OR oi.rentalDays = 0) AND " +
           "o.status IN ('PAID', 'SHIPPED', 'DELIVERED')")
    Long calculateTotalQuantitySold(@Param("productId") UUID productId);
    
    // Calculate total quantity rented for a product
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi JOIN oi.order o WHERE " +
           "oi.product.id = :productId AND (oi.rentalDays IS NOT NULL AND oi.rentalDays > 0) AND " +
           "o.status IN ('PAID', 'SHIPPED', 'DELIVERED')")
    Long calculateTotalQuantityRented(@Param("productId") UUID productId);
    
    // Find top selling products
    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) as totalSold FROM OrderItem oi JOIN oi.order o WHERE " +
           "(oi.rentalDays IS NULL OR oi.rentalDays = 0) AND o.status IN ('PAID', 'SHIPPED', 'DELIVERED') " +
           "GROUP BY oi.product.id, oi.product.name ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts(Pageable pageable);
    
    // Find top rented products
    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) as totalRented FROM OrderItem oi JOIN oi.order o WHERE " +
           "(oi.rentalDays IS NOT NULL AND oi.rentalDays > 0) AND o.status IN ('PAID', 'SHIPPED', 'DELIVERED') " +
           "GROUP BY oi.product.id, oi.product.name ORDER BY totalRented DESC")
    List<Object[]> findTopRentedProducts(Pageable pageable);
    
    // Calculate revenue by product
    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.totalPrice) as totalRevenue FROM OrderItem oi JOIN oi.order o WHERE " +
           "o.status IN ('PAID', 'SHIPPED', 'DELIVERED') " +
           "GROUP BY oi.product.id, oi.product.name ORDER BY totalRevenue DESC")
    List<Object[]> calculateRevenueByProduct(Pageable pageable);
    
    // Find items with specific product name (since SKU is computed)
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.name LIKE %:name% ORDER BY oi.createdAt DESC")
    Page<OrderItem> findByProductName(@Param("name") String name, Pageable pageable);
    
    // Count items by rental status
    @Query("SELECT CASE WHEN (oi.rentalDays IS NOT NULL AND oi.rentalDays > 0) THEN true ELSE false END as isRental, COUNT(oi) FROM OrderItem oi GROUP BY CASE WHEN (oi.rentalDays IS NOT NULL AND oi.rentalDays > 0) THEN true ELSE false END")
    List<Object[]> countItemsByRentalStatus();
    
    // Find items by order status
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE o.status = :status ORDER BY oi.createdAt DESC")
    Page<OrderItem> findByOrderStatus(@Param("status") com.hopngo.market.entity.OrderStatus status, Pageable pageable);
    
    // Calculate average rental duration
    @Query("SELECT AVG(oi.rentalDays) FROM OrderItem oi WHERE (oi.rentalDays IS NOT NULL AND oi.rentalDays > 0)")
    Double calculateAverageRentalDuration();
    
    // Find items by date range
    @Query("SELECT oi FROM OrderItem oi WHERE oi.createdAt BETWEEN :startDate AND :endDate ORDER BY oi.createdAt DESC")
    Page<OrderItem> findByCreatedAtBetween(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate,
        Pageable pageable
    );
}
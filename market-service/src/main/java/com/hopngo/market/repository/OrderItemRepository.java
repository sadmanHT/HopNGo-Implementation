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
    List<OrderItem> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
    
    // Find items by product
    Page<OrderItem> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);
    
    // Find rental items
    Page<OrderItem> findByIsRentalTrueOrderByCreatedAtDesc(Pageable pageable);
    
    // Find purchase items
    Page<OrderItem> findByIsRentalFalseOrderByCreatedAtDesc(Pageable pageable);
    
    // Find rental items by date range
    @Query("SELECT oi FROM OrderItem oi WHERE oi.isRental = true AND " +
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
           "oi.isRental = true AND " +
           "o.status IN ('PAID', 'SHIPPED', 'DELIVERED') AND " +
           "oi.rentalStartDate <= CURRENT_DATE AND " +
           "oi.rentalEndDate >= CURRENT_DATE " +
           "ORDER BY oi.rentalEndDate ASC")
    List<OrderItem> findActiveRentalItems();
    
    // Find overdue rental items
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE " +
           "oi.isRental = true AND " +
           "o.status = 'DELIVERED' AND " +
           "oi.rentalEndDate < CURRENT_DATE " +
           "ORDER BY oi.rentalEndDate ASC")
    List<OrderItem> findOverdueRentalItems();
    
    // Find items by product and rental status
    @Query("SELECT oi FROM OrderItem oi WHERE oi.productId = :productId AND oi.isRental = :isRental ORDER BY oi.createdAt DESC")
    Page<OrderItem> findByProductIdAndIsRental(
        @Param("productId") UUID productId,
        @Param("isRental") Boolean isRental,
        Pageable pageable
    );
    
    // Calculate total quantity sold for a product
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi JOIN oi.order o WHERE " +
           "oi.productId = :productId AND oi.isRental = false AND " +
           "o.status IN ('PAID', 'SHIPPED', 'DELIVERED')")
    Long calculateTotalQuantitySold(@Param("productId") UUID productId);
    
    // Calculate total quantity rented for a product
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi JOIN oi.order o WHERE " +
           "oi.productId = :productId AND oi.isRental = true AND " +
           "o.status IN ('PAID', 'SHIPPED', 'DELIVERED')")
    Long calculateTotalQuantityRented(@Param("productId") UUID productId);
    
    // Find top selling products
    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity) as totalSold FROM OrderItem oi JOIN oi.order o WHERE " +
           "oi.isRental = false AND o.status IN ('PAID', 'SHIPPED', 'DELIVERED') " +
           "GROUP BY oi.productId, oi.productName ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts(Pageable pageable);
    
    // Find top rented products
    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity) as totalRented FROM OrderItem oi JOIN oi.order o WHERE " +
           "oi.isRental = true AND o.status IN ('PAID', 'SHIPPED', 'DELIVERED') " +
           "GROUP BY oi.productId, oi.productName ORDER BY totalRented DESC")
    List<Object[]> findTopRentedProducts(Pageable pageable);
    
    // Calculate revenue by product
    @Query("SELECT oi.productId, oi.productName, SUM(oi.totalPrice) as totalRevenue FROM OrderItem oi JOIN oi.order o WHERE " +
           "o.status IN ('PAID', 'SHIPPED', 'DELIVERED') " +
           "GROUP BY oi.productId, oi.productName ORDER BY totalRevenue DESC")
    List<Object[]> calculateRevenueByProduct(Pageable pageable);
    
    // Find items with specific product SKU
    @Query("SELECT oi FROM OrderItem oi WHERE oi.productSku = :sku ORDER BY oi.createdAt DESC")
    Page<OrderItem> findByProductSku(@Param("sku") String sku, Pageable pageable);
    
    // Count items by rental status
    @Query("SELECT oi.isRental, COUNT(oi) FROM OrderItem oi GROUP BY oi.isRental")
    List<Object[]> countItemsByRentalStatus();
    
    // Find items by order status
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE o.status = :status ORDER BY oi.createdAt DESC")
    Page<OrderItem> findByOrderStatus(@Param("status") com.hopngo.market.entity.OrderStatus status, Pageable pageable);
    
    // Calculate average rental duration
    @Query("SELECT AVG(oi.rentalDays) FROM OrderItem oi WHERE oi.isRental = true AND oi.rentalDays IS NOT NULL")
    Double calculateAverageRentalDuration();
    
    // Find items by date range
    @Query("SELECT oi FROM OrderItem oi WHERE oi.createdAt BETWEEN :startDate AND :endDate ORDER BY oi.createdAt DESC")
    Page<OrderItem> findByCreatedAtBetween(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate,
        Pageable pageable
    );
}
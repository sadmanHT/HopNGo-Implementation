package com.hopngo.market.repository;

import com.hopngo.market.entity.Order;
import com.hopngo.market.entity.OrderStatus;
import com.hopngo.market.entity.OrderType;
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
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    // Find all orders
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Find orders by user
    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    // Find orders by user and status
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, OrderStatus status, Pageable pageable);
    
    // Find orders by user and type
    Page<Order> findByUserIdAndOrderTypeOrderByCreatedAtDesc(UUID userId, OrderType orderType, Pageable pageable);
    
    // Find orders by status
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
    
    // Find orders by type
    Page<Order> findByOrderTypeOrderByCreatedAtDesc(OrderType orderType, Pageable pageable);
    
    // Find orders by tracking number
    Optional<Order> findByTrackingNumber(String trackingNumber);
    
    // Find orders created within date range
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    Page<Order> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Find orders by user within date range
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdAndCreatedAtBetween(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Find pending orders (created but not paid)
    @Query("SELECT o FROM Order o WHERE o.status = 'CREATED' AND o.createdAt < :cutoffTime ORDER BY o.createdAt ASC")
    List<Order> findPendingOrdersOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Find orders that need shipping (paid but not shipped)
    @Query("SELECT o FROM Order o WHERE o.status = 'PAID' ORDER BY o.paidAt ASC")
    Page<Order> findOrdersReadyForShipping(Pageable pageable);
    
    // Find shipped orders (for delivery tracking)
    @Query("SELECT o FROM Order o WHERE o.status = 'SHIPPED' ORDER BY o.updatedAt ASC")
    Page<Order> findShippedOrders(Pageable pageable);
    
    // Find rental orders by date range
    @Query("SELECT o FROM Order o WHERE o.orderType = 'RENTAL' AND " +
           "(:startDate IS NULL OR o.rentalStartDate >= :startDate) AND " +
           "(:endDate IS NULL OR o.rentalEndDate <= :endDate) " +
           "ORDER BY o.rentalStartDate ASC")
    Page<Order> findRentalOrdersByDateRange(
        @Param("startDate") java.time.LocalDate startDate,
        @Param("endDate") java.time.LocalDate endDate,
        Pageable pageable
    );
    
    // Find active rental orders (currently rented)
    @Query("SELECT o FROM Order o WHERE o.orderType = 'RENTAL' AND " +
           "o.status IN ('PAID', 'SHIPPED', 'DELIVERED') AND " +
           "o.rentalStartDate <= CURRENT_DATE AND " +
           "o.rentalEndDate >= CURRENT_DATE " +
           "ORDER BY o.rentalEndDate ASC")
    List<Order> findActiveRentalOrders();
    
    // Find overdue rental orders
    @Query("SELECT o FROM Order o WHERE o.orderType = 'RENTAL' AND " +
           "o.status = 'DELIVERED' AND " +
           "o.rentalEndDate < CURRENT_DATE " +
           "ORDER BY o.rentalEndDate ASC")
    List<Order> findOverdueRentalOrders();
    
    // Count orders by status
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();
    
    // Count orders by specific status
    long countByStatus(OrderStatus status);
    
    // Count orders by type
    @Query("SELECT o.orderType, COUNT(o) FROM Order o GROUP BY o.orderType")
    List<Object[]> countOrdersByType();
    
    // Calculate total revenue
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED')")
    java.math.BigDecimal calculateTotalRevenue();
    
    // Calculate revenue by date range
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE " +
           "o.status IN ('PAID', 'SHIPPED', 'DELIVERED') AND " +
           "o.paidAt BETWEEN :startDate AND :endDate")
    java.math.BigDecimal calculateRevenueByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Find orders by multiple statuses
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses ORDER BY o.createdAt DESC")
    Page<Order> findByStatusIn(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);
    
    // Find user's recent orders
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findTop10ByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);
}
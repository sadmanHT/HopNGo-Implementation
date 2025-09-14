package com.hopngo.repository;

import com.hopngo.entity.Order;
import com.hopngo.entity.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    List<Order> findByUser_Id(Long userId);
    
    List<Order> findByStatus(OrderStatus status);
    
    Page<Order> findByUser_IdAndStatus(Long userId, OrderStatus status, Pageable pageable);
    
    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status = :status")
    List<Order> findByDateRangeAndStatus(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate, 
                                        @Param("status") OrderStatus status);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByStatusAndDateRange(@Param("status") OrderStatus status,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(o.platformFee) FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumPlatformFeeByStatusAndDateRange(@Param("status") OrderStatus status,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(o.providerFee) FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumProviderFeeByStatusAndDateRange(@Param("status") OrderStatus status,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndDateRange(@Param("status") OrderStatus status,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o WHERE o.totalAmount >= :minAmount AND o.totalAmount <= :maxAmount")
    List<Order> findByAmountRange(@Param("minAmount") BigDecimal minAmount, 
                                 @Param("maxAmount") BigDecimal maxAmount);
    
    @Query("SELECT DISTINCT o.currency FROM Order o")
    List<String> findDistinctCurrencies();
    
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses")
    List<Order> findByStatusIn(@Param("statuses") List<OrderStatus> statuses);
    
    // Monthly aggregation queries
    @Query("SELECT YEAR(o.createdAt) as year, MONTH(o.createdAt) as month, " +
           "COUNT(o) as orderCount, SUM(o.totalAmount) as totalAmount " +
           "FROM Order o WHERE o.status = :status " +
           "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) " +
           "ORDER BY YEAR(o.createdAt) DESC, MONTH(o.createdAt) DESC")
    List<Object[]> getMonthlyOrderSummary(@Param("status") OrderStatus status);
    
    // Daily aggregation for recent activity
    @Query("SELECT DATE(o.createdAt) as orderDate, COUNT(o) as orderCount, SUM(o.totalAmount) as totalAmount " +
           "FROM Order o WHERE o.createdAt >= :fromDate AND o.status = :status " +
           "GROUP BY DATE(o.createdAt) " +
           "ORDER BY DATE(o.createdAt) DESC")
    List<Object[]> getDailyOrderSummary(@Param("fromDate") LocalDateTime fromDate, 
                                       @Param("status") OrderStatus status);
}
package com.hopngo.support.repository;

import com.hopngo.support.entity.Ticket;
import com.hopngo.support.enums.TicketPriority;
import com.hopngo.support.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Find tickets by user
    List<Ticket> findByUserIdOrderByCreatedAtDesc(String userId);
    
    Page<Ticket> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Find tickets by email (for anonymous users)
    List<Ticket> findByEmailOrderByCreatedAtDesc(String email);
    
    Page<Ticket> findByEmailOrderByCreatedAtDesc(String email, Pageable pageable);
    
    // Find tickets by status
    List<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status);
    
    Page<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);
    
    // Find tickets by priority
    List<Ticket> findByPriorityOrderByCreatedAtDesc(TicketPriority priority);
    
    Page<Ticket> findByPriorityOrderByCreatedAtDesc(TicketPriority priority, Pageable pageable);
    
    // Find tickets by assigned agent
    List<Ticket> findByAssignedAgentIdOrderByCreatedAtDesc(String agentId);
    
    Page<Ticket> findByAssignedAgentIdOrderByCreatedAtDesc(String agentId, Pageable pageable);
    
    // Find unassigned tickets
    List<Ticket> findByAssignedAgentIdIsNullOrderByCreatedAtDesc();
    
    Page<Ticket> findByAssignedAgentIdIsNullOrderByCreatedAtDesc(Pageable pageable);
    
    // Find assigned tickets
    List<Ticket> findByAssignedAgentIdIsNotNullOrderByCreatedAtDesc();
    
    Page<Ticket> findByAssignedAgentIdIsNotNullOrderByCreatedAtDesc(Pageable pageable);
    
    // Find all tickets ordered by creation date
    List<Ticket> findAllByOrderByCreatedAtDesc();
    
    Page<Ticket> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Find tickets by status and priority
    List<Ticket> findByStatusAndPriorityOrderByCreatedAtDesc(TicketStatus status, TicketPriority priority);
    
    Page<Ticket> findByStatusAndPriorityOrderByCreatedAtDesc(TicketStatus status, TicketPriority priority, Pageable pageable);
    
    // Find tickets by status and assignment status
    Page<Ticket> findByStatusAndAssignedAgentIdIsNotNullOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);
    
    Page<Ticket> findByStatusAndAssignedAgentIdIsNullOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);
    
    // Find tickets by status, priority and assignment status
    Page<Ticket> findByStatusAndPriorityAndAssignedAgentIdIsNotNullOrderByCreatedAtDesc(TicketStatus status, TicketPriority priority, Pageable pageable);
    
    Page<Ticket> findByStatusAndPriorityAndAssignedAgentIdIsNullOrderByCreatedAtDesc(TicketStatus status, TicketPriority priority, Pageable pageable);
    
    // Count assigned tickets
    long countByAssignedAgentIdIsNotNull();
    
    // Find active tickets (OPEN or PENDING)
    @Query("SELECT t FROM Ticket t WHERE t.status IN ('OPEN', 'PENDING') ORDER BY t.priority DESC, t.createdAt ASC")
    List<Ticket> findActiveTicketsOrderByPriorityAndAge();
    
    @Query("SELECT t FROM Ticket t WHERE t.status IN ('OPEN', 'PENDING') ORDER BY t.priority DESC, t.createdAt ASC")
    Page<Ticket> findActiveTicketsOrderByPriorityAndAge(Pageable pageable);
    
    // Search tickets by subject or body
    @Query("SELECT t FROM Ticket t WHERE LOWER(t.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.body) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY t.createdAt DESC")
    List<Ticket> searchTickets(@Param("query") String query);
    
    @Query("SELECT t FROM Ticket t WHERE LOWER(t.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.body) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY t.createdAt DESC")
    Page<Ticket> searchTickets(@Param("query") String query, Pageable pageable);
    
    // Find tickets created within date range
    List<Ticket> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant startDate, Instant endDate);
    
    Page<Ticket> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant startDate, Instant endDate, Pageable pageable);
    
    // Find tickets that need escalation (old tickets with high priority)
    @Query("SELECT t FROM Ticket t WHERE t.status IN ('OPEN', 'PENDING') AND t.priority = 'HIGH' AND t.createdAt < :escalationTime ORDER BY t.createdAt ASC")
    List<Ticket> findTicketsNeedingEscalation(@Param("escalationTime") Instant escalationTime);
    
    // Count tickets by status
    long countByStatus(TicketStatus status);
    
    // Count tickets by priority
    long countByPriority(TicketPriority priority);
    
    // Count tickets by assigned agent
    long countByAssignedAgentId(String agentId);
    
    // Count unassigned tickets
    long countByAssignedAgentIdIsNull();
    
    // Count tickets created today
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.createdAt >= :startOfDay")
    long countTicketsCreatedToday(@Param("startOfDay") Instant startOfDay);
    
    // Find tickets by user or email (for both authenticated and anonymous users)
    @Query("SELECT t FROM Ticket t WHERE (t.userId = :userId OR t.email = :email) ORDER BY t.createdAt DESC")
    List<Ticket> findByUserIdOrEmail(@Param("userId") String userId, @Param("email") String email);
    
    @Query("SELECT t FROM Ticket t WHERE (t.userId = :userId OR t.email = :email) ORDER BY t.createdAt DESC")
    Page<Ticket> findByUserIdOrEmail(@Param("userId") String userId, @Param("email") String email, Pageable pageable);
    
    // Find tickets that can be auto-closed (resolved tickets older than specified days)
    @Query("SELECT t FROM Ticket t WHERE t.status = 'RESOLVED' AND t.updatedAt < :autoCloseTime")
    List<Ticket> findTicketsForAutoClose(@Param("autoCloseTime") Instant autoCloseTime);
    
    // Custom query to get ticket statistics
    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    List<Object[]> getTicketStatusStatistics();
    
    @Query("SELECT t.priority, COUNT(t) FROM Ticket t GROUP BY t.priority")
    List<Object[]> getTicketPriorityStatistics();
}
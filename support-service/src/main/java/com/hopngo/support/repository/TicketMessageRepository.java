package com.hopngo.support.repository;

import com.hopngo.support.entity.TicketMessage;
import com.hopngo.support.enums.MessageSender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    // Find messages by ticket ID
    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
    
    Page<TicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId, Pageable pageable);
    
    // Find messages by sender type
    List<TicketMessage> findBySenderOrderByCreatedAtDesc(MessageSender sender);
    
    Page<TicketMessage> findBySenderOrderByCreatedAtDesc(MessageSender sender, Pageable pageable);
    
    // Find messages by sender ID (for tracking agent responses)
    List<TicketMessage> findBySenderIdOrderByCreatedAtDesc(String senderId);
    
    Page<TicketMessage> findBySenderIdOrderByCreatedAtDesc(String senderId, Pageable pageable);
    
    // Find messages by ticket and sender
    List<TicketMessage> findByTicketIdAndSenderOrderByCreatedAtAsc(Long ticketId, MessageSender sender);
    
    // Find latest message for a ticket
    @Query("SELECT tm FROM TicketMessage tm WHERE tm.ticket.id = :ticketId ORDER BY tm.createdAt DESC LIMIT 1")
    TicketMessage findLatestMessageByTicketId(@Param("ticketId") Long ticketId);
    
    // Find messages created within date range
    List<TicketMessage> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant startDate, Instant endDate);
    
    Page<TicketMessage> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant startDate, Instant endDate, Pageable pageable);
    
    // Count messages by ticket
    long countByTicketId(Long ticketId);
    
    // Count messages by sender type
    long countBySender(MessageSender sender);
    
    // Count messages by sender ID
    long countBySenderId(String senderId);
    
    // Find human messages (USER or AGENT) for a ticket
    @Query("SELECT tm FROM TicketMessage tm WHERE tm.ticket.id = :ticketId AND tm.sender IN ('USER', 'AGENT') ORDER BY tm.createdAt ASC")
    List<TicketMessage> findHumanMessagesByTicketId(@Param("ticketId") Long ticketId);
    
    // Find system messages for a ticket
    @Query("SELECT tm FROM TicketMessage tm WHERE tm.ticket.id = :ticketId AND tm.sender = 'SYSTEM' ORDER BY tm.createdAt ASC")
    List<TicketMessage> findSystemMessagesByTicketId(@Param("ticketId") Long ticketId);
    
    // Search messages by content
    @Query("SELECT tm FROM TicketMessage tm WHERE LOWER(tm.body) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY tm.createdAt DESC")
    List<TicketMessage> searchMessagesByContent(@Param("query") String query);
    
    @Query("SELECT tm FROM TicketMessage tm WHERE LOWER(tm.body) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY tm.createdAt DESC")
    Page<TicketMessage> searchMessagesByContent(@Param("query") String query, Pageable pageable);
    
    // Find messages by ticket and date range
    @Query("SELECT tm FROM TicketMessage tm WHERE tm.ticket.id = :ticketId AND tm.createdAt BETWEEN :startDate AND :endDate ORDER BY tm.createdAt ASC")
    List<TicketMessage> findByTicketIdAndDateRange(@Param("ticketId") Long ticketId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    // Get message statistics
    @Query("SELECT tm.sender, COUNT(tm) FROM TicketMessage tm GROUP BY tm.sender")
    List<Object[]> getMessageSenderStatistics();
    
    // Count messages created today
    @Query("SELECT COUNT(tm) FROM TicketMessage tm WHERE tm.createdAt >= :startOfDay")
    long countMessagesCreatedToday(@Param("startOfDay") Instant startOfDay);
    
    // Find recent messages by agent (for activity tracking)
    @Query("SELECT tm FROM TicketMessage tm WHERE tm.sender = 'AGENT' AND tm.senderId = :agentId AND tm.createdAt >= :since ORDER BY tm.createdAt DESC")
    List<TicketMessage> findRecentAgentMessages(@Param("agentId") String agentId, @Param("since") Instant since);
    
    // Delete messages by ticket ID (for cleanup)
    void deleteByTicketId(Long ticketId);
}
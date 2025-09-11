package com.hopngo.support.service;

import com.hopngo.support.dto.MessageCreateRequest;
import com.hopngo.support.dto.TicketCreateRequest;
import com.hopngo.support.entity.Ticket;
import com.hopngo.support.entity.TicketMessage;
import com.hopngo.support.enums.MessageSender;
import com.hopngo.support.enums.TicketPriority;
import com.hopngo.support.enums.TicketStatus;
import com.hopngo.support.repository.TicketMessageRepository;
import com.hopngo.support.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final NotificationService notificationService;

    @Autowired
    public TicketService(TicketRepository ticketRepository, 
                        TicketMessageRepository ticketMessageRepository,
                        NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.notificationService = notificationService;
    }

    public Ticket createTicket(TicketCreateRequest request) {
        Ticket ticket = new Ticket();
        ticket.setUserId(request.getUserId());
        ticket.setEmail(request.getEmail());
        ticket.setSubject(request.getSubject());
        ticket.setBody(request.getBody());
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        
        Ticket savedTicket = ticketRepository.save(ticket);
        
        // Create initial system message
        TicketMessage systemMessage = new TicketMessage();
        systemMessage.setTicket(savedTicket);
        systemMessage.setSender(MessageSender.SYSTEM);
        systemMessage.setSenderId("SYSTEM");
        systemMessage.setSenderName("Support System");
        systemMessage.setBody("Ticket created. We'll get back to you soon!");
        ticketMessageRepository.save(systemMessage);
        
        // Send notification to agents
        notificationService.notifyNewTicket(savedTicket);
        
        return savedTicket;
    }

    @Transactional(readOnly = true)
    public Ticket getTicketById(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));
    }

    @Transactional(readOnly = true)
    public Page<Ticket> getTicketsByUserId(String userId, Pageable pageable) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Ticket> getTicketsByEmail(String email, Pageable pageable) {
        return ticketRepository.findByEmailOrderByCreatedAtDesc(email, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Ticket> getTicketsForQueue(TicketStatus status, TicketPriority priority, 
                                          Boolean assigned, Pageable pageable) {
        if (status != null && priority != null && assigned != null) {
            if (assigned) {
                return ticketRepository.findByStatusAndPriorityAndAssignedAgentIdIsNotNullOrderByCreatedAtDesc(
                        status, priority, pageable);
            } else {
                return ticketRepository.findByStatusAndPriorityAndAssignedAgentIdIsNullOrderByCreatedAtDesc(
                        status, priority, pageable);
            }
        } else if (status != null && assigned != null) {
            if (assigned) {
                return ticketRepository.findByStatusAndAssignedAgentIdIsNotNullOrderByCreatedAtDesc(
                        status, pageable);
            } else {
                return ticketRepository.findByStatusAndAssignedAgentIdIsNullOrderByCreatedAtDesc(
                        status, pageable);
            }
        } else if (status != null) {
            return ticketRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (assigned != null) {
            if (assigned) {
                return ticketRepository.findByAssignedAgentIdIsNotNullOrderByCreatedAtDesc(pageable);
            } else {
                return ticketRepository.findByAssignedAgentIdIsNullOrderByCreatedAtDesc(pageable);
            }
        } else {
            return ticketRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
    }

    public TicketMessage addMessageToTicket(Long ticketId, MessageCreateRequest request) {
        Ticket ticket = getTicketById(ticketId);
        
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(request.getSender());
        message.setSenderId(request.getSenderId());
        message.setSenderName(request.getSenderName());
        message.setBody(request.getBody());
        
        TicketMessage savedMessage = ticketMessageRepository.save(message);
        
        // Update ticket status if it was closed and user is replying
        if (ticket.getStatus() == TicketStatus.CLOSED && request.getSender() == MessageSender.USER) {
            ticket.setStatus(TicketStatus.OPEN);
            ticketRepository.save(ticket);
        }
        
        // Send notification
        if (request.getSender() == MessageSender.USER) {
            notificationService.notifyNewUserMessage(ticket, savedMessage);
        }
        
        return savedMessage;
    }

    public TicketMessage addAgentReply(Long ticketId, MessageCreateRequest request) {
        Ticket ticket = getTicketById(ticketId);
        
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(MessageSender.AGENT);
        message.setSenderId(request.getSenderId());
        message.setSenderName(request.getSenderName());
        message.setBody(request.getBody());
        
        TicketMessage savedMessage = ticketMessageRepository.save(message);
        
        // Update ticket status to pending if it was open
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.PENDING);
            ticketRepository.save(ticket);
        }
        
        // Send notification to user
        notificationService.notifyAgentReply(ticket, savedMessage);
        
        return savedMessage;
    }

    @Transactional(readOnly = true)
    public Page<TicketMessage> getTicketMessages(Long ticketId, Pageable pageable) {
        return ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId, pageable);
    }

    public Ticket assignTicket(Long ticketId, String agentId) {
        Ticket ticket = getTicketById(ticketId);
        ticket.setAssignedAgentId(agentId);
        
        Ticket savedTicket = ticketRepository.save(ticket);
        
        // Add system message
        TicketMessage systemMessage = new TicketMessage();
        systemMessage.setTicket(savedTicket);
        systemMessage.setSender(MessageSender.SYSTEM);
        systemMessage.setSenderId("SYSTEM");
        systemMessage.setSenderName("Support System");
        systemMessage.setBody("Ticket assigned to agent: " + agentId);
        ticketMessageRepository.save(systemMessage);
        
        // Send notification
        notificationService.notifyTicketAssigned(savedTicket, agentId);
        
        return savedTicket;
    }

    public Ticket updateTicketStatus(Long ticketId, TicketStatus status) {
        Ticket ticket = getTicketById(ticketId);
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(status);
        
        Ticket savedTicket = ticketRepository.save(ticket);
        
        // Add system message
        TicketMessage systemMessage = new TicketMessage();
        systemMessage.setTicket(savedTicket);
        systemMessage.setSender(MessageSender.SYSTEM);
        systemMessage.setSenderId("SYSTEM");
        systemMessage.setSenderName("Support System");
        systemMessage.setBody("Status changed from " + oldStatus.getDisplayName() + " to " + status.getDisplayName());
        ticketMessageRepository.save(systemMessage);
        
        // Send notification
        notificationService.notifyStatusChange(savedTicket, oldStatus, status);
        
        return savedTicket;
    }

    public Ticket updateTicketPriority(Long ticketId, TicketPriority priority) {
        Ticket ticket = getTicketById(ticketId);
        TicketPriority oldPriority = ticket.getPriority();
        ticket.setPriority(priority);
        
        Ticket savedTicket = ticketRepository.save(ticket);
        
        // Add system message
        TicketMessage systemMessage = new TicketMessage();
        systemMessage.setTicket(savedTicket);
        systemMessage.setSender(MessageSender.SYSTEM);
        systemMessage.setSenderId("SYSTEM");
        systemMessage.setSenderName("Support System");
        systemMessage.setBody("Priority changed from " + oldPriority.getDisplayName() + " to " + priority.getDisplayName());
        ticketMessageRepository.save(systemMessage);
        
        // Send notification for priority changes, especially urgent ones
        if (priority == TicketPriority.URGENT || oldPriority != priority) {
            notificationService.notifyEscalation(savedTicket, "Priority changed to " + priority.getDisplayName());
        }
        
        return savedTicket;
    }

    @Transactional(readOnly = true)
    public Page<Ticket> searchTickets(String query, Pageable pageable) {
        return ticketRepository.searchTickets(query, pageable);
    }

    @Transactional(readOnly = true)
    public boolean hasAccessToTicket(Ticket ticket, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // Anonymous users can only access tickets by email match
            return false;
        }
        
        // Check if user is agent or admin
        boolean isAgent = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_AGENT") || role.equals("ROLE_ADMIN"));
        
        if (isAgent) {
            return true;
        }
        
        // Regular users can only access their own tickets
        return authentication.getName().equals(ticket.getUserId());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTicketStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalTickets", ticketRepository.count());
        stats.put("openTickets", ticketRepository.countByStatus(TicketStatus.OPEN));
        stats.put("pendingTickets", ticketRepository.countByStatus(TicketStatus.PENDING));
        stats.put("resolvedTickets", ticketRepository.countByStatus(TicketStatus.RESOLVED));
        stats.put("closedTickets", ticketRepository.countByStatus(TicketStatus.CLOSED));
        
        stats.put("highPriorityTickets", ticketRepository.countByPriority(TicketPriority.HIGH));
        stats.put("mediumPriorityTickets", ticketRepository.countByPriority(TicketPriority.MEDIUM));
        stats.put("lowPriorityTickets", ticketRepository.countByPriority(TicketPriority.LOW));
        
        stats.put("unassignedTickets", ticketRepository.countByAssignedAgentIdIsNull());
        stats.put("assignedTickets", ticketRepository.countByAssignedAgentIdIsNotNull());
        
        return stats;
    }
}
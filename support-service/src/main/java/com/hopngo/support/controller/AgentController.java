package com.hopngo.support.controller;

import com.hopngo.support.dto.*;
import com.hopngo.support.entity.Ticket;
import com.hopngo.support.entity.TicketMessage;
import com.hopngo.support.service.CannedReplyService;
import com.hopngo.support.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/support/agent")
@PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
@Tag(name = "Agent", description = "Support agent functions for ticket management")
public class AgentController {

    private final TicketService ticketService;
    private final CannedReplyService cannedReplyService;

    @Autowired
    public AgentController(TicketService ticketService, CannedReplyService cannedReplyService) {
        this.ticketService = ticketService;
        this.cannedReplyService = cannedReplyService;
    }

    // Ticket Management
    @GetMapping("/tickets")
    @Operation(summary = "Get tickets for agent", description = "Get tickets assigned to agent or in agent's queue")
    public ResponseEntity<List<TicketResponse>> getAgentTickets(
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Filter by status") @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "Filter by priority") @RequestParam(value = "priority", required = false) String priority,
            @Parameter(description = "Filter by queue") @RequestParam(value = "queue", required = false) String queue,
            Authentication authentication) {
        
        Pageable pageable = PageRequest.of(page, size);
        String agentId = authentication.getName();
        
        Page<Ticket> tickets = ticketService.getTicketsForQueue(status, priority, queue, pageable);
        
        // Filter tickets assigned to this agent or unassigned in their queue
        List<TicketResponse> response = tickets.getContent().stream()
                .filter(ticket -> agentId.equals(ticket.getAssignedTo()) || ticket.getAssignedTo() == null)
                .map(TicketResponse::fromWithoutMessages)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tickets/{ticketId}")
    @Operation(summary = "Get ticket details", description = "Get full ticket details including messages")
    public ResponseEntity<TicketResponse> getTicketDetails(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            Authentication authentication) {
        
        Ticket ticket = ticketService.getTicketById(ticketId);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }
        
        String agentId = authentication.getName();
        
        // Check if agent has access to this ticket
        if (!ticketService.canAgentAccessTicket(ticketId, agentId)) {
            return ResponseEntity.status(403).build();
        }
        
        List<TicketMessage> messages = ticketService.getTicketMessages(ticketId);
        return ResponseEntity.ok(TicketResponse.from(ticket, messages));
    }

    @PutMapping("/tickets/{ticketId}/assign")
    @Operation(summary = "Assign ticket", description = "Assign ticket to agent")
    public ResponseEntity<TicketResponse> assignTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @Parameter(description = "Agent ID to assign to") @RequestParam(value = "agentId", required = false) String assignToAgentId,
            Authentication authentication) {
        
        String currentAgentId = authentication.getName();
        String targetAgentId = assignToAgentId != null ? assignToAgentId : currentAgentId;
        
        Ticket updatedTicket = ticketService.assignTicket(ticketId, targetAgentId, currentAgentId);
        return ResponseEntity.ok(TicketResponse.fromWithoutMessages(updatedTicket));
    }

    @PutMapping("/tickets/{ticketId}/status")
    @Operation(summary = "Update ticket status", description = "Update ticket status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @Valid @RequestBody TicketStatusUpdateRequest request,
            Authentication authentication) {
        
        String agentId = authentication.getName();
        
        if (!ticketService.canAgentAccessTicket(ticketId, agentId)) {
            return ResponseEntity.status(403).build();
        }
        
        Ticket updatedTicket = ticketService.updateTicketStatus(ticketId, request.getStatus(), agentId);
        return ResponseEntity.ok(TicketResponse.fromWithoutMessages(updatedTicket));
    }

    @PutMapping("/tickets/{ticketId}/priority")
    @Operation(summary = "Update ticket priority", description = "Update ticket priority")
    public ResponseEntity<TicketResponse> updateTicketPriority(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @Valid @RequestBody TicketPriorityUpdateRequest request,
            Authentication authentication) {
        
        String agentId = authentication.getName();
        
        if (!ticketService.canAgentAccessTicket(ticketId, agentId)) {
            return ResponseEntity.status(403).build();
        }
        
        Ticket updatedTicket = ticketService.updateTicketPriority(ticketId, request.getPriority(), agentId);
        return ResponseEntity.ok(TicketResponse.fromWithoutMessages(updatedTicket));
    }

    @PostMapping("/tickets/{ticketId}/reply")
    @Operation(summary = "Reply to ticket", description = "Add agent reply to ticket")
    public ResponseEntity<TicketMessageResponse> replyToTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @Valid @RequestBody TicketReplyRequest request,
            Authentication authentication) {
        
        String agentId = authentication.getName();
        
        if (!ticketService.canAgentAccessTicket(ticketId, agentId)) {
            return ResponseEntity.status(403).build();
        }
        
        TicketMessage message = ticketService.addAgentReply(ticketId, request.getMessage(), agentId);
        return ResponseEntity.ok(TicketMessageResponse.from(message));
    }

    // Canned Replies Management
    @GetMapping("/canned-replies")
    @Operation(summary = "Get canned replies", description = "Get canned replies for agent")
    public ResponseEntity<List<CannedReplyResponse>> getCannedReplies(
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Filter by category") @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "Search in title and body") @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "Include body content") @RequestParam(value = "includeBody", defaultValue = "true") boolean includeBody,
            Authentication authentication) {
        
        Pageable pageable = PageRequest.of(page, size);
        String agentId = authentication.getName();
        
        Page<com.hopngo.support.entity.CannedReply> replies;
        
        if (search != null && !search.trim().isEmpty()) {
            replies = cannedReplyService.searchCannedReplies(search.trim(), pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            replies = cannedReplyService.getCannedRepliesByCategory(category.trim(), pageable);
        } else {
            replies = cannedReplyService.getCannedReplies(pageable);
        }
        
        List<CannedReplyResponse> response = replies.getContent().stream()
                .map(reply -> includeBody ? 
                    CannedReplyResponse.from(reply) : 
                    CannedReplyResponse.fromWithoutBody(reply))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/canned-replies/{id}")
    @Operation(summary = "Get canned reply by ID", description = "Get specific canned reply")
    public ResponseEntity<CannedReplyResponse> getCannedReplyById(
            @Parameter(description = "Canned reply ID") @PathVariable Long id) {
        
        com.hopngo.support.entity.CannedReply reply = cannedReplyService.getCannedReplyById(id);
        if (reply == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(CannedReplyResponse.from(reply));
    }

    @PostMapping("/canned-replies")
    @Operation(summary = "Create canned reply", description = "Create new canned reply")
    public ResponseEntity<CannedReplyResponse> createCannedReply(
            @Valid @RequestBody CannedReplyCreateRequest request,
            Authentication authentication) {
        
        request.setCreatedBy(authentication.getName());
        com.hopngo.support.entity.CannedReply reply = cannedReplyService.createCannedReply(request);
        return ResponseEntity.ok(CannedReplyResponse.from(reply));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get agent dashboard", description = "Get agent-specific dashboard data")
    public ResponseEntity<Map<String, Object>> getAgentDashboard(Authentication authentication) {
        String agentId = authentication.getName();
        
        Map<String, Object> dashboard = Map.of(
            "assignedTickets", ticketService.getAssignedTicketCount(agentId),
            "openTickets", ticketService.getOpenTicketCount(agentId),
            "resolvedToday", ticketService.getResolvedTodayCount(agentId),
            "avgResponseTime", ticketService.getAverageResponseTime(agentId)
        );
        
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/canned-replies/categories")
    @Operation(summary = "Get canned reply categories", description = "Get all available canned reply categories")
    public ResponseEntity<List<String>> getCannedReplyCategories() {
        List<String> categories = cannedReplyService.getCategories();
        return ResponseEntity.ok(categories);
    }
}
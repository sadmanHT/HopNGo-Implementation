package com.hopngo.support.controller;

import com.hopngo.support.dto.*;
import com.hopngo.support.entity.Ticket;
import com.hopngo.support.entity.TicketMessage;
import com.hopngo.support.enums.MessageSender;
import com.hopngo.support.enums.TicketPriority;
import com.hopngo.support.enums.TicketStatus;
import com.hopngo.support.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/support/tickets")
@Tag(name = "Tickets", description = "Support ticket management")
public class TicketController {

    private final TicketService ticketService;

    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @Operation(summary = "Create a new support ticket", description = "Create a new support ticket. Anonymous users must provide email.")
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody TicketCreateRequest request,
            Authentication authentication) {
        
        // Set user ID if authenticated
        if (authentication != null && authentication.isAuthenticated()) {
            request.setUserId(authentication.getName());
        }
        
        Ticket ticket = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TicketResponse.fromWithoutMessages(ticket));
    }

    @GetMapping
    @Operation(summary = "Get tickets", description = "Get tickets for the current user or by email")
    public ResponseEntity<List<TicketResponse>> getTickets(
            @RequestParam(value = "me", defaultValue = "0") int me,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Authentication authentication) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Ticket> tickets;
        
        if (me == 1 && authentication != null && authentication.isAuthenticated()) {
            // Get tickets for authenticated user
            tickets = ticketService.getTicketsByUserId(authentication.getName(), pageable);
        } else if (email != null && !email.trim().isEmpty()) {
            // Get tickets by email (for anonymous users)
            tickets = ticketService.getTicketsByEmail(email, pageable);
        } else {
            return ResponseEntity.badRequest().build();
        }
        
        List<TicketResponse> response = tickets.getContent().stream()
                .map(TicketResponse::fromWithoutMessages)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get ticket by ID", description = "Get a specific ticket with messages")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable Long ticketId,
            Authentication authentication) {
        
        Ticket ticket = ticketService.getTicketById(ticketId);
        
        // Check if user has access to this ticket
        if (!ticketService.hasAccessToTicket(ticket, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    @PostMapping("/{ticketId}/messages")
    @Operation(summary = "Add message to ticket", description = "Add a new message to an existing ticket")
    public ResponseEntity<TicketMessageResponse> addMessage(
            @PathVariable Long ticketId,
            @Valid @RequestBody MessageCreateRequest request,
            Authentication authentication) {
        
        Ticket ticket = ticketService.getTicketById(ticketId);
        
        // Check if user has access to this ticket
        if (!ticketService.hasAccessToTicket(ticket, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Set sender information based on authentication
        if (authentication != null && authentication.isAuthenticated()) {
            request.setSender(MessageSender.USER);
            request.setSenderId(authentication.getName());
            if (request.getSenderName() == null) {
                request.setSenderName(authentication.getName());
            }
        } else {
            request.setSender(MessageSender.USER);
            request.setSenderName(ticket.getEmail());
        }
        
        TicketMessage message = ticketService.addMessageToTicket(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TicketMessageResponse.from(message));
    }

    @GetMapping("/{ticketId}/messages")
    @Operation(summary = "Get ticket messages", description = "Get all messages for a specific ticket")
    public ResponseEntity<List<TicketMessageResponse>> getTicketMessages(
            @PathVariable Long ticketId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            Authentication authentication) {
        
        Ticket ticket = ticketService.getTicketById(ticketId);
        
        // Check if user has access to this ticket
        if (!ticketService.hasAccessToTicket(ticket, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TicketMessage> messages = ticketService.getTicketMessages(ticketId, pageable);
        
        List<TicketMessageResponse> response = messages.getContent().stream()
                .map(TicketMessageResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    // Agent endpoints (require AGENT or ADMIN role)
    @GetMapping("/queue")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    @Operation(summary = "Get ticket queue", description = "Get tickets for agent triage (requires AGENT role)")
    public ResponseEntity<List<TicketResponse>> getTicketQueue(
            @RequestParam(value = "status", required = false) TicketStatus status,
            @RequestParam(value = "priority", required = false) TicketPriority priority,
            @RequestParam(value = "assigned", required = false) Boolean assigned,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Authentication authentication) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Ticket> tickets = ticketService.getTicketsForQueue(status, priority, assigned, pageable);
        
        List<TicketResponse> response = tickets.getContent().stream()
                .map(TicketResponse::fromWithoutMessages)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{ticketId}/assign")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    @Operation(summary = "Assign ticket to agent", description = "Assign a ticket to an agent (requires AGENT role)")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Long ticketId,
            @RequestParam(value = "agentId", required = false) String agentId,
            Authentication authentication) {
        
        // If no agentId provided, assign to current user
        if (agentId == null) {
            agentId = authentication.getName();
        }
        
        Ticket ticket = ticketService.assignTicket(ticketId, agentId);
        return ResponseEntity.ok(TicketResponse.fromWithoutMessages(ticket));
    }

    @PutMapping("/{ticketId}/status")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    @Operation(summary = "Update ticket status", description = "Update the status of a ticket (requires AGENT role)")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestParam TicketStatus status,
            Authentication authentication) {
        
        Ticket ticket = ticketService.updateTicketStatus(ticketId, status);
        return ResponseEntity.ok(TicketResponse.fromWithoutMessages(ticket));
    }

    @PutMapping("/{ticketId}/priority")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    @Operation(summary = "Update ticket priority", description = "Update the priority of a ticket (requires AGENT role)")
    public ResponseEntity<TicketResponse> updateTicketPriority(
            @PathVariable Long ticketId,
            @RequestParam TicketPriority priority,
            Authentication authentication) {
        
        Ticket ticket = ticketService.updateTicketPriority(ticketId, priority);
        return ResponseEntity.ok(TicketResponse.fromWithoutMessages(ticket));
    }

    @PostMapping("/{ticketId}/agent-reply")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    @Operation(summary = "Agent reply to ticket", description = "Add an agent reply to a ticket (requires AGENT role)")
    public ResponseEntity<TicketMessageResponse> agentReply(
            @PathVariable Long ticketId,
            @Valid @RequestBody MessageCreateRequest request,
            Authentication authentication) {
        
        // Set agent information
        request.setSender(MessageSender.AGENT);
        request.setSenderId(authentication.getName());
        if (request.getSenderName() == null) {
            request.setSenderName(authentication.getName());
        }
        
        TicketMessage message = ticketService.addAgentReply(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TicketMessageResponse.from(message));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    @Operation(summary = "Search tickets", description = "Search tickets by query (requires AGENT role)")
    public ResponseEntity<List<TicketResponse>> searchTickets(
            @RequestParam String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Ticket> tickets = ticketService.searchTickets(query, pageable);
        
        List<TicketResponse> response = tickets.getContent().stream()
                .map(TicketResponse::fromWithoutMessages)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    @Operation(summary = "Get ticket statistics", description = "Get ticket statistics (requires AGENT role)")
    public ResponseEntity<Object> getTicketStats() {
        return ResponseEntity.ok(ticketService.getTicketStatistics());
    }
}
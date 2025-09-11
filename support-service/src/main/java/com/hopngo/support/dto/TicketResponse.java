package com.hopngo.support.dto;

import com.hopngo.support.entity.Ticket;
import com.hopngo.support.enums.TicketPriority;
import com.hopngo.support.enums.TicketStatus;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class TicketResponse {

    private Long id;
    private String userId;
    private String email;
    private String subject;
    private String body;
    private TicketStatus status;
    private TicketPriority priority;
    private String assignedAgentId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<TicketMessageResponse> messages;
    private int messageCount;

    // Constructors
    public TicketResponse() {}

    public TicketResponse(Ticket ticket) {
        this.id = ticket.getId();
        this.userId = ticket.getUserId();
        this.email = ticket.getEmail();
        this.subject = ticket.getSubject();
        this.body = ticket.getBody();
        this.status = ticket.getStatus();
        this.priority = ticket.getPriority();
        this.assignedAgentId = ticket.getAssignedAgentId();
        this.createdAt = ticket.getCreatedAt();
        this.updatedAt = ticket.getUpdatedAt();
        this.messageCount = ticket.getMessages() != null ? ticket.getMessages().size() : 0;
        
        if (ticket.getMessages() != null) {
            this.messages = ticket.getMessages().stream()
                    .map(TicketMessageResponse::new)
                    .collect(Collectors.toList());
        }
    }

    public TicketResponse(Ticket ticket, boolean includeMessages) {
        this(ticket);
        if (!includeMessages) {
            this.messages = null;
        }
    }

    // Static factory methods
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(ticket);
    }

    public static TicketResponse fromWithoutMessages(Ticket ticket) {
        return new TicketResponse(ticket, false);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public String getAssignedAgentId() {
        return assignedAgentId;
    }

    public void setAssignedAgentId(String assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TicketMessageResponse> getMessages() {
        return messages;
    }

    public void setMessages(List<TicketMessageResponse> messages) {
        this.messages = messages;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    // Helper methods
    public boolean isOpen() {
        return status == TicketStatus.OPEN;
    }

    public boolean isClosed() {
        return status == TicketStatus.CLOSED || status == TicketStatus.RESOLVED;
    }

    public boolean isAssigned() {
        return assignedAgentId != null && !assignedAgentId.trim().isEmpty();
    }

    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "Unknown";
    }

    public String getPriorityDisplayName() {
        return priority != null ? priority.getDisplayName() : "Unknown";
    }

    @Override
    public String toString() {
        return "TicketResponse{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", subject='" + subject + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", assignedAgentId='" + assignedAgentId + '\'' +
                ", messageCount=" + messageCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
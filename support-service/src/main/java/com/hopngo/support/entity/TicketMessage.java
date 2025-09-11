package com.hopngo.support.entity;

import com.hopngo.support.enums.MessageSender;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "ticket_messages")
@EntityListeners(AuditingEntityListener.class)
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageSender sender;

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "sender_name")
    private String senderName;

    @NotBlank(message = "Message body is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public TicketMessage() {}

    public TicketMessage(Ticket ticket, MessageSender sender, String body) {
        this.ticket = ticket;
        this.sender = sender;
        this.body = body;
    }

    public TicketMessage(Ticket ticket, MessageSender sender, String senderId, String senderName, String body) {
        this.ticket = ticket;
        this.sender = sender;
        this.senderId = senderId;
        this.senderName = senderName;
        this.body = body;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public MessageSender getSender() {
        return sender;
    }

    public void setSender(MessageSender sender) {
        this.sender = sender;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods
    public boolean isFromUser() {
        return sender == MessageSender.USER;
    }

    public boolean isFromAgent() {
        return sender == MessageSender.AGENT;
    }

    public boolean isFromSystem() {
        return sender == MessageSender.SYSTEM;
    }

    public boolean isHumanMessage() {
        return sender.isHuman();
    }

    public boolean isAutomatedMessage() {
        return sender.isAutomated();
    }

    @Override
    public String toString() {
        return "TicketMessage{" +
                "id=" + id +
                ", ticketId=" + (ticket != null ? ticket.getId() : null) +
                ", sender=" + sender +
                ", senderId='" + senderId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
package com.hopngo.support.dto;

import com.hopngo.support.entity.TicketMessage;
import com.hopngo.support.enums.MessageSender;

import java.time.Instant;

public class TicketMessageResponse {

    private Long id;
    private Long ticketId;
    private MessageSender sender;
    private String senderId;
    private String senderName;
    private String body;
    private Instant createdAt;

    // Constructors
    public TicketMessageResponse() {}

    public TicketMessageResponse(TicketMessage message) {
        this.id = message.getId();
        this.ticketId = message.getTicket() != null ? message.getTicket().getId() : null;
        this.sender = message.getSender();
        this.senderId = message.getSenderId();
        this.senderName = message.getSenderName();
        this.body = message.getBody();
        this.createdAt = message.getCreatedAt();
    }

    // Static factory method
    public static TicketMessageResponse from(TicketMessage message) {
        return new TicketMessageResponse(message);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
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

    public String getSenderDisplayName() {
        return sender != null ? sender.getDisplayName() : "Unknown";
    }

    @Override
    public String toString() {
        return "TicketMessageResponse{" +
                "id=" + id +
                ", ticketId=" + ticketId +
                ", sender=" + sender +
                ", senderId='" + senderId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
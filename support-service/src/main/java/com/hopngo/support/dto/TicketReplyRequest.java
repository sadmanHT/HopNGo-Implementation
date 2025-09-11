package com.hopngo.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TicketReplyRequest {
    
    @NotBlank(message = "Message is required")
    @Size(max = 10000, message = "Message must not exceed 10000 characters")
    private String message;
    
    private boolean internal = false;
    
    private Long cannedReplyId;
    
    private boolean closeTicket = false;
    
    // Constructors
    public TicketReplyRequest() {}
    
    public TicketReplyRequest(String message) {
        this.message = message;
    }
    
    public TicketReplyRequest(String message, boolean internal) {
        this.message = message;
        this.internal = internal;
    }
    
    public TicketReplyRequest(String message, boolean internal, boolean closeTicket) {
        this.message = message;
        this.internal = internal;
        this.closeTicket = closeTicket;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isInternal() {
        return internal;
    }
    
    public void setInternal(boolean internal) {
        this.internal = internal;
    }
    
    public Long getCannedReplyId() {
        return cannedReplyId;
    }
    
    public void setCannedReplyId(Long cannedReplyId) {
        this.cannedReplyId = cannedReplyId;
    }
    
    public boolean isCloseTicket() {
        return closeTicket;
    }
    
    public void setCloseTicket(boolean closeTicket) {
        this.closeTicket = closeTicket;
    }
    
    // Helper methods
    public boolean isPublicReply() {
        return !internal;
    }
    
    public boolean isFromCannedReply() {
        return cannedReplyId != null;
    }
    
    public boolean shouldCloseAfterReply() {
        return closeTicket;
    }
    
    public int getMessageLength() {
        return message != null ? message.length() : 0;
    }
    
    public String getMessagePreview() {
        if (message == null || message.length() <= 100) {
            return message;
        }
        return message.substring(0, 97) + "...";
    }
    
    @Override
    public String toString() {
        return "TicketReplyRequest{" +
                "messageLength=" + getMessageLength() +
                ", internal=" + internal +
                ", cannedReplyId=" + cannedReplyId +
                ", closeTicket=" + closeTicket +
                '}';
    }
}
package com.hopngo.support.enums;

public enum TicketStatus {
    OPEN("Open", "Ticket is newly created and awaiting initial review"),
    PENDING("Pending", "Ticket is being worked on and awaiting response"),
    RESOLVED("Resolved", "Ticket has been resolved and awaiting confirmation"),
    CLOSED("Closed", "Ticket is closed and no further action is needed");

    private final String displayName;
    private final String description;

    TicketStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == OPEN || this == PENDING;
    }

    public boolean isFinal() {
        return this == RESOLVED || this == CLOSED;
    }

    public static TicketStatus fromString(String status) {
        if (status == null) {
            return null;
        }
        
        for (TicketStatus ticketStatus : TicketStatus.values()) {
            if (ticketStatus.name().equalsIgnoreCase(status) || 
                ticketStatus.displayName.equalsIgnoreCase(status)) {
                return ticketStatus;
            }
        }
        
        throw new IllegalArgumentException("Unknown ticket status: " + status);
    }
}
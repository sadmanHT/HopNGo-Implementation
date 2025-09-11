package com.hopngo.support.enums;

public enum MessageSender {
    USER("User", "Message sent by the ticket creator or user"),
    AGENT("Agent", "Message sent by a support agent"),
    SYSTEM("System", "Automated message sent by the system");

    private final String displayName;
    private final String description;

    MessageSender(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHuman() {
        return this == USER || this == AGENT;
    }

    public boolean isAutomated() {
        return this == SYSTEM;
    }

    public static MessageSender fromString(String sender) {
        if (sender == null) {
            return null;
        }
        
        for (MessageSender messageSender : MessageSender.values()) {
            if (messageSender.name().equalsIgnoreCase(sender) || 
                messageSender.displayName.equalsIgnoreCase(sender)) {
                return messageSender;
            }
        }
        
        throw new IllegalArgumentException("Unknown message sender: " + sender);
    }
}
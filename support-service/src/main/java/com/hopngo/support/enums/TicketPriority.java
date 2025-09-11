package com.hopngo.support.enums;

public enum TicketPriority {
    LOW("Low", "Low priority - can be addressed in normal workflow", 1),
    MEDIUM("Medium", "Medium priority - standard response time", 2),
    HIGH("High", "High priority - requires urgent attention", 3);

    private final String displayName;
    private final String description;
    private final int level;

    TicketPriority(String displayName, String description, int level) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    public boolean isHigherThan(TicketPriority other) {
        return this.level > other.level;
    }

    public boolean isLowerThan(TicketPriority other) {
        return this.level < other.level;
    }

    public static TicketPriority fromString(String priority) {
        if (priority == null) {
            return null;
        }
        
        for (TicketPriority ticketPriority : TicketPriority.values()) {
            if (ticketPriority.name().equalsIgnoreCase(priority) || 
                ticketPriority.displayName.equalsIgnoreCase(priority)) {
                return ticketPriority;
            }
        }
        
        throw new IllegalArgumentException("Unknown ticket priority: " + priority);
    }

    public static TicketPriority fromLevel(int level) {
        for (TicketPriority priority : TicketPriority.values()) {
            if (priority.level == level) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unknown priority level: " + level);
    }
}
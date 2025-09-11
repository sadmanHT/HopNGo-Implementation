package com.hopngo.tripplanning.enums;

/**
 * Enumeration for itinerary sharing visibility levels
 */
public enum ShareVisibility {
    /**
     * Private - only accessible by owner
     */
    PRIVATE,
    
    /**
     * Link - accessible by anyone with the share link
     */
    LINK,
    
    /**
     * Public - accessible to everyone
     */
    PUBLIC
}
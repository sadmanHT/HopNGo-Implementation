package com.hopngo.market.entity;

/**
 * Enum representing the status of webhook event processing.
 */
public enum WebhookEventStatus {
    /**
     * Webhook event has been received but not yet processed.
     */
    RECEIVED,
    
    /**
     * Webhook event is currently being processed.
     */
    PROCESSING,
    
    /**
     * Webhook event has been successfully processed.
     */
    PROCESSED,
    
    /**
     * Webhook event processing failed.
     */
    FAILED
}
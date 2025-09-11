package com.hopngo.market.entity;

/**
 * Enumeration representing the various states of an invoice in the system.
 */
public enum InvoiceStatus {
    
    /**
     * Invoice has been created but not yet issued to the customer.
     * This is the initial state when an invoice is first created.
     */
    DRAFT,
    
    /**
     * Invoice has been issued to the customer and is awaiting payment.
     * The invoice is now visible to the customer and payment is expected.
     */
    ISSUED,
    
    /**
     * Invoice has been fully paid by the customer.
     * This is a terminal state for successful transactions.
     */
    PAID,
    
    /**
     * Invoice has been cancelled before payment.
     * This could happen due to order cancellation or other business reasons.
     */
    CANCELLED,
    
    /**
     * Invoice was paid but has been refunded to the customer.
     * This indicates a reversal of the original transaction.
     */
    REFUNDED;
    
    /**
     * Check if the invoice status represents a finalized state.
     * @return true if the status is PAID, CANCELLED, or REFUNDED
     */
    public boolean isFinal() {
        return this == PAID || this == CANCELLED || this == REFUNDED;
    }
    
    /**
     * Check if the invoice status allows for payment.
     * @return true if the status is ISSUED
     */
    public boolean canBePaid() {
        return this == ISSUED;
    }
    
    /**
     * Check if the invoice status allows for cancellation.
     * @return true if the status is DRAFT or ISSUED
     */
    public boolean canBeCancelled() {
        return this == DRAFT || this == ISSUED;
    }
    
    /**
     * Check if the invoice status allows for refunding.
     * @return true if the status is PAID
     */
    public boolean canBeRefunded() {
        return this == PAID;
    }
    
    /**
     * Check if the invoice is in an active state (not cancelled or refunded).
     * @return true if the status is DRAFT, ISSUED, or PAID
     */
    public boolean isActive() {
        return this == DRAFT || this == ISSUED || this == PAID;
    }
}
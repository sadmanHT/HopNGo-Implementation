package com.hopngo.market.exception;

/**
 * Exception thrown when a payout is not found.
 */
public class PayoutNotFoundException extends RuntimeException {
    
    public PayoutNotFoundException(String message) {
        super(message);
    }
    
    public PayoutNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
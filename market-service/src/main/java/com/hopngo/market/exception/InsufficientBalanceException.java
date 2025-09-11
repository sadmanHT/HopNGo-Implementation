package com.hopngo.market.exception;

/**
 * Exception thrown when a provider doesn't have sufficient balance for a payout.
 */
public class InsufficientBalanceException extends RuntimeException {
    
    public InsufficientBalanceException(String message) {
        super(message);
    }
    
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
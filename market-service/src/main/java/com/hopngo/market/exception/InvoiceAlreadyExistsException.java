package com.hopngo.market.exception;

/**
 * Exception thrown when attempting to create an invoice that already exists.
 */
public class InvoiceAlreadyExistsException extends RuntimeException {
    
    public InvoiceAlreadyExistsException(String message) {
        super(message);
    }
    
    public InvoiceAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
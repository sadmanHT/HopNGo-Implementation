package com.hopngo.market.exception;

/**
 * Exception thrown when an invoice is not found in the system.
 */
public class InvoiceNotFoundException extends RuntimeException {
    
    public InvoiceNotFoundException(String message) {
        super(message);
    }
    
    public InvoiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
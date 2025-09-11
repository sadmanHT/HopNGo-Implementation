package com.hopngo.market.exception;

/**
 * Exception thrown when an operation is attempted on an invoice in an invalid state.
 */
public class InvalidInvoiceStateException extends RuntimeException {
    
    public InvalidInvoiceStateException(String message) {
        super(message);
    }
    
    public InvalidInvoiceStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
package com.hopngo.common.exception;

/**
 * Exception for business logic violations that should result in 4xx responses
 * These are expected exceptions that don't need to be sent to Sentry
 */
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    // Common business exception factory methods
    public static BusinessException invalidInput(String message) {
        return new BusinessException(message, "INVALID_INPUT");
    }
    
    public static BusinessException insufficientFunds(String message) {
        return new BusinessException(message, "INSUFFICIENT_FUNDS");
    }
    
    public static BusinessException bookingConflict(String message) {
        return new BusinessException(message, "BOOKING_CONFLICT");
    }
    
    public static BusinessException paymentFailed(String message) {
        return new BusinessException(message, "PAYMENT_FAILED");
    }
    
    public static BusinessException userNotEligible(String message) {
        return new BusinessException(message, "USER_NOT_ELIGIBLE");
    }
    
    public static BusinessException rateLimit(String message) {
        return new BusinessException(message, "RATE_LIMIT_EXCEEDED");
    }
}
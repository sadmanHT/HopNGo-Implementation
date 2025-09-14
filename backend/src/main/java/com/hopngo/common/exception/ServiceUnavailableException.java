package com.hopngo.common.exception;

/**
 * Exception for when a service is temporarily unavailable (503)
 * These should be sent to Sentry as they indicate infrastructure issues
 */
public class ServiceUnavailableException extends RuntimeException {
    
    private final String serviceName;
    private final String errorCode;
    
    public ServiceUnavailableException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        this.errorCode = "SERVICE_UNAVAILABLE";
    }
    
    public ServiceUnavailableException(String serviceName, String message, String errorCode) {
        super(message);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }
    
    public ServiceUnavailableException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.errorCode = "SERVICE_UNAVAILABLE";
    }
    
    public ServiceUnavailableException(String serviceName, String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    // Common service unavailable factory methods
    public static ServiceUnavailableException database(String message) {
        return new ServiceUnavailableException("Database", message, "DATABASE_UNAVAILABLE");
    }
    
    public static ServiceUnavailableException database(String message, Throwable cause) {
        return new ServiceUnavailableException("Database", message, "DATABASE_UNAVAILABLE", cause);
    }
    
    public static ServiceUnavailableException redis(String message) {
        return new ServiceUnavailableException("Redis", message, "CACHE_UNAVAILABLE");
    }
    
    public static ServiceUnavailableException redis(String message, Throwable cause) {
        return new ServiceUnavailableException("Redis", message, "CACHE_UNAVAILABLE", cause);
    }
    
    public static ServiceUnavailableException externalApi(String apiName, String message) {
        return new ServiceUnavailableException(apiName, message, "EXTERNAL_API_UNAVAILABLE");
    }
    
    public static ServiceUnavailableException externalApi(String apiName, String message, Throwable cause) {
        return new ServiceUnavailableException(apiName, message, "EXTERNAL_API_UNAVAILABLE", cause);
    }
    
    public static ServiceUnavailableException messageQueue(String message) {
        return new ServiceUnavailableException("Message Queue", message, "MESSAGE_QUEUE_UNAVAILABLE");
    }
    
    public static ServiceUnavailableException messageQueue(String message, Throwable cause) {
        return new ServiceUnavailableException("Message Queue", message, "MESSAGE_QUEUE_UNAVAILABLE", cause);
    }
    
    public static ServiceUnavailableException paymentGateway(String message) {
        return new ServiceUnavailableException("Payment Gateway", message, "PAYMENT_GATEWAY_UNAVAILABLE");
    }
    
    public static ServiceUnavailableException paymentGateway(String message, Throwable cause) {
        return new ServiceUnavailableException("Payment Gateway", message, "PAYMENT_GATEWAY_UNAVAILABLE", cause);
    }
    
    public static ServiceUnavailableException mapService(String message) {
        return new ServiceUnavailableException("Map Service", message, "MAP_SERVICE_UNAVAILABLE");
    }
    
    public static ServiceUnavailableException mapService(String message, Throwable cause) {
        return new ServiceUnavailableException("Map Service", message, "MAP_SERVICE_UNAVAILABLE", cause);
    }
}
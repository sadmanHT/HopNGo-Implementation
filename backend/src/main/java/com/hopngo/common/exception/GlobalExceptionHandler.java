package com.hopngo.common.exception;

import com.hopngo.common.monitoring.SentryConfig;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle validation errors (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid input parameters")
                .path(request.getRequestURI())
                .validationErrors(errors)
                .build();

        // Don't send validation errors to Sentry (they're client errors)
        logger.debug("Validation error on {}: {}", request.getRequestURI(), errors);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle constraint violation errors (400)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message("Invalid input constraints")
                .path(request.getRequestURI())
                .validationErrors(errors)
                .build();

        logger.debug("Constraint violation on {}: {}", request.getRequestURI(), errors);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle access denied errors (403)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("You don't have permission to access this resource")
                .path(request.getRequestURI())
                .build();

        // Log security events but don't spam Sentry
        logger.warn("Access denied for {} on {}", getClientIp(request), request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle not found errors (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("The requested resource was not found")
                .path(request.getRequestURI())
                .build();

        logger.debug("Resource not found: {}", request.getRequestURI());
        
        return ResponseEntity.notFound().build();
    }

    /**
     * Handle business logic exceptions (400)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Business Logic Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .build();

        // Log business exceptions but don't send to Sentry (they're expected)
        logger.info("Business exception on {}: {} (code: {})", 
                   request.getRequestURI(), ex.getMessage(), ex.getErrorCode());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle resource not found exceptions (404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Resource Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .build();

        logger.debug("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity.notFound().build();
    }

    /**
     * Handle service unavailable exceptions (503)
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailableException(
            ServiceUnavailableException ex, HttpServletRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("Service is temporarily unavailable. Please try again later.")
                .path(request.getRequestURI())
                .errorId(errorId)
                .build();

        // Send service unavailable errors to Sentry
        Map<String, Object> extra = new HashMap<>();
        extra.put("error_id", errorId);
        extra.put("service", ex.getServiceName());
        extra.put("endpoint", request.getRequestURI());
        
        SentryConfig.captureException(ex, extra);
        logger.error("Service unavailable ({}): {} on {}", 
                    errorId, ex.getMessage(), request.getRequestURI(), ex);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * Handle all other exceptions (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .errorId(errorId)
                .build();

        // Send all 5xx errors to Sentry with full context
        Map<String, Object> extra = new HashMap<>();
        extra.put("error_id", errorId);
        extra.put("endpoint", request.getRequestURI());
        extra.put("method", request.getMethod());
        extra.put("user_agent", request.getHeader("User-Agent"));
        extra.put("client_ip", getClientIp(request));
        
        // Add query parameters if present
        if (request.getQueryString() != null) {
            extra.put("query_string", request.getQueryString());
        }
        
        // Add trace ID if present
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId != null) {
            extra.put("trace_id", traceId);
        }
        
        SentryConfig.captureException(ex, extra);
        logger.error("Unexpected error ({}): {} on {}", 
                    errorId, ex.getMessage(), request.getRequestURI(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Error response DTO
     */
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private String errorId;
        private String errorCode;
        private Map<String, String> validationErrors;

        // Builder pattern
        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }

        // Getters and setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getErrorId() { return errorId; }
        public void setErrorId(String errorId) { this.errorId = errorId; }
        
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        
        public Map<String, String> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; }

        public static class ErrorResponseBuilder {
            private ErrorResponse errorResponse = new ErrorResponse();

            public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
                errorResponse.setTimestamp(timestamp);
                return this;
            }

            public ErrorResponseBuilder status(int status) {
                errorResponse.setStatus(status);
                return this;
            }

            public ErrorResponseBuilder error(String error) {
                errorResponse.setError(error);
                return this;
            }

            public ErrorResponseBuilder message(String message) {
                errorResponse.setMessage(message);
                return this;
            }

            public ErrorResponseBuilder path(String path) {
                errorResponse.setPath(path);
                return this;
            }

            public ErrorResponseBuilder errorId(String errorId) {
                errorResponse.setErrorId(errorId);
                return this;
            }

            public ErrorResponseBuilder errorCode(String errorCode) {
                errorResponse.setErrorCode(errorCode);
                return this;
            }

            public ErrorResponseBuilder validationErrors(Map<String, String> validationErrors) {
                errorResponse.setValidationErrors(validationErrors);
                return this;
            }

            public ErrorResponse build() {
                return errorResponse;
            }
        }
    }
}
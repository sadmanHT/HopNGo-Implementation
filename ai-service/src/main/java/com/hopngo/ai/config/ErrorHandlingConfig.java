package com.hopngo.ai.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class ErrorHandlingConfig {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingConfig.class);

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleTimeoutException(TimeoutException ex) {
        logger.error("Request timeout occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "REQUEST_TIMEOUT");
        errorResponse.put("message", "The request took too long to process. Please try again.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("suggestion", "Consider reducing the complexity of your request or try again later.");
        
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAccessException(ResourceAccessException ex) {
        logger.error("Resource access error (likely timeout): {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "SERVICE_UNAVAILABLE");
        errorResponse.put("message", "External AI service is temporarily unavailable. Please try again later.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("suggestion", "The service may be experiencing high load. Please retry in a few moments.");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler({JsonProcessingException.class, JsonMappingException.class})
    public ResponseEntity<Map<String, Object>> handleJsonProcessingException(Exception ex) {
        logger.error("JSON serialization/deserialization error: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "JSON_PROCESSING_ERROR");
        errorResponse.put("message", "Error processing JSON data. Please check your request format.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("suggestion", "Ensure your request body is valid JSON and all required fields are present.");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpClientErrorException(HttpClientErrorException ex) {
        logger.error("HTTP client error: {} - {}", ex.getStatusCode(), ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "EXTERNAL_API_ERROR");
        errorResponse.put("message", "External AI service returned an error. Please check your request.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("httpStatus", ex.getStatusCode().value());
        
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            errorResponse.put("suggestion", "API authentication failed. Please check your API keys.");
        } else if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            errorResponse.put("suggestion", "Rate limit exceeded. Please wait before making another request.");
        } else {
            errorResponse.put("suggestion", "Please verify your request parameters and try again.");
        }
        
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpServerErrorException(HttpServerErrorException ex) {
        logger.error("HTTP server error: {} - {}", ex.getStatusCode(), ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "EXTERNAL_SERVICE_ERROR");
        errorResponse.put("message", "External AI service is experiencing issues. Please try again later.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("httpStatus", ex.getStatusCode().value());
        errorResponse.put("suggestion", "The external service is temporarily unavailable. Please retry in a few minutes.");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        logger.error("File upload size exceeded: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "FILE_TOO_LARGE");
        errorResponse.put("message", "The uploaded file is too large. Maximum size is 10MB.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("suggestion", "Please compress your image or use a smaller file.");
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.error("Validation error: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "VALIDATION_ERROR");
        errorResponse.put("message", "Request validation failed.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        errorResponse.put("fieldErrors", fieldErrors);
        errorResponse.put("suggestion", "Please correct the validation errors and try again.");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        logger.error("Constraint violation: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "CONSTRAINT_VIOLATION");
        errorResponse.put("message", "Request constraints violated.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("violations", ex.getConstraintViolations().toString());
        errorResponse.put("suggestion", "Please check your request parameters and ensure they meet the requirements.");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.error("Illegal argument: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "INVALID_ARGUMENT");
        errorResponse.put("message", "Invalid request parameter: " + ex.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("suggestion", "Please check your request parameters and try again.");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "INTERNAL_ERROR");
        errorResponse.put("message", "An unexpected error occurred while processing your request.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("suggestion", "Please try again. If the problem persists, contact support.");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected exception: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "UNEXPECTED_ERROR");
        errorResponse.put("message", "An unexpected error occurred. Please try again later.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("suggestion", "If this error persists, please contact technical support.");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
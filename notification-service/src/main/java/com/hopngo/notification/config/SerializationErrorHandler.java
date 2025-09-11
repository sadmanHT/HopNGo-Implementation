package com.hopngo.notification.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class SerializationErrorHandler {

    private final Counter rabbitMQSerializationErrors;
    private final NotificationMonitoringService monitoringService;
    private final AtomicLong totalSerializationAttempts = new AtomicLong(0);
    private final AtomicLong failedSerializationAttempts = new AtomicLong(0);

    public void handleSerializationError(String operation, Object data, Exception exception) {
        failedSerializationAttempts.incrementAndGet();
        rabbitMQSerializationErrors.increment();
        monitoringService.incrementSerializationErrors();
        
        log.error("Serialization error during {} operation at {}", 
                operation, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Log detailed error information
        if (exception instanceof JsonProcessingException) {
            JsonProcessingException jsonException = (JsonProcessingException) exception;
            log.error("JSON Processing Error - Location: {}, Message: {}", 
                    jsonException.getLocation(), jsonException.getOriginalMessage());
        } else if (exception instanceof JsonMappingException) {
            JsonMappingException mappingException = (JsonMappingException) exception;
            log.error("JSON Mapping Error - Path: {}, Message: {}", 
                    mappingException.getPathReference(), mappingException.getOriginalMessage());
        }
        
        // Log the problematic data (safely)
        logProblematicData(data, exception);
        
        // Log serialization statistics
        long total = totalSerializationAttempts.get();
        long failed = failedSerializationAttempts.get();
        double errorRate = total > 0 ? (double) failed / total * 100 : 0;
        
        log.warn("Serialization Error Rate: {:.2f}% ({} failed out of {} total attempts)", 
                errorRate, failed, total);
        
        // Alert if error rate is too high
        if (errorRate > 5.0 && total > 100) {
            log.error("ALERT: High serialization error rate detected: {:.2f}%", errorRate);
        }
    }

    public void recordSuccessfulSerialization() {
        totalSerializationAttempts.incrementAndGet();
    }

    private void logProblematicData(Object data, Exception exception) {
        try {
            if (data == null) {
                log.error("Serialization failed for null data");
                return;
            }
            
            String dataType = data.getClass().getSimpleName();
            String dataString = data.toString();
            
            // Truncate long data strings
            if (dataString.length() > 500) {
                dataString = dataString.substring(0, 500) + "... (truncated)";
            }
            
            log.error("Problematic data - Type: {}, Content: {}, Exception: {}", 
                    dataType, dataString, exception.getMessage());
            
            // Try to identify specific problematic fields
            if (exception.getMessage() != null) {
                String message = exception.getMessage().toLowerCase();
                if (message.contains("cannot deserialize")) {
                    log.error("Deserialization issue detected - likely data type mismatch or missing fields");
                } else if (message.contains("cannot serialize")) {
                    log.error("Serialization issue detected - likely circular reference or unsupported type");
                } else if (message.contains("unexpected token")) {
                    log.error("JSON format issue detected - malformed JSON structure");
                }
            }
            
        } catch (Exception e) {
            log.error("Error while logging problematic data", e);
        }
    }

    public double getSerializationErrorRate() {
        long total = totalSerializationAttempts.get();
        long failed = failedSerializationAttempts.get();
        return total > 0 ? (double) failed / total * 100 : 0;
    }

    public void resetCounters() {
        totalSerializationAttempts.set(0);
        failedSerializationAttempts.set(0);
    }
}

@Configuration
@RequiredArgsConstructor
class SerializationConfig {

    private final SerializationErrorHandler errorHandler;

    @Bean
    @Primary
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        
        return new Jackson2JsonMessageConverter(objectMapper) {
            @Override
            public Object fromMessage(Message message) throws MessageConversionException {
                try {
                    errorHandler.recordSuccessfulSerialization();
                    return super.fromMessage(message);
                } catch (MessageConversionException e) {
                    handleDeserializationError(message, e);
                    throw e;
                } catch (Exception e) {
                    handleDeserializationError(message, e);
                    throw new MessageConversionException("Failed to convert message", e);
                }
            }

            @Override
            protected Message createMessage(Object object, MessageProperties messageProperties) {
                try {
                    errorHandler.recordSuccessfulSerialization();
                    return super.createMessage(object, messageProperties);
                } catch (MessageConversionException e) {
                    handleSerializationError(object, e);
                    throw e;
                } catch (Exception e) {
                    handleSerializationError(object, e);
                    throw new MessageConversionException("Failed to create message", e);
                }
            }

            private void handleDeserializationError(Message message, Exception e) {
                String messageBody = "";
                try {
                    messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
                    if (messageBody.length() > 200) {
                        messageBody = messageBody.substring(0, 200) + "... (truncated)";
                    }
                } catch (Exception ex) {
                    messageBody = "[Unable to read message body]";
                }
                
                log.error("Message deserialization failed - Exchange: {}, Routing Key: {}, Message Body: {}",
                        message.getMessageProperties().getReceivedExchange(),
                        message.getMessageProperties().getReceivedRoutingKey(),
                        messageBody);
                
                errorHandler.handleSerializationError("deserialization", messageBody, e);
            }

            private void handleSerializationError(Object object, Exception e) {
                errorHandler.handleSerializationError("serialization", object, e);
            }
        };
    }

    @Bean
    public ObjectMapper rabbitMQObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        
        // Configure mapper for better error handling
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        return mapper;
    }
}

@Component
@Slf4j
class SerializationMonitor {

    private final SerializationErrorHandler errorHandler;

    public SerializationMonitor(SerializationErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logSerializationStatistics() {
        double errorRate = errorHandler.getSerializationErrorRate();
        
        if (errorRate > 0) {
            log.info("Serialization Error Rate: {:.2f}%", errorRate);
            
            if (errorRate > 2.0) {
                log.warn("Elevated serialization error rate detected: {:.2f}%", errorRate);
            }
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    public void resetSerializationCounters() {
        double finalErrorRate = errorHandler.getSerializationErrorRate();
        log.info("Daily serialization summary - Final error rate: {:.2f}%", finalErrorRate);
        errorHandler.resetCounters();
    }
}
package com.hopngo.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Component;

import jakarta.validation.ValidationException;

@Component("rabbitListenerErrorHandler")
public class CustomRabbitListenerErrorHandler implements RabbitListenerErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomRabbitListenerErrorHandler.class);

    @Override
    public Object handleError(Message amqpMessage, org.springframework.messaging.Message<?> message,
                            ListenerExecutionFailedException exception) throws Exception {
        
        logger.error("RabbitMQ Listener Error occurred. Message: {}, Exception: {}", 
                    new String(amqpMessage.getBody()), exception.getMessage(), exception);
        
        // Log message properties for debugging
        logMessageDetails(amqpMessage, message);
        
        // Determine if the error should be retried or sent to DLQ
        Throwable cause = exception.getCause();
        
        if (cause instanceof ValidationException) {
            logger.error("Validation error - sending message to DLQ: {}", cause.getMessage());
            // Don't retry validation errors
            throw new AmqpRejectAndDontRequeueException("Validation failed", cause);
        }
        
        if (cause instanceof IllegalArgumentException) {
            logger.error("Illegal argument error - sending message to DLQ: {}", cause.getMessage());
            // Don't retry illegal argument errors
            throw new AmqpRejectAndDontRequeueException("Illegal argument", cause);
        }
        
        if (cause instanceof NullPointerException) {
            logger.error("Null pointer error - sending message to DLQ: {}", cause.getMessage());
            // Don't retry NPE errors
            throw new AmqpRejectAndDontRequeueException("Null pointer exception", cause);
        }
        
        // For other exceptions, allow retry by re-throwing
        logger.warn("Retryable error occurred, message will be retried: {}", cause.getMessage());
        throw exception;
    }
    
    private void logMessageDetails(Message amqpMessage, org.springframework.messaging.Message<?> message) {
        try {
            logger.debug("AMQP Message Details:");
            logger.debug("  - Body: {}", new String(amqpMessage.getBody()));
            logger.debug("  - Routing Key: {}", amqpMessage.getMessageProperties().getReceivedRoutingKey());
            logger.debug("  - Exchange: {}", amqpMessage.getMessageProperties().getReceivedExchange());
            logger.debug("  - Queue: {}", amqpMessage.getMessageProperties().getConsumerQueue());
            logger.debug("  - Delivery Tag: {}", amqpMessage.getMessageProperties().getDeliveryTag());
            logger.debug("  - Redelivered: {}", amqpMessage.getMessageProperties().isRedelivered());
            logger.debug("  - Headers: {}", amqpMessage.getMessageProperties().getHeaders());
            
            if (message != null) {
                logger.debug("Spring Message Details:");
                logger.debug("  - Payload: {}", message.getPayload());
                logger.debug("  - Headers: {}", message.getHeaders());
            }
        } catch (Exception e) {
            logger.warn("Failed to log message details: {}", e.getMessage());
        }
    }
}
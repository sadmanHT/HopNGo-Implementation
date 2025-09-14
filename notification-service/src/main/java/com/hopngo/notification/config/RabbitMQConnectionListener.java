package com.hopngo.notification.config;


import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownSignalException;

import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RabbitMQConnectionListener implements ConnectionListener {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConnectionListener.class);
    
    private final Counter rabbitMQConnectionErrors;
    private final NotificationMonitoringService monitoringService;
    
    public RabbitMQConnectionListener(Counter rabbitMQConnectionErrors, NotificationMonitoringService monitoringService) {
        this.rabbitMQConnectionErrors = rabbitMQConnectionErrors;
        this.monitoringService = monitoringService;
    }
    
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicInteger reconnectionAttempts = new AtomicInteger(0);
    private volatile LocalDateTime lastConnectionTime;
    private volatile LocalDateTime lastDisconnectionTime;
    private volatile String lastErrorMessage;

    @Override
    public void onCreate(org.springframework.amqp.rabbit.connection.Connection connection) {
        isConnected.set(true);
        lastConnectionTime = LocalDateTime.now();
        reconnectionAttempts.set(0);
        
        log.info("RabbitMQ connection established successfully at {}", 
                lastConnectionTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Log connection details
        log.debug("RabbitMQ Connection established");
    }

    @Override
    public void onClose(org.springframework.amqp.rabbit.connection.Connection connection) {
        isConnected.set(false);
        lastDisconnectionTime = LocalDateTime.now();
        
        log.warn("RabbitMQ connection closed at {}", 
                lastDisconnectionTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Log connection closure
        log.debug("RabbitMQ Connection closed");
    }

    @Override
    public void onShutDown(ShutdownSignalException signal) {
        isConnected.set(false);
        lastDisconnectionTime = LocalDateTime.now();
        
        if (signal.isHardError()) {
            if (signal.getReason() != null) {
                 handleConnectionError("Hard connection error", signal);
                 log.error("RabbitMQ hard connection error: {}", signal.getReason().toString());
            } else {
                handleConnectionError("Unknown hard connection error", signal);
            }
        } else {
            log.warn("RabbitMQ channel shutdown: {}", signal.getMessage());
        }
    }

    @Async
    public void handleConnectionError(String errorType, Exception exception) {
        lastErrorMessage = String.format("%s: %s", errorType, exception.getMessage());
        
        // Increment error counters
        rabbitMQConnectionErrors.increment();
        monitoringService.incrementRabbitMQErrors();
        
        // Log detailed error information
        log.error("RabbitMQ Connection Error [{}] - {}", errorType, exception.getMessage(), exception);
        
        // Log connection state
        log.error("Connection State - Connected: {}, Last Connection: {}, Last Disconnection: {}, Reconnection Attempts: {}",
                isConnected.get(),
                lastConnectionTime != null ? lastConnectionTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "Never",
                lastDisconnectionTime != null ? lastDisconnectionTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "Never",
                reconnectionAttempts.get());
        
        // Increment reconnection attempts
        reconnectionAttempts.incrementAndGet();
        
        // Alert if too many reconnection attempts
        if (reconnectionAttempts.get() > 5) {
            log.error("ALERT: Multiple RabbitMQ reconnection attempts detected ({}). This may indicate a persistent connection issue.", 
                    reconnectionAttempts.get());
        }
        
        // Log system resources that might affect connection
        logSystemResources();
    }

    private void logSystemResources() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            log.debug("System Resources - Memory Usage: {:.2f}% ({} MB used / {} MB max), Available Processors: {}",
                    memoryUsagePercent,
                    usedMemory / (1024 * 1024),
                    maxMemory / (1024 * 1024),
                    runtime.availableProcessors());
            
            if (memoryUsagePercent > 90) {
                log.warn("High memory usage detected: {:.2f}%. This may affect RabbitMQ connection stability.", memoryUsagePercent);
            }
        } catch (Exception e) {
            log.debug("Could not retrieve system resource information", e);
        }
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public LocalDateTime getLastConnectionTime() {
        return lastConnectionTime;
    }

    public LocalDateTime getLastDisconnectionTime() {
        return lastDisconnectionTime;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public int getReconnectionAttempts() {
        return reconnectionAttempts.get();
    }

    public void resetReconnectionAttempts() {
        reconnectionAttempts.set(0);
    }
}

@Configuration
class RabbitMQMonitoringConfig {

    private final RabbitMQConnectionListener connectionListener;
    
    public RabbitMQMonitoringConfig(RabbitMQConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    @Bean
    public RabbitTemplate monitoredRabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // Add connection listener
        if (connectionFactory instanceof org.springframework.amqp.rabbit.connection.CachingConnectionFactory) {
            ((org.springframework.amqp.rabbit.connection.CachingConnectionFactory) connectionFactory)
                    .addConnectionListener(connectionListener);
        }
        
        // Configure template for better error handling
        template.setMandatory(true);
        template.setReturnsCallback(returnedMessage -> {
            org.slf4j.LoggerFactory.getLogger(RabbitMQMonitoringConfig.class)
                .error("Message returned - Exchange: {}, Routing Key: {}, Reply Code: {}, Reply Text: {}",
                    returnedMessage.getExchange(), returnedMessage.getRoutingKey(), 
                    returnedMessage.getReplyCode(), returnedMessage.getReplyText());
        });
        
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                org.slf4j.LoggerFactory.getLogger(RabbitMQMonitoringConfig.class)
                    .error("Message not acknowledged - Correlation Data: {}, Cause: {}", correlationData, cause);
            }
        });
        
        return template;
    }
}
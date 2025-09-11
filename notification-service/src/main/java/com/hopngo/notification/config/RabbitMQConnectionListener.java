package com.hopngo.notification.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownSignalException;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQConnectionListener implements ConnectionListener {

    private final Counter rabbitMQConnectionErrors;
    private final NotificationMonitoringService monitoringService;
    
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicInteger reconnectionAttempts = new AtomicInteger(0);
    private volatile LocalDateTime lastConnectionTime;
    private volatile LocalDateTime lastDisconnectionTime;
    private volatile String lastErrorMessage;

    @Override
    public void onCreate(Connection connection) {
        isConnected.set(true);
        lastConnectionTime = LocalDateTime.now();
        reconnectionAttempts.set(0);
        
        log.info("RabbitMQ connection established successfully at {}", 
                lastConnectionTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Log connection details
        log.debug("RabbitMQ Connection Details - Host: {}, Port: {}, Virtual Host: {}",
                connection.getAddress().getHostAddress(),
                connection.getPort(),
                connection.getServerProperties().get("virtual_host"));
    }

    @Override
    public void onClose(Connection connection) {
        isConnected.set(false);
        lastDisconnectionTime = LocalDateTime.now();
        
        log.warn("RabbitMQ connection closed at {}", 
                lastDisconnectionTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Check if this was an unexpected closure
        if (connection.getCloseReason() != null) {
            ShutdownSignalException reason = connection.getCloseReason();
            if (!reason.isInitiatedByApplication()) {
                handleConnectionError("Unexpected connection closure", reason);
            }
        }
    }

    @Override
    public void onShutDown(ShutdownSignalException signal) {
        isConnected.set(false);
        lastDisconnectionTime = LocalDateTime.now();
        
        if (signal.isHardError()) {
            if (signal.getReason() instanceof Connection.CloseReason) {
                Connection.CloseReason closeReason = (Connection.CloseReason) signal.getReason();
                handleConnectionError("Hard connection error", signal);
                log.error("RabbitMQ hard connection error - Code: {}, Text: {}", 
                        closeReason.protocolMethodName(), closeReason.protocolClassId());
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
@RequiredArgsConstructor
class RabbitMQMonitoringConfig {

    private final RabbitMQConnectionListener connectionListener;

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
        template.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("Message returned - Exchange: {}, Routing Key: {}, Reply Code: {}, Reply Text: {}",
                    exchange, routingKey, replyCode, replyText);
        });
        
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Message not acknowledged - Correlation Data: {}, Cause: {}", correlationData, cause);
            }
        });
        
        return template;
    }
}
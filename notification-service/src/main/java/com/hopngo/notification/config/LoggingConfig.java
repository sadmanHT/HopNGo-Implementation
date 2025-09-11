package com.hopngo.notification.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@RequiredArgsConstructor
public class LoggingConfig {

    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void configureLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Configure RabbitMQ specific logging
        configureRabbitMQLogging(context);
        
        // Configure notification service logging
        configureNotificationLogging(context);
    }

    private void configureRabbitMQLogging(LoggerContext context) {
        // Create RabbitMQ specific appender
        RollingFileAppender<ILoggingEvent> rabbitmqAppender = new RollingFileAppender<>();
        rabbitmqAppender.setContext(context);
        rabbitmqAppender.setName("RABBITMQ_FILE");
        rabbitmqAppender.setFile("logs/rabbitmq.log");
        
        TimeBasedRollingPolicy<ILoggingEvent> rabbitmqPolicy = new TimeBasedRollingPolicy<>();
        rabbitmqPolicy.setContext(context);
        rabbitmqPolicy.setParent(rabbitmqAppender);
        rabbitmqPolicy.setFileNamePattern("logs/rabbitmq.%d{yyyy-MM-dd}.%i.log.gz");
        rabbitmqPolicy.setMaxHistory(30);
        rabbitmqPolicy.start();
        
        PatternLayoutEncoder rabbitmqEncoder = new PatternLayoutEncoder();
        rabbitmqEncoder.setContext(context);
        rabbitmqEncoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{36}] - %msg%n");
        rabbitmqEncoder.start();
        
        rabbitmqAppender.setRollingPolicy(rabbitmqPolicy);
        rabbitmqAppender.setEncoder(rabbitmqEncoder);
        rabbitmqAppender.start();
        
        // Configure RabbitMQ loggers
        Logger rabbitmqLogger = context.getLogger("org.springframework.amqp");
        rabbitmqLogger.addAppender(rabbitmqAppender);
        rabbitmqLogger.setAdditive(false);
        
        Logger connectionLogger = context.getLogger("com.rabbitmq.client");
        connectionLogger.addAppender(rabbitmqAppender);
        connectionLogger.setAdditive(false);
    }

    private void configureNotificationLogging(LoggerContext context) {
        // Create notification specific appender
        RollingFileAppender<ILoggingEvent> notificationAppender = new RollingFileAppender<>();
        notificationAppender.setContext(context);
        notificationAppender.setName("NOTIFICATION_FILE");
        notificationAppender.setFile("logs/notification.log");
        
        TimeBasedRollingPolicy<ILoggingEvent> notificationPolicy = new TimeBasedRollingPolicy<>();
        notificationPolicy.setContext(context);
        notificationPolicy.setParent(notificationAppender);
        notificationPolicy.setFileNamePattern("logs/notification.%d{yyyy-MM-dd}.%i.log.gz");
        notificationPolicy.setMaxHistory(30);
        notificationPolicy.start();
        
        PatternLayoutEncoder notificationEncoder = new PatternLayoutEncoder();
        notificationEncoder.setContext(context);
        notificationEncoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{36}] - %msg%n");
        notificationEncoder.start();
        
        notificationAppender.setRollingPolicy(notificationPolicy);
        notificationAppender.setEncoder(notificationEncoder);
        notificationAppender.start();
        
        // Configure notification service loggers
        Logger notificationLogger = context.getLogger("com.hopngo.notification");
        notificationLogger.addAppender(notificationAppender);
        notificationLogger.setAdditive(true);
    }

    @Bean
    public Counter rabbitMQConnectionErrors() {
        return Counter.builder("rabbitmq.connection.errors")
                .description("Number of RabbitMQ connection errors")
                .register(meterRegistry);
    }

    @Bean
    public Counter rabbitMQSerializationErrors() {
        return Counter.builder("rabbitmq.serialization.errors")
                .description("Number of RabbitMQ serialization errors")
                .register(meterRegistry);
    }

    @Bean
    public Counter notificationProcessingErrors() {
        return Counter.builder("notification.processing.errors")
                .description("Number of notification processing errors")
                .register(meterRegistry);
    }

    @Bean
    public Timer notificationProcessingTime() {
        return Timer.builder("notification.processing.time")
                .description("Time taken to process notifications")
                .register(meterRegistry);
    }

    @Bean
    public Counter emailDeliveryErrors() {
        return Counter.builder("email.delivery.errors")
                .description("Number of email delivery errors")
                .register(meterRegistry);
    }

    @Bean
    public Counter smsDeliveryErrors() {
        return Counter.builder("sms.delivery.errors")
                .description("Number of SMS delivery errors")
                .register(meterRegistry);
    }

    @Bean
    public Counter pushNotificationErrors() {
        return Counter.builder("push.notification.errors")
                .description("Number of push notification errors")
                .register(meterRegistry);
    }
}

@Component
@RequiredArgsConstructor
class NotificationMonitoringService implements HealthIndicator {

    private final AtomicLong totalNotifications = new AtomicLong(0);
    private final AtomicLong failedNotifications = new AtomicLong(0);
    private final AtomicLong rabbitMQErrors = new AtomicLong(0);
    private final AtomicLong serializationErrors = new AtomicLong(0);

    public void incrementTotalNotifications() {
        totalNotifications.incrementAndGet();
    }

    public void incrementFailedNotifications() {
        failedNotifications.incrementAndGet();
    }

    public void incrementRabbitMQErrors() {
        rabbitMQErrors.incrementAndGet();
    }

    public void incrementSerializationErrors() {
        serializationErrors.incrementAndGet();
    }

    @Override
    public Health health() {
        long total = totalNotifications.get();
        long failed = failedNotifications.get();
        long rabbitmqErrs = rabbitMQErrors.get();
        long serializationErrs = serializationErrors.get();
        
        double failureRate = total > 0 ? (double) failed / total * 100 : 0;
        
        Health.Builder builder = Health.up()
                .withDetail("total_notifications", total)
                .withDetail("failed_notifications", failed)
                .withDetail("failure_rate_percent", String.format("%.2f", failureRate))
                .withDetail("rabbitmq_errors", rabbitmqErrs)
                .withDetail("serialization_errors", serializationErrs);
        
        // Mark as down if failure rate is too high
        if (failureRate > 10.0 || rabbitmqErrs > 100 || serializationErrs > 50) {
            builder = Health.down()
                    .withDetail("reason", "High error rate detected")
                    .withDetail("total_notifications", total)
                    .withDetail("failed_notifications", failed)
                    .withDetail("failure_rate_percent", String.format("%.2f", failureRate))
                    .withDetail("rabbitmq_errors", rabbitmqErrs)
                    .withDetail("serialization_errors", serializationErrs);
        }
        
        return builder.build();
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logStatistics() {
        long total = totalNotifications.get();
        long failed = failedNotifications.get();
        long rabbitmqErrs = rabbitMQErrors.get();
        long serializationErrs = serializationErrors.get();
        
        double failureRate = total > 0 ? (double) failed / total * 100 : 0;
        
        org.slf4j.Logger logger = LoggerFactory.getLogger(NotificationMonitoringService.class);
        logger.info("Notification Statistics - Total: {}, Failed: {}, Failure Rate: {:.2f}%, RabbitMQ Errors: {}, Serialization Errors: {}",
                total, failed, failureRate, rabbitmqErrs, serializationErrs);
        
        // Alert if error rates are high
        if (failureRate > 5.0) {
            logger.warn("High notification failure rate detected: {:.2f}%", failureRate);
        }
        
        if (rabbitmqErrs > 10) {
            logger.warn("High RabbitMQ error count detected: {}", rabbitmqErrs);
        }
        
        if (serializationErrs > 5) {
            logger.warn("High serialization error count detected: {}", serializationErrs);
        }
    }

    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    public void resetDailyCounters() {
        org.slf4j.Logger logger = LoggerFactory.getLogger(NotificationMonitoringService.class);
        logger.info("Resetting daily counters - Final stats: Total: {}, Failed: {}, RabbitMQ Errors: {}, Serialization Errors: {}",
                totalNotifications.get(), failedNotifications.get(), rabbitMQErrors.get(), serializationErrors.get());
        
        totalNotifications.set(0);
        failedNotifications.set(0);
        rabbitMQErrors.set(0);
        serializationErrors.set(0);
    }
}
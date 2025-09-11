package com.hopngo.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String BOOKING_QUEUE = "booking.notifications";
    public static final String CHAT_QUEUE = "chat.notifications";
    public static final String PAYMENT_QUEUE = "payment.notifications";
    public static final String EMERGENCY_QUEUE = "emergency.notifications";
    
    // Dead Letter Queue names
    public static final String BOOKING_DLQ = "booking.notifications.dlq";
    public static final String CHAT_DLQ = "chat.notifications.dlq";
    public static final String PAYMENT_DLQ = "payment.notifications.dlq";
    public static final String EMERGENCY_DLQ = "emergency.notifications.dlq";
    
    // Exchange names
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String DLQ_EXCHANGE = "notification.dlq.exchange";
    
    // Routing keys
    public static final String BOOKING_ROUTING_KEY = "booking.created";
    public static final String CHAT_ROUTING_KEY = "chat.message";
    public static final String PAYMENT_ROUTING_KEY = "payment.processed";
    public static final String EMERGENCY_ROUTING_KEY = "emergency.alert";

    @Value("${spring.rabbitmq.listener.simple.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${spring.rabbitmq.listener.simple.retry.initial-interval:1000}")
    private long initialRetryInterval;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("Message not delivered: " + cause);
            }
        });
        template.setReturnsCallback(returned -> {
            System.err.println("Message returned: " + returned.getMessage() + 
                             " Reply Code: " + returned.getReplyCode() + 
                             " Reply Text: " + returned.getReplyText());
        });
        return template;
    }

    // Main Exchange
    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder
            .topicExchange(NOTIFICATION_EXCHANGE)
            .durable(true)
            .build();
    }

    // Dead Letter Exchange
    @Bean
    public TopicExchange dlqExchange() {
        return ExchangeBuilder
            .topicExchange(DLQ_EXCHANGE)
            .durable(true)
            .build();
    }

    // Booking Queue with DLQ
    @Bean
    public Queue bookingQueue() {
        return QueueBuilder
            .durable(BOOKING_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", BOOKING_ROUTING_KEY + ".dlq")
            .withArgument("x-message-ttl", 300000) // 5 minutes TTL
            .build();
    }

    @Bean
    public Queue bookingDlq() {
        return QueueBuilder
            .durable(BOOKING_DLQ)
            .build();
    }

    @Bean
    public Binding bookingBinding() {
        return BindingBuilder
            .bind(bookingQueue())
            .to(notificationExchange())
            .with(BOOKING_ROUTING_KEY);
    }

    @Bean
    public Binding bookingDlqBinding() {
        return BindingBuilder
            .bind(bookingDlq())
            .to(dlqExchange())
            .with(BOOKING_ROUTING_KEY + ".dlq");
    }

    // Chat Queue with DLQ
    @Bean
    public Queue chatQueue() {
        return QueueBuilder
            .durable(CHAT_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", CHAT_ROUTING_KEY + ".dlq")
            .withArgument("x-message-ttl", 300000) // 5 minutes TTL
            .build();
    }

    @Bean
    public Queue chatDlq() {
        return QueueBuilder
            .durable(CHAT_DLQ)
            .build();
    }

    @Bean
    public Binding chatBinding() {
        return BindingBuilder
            .bind(chatQueue())
            .to(notificationExchange())
            .with(CHAT_ROUTING_KEY);
    }

    @Bean
    public Binding chatDlqBinding() {
        return BindingBuilder
            .bind(chatDlq())
            .to(dlqExchange())
            .with(CHAT_ROUTING_KEY + ".dlq");
    }

    // Payment Queue with DLQ
    @Bean
    public Queue paymentQueue() {
        return QueueBuilder
            .durable(PAYMENT_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", PAYMENT_ROUTING_KEY + ".dlq")
            .withArgument("x-message-ttl", 300000) // 5 minutes TTL
            .build();
    }

    @Bean
    public Queue paymentDlq() {
        return QueueBuilder
            .durable(PAYMENT_DLQ)
            .build();
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder
            .bind(paymentQueue())
            .to(notificationExchange())
            .with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Binding paymentDlqBinding() {
        return BindingBuilder
            .bind(paymentDlq())
            .to(dlqExchange())
            .with(PAYMENT_ROUTING_KEY + ".dlq");
    }

    // Emergency Queue with DLQ (higher priority)
    @Bean
    public Queue emergencyQueue() {
        return QueueBuilder
            .durable(EMERGENCY_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", EMERGENCY_ROUTING_KEY + ".dlq")
            .withArgument("x-message-ttl", 60000) // 1 minute TTL for emergency
            .withArgument("x-max-priority", 10) // Priority queue
            .build();
    }

    @Bean
    public Queue emergencyDlq() {
        return QueueBuilder
            .durable(EMERGENCY_DLQ)
            .build();
    }

    @Bean
    public Binding emergencyBinding() {
        return BindingBuilder
            .bind(emergencyQueue())
            .to(notificationExchange())
            .with(EMERGENCY_ROUTING_KEY);
    }

    @Bean
    public Binding emergencyDlqBinding() {
        return BindingBuilder
            .bind(emergencyDlq())
            .to(dlqExchange())
            .with(EMERGENCY_ROUTING_KEY + ".dlq");
    }

    // Retry Template for message processing
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxRetryAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialRetryInterval);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }

    // Message Recoverer for failed messages
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLQ_EXCHANGE);
    }

    // Custom Rabbit Listener Container Factory with error handling
    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageRecoverer messageRecoverer) {
        
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        
        // Concurrency settings
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(10);
        
        // Prefetch settings
        factory.setPrefetchCount(5);
        
        // Acknowledgment mode
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        
        // Error handling
        factory.setDefaultRequeueRejected(false);
        factory.setMismatchedQueuesFatal(false);
        
        // Retry template
        factory.setRetryTemplate(retryTemplate());
        factory.setRecoveryCallback(context -> {
            System.err.println("Message processing failed after all retries: " + context.getLastThrowable().getMessage());
            return null;
        });
        
        return factory;
    }
}
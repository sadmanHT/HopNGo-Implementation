package com.hopngo.analytics.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for analytics service
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {
    
    // Exchange names
    public static final String DOMAIN_EVENTS_EXCHANGE = "hopngo.domain.events";
    
    // Queue names
    public static final String BOOKING_EVENTS_QUEUE = "analytics.booking.events";
    public static final String PAYMENT_EVENTS_QUEUE = "analytics.payment.events";
    public static final String USER_EVENTS_QUEUE = "analytics.user.events";
    public static final String CHAT_EVENTS_QUEUE = "analytics.chat.events";
    
    // Routing keys
    public static final String BOOKING_ROUTING_KEY = "booking.*";
    public static final String PAYMENT_ROUTING_KEY = "payment.*";
    public static final String USER_ROUTING_KEY = "user.*";
    public static final String CHAT_ROUTING_KEY = "chat.*";
    
    // Dead letter exchange and queue
    public static final String DLX_EXCHANGE = "hopngo.domain.events.dlx";
    public static final String DLQ_QUEUE = "analytics.events.dlq";
    
    /**
     * Configure message converter for JSON serialization
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * Configure RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
    
    /**
     * Configure listener container factory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
    
    // ========== EXCHANGES ==========
    
    /**
     * Main domain events exchange
     */
    @Bean
    public TopicExchange domainEventsExchange() {
        return ExchangeBuilder
                .topicExchange(DOMAIN_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }
    
    /**
     * Dead letter exchange
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }
    
    // ========== QUEUES ==========
    
    /**
     * Booking events queue
     */
    @Bean
    public Queue bookingEventsQueue() {
        return QueueBuilder
                .durable(BOOKING_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "booking.failed")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    
    /**
     * Payment events queue
     */
    @Bean
    public Queue paymentEventsQueue() {
        return QueueBuilder
                .durable(PAYMENT_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "payment.failed")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    
    /**
     * User events queue
     */
    @Bean
    public Queue userEventsQueue() {
        return QueueBuilder
                .durable(USER_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "user.failed")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    
    /**
     * Chat events queue
     */
    @Bean
    public Queue chatEventsQueue() {
        return QueueBuilder
                .durable(CHAT_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "chat.failed")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }
    
    /**
     * Dead letter queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(DLQ_QUEUE)
                .build();
    }
    
    // ========== BINDINGS ==========
    
    /**
     * Bind booking events queue to domain events exchange
     */
    @Bean
    public Binding bookingEventsBinding() {
        return BindingBuilder
                .bind(bookingEventsQueue())
                .to(domainEventsExchange())
                .with(BOOKING_ROUTING_KEY);
    }
    
    /**
     * Bind payment events queue to domain events exchange
     */
    @Bean
    public Binding paymentEventsBinding() {
        return BindingBuilder
                .bind(paymentEventsQueue())
                .to(domainEventsExchange())
                .with(PAYMENT_ROUTING_KEY);
    }
    
    /**
     * Bind user events queue to domain events exchange
     */
    @Bean
    public Binding userEventsBinding() {
        return BindingBuilder
                .bind(userEventsQueue())
                .to(domainEventsExchange())
                .with(USER_ROUTING_KEY);
    }
    
    /**
     * Bind chat events queue to domain events exchange
     */
    @Bean
    public Binding chatEventsBinding() {
        return BindingBuilder
                .bind(chatEventsQueue())
                .to(domainEventsExchange())
                .with(CHAT_ROUTING_KEY);
    }
    
    /**
     * Bind dead letter queue to dead letter exchange
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("*.failed");
    }
    
    // ========== ADMIN DECLARATIONS ==========
    
    /**
     * Declare all exchanges, queues, and bindings
     */
    @Bean
    public Declarables rabbitDeclarables() {
        return new Declarables(
            // Exchanges
            domainEventsExchange(),
            deadLetterExchange(),
            
            // Queues
            bookingEventsQueue(),
            paymentEventsQueue(),
            userEventsQueue(),
            chatEventsQueue(),
            deadLetterQueue(),
            
            // Bindings
            bookingEventsBinding(),
            paymentEventsBinding(),
            userEventsBinding(),
            chatEventsBinding(),
            deadLetterBinding()
        );
    }
}
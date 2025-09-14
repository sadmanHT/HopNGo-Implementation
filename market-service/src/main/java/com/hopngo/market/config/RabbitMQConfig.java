package com.hopngo.market.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // Exchange names
    public static final String PAYMENT_EVENTS_EXCHANGE = "payment.events";
    public static final String ORDER_EVENTS_EXCHANGE = "order.events";
    
    // Queue names
    public static final String PAYMENT_SUCCEEDED_QUEUE = "payment.succeeded.queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.queue";
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_STATUS_CHANGED_QUEUE = "order.status.changed.queue";
    
    // Routing keys
    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_STATUS_CHANGED_ROUTING_KEY = "order.status_changed";
    
    // Payment Events Exchange
    @Bean
    public TopicExchange paymentEventsExchange() {
        return ExchangeBuilder
                .topicExchange(PAYMENT_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }
    
    // Order Events Exchange
    @Bean
    public TopicExchange orderEventsExchange() {
        return ExchangeBuilder
                .topicExchange(ORDER_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }
    
    // Payment Succeeded Queue
    @Bean
    public Queue paymentSucceededQueue() {
        return QueueBuilder
                .durable(PAYMENT_SUCCEEDED_QUEUE)
                .withArgument("x-dead-letter-exchange", PAYMENT_EVENTS_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.payment.succeeded")
                .build();
    }
    
    // Payment Failed Queue
    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder
                .durable(PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", PAYMENT_EVENTS_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.payment.failed")
                .build();
    }
    
    // Order Created Queue
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder
                .durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_EVENTS_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.order.created")
                .build();
    }
    
    // Order Status Changed Queue
    @Bean
    public Queue orderStatusChangedQueue() {
        return QueueBuilder
                .durable(ORDER_STATUS_CHANGED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_EVENTS_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.order.status.changed")
                .build();
    }
    
    // Dead Letter Exchange for Payment Events
    @Bean
    public TopicExchange paymentEventsDeadLetterExchange() {
        return ExchangeBuilder
                .topicExchange(PAYMENT_EVENTS_EXCHANGE + ".dlx")
                .durable(true)
                .build();
    }
    
    // Dead Letter Exchange for Order Events
    @Bean
    public TopicExchange orderEventsDeadLetterExchange() {
        return ExchangeBuilder
                .topicExchange(ORDER_EVENTS_EXCHANGE + ".dlx")
                .durable(true)
                .build();
    }
    
    // Dead Letter Queues
    @Bean
    public Queue paymentSucceededDeadLetterQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCEEDED_QUEUE + ".dlq").build();
    }
    
    @Bean
    public Queue paymentFailedDeadLetterQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE + ".dlq").build();
    }
    
    @Bean
    public Queue orderCreatedDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE + ".dlq").build();
    }
    
    @Bean
    public Queue orderStatusChangedDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_STATUS_CHANGED_QUEUE + ".dlq").build();
    }
    
    // Bindings for Payment Events
    @Bean
    public Binding paymentSucceededBinding() {
        return BindingBuilder
                .bind(paymentSucceededQueue())
                .to(paymentEventsExchange())
                .with(PAYMENT_SUCCEEDED_ROUTING_KEY);
    }
    
    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder
                .bind(paymentFailedQueue())
                .to(paymentEventsExchange())
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }
    
    // Bindings for Order Events
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
                .bind(orderCreatedQueue())
                .to(orderEventsExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }
    
    @Bean
    public Binding orderStatusChangedBinding() {
        return BindingBuilder
                .bind(orderStatusChangedQueue())
                .to(orderEventsExchange())
                .with(ORDER_STATUS_CHANGED_ROUTING_KEY);
    }
    
    // Dead Letter Bindings
    @Bean
    public Binding paymentSucceededDeadLetterBinding() {
        return BindingBuilder
                .bind(paymentSucceededDeadLetterQueue())
                .to(paymentEventsDeadLetterExchange())
                .with("dlq.payment.succeeded");
    }
    
    @Bean
    public Binding paymentFailedDeadLetterBinding() {
        return BindingBuilder
                .bind(paymentFailedDeadLetterQueue())
                .to(paymentEventsDeadLetterExchange())
                .with("dlq.payment.failed");
    }
    
    @Bean
    public Binding orderCreatedDeadLetterBinding() {
        return BindingBuilder
                .bind(orderCreatedDeadLetterQueue())
                .to(orderEventsDeadLetterExchange())
                .with("dlq.order.created");
    }
    
    @Bean
    public Binding orderStatusChangedDeadLetterBinding() {
        return BindingBuilder
                .bind(orderStatusChangedDeadLetterQueue())
                .to(orderEventsDeadLetterExchange())
                .with("dlq.order.status.changed");
    }
    
    // RabbitTemplate with JSON converter
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
    
    // JSON Message Converter
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
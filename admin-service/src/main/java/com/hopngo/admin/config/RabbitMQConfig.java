package com.hopngo.admin.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Value("${app.rabbitmq.exchanges.content:content.exchange}")
    private String contentExchange;
    
    @Value("${app.rabbitmq.queues.content-flagged:content.flagged}")
    private String contentFlaggedQueue;
    
    @Value("${app.rabbitmq.routing-keys.content-flagged:content.flagged}")
    private String contentFlaggedRoutingKey;
    
    // Exchange for content-related events
    @Bean
    public TopicExchange contentExchange() {
        return new TopicExchange(contentExchange, true, false);
    }
    
    // Queue for content flagged events
    @Bean
    public Queue contentFlaggedQueue() {
        return QueueBuilder.durable(contentFlaggedQueue)
                .withArgument("x-dead-letter-exchange", contentExchange + ".dlx")
                .withArgument("x-dead-letter-routing-key", contentFlaggedRoutingKey + ".dlq")
                .build();
    }
    
    // Binding for content flagged events
    @Bean
    public Binding contentFlaggedBinding() {
        return BindingBuilder
                .bind(contentFlaggedQueue())
                .to(contentExchange())
                .with(contentFlaggedRoutingKey);
    }
    
    // Dead Letter Exchange for failed messages
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(contentExchange + ".dlx", true, false);
    }
    
    // Dead Letter Queue for failed content flagged events
    @Bean
    public Queue contentFlaggedDeadLetterQueue() {
        return QueueBuilder.durable(contentFlaggedQueue + ".dlq").build();
    }
    
    // Binding for dead letter queue
    @Bean
    public Binding contentFlaggedDeadLetterBinding() {
        return BindingBuilder
                .bind(contentFlaggedDeadLetterQueue())
                .to(deadLetterExchange())
                .with(contentFlaggedRoutingKey + ".dlq");
    }
    
    // JSON message converter
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    // RabbitTemplate with JSON converter
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
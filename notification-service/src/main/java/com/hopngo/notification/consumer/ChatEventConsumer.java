package com.hopngo.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.notification.dto.ChatEvent;
import com.hopngo.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class ChatEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatEventConsumer.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Bean
    public Consumer<Message<String>> chatEventsConsumer() {
        return message -> {
            try {
                String payload = message.getPayload();
                String routingKey = (String) message.getHeaders().get("amqp_receivedRoutingKey");
                
                logger.info("Received chat event with routing key: {}, payload: {}", routingKey, payload);
                
                ChatEvent event = objectMapper.readValue(payload, ChatEvent.class);
                
                // Process different types of chat events
                switch (routingKey) {
                    case "chat.message":
                        handleChatMessage(event);
                        break;
                    case "chat.mention":
                        handleChatMention(event);
                        break;
                    case "chat.conversation.created":
                        handleConversationCreated(event);
                        break;
                    case "chat.conversation.joined":
                        handleConversationJoined(event);
                        break;
                    case "chat.urgent":
                        handleUrgentMessage(event);
                        break;
                    default:
                        logger.warn("Unknown chat event type: {}", routingKey);
                }
                
            } catch (Exception e) {
                logger.error("Error processing chat event: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process chat event", e);
            }
        };
    }
    
    private void handleChatMessage(ChatEvent event) {
        logger.info("Processing chat message event for message: {}", event.getMessageId());
        
        try {
            notificationService.sendChatMessageNotification(event);
            logger.info("Successfully processed chat message notification for message: {}", event.getMessageId());
        } catch (Exception e) {
            logger.error("Failed to send chat message notification for message: {}", event.getMessageId(), e);
            throw e;
        }
    }
    
    private void handleChatMention(ChatEvent event) {
        logger.info("Processing chat mention event for message: {}", event.getMessageId());
        
        try {
            notificationService.sendChatMentionNotification(event);
            logger.info("Successfully processed chat mention notification for message: {}", event.getMessageId());
        } catch (Exception e) {
            logger.error("Failed to send chat mention notification for message: {}", event.getMessageId(), e);
            throw e;
        }
    }
    
    private void handleConversationCreated(ChatEvent event) {
        logger.info("Processing conversation created event for conversation: {}", event.getConversationId());
        
        try {
            notificationService.sendConversationCreatedNotification(event);
            logger.info("Successfully processed conversation created notification for conversation: {}", event.getConversationId());
        } catch (Exception e) {
            logger.error("Failed to send conversation created notification for conversation: {}", event.getConversationId(), e);
            throw e;
        }
    }
    
    private void handleConversationJoined(ChatEvent event) {
        logger.info("Processing conversation joined event for conversation: {}", event.getConversationId());
        
        try {
            notificationService.sendConversationJoinedNotification(event);
            logger.info("Successfully processed conversation joined notification for conversation: {}", event.getConversationId());
        } catch (Exception e) {
            logger.error("Failed to send conversation joined notification for conversation: {}", event.getConversationId(), e);
            throw e;
        }
    }
    
    private void handleUrgentMessage(ChatEvent event) {
        logger.info("Processing urgent message event for message: {}", event.getMessageId());
        
        try {
            notificationService.sendUrgentChatNotification(event);
            logger.info("Successfully processed urgent chat notification for message: {}", event.getMessageId());
        } catch (Exception e) {
            logger.error("Failed to send urgent chat notification for message: {}", event.getMessageId(), e);
            throw e;
        }
    }
}
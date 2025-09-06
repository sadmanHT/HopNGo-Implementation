package com.hopngo.chatservice.controller;

import com.hopngo.chatservice.dto.CreateConversationRequest;
import com.hopngo.chatservice.dto.SendMessageRequest;
import com.hopngo.chatservice.model.Conversation;
import com.hopngo.chatservice.model.Message;
import com.hopngo.chatservice.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketController.class);
    
    @Autowired
    private ChatService chatService;
    
    // Note: WebSocket endpoints will be implemented once Spring messaging dependencies are properly resolved
    // For now, this is a placeholder to allow compilation
    
    public Conversation createConversation(CreateConversationRequest request, String userId) {
        try {
            logger.info("Creating conversation for user: {}", userId);
            Conversation conversation = chatService.createConversation(request, userId);
            logger.info("Successfully created conversation: {}", conversation.getId());
            return conversation;
        } catch (Exception e) {
            logger.error("Error creating conversation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create conversation: " + e.getMessage());
        }
    }
    
    public void sendMessage(SendMessageRequest request, String userId) {
        try {
            logger.info("Sending message from user: {} to conversation: {}", userId, request.getConvoId());
            Message message = chatService.sendMessage(request, userId);
            logger.info("Successfully sent message: {}", message.getId());
        } catch (Exception e) {
            logger.error("Error sending message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }
    
    public void markMessageAsRead(String messageId, String userId) {
        try {
            logger.info("Marking message {} as read by user: {}", messageId, userId);
            chatService.markMessageAsRead(messageId, userId);
        } catch (Exception e) {
            logger.error("Error marking message as read: {}", e.getMessage(), e);
        }
    }
}
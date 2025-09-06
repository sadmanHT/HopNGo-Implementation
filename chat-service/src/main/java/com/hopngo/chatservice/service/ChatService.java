package com.hopngo.chatservice.service;

import com.hopngo.chatservice.dto.ChatMessageCreatedEvent;
import com.hopngo.chatservice.dto.CreateConversationRequest;
import com.hopngo.chatservice.dto.SendMessageRequest;
import com.hopngo.chatservice.model.Conversation;
import com.hopngo.chatservice.model.ConversationType;
import com.hopngo.chatservice.model.Message;
// import com.hopngo.chatservice.repository.ConversationRepository;
// import com.hopngo.chatservice.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    // @Autowired
    // private ConversationRepository conversationRepository;

    // @Autowired
    // private MessageRepository messageRepository;
    

    
    @Autowired
    private StreamBridge streamBridge;
    
    public Conversation createConversation(CreateConversationRequest request, String creatorId) {
        logger.info("Creating conversation of type {} with members: {}", request.getType(), request.getMemberIds());
        
        // Validate that creator is included in member list
        if (!request.getMemberIds().contains(creatorId)) {
            request.getMemberIds().add(creatorId);
        }
        
        // MongoDB repositories disabled - returning mock conversation
        Conversation conversation = new Conversation(request.getType(), request.getMemberIds(), request.getName());
        conversation.setId("mock-conversation-id-" + System.currentTimeMillis());
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setLastMessageAt(LocalDateTime.now());
        
        logger.info("Mock conversation created with ID: {}", conversation.getId());
        return conversation;
    }
    
    public Message sendMessage(SendMessageRequest request, String senderId) {
        logger.info("Sending message to conversation: {} from user: {}", request.getConvoId(), senderId);
        
        // MongoDB repositories disabled - returning mock message
        Message message = new Message(request.getConvoId(), senderId, request.getBody(), request.getMediaUrl());
        message.setId("mock-message-id-" + System.currentTimeMillis());
        message.setSentAt(LocalDateTime.now());
        
        logger.info("Mock message sent with ID: {}", message.getId());
        
        // Publish event to RabbitMQ
        ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(
                message.getId(),
                message.getConvoId(),
                message.getSenderId(),
                message.getPreview(),
                message.getSentAt()
        );
        
        streamBridge.send("chatMessageCreated-out-0", event);
        logger.info("Published chat.message.created event for message: {}", message.getId());
        
        return message;
    }
    
    public List<Conversation> getUserConversations(String userId) {
        // MongoDB repositories disabled - returning empty list
        logger.info("Mock getUserConversations called for user: {}", userId);
        return List.of();
    }
    
    public Page<Conversation> getUserConversations(String userId, Pageable pageable) {
        // MongoDB repositories disabled - returning empty page
        logger.info("Mock getUserConversations with pagination called for user: {}", userId);
        return Page.empty(pageable);
    }
    
    public Page<Message> getConversationMessages(String convoId, String userId, Pageable pageable) {
        // MongoDB repositories disabled - returning empty page
        logger.info("Mock getConversationMessages called for conversation: {} and user: {}", convoId, userId);
        return Page.empty(pageable);
    }
    
    public void markMessageAsRead(String messageId, String userId) {
        // MongoDB repositories disabled - mock implementation
        logger.info("Mock markMessageAsRead called for message: {} by user: {}", messageId, userId);
    }
}
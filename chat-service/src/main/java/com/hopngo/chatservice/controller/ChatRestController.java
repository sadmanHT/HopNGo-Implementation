package com.hopngo.chatservice.controller;

import com.hopngo.chatservice.model.Conversation;
import com.hopngo.chatservice.model.Message;
import com.hopngo.chatservice.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat", description = "Chat REST API")
public class ChatRestController {
    
    @Autowired
    private ChatService chatService;
    
    @GetMapping("/conversations")
    @Operation(summary = "Get user conversations", description = "Retrieve all conversations for the authenticated user")
    public ResponseEntity<List<Conversation>> getUserConversations(
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-Id") String userId) {
        
        List<Conversation> conversations = chatService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/conversations/paginated")
    @Operation(summary = "Get user conversations with pagination", description = "Retrieve conversations for the authenticated user with pagination")
    public ResponseEntity<Page<Conversation>> getUserConversationsPaginated(
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> conversations = chatService.getUserConversations(userId, pageable);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/messages")
    @Operation(summary = "Get conversation messages", description = "Retrieve messages for a specific conversation with pagination")
    public ResponseEntity<Page<Message>> getConversationMessages(
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Conversation ID", required = true)
            @RequestParam String convoId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = chatService.getConversationMessages(convoId, userId, pageable);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "Get conversation details", description = "Retrieve details of a specific conversation")
    public ResponseEntity<Conversation> getConversation(
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Conversation ID", required = true)
            @PathVariable String conversationId) {
        
        // This would require a method in ChatService to get a single conversation
        // For now, we'll return a simple response
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/messages/{messageId}/read")
    @Operation(summary = "Mark message as read", description = "Mark a specific message as read by the user")
    public ResponseEntity<Void> markMessageAsRead(
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Message ID", required = true)
            @PathVariable String messageId) {
        
        chatService.markMessageAsRead(messageId, userId);
        return ResponseEntity.ok().build();
    }
}
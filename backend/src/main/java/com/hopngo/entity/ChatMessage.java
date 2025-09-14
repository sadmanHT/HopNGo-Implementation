package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "content", length = 2000)
    private String content;

    @NotNull
    @Column(name = "sender_type")
    @Enumerated(EnumType.STRING)
    private SenderType senderType;

    @Column(name = "message_type")
    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "is_demo")
    private Boolean isDemo = false;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "attachment_type")
    private String attachmentType;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private ChatConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id")
    private User senderUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_provider_id")
    private Provider senderProvider;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public ChatMessage() {}

    public ChatMessage(String content, SenderType senderType, ChatConversation conversation) {
        this.content = content;
        this.senderType = senderType;
        this.conversation = conversation;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public SenderType getSenderType() {
        return senderType;
    }

    public void setSenderType(SenderType senderType) {
        this.senderType = senderType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Boolean getIsDemo() {
        return isDemo;
    }

    public void setIsDemo(Boolean isDemo) {
        this.isDemo = isDemo;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public ChatConversation getConversation() {
        return conversation;
    }

    public void setConversation(ChatConversation conversation) {
        this.conversation = conversation;
    }

    public User getSenderUser() {
        return senderUser;
    }

    public void setSenderUser(User senderUser) {
        this.senderUser = senderUser;
    }

    public Provider getSenderProvider() {
        return senderProvider;
    }

    public void setSenderProvider(Provider senderProvider) {
        this.senderProvider = senderProvider;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id +
                ", senderType=" + senderType +
                ", messageType=" + messageType +
                ", isRead=" + isRead +
                ", isDemo=" + isDemo +
                ", createdAt=" + createdAt +
                '}';
    }

    public enum SenderType {
        USER, PROVIDER, SYSTEM, AI_ASSISTANT
    }

    public enum MessageType {
        TEXT, IMAGE, FILE, LOCATION, BOOKING_REQUEST, BOOKING_CONFIRMATION, EMERGENCY_ALERT
    }
}
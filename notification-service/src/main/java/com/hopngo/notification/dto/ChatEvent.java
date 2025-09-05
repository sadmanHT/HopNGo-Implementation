package com.hopngo.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ChatEvent {
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("eventType")
    private String eventType;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("messageId")
    private String messageId;
    
    @JsonProperty("conversationId")
    private String conversationId;
    
    @JsonProperty("senderId")
    private String senderId;
    
    @JsonProperty("senderName")
    private String senderName;
    
    @JsonProperty("senderEmail")
    private String senderEmail;
    
    @JsonProperty("recipientIds")
    private List<String> recipientIds;
    
    @JsonProperty("recipientEmails")
    private List<String> recipientEmails;
    
    @JsonProperty("recipientNames")
    private List<String> recipientNames;
    
    @JsonProperty("messageContent")
    private String messageContent;
    
    @JsonProperty("messageType")
    private String messageType;
    
    @JsonProperty("conversationType")
    private String conversationType;
    
    @JsonProperty("conversationTitle")
    private String conversationTitle;
    
    @JsonProperty("mentionedUserIds")
    private List<String> mentionedUserIds;
    
    @JsonProperty("mentionedUserEmails")
    private List<String> mentionedUserEmails;
    
    @JsonProperty("attachments")
    private List<String> attachments;
    
    @JsonProperty("isUrgent")
    private Boolean isUrgent;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructors
    public ChatEvent() {}
    
    // Getters and Setters
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getSenderEmail() {
        return senderEmail;
    }
    
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
    
    public List<String> getRecipientIds() {
        return recipientIds;
    }
    
    public void setRecipientIds(List<String> recipientIds) {
        this.recipientIds = recipientIds;
    }
    
    public List<String> getRecipientEmails() {
        return recipientEmails;
    }
    
    public void setRecipientEmails(List<String> recipientEmails) {
        this.recipientEmails = recipientEmails;
    }
    
    public List<String> getRecipientNames() {
        return recipientNames;
    }
    
    public void setRecipientNames(List<String> recipientNames) {
        this.recipientNames = recipientNames;
    }
    
    public String getMessageContent() {
        return messageContent;
    }
    
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public String getConversationType() {
        return conversationType;
    }
    
    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }
    
    public String getConversationTitle() {
        return conversationTitle;
    }
    
    public void setConversationTitle(String conversationTitle) {
        this.conversationTitle = conversationTitle;
    }
    
    public List<String> getMentionedUserIds() {
        return mentionedUserIds;
    }
    
    public void setMentionedUserIds(List<String> mentionedUserIds) {
        this.mentionedUserIds = mentionedUserIds;
    }
    
    public List<String> getMentionedUserEmails() {
        return mentionedUserEmails;
    }
    
    public void setMentionedUserEmails(List<String> mentionedUserEmails) {
        this.mentionedUserEmails = mentionedUserEmails;
    }
    
    public List<String> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }
    
    public Boolean getIsUrgent() {
        return isUrgent;
    }
    
    public void setIsUrgent(Boolean isUrgent) {
        this.isUrgent = isUrgent;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
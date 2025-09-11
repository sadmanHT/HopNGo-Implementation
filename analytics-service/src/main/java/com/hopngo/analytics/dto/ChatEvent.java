package com.hopngo.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain event for chat-related activities
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatEvent extends DomainEvent {
    
    @JsonProperty("chatId")
    private String chatId;
    
    @JsonProperty("messageId")
    private String messageId;
    
    @JsonProperty("senderId")
    private String senderId;
    
    @JsonProperty("receiverId")
    private String receiverId;
    
    @JsonProperty("messageType")
    private String messageType;
    
    @JsonProperty("messageLength")
    private Integer messageLength;
    
    @JsonProperty("hasAttachment")
    private Boolean hasAttachment;
    
    @JsonProperty("attachmentType")
    private String attachmentType;
    
    @JsonProperty("chatType")
    private String chatType;
    
    @JsonProperty("action")
    private String action;
    
    @JsonProperty("responseTime")
    private Long responseTime;
    
    @JsonProperty("isRead")
    private Boolean isRead;
    
    @JsonProperty("bookingId")
    private String bookingId;
    
    // Default constructor
    public ChatEvent() {
        super();
    }
    
    @Override
    public EventRequest toAnalyticsEvent() {
        EventRequest eventRequest = new EventRequest();
        eventRequest.setEventId(getEventId());
        eventRequest.setEventType("chat_" + determineChatAction());
        eventRequest.setEventCategory("chat");
        eventRequest.setUserId(getUserId());
        eventRequest.setSessionId(getSessionId());
        eventRequest.setTimestamp(getTimestamp().toString());
        
        // Build event data
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("chatId", chatId);
        eventData.put("messageId", messageId);
        eventData.put("senderId", senderId);
        eventData.put("receiverId", receiverId);
        eventData.put("messageType", messageType);
        eventData.put("messageLength", messageLength);
        eventData.put("hasAttachment", hasAttachment);
        eventData.put("attachmentType", attachmentType);
        eventData.put("chatType", chatType);
        eventData.put("action", action);
        eventData.put("responseTime", responseTime);
        eventData.put("isRead", isRead);
        eventData.put("bookingId", bookingId);
        eventData.put("aggregateId", getAggregateId());
        eventData.put("aggregateType", getAggregateType());
        
        eventRequest.setEventData(eventData);
        eventRequest.setMetadata(getMetadata());
        
        return eventRequest;
    }
    
    private String determineChatAction() {
        if (action == null) {
            return "unknown";
        }
        
        return switch (action.toLowerCase()) {
            case "message_sent", "sent" -> "message_sent";
            case "message_received", "received" -> "message_received";
            case "message_read", "read" -> "message_read";
            case "message_deleted", "deleted" -> "message_deleted";
            case "chat_started", "started" -> "chat_started";
            case "chat_ended", "ended" -> "chat_ended";
            case "typing_started" -> "typing_started";
            case "typing_stopped" -> "typing_stopped";
            case "attachment_sent" -> "attachment_sent";
            case "attachment_downloaded" -> "attachment_downloaded";
            case "chat_archived" -> "chat_archived";
            case "chat_unarchived" -> "chat_unarchived";
            case "participant_added" -> "participant_added";
            case "participant_removed" -> "participant_removed";
            default -> action.toLowerCase().replace(" ", "_");
        };
    }
    
    // Getters and Setters
    public String getChatId() {
        return chatId;
    }
    
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public Integer getMessageLength() {
        return messageLength;
    }
    
    public void setMessageLength(Integer messageLength) {
        this.messageLength = messageLength;
    }
    
    public Boolean getHasAttachment() {
        return hasAttachment;
    }
    
    public void setHasAttachment(Boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
    }
    
    public String getAttachmentType() {
        return attachmentType;
    }
    
    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }
    
    public String getChatType() {
        return chatType;
    }
    
    public void setChatType(String chatType) {
        this.chatType = chatType;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public Long getResponseTime() {
        return responseTime;
    }
    
    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
    
    public String getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
    
    @Override
    public String toString() {
        return "ChatEvent{" +
                "chatId='" + chatId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", messageLength=" + messageLength +
                ", hasAttachment=" + hasAttachment +
                ", attachmentType='" + attachmentType + '\'' +
                ", chatType='" + chatType + '\'' +
                ", action='" + action + '\'' +
                ", responseTime=" + responseTime +
                ", isRead=" + isRead +
                ", bookingId='" + bookingId + '\'' +
                "} " + super.toString();
    }
}
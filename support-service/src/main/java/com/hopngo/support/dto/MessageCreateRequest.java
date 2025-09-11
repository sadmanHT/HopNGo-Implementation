package com.hopngo.support.dto;

import com.hopngo.support.enums.MessageSender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MessageCreateRequest {

    @NotNull(message = "Sender is required")
    private MessageSender sender;

    private String senderId;
    private String senderName;

    @NotBlank(message = "Message body is required")
    @Size(max = 10000, message = "Message body must not exceed 10000 characters")
    private String body;

    // Constructors
    public MessageCreateRequest() {}

    public MessageCreateRequest(MessageSender sender, String body) {
        this.sender = sender;
        this.body = body;
    }

    public MessageCreateRequest(MessageSender sender, String senderId, String senderName, String body) {
        this.sender = sender;
        this.senderId = senderId;
        this.senderName = senderName;
        this.body = body;
    }

    // Getters and Setters
    public MessageSender getSender() {
        return sender;
    }

    public void setSender(MessageSender sender) {
        this.sender = sender;
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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "MessageCreateRequest{" +
                "sender=" + sender +
                ", senderId='" + senderId + '\'' +
                ", senderName='" + senderName + '\'' +
                '}';
    }
}
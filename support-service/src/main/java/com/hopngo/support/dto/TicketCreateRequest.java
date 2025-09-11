package com.hopngo.support.dto;

import com.hopngo.support.enums.TicketPriority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TicketCreateRequest {

    private String userId; // Optional for authenticated users

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Subject is required")
    @Size(max = 500, message = "Subject must not exceed 500 characters")
    private String subject;

    @NotBlank(message = "Body is required")
    @Size(max = 10000, message = "Body must not exceed 10000 characters")
    private String body;

    private TicketPriority priority = TicketPriority.MEDIUM;

    // Constructors
    public TicketCreateRequest() {}

    public TicketCreateRequest(String email, String subject, String body) {
        this.email = email;
        this.subject = subject;
        this.body = body;
    }

    public TicketCreateRequest(String userId, String email, String subject, String body, TicketPriority priority) {
        this.userId = userId;
        this.email = email;
        this.subject = subject;
        this.body = body;
        this.priority = priority;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority != null ? priority : TicketPriority.MEDIUM;
    }

    @Override
    public String toString() {
        return "TicketCreateRequest{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", subject='" + subject + '\'' +
                ", priority=" + priority +
                '}';
    }
}
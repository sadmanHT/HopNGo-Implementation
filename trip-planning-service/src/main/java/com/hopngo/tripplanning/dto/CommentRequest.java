package com.hopngo.tripplanning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating comments on itineraries
 */
public class CommentRequest {

    @NotBlank(message = "Comment message is required")
    @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
    private String message;

    // Constructors
    public CommentRequest() {}

    public CommentRequest(String message) {
        this.message = message;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "CommentRequest{" +
                "message='" + message + '\'' +
                '}';
    }
}
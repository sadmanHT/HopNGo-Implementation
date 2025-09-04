package com.hopngo.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCommentRequest {
    
    @NotBlank(message = "Post ID cannot be empty")
    private String postId;
    
    @NotBlank(message = "Comment text cannot be empty")
    @Size(max = 1000, message = "Comment text cannot exceed 1000 characters")
    private String text;
    
    // Constructors
    public CreateCommentRequest() {}
    
    public CreateCommentRequest(String postId, String text) {
        this.postId = postId;
        this.text = text;
    }
    
    // Getters and Setters
    public String getPostId() {
        return postId;
    }
    
    public void setPostId(String postId) {
        this.postId = postId;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
}
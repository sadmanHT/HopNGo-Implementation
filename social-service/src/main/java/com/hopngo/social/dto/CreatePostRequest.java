package com.hopngo.social.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreatePostRequest {
    
    @NotBlank(message = "Post text cannot be empty")
    @Size(max = 2000, message = "Post text cannot exceed 2000 characters")
    private String text;
    
    @Size(max = 10, message = "Cannot have more than 10 media URLs")
    private List<String> mediaUrls;
    
    @Size(max = 20, message = "Cannot have more than 20 tags")
    private List<String> tags;
    
    @Valid
    private LocationDto location;
    
    // Constructors
    public CreatePostRequest() {}
    
    public CreatePostRequest(String text, List<String> mediaUrls, List<String> tags, LocationDto location) {
        this.text = text;
        this.mediaUrls = mediaUrls;
        this.tags = tags;
        this.location = location;
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public List<String> getMediaUrls() {
        return mediaUrls;
    }
    
    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public LocationDto getLocation() {
        return location;
    }
    
    public void setLocation(LocationDto location) {
        this.location = location;
    }
}
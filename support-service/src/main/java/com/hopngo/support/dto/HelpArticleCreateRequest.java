package com.hopngo.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class HelpArticleCreateRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @NotBlank(message = "Body content is required")
    @Size(max = 50000, message = "Body content must not exceed 50000 characters")
    private String bodyMd;
    
    private List<String> tags;
    
    private boolean published = false;
    
    // Constructors
    public HelpArticleCreateRequest() {}
    
    public HelpArticleCreateRequest(String title, String bodyMd, List<String> tags, boolean published) {
        this.title = title;
        this.bodyMd = bodyMd;
        this.tags = tags;
        this.published = published;
    }
    
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getBodyMd() {
        return bodyMd;
    }
    
    public void setBodyMd(String bodyMd) {
        this.bodyMd = bodyMd;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public boolean isPublished() {
        return published;
    }
    
    public void setPublished(boolean published) {
        this.published = published;
    }
    
    // Helper methods
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
    
    public int getTagCount() {
        return tags != null ? tags.size() : 0;
    }
    
    public String getTagsAsString() {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(", ", tags);
    }
    
    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }
    
    @Override
    public String toString() {
        return "HelpArticleCreateRequest{" +
                "title='" + title + '\'' +
                ", bodyLength=" + (bodyMd != null ? bodyMd.length() : 0) +
                ", tags=" + tags +
                ", published=" + published +
                '}';
    }
}
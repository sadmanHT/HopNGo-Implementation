package com.hopngo.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CannedReplyCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Body is required")
    @Size(max = 2000, message = "Body must not exceed 2000 characters")
    private String body;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    private String createdBy;

    // Constructors
    public CannedReplyCreateRequest() {}

    public CannedReplyCreateRequest(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public CannedReplyCreateRequest(String title, String body, String category) {
        this.title = title;
        this.body = body;
        this.category = category;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // Helper methods
    public boolean hasCategory() {
        return category != null && !category.trim().isEmpty();
    }

    public String getCategoryOrDefault() {
        return hasCategory() ? category.trim() : "General";
    }

    @Override
    public String toString() {
        return "CannedReplyCreateRequest{" +
                "title='" + title + '\'' +
                ", body='" + (body != null ? body.substring(0, Math.min(body.length(), 50)) + "..." : "null") + '\'' +
                ", category='" + category + '\'' +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }
}
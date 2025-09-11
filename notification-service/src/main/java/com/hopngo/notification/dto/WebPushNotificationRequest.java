package com.hopngo.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class WebPushNotificationRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Body is required")
    private String body;
    
    private String icon;
    private String badge;
    private String image;
    private String url;
    private String tag;
    private Boolean requireInteraction;
    private Boolean silent;
    private Map<String, Object> data;
    
    // Constructors
    public WebPushNotificationRequest() {}
    
    public WebPushNotificationRequest(String userId, String title, String body) {
        this.userId = userId;
        this.title = title;
        this.body = body;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
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
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getBadge() {
        return badge;
    }
    
    public void setBadge(String badge) {
        this.badge = badge;
    }
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public Boolean getRequireInteraction() {
        return requireInteraction;
    }
    
    public void setRequireInteraction(Boolean requireInteraction) {
        this.requireInteraction = requireInteraction;
    }
    
    public Boolean getSilent() {
        return silent;
    }
    
    public void setSilent(Boolean silent) {
        this.silent = silent;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
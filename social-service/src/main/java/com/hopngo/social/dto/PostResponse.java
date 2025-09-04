package com.hopngo.social.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PostResponse {
    
    private String id;
    private String userId;
    private String text;
    private List<String> mediaUrls;
    private List<String> tags;
    private LocationDto location;
    private int likeCount;
    private int commentCount;
    private boolean isLikedByCurrentUser;
    private boolean isBookmarkedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public PostResponse() {}
    
    public PostResponse(String id, String userId, String text, List<String> mediaUrls, 
                       List<String> tags, LocationDto location, int likeCount, int commentCount,
                       boolean isLikedByCurrentUser, boolean isBookmarkedByCurrentUser,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.text = text;
        this.mediaUrls = mediaUrls;
        this.tags = tags;
        this.location = location;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.isLikedByCurrentUser = isLikedByCurrentUser;
        this.isBookmarkedByCurrentUser = isBookmarkedByCurrentUser;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
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
    
    public int getLikeCount() {
        return likeCount;
    }
    
    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }
    
    public int getCommentCount() {
        return commentCount;
    }
    
    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }
    
    public boolean isLikedByCurrentUser() {
        return isLikedByCurrentUser;
    }
    
    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        isLikedByCurrentUser = likedByCurrentUser;
    }
    
    public boolean isBookmarkedByCurrentUser() {
        return isBookmarkedByCurrentUser;
    }
    
    public void setBookmarkedByCurrentUser(boolean bookmarkedByCurrentUser) {
        isBookmarkedByCurrentUser = bookmarkedByCurrentUser;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
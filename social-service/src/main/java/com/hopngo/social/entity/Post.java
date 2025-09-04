package com.hopngo.social.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Document(collection = "posts")
public class Post {
    
    @Id
    private String id;
    
    private String userId;
    
    private String text;
    
    private List<String> mediaUrls;
    
    private List<String> tags;
    
    private Location location;
    
    private Set<String> likedBy = new HashSet<>();
    
    private int likeCount = 0;
    
    private int commentCount = 0;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Constructors
    public Post() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Post(String userId, String text, List<String> mediaUrls, List<String> tags, Location location) {
        this();
        this.userId = userId;
        this.text = text;
        this.mediaUrls = mediaUrls;
        this.tags = tags;
        this.location = location;
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
        this.updatedAt = LocalDateTime.now();
    }
    
    public List<String> getMediaUrls() {
        return mediaUrls;
    }
    
    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
        this.updatedAt = LocalDateTime.now();
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Location getLocation() {
        return location;
    }
    
    public void setLocation(Location location) {
        this.location = location;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Set<String> getLikedBy() {
        return likedBy;
    }
    
    public void setLikedBy(Set<String> likedBy) {
        this.likedBy = likedBy;
        this.likeCount = likedBy.size();
        this.updatedAt = LocalDateTime.now();
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
    
    // Helper methods
    public boolean toggleLike(String userId) {
        if (likedBy.contains(userId)) {
            likedBy.remove(userId);
            likeCount--;
            this.updatedAt = LocalDateTime.now();
            return false; // unliked
        } else {
            likedBy.add(userId);
            likeCount++;
            this.updatedAt = LocalDateTime.now();
            return true; // liked
        }
    }
    
    public void incrementCommentCount() {
        this.commentCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    // Nested Location class
    public static class Location {
        private double lat;
        private double lng;
        private String place;
        
        public Location() {}
        
        public Location(double lat, double lng, String place) {
            this.lat = lat;
            this.lng = lng;
            this.place = place;
        }
        
        public double getLat() {
            return lat;
        }
        
        public void setLat(double lat) {
            this.lat = lat;
        }
        
        public double getLng() {
            return lng;
        }
        
        public void setLng(double lng) {
            this.lng = lng;
        }
        
        public String getPlace() {
            return place;
        }
        
        public void setPlace(String place) {
            this.place = place;
        }
    }
}
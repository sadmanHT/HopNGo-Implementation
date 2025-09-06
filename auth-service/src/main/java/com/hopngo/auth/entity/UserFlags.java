package com.hopngo.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_flags")
public class UserFlags {
    
    @Id
    @Column(name = "user_id")
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Column(name = "verified_provider", nullable = false)
    private Boolean verifiedProvider = false;
    
    @Column(nullable = false)
    private Boolean banned = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public UserFlags() {}
    
    public UserFlags(Long userId) {
        this.userId = userId;
    }
    
    public UserFlags(Long userId, Boolean verifiedProvider, Boolean banned) {
        this.userId = userId;
        this.verifiedProvider = verifiedProvider;
        this.banned = banned;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Boolean getVerifiedProvider() {
        return verifiedProvider;
    }
    
    public void setVerifiedProvider(Boolean verifiedProvider) {
        this.verifiedProvider = verifiedProvider;
    }
    
    public Boolean getBanned() {
        return banned;
    }
    
    public void setBanned(Boolean banned) {
        this.banned = banned;
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
    public boolean isVerifiedProvider() {
        return Boolean.TRUE.equals(this.verifiedProvider);
    }
    
    public boolean isBanned() {
        return Boolean.TRUE.equals(this.banned);
    }
    
    public boolean canCreateListings() {
        return isVerifiedProvider() && !isBanned();
    }
    
    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserFlags userFlags = (UserFlags) o;
        return Objects.equals(userId, userFlags.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
    
    @Override
    public String toString() {
        return "UserFlags{" +
                "userId=" + userId +
                ", verifiedProvider=" + verifiedProvider +
                ", banned=" + banned +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
package com.hopngo.analytics.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_content_stats", indexes = {
    @Index(name = "idx_user_content_stats_user_id", columnList = "userId"),
    @Index(name = "idx_user_content_stats_posts_count", columnList = "postsCount"),
    @Index(name = "idx_user_content_stats_likes_received", columnList = "likesReceived"),
    @Index(name = "idx_user_content_stats_updated_at", columnList = "updatedAt")
})
@EntityListeners(AuditingEntityListener.class)
public class UserContentStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    @NotBlank(message = "User ID is required")
    private String userId;

    @Column(name = "posts_count", nullable = false)
    @NotNull
    private Integer postsCount = 0;

    @Column(name = "likes_given", nullable = false)
    @NotNull
    private Integer likesGiven = 0;

    @Column(name = "likes_received", nullable = false)
    @NotNull
    private Integer likesReceived = 0;

    @Column(name = "bookmarks_count", nullable = false)
    @NotNull
    private Integer bookmarksCount = 0;

    @Column(name = "follows_count", nullable = false)
    @NotNull
    private Integer followsCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @NotNull
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private OffsetDateTime updatedAt;

    // Constructors
    public UserContentStats() {}

    public UserContentStats(String userId) {
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getPostsCount() {
        return postsCount;
    }

    public void setPostsCount(Integer postsCount) {
        this.postsCount = postsCount;
    }

    public Integer getLikesGiven() {
        return likesGiven;
    }

    public void setLikesGiven(Integer likesGiven) {
        this.likesGiven = likesGiven;
    }

    public Integer getLikesReceived() {
        return likesReceived;
    }

    public void setLikesReceived(Integer likesReceived) {
        this.likesReceived = likesReceived;
    }

    public Integer getBookmarksCount() {
        return bookmarksCount;
    }

    public void setBookmarksCount(Integer bookmarksCount) {
        this.bookmarksCount = bookmarksCount;
    }

    public Integer getFollowsCount() {
        return followsCount;
    }

    public void setFollowsCount(Integer followsCount) {
        this.followsCount = followsCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    public void incrementPostsCount() {
        this.postsCount++;
    }

    public void incrementLikesGiven() {
        this.likesGiven++;
    }

    public void incrementLikesReceived() {
        this.likesReceived++;
    }

    public void incrementBookmarksCount() {
        this.bookmarksCount++;
    }

    public void incrementFollowsCount() {
        this.followsCount++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserContentStats that = (UserContentStats) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "UserContentStats{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", postsCount=" + postsCount +
                ", likesGiven=" + likesGiven +
                ", likesReceived=" + likesReceived +
                ", bookmarksCount=" + bookmarksCount +
                ", followsCount=" + followsCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
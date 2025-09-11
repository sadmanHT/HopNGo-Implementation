package com.hopngo.analytics.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "item_popularity", 
    indexes = {
        @Index(name = "idx_item_popularity_item_id", columnList = "itemId"),
        @Index(name = "idx_item_popularity_item_type", columnList = "itemType"),
        @Index(name = "idx_item_popularity_likes", columnList = "likes"),
        @Index(name = "idx_item_popularity_bookmarks", columnList = "bookmarks"),
        @Index(name = "idx_item_popularity_recency_score", columnList = "recencyScore"),
        @Index(name = "idx_item_popularity_popularity_score", columnList = "popularityScore"),
        @Index(name = "idx_item_popularity_type_popularity", columnList = "itemType, popularityScore"),
        @Index(name = "idx_item_popularity_updated_at", columnList = "updatedAt")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_item_popularity_item_type", columnNames = {"itemId", "itemType"})
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ItemPopularity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false, length = 36)
    @NotBlank(message = "Item ID is required")
    private String itemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    @NotNull(message = "Item type is required")
    private ItemType itemType;

    @Column(name = "likes", nullable = false)
    @NotNull
    private Integer likes = 0;

    @Column(name = "bookmarks", nullable = false)
    @NotNull
    private Integer bookmarks = 0;

    @Column(name = "views", nullable = false)
    @NotNull
    private Integer views = 0;

    @Column(name = "recency_score", precision = 10, scale = 4)
    @NotNull
    private BigDecimal recencyScore = BigDecimal.ZERO;

    @Column(name = "popularity_score", precision = 10, scale = 4)
    @NotNull
    private BigDecimal popularityScore = BigDecimal.ZERO;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @NotNull
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private OffsetDateTime updatedAt;

    // Enum for item types
    public enum ItemType {
        POST, LISTING
    }

    // Constructors
    public ItemPopularity() {}

    public ItemPopularity(String itemId, ItemType itemType) {
        this.itemId = itemId;
        this.itemType = itemType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public Integer getBookmarks() {
        return bookmarks;
    }

    public void setBookmarks(Integer bookmarks) {
        this.bookmarks = bookmarks;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public BigDecimal getRecencyScore() {
        return recencyScore;
    }

    public void setRecencyScore(BigDecimal recencyScore) {
        this.recencyScore = recencyScore;
    }

    public BigDecimal getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(BigDecimal popularityScore) {
        this.popularityScore = popularityScore;
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
    public void incrementLikes() {
        this.likes++;
    }

    public void incrementBookmarks() {
        this.bookmarks++;
    }

    public void incrementViews() {
        this.views++;
    }

    /**
     * Calculate and update the popularity score based on likes, bookmarks, views, and recency
     */
    public void calculatePopularityScore() {
        // Weighted formula: likes * 3 + bookmarks * 5 + views * 1 + recency_score * 2
        BigDecimal likesScore = BigDecimal.valueOf(likes).multiply(BigDecimal.valueOf(3));
        BigDecimal bookmarksScore = BigDecimal.valueOf(bookmarks).multiply(BigDecimal.valueOf(5));
        BigDecimal viewsScore = BigDecimal.valueOf(views);
        BigDecimal recencyWeight = recencyScore.multiply(BigDecimal.valueOf(2));
        
        this.popularityScore = likesScore.add(bookmarksScore).add(viewsScore).add(recencyWeight);
    }

    /**
     * Calculate recency score based on creation time (higher for newer content)
     */
    public void calculateRecencyScore() {
        if (createdAt != null) {
            long hoursAgo = java.time.Duration.between(createdAt, OffsetDateTime.now()).toHours();
            // Exponential decay: score decreases by half every 24 hours
            double decayFactor = Math.pow(0.5, hoursAgo / 24.0);
            this.recencyScore = BigDecimal.valueOf(Math.max(0.1, decayFactor * 100));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemPopularity that = (ItemPopularity) o;
        return Objects.equals(itemId, that.itemId) && itemType == that.itemType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, itemType);
    }

    @Override
    public String toString() {
        return "ItemPopularity{" +
                "id=" + id +
                ", itemId='" + itemId + '\'' +
                ", itemType=" + itemType +
                ", likes=" + likes +
                ", bookmarks=" + bookmarks +
                ", views=" + views +
                ", recencyScore=" + recencyScore +
                ", popularityScore=" + popularityScore +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
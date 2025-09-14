package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "itineraries")
public class Itinerary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "title")
    private String title;

    @Column(name = "description", length = 3000)
    private String description;

    @NotNull
    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "estimated_cost", precision = 19, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "currency")
    private String currency = "BDT";

    @Column(name = "difficulty_level")
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;

    @Column(name = "category")
    private String category;

    @ElementCollection
    @CollectionTable(name = "itinerary_tags", joinColumns = @JoinColumn(name = "itinerary_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "itinerary_destinations", joinColumns = @JoinColumn(name = "itinerary_id"))
    @Column(name = "destination")
    private List<String> destinations = new ArrayList<>();

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("dayNumber ASC")
    private List<ItineraryDay> days = new ArrayList<>();

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @ElementCollection
    @CollectionTable(name = "itinerary_images", joinColumns = @JoinColumn(name = "itinerary_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "is_template")
    private Boolean isTemplate = false;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "is_demo")
    private Boolean isDemo = false;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "booking_count")
    private Integer bookingCount = 0;

    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Itinerary() {}

    public Itinerary(String title, String description, Integer durationDays) {
        this.title = title;
        this.description = description;
        this.durationDays = durationDays;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<String> destinations) {
        this.destinations = destinations;
    }

    public List<ItineraryDay> getDays() {
        return days;
    }

    public void setDays(List<ItineraryDay> days) {
        this.days = days;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public Boolean getIsTemplate() {
        return isTemplate;
    }

    public void setIsTemplate(Boolean isTemplate) {
        this.isTemplate = isTemplate;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Boolean getIsDemo() {
        return isDemo;
    }

    public void setIsDemo(Boolean isDemo) {
        this.isDemo = isDemo;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getBookingCount() {
        return bookingCount;
    }

    public void setBookingCount(Integer bookingCount) {
        this.bookingCount = bookingCount;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
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

    @Override
    public String toString() {
        return "Itinerary{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", durationDays=" + durationDays +
                ", estimatedCost=" + estimatedCost +
                ", difficultyLevel=" + difficultyLevel +
                ", isTemplate=" + isTemplate +
                ", isDemo=" + isDemo +
                '}';
    }

    public enum DifficultyLevel {
        EASY, MODERATE, CHALLENGING, EXTREME
    }
}
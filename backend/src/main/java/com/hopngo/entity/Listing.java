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
@Table(name = "listings")
public class Listing {
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
    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "currency")
    private String currency = "BDT";

    @NotBlank
    @Column(name = "location")
    private String location;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "listing_type")
    @Enumerated(EnumType.STRING)
    private ListingType listingType;

    @Column(name = "category")
    private String category;

    @Column(name = "subcategory")
    private String subcategory;

    @ElementCollection
    @CollectionTable(name = "listing_images", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "listing_amenities", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

    @Column(name = "max_guests")
    private Integer maxGuests;

    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "is_demo")
    private Boolean isDemo = false;

    @Column(name = "instant_book")
    private Boolean instantBook = false;

    @Column(name = "cancellation_policy")
    private String cancellationPolicy;

    @Column(name = "check_in_time")
    private String checkInTime;

    @Column(name = "check_out_time")
    private String checkOutTime;

    @Column(name = "house_rules", length = 1000)
    private String houseRules;

    @Column(name = "minimum_stay")
    private Integer minimumStay = 1;

    @Column(name = "maximum_stay")
    private Integer maximumStay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Listing() {}

    public Listing(String title, String description, BigDecimal price, String location, ListingType listingType) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.location = location;
        this.listingType = listingType;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public ListingType getListingType() {
        return listingType;
    }

    public void setListingType(ListingType listingType) {
        this.listingType = listingType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    public Boolean getInstantBook() {
        return instantBook;
    }

    public void setInstantBook(Boolean instantBook) {
        this.instantBook = instantBook;
    }

    public String getCancellationPolicy() {
        return cancellationPolicy;
    }

    public void setCancellationPolicy(String cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(String checkInTime) {
        this.checkInTime = checkInTime;
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(String checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getHouseRules() {
        return houseRules;
    }

    public void setHouseRules(String houseRules) {
        this.houseRules = houseRules;
    }

    public Integer getMinimumStay() {
        return minimumStay;
    }

    public void setMinimumStay(Integer minimumStay) {
        this.minimumStay = minimumStay;
    }

    public Integer getMaximumStay() {
        return maximumStay;
    }

    public void setMaximumStay(Integer maximumStay) {
        this.maximumStay = maximumStay;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
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
        return "Listing{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", location='" + location + '\'' +
                ", listingType=" + listingType +
                ", rating=" + rating +
                ", isDemo=" + isDemo +
                '}';
    }

    public enum ListingType {
        HOTEL, RESORT, GUESTHOUSE, APARTMENT, VILLA, TOUR, EXPERIENCE, RESTAURANT
    }
}
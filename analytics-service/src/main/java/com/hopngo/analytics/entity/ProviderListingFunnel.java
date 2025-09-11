package com.hopngo.analytics.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "provider_listing_funnel", 
    indexes = {
        @Index(name = "idx_provider_listing_funnel_provider", columnList = "providerId"),
        @Index(name = "idx_provider_listing_funnel_date", columnList = "date"),
        @Index(name = "idx_provider_listing_funnel_impressions", columnList = "impressions")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "idx_provider_listing_funnel_unique", columnNames = {"providerId", "date"})
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ProviderListingFunnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false, length = 36)
    @NotBlank(message = "Provider ID is required")
    private String providerId;

    @Column(name = "date", nullable = false)
    @NotNull(message = "Date is required")
    private LocalDate date;

    @Column(name = "impressions", nullable = false)
    @NotNull
    private Integer impressions = 0;

    @Column(name = "detail_views", nullable = false)
    @NotNull
    private Integer detailViews = 0;

    @Column(name = "add_to_cart", nullable = false)
    @NotNull
    private Integer addToCart = 0;

    @Column(name = "bookings", nullable = false)
    @NotNull
    private Integer bookings = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @NotNull
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private OffsetDateTime updatedAt;

    // Constructors
    public ProviderListingFunnel() {}

    public ProviderListingFunnel(String providerId) {
        this.providerId = providerId;
        this.date = LocalDate.now();
    }

    public ProviderListingFunnel(String providerId, LocalDate date) {
        this.providerId = providerId;
        this.date = date;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getImpressions() {
        return impressions;
    }

    public void setImpressions(Integer impressions) {
        this.impressions = impressions;
    }

    public Integer getDetailViews() {
        return detailViews;
    }

    public void setDetailViews(Integer detailViews) {
        this.detailViews = detailViews;
    }

    public Integer getAddToCart() {
        return addToCart;
    }

    public void setAddToCart(Integer addToCart) {
        this.addToCart = addToCart;
    }

    public Integer getBookings() {
        return bookings;
    }

    public void setBookings(Integer bookings) {
        this.bookings = bookings;
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
    public void incrementImpressions() {
        this.impressions++;
    }

    public void incrementDetailViews() {
        this.detailViews++;
    }

    public void incrementAddToCart() {
        this.addToCart++;
    }

    public void incrementBookings() {
        this.bookings++;
    }

    public void incrementAddToCarts() {
        this.addToCart++;
    }

    public Integer getAddToCarts() {
        return this.addToCart;
    }

    // Conversion rate calculations
    public Double getImpressionToDetailRate() {
        if (impressions == 0) return 0.0;
        return (detailViews.doubleValue() / impressions.doubleValue()) * 100.0;
    }

    public Double getDetailToCartRate() {
        if (detailViews == 0) return 0.0;
        return (addToCart.doubleValue() / detailViews.doubleValue()) * 100.0;
    }

    public Double getCartToBookingRate() {
        if (addToCart == 0) return 0.0;
        return (bookings.doubleValue() / addToCart.doubleValue()) * 100.0;
    }

    public Double getOverallConversionRate() {
        if (impressions == 0) return 0.0;
        return (bookings.doubleValue() / impressions.doubleValue()) * 100.0;
    }

    // Calculate methods for service compatibility
    public Double calculateImpressionToDetailRate() {
        return getImpressionToDetailRate();
    }

    public Double calculateDetailToCartRate() {
        return getDetailToCartRate();
    }

    public Double calculateCartToBookingRate() {
        return getCartToBookingRate();
    }

    public Double calculateOverallConversionRate() {
        return getOverallConversionRate();
    }

    // Funnel drop-off calculations
    public Integer getImpressionToDetailDropOff() {
        return impressions - detailViews;
    }

    public Integer getDetailToCartDropOff() {
        return detailViews - addToCart;
    }

    public Integer getCartToBookingDropOff() {
        return addToCart - bookings;
    }

    // Calculate drop-off methods for service compatibility
    public Integer calculateImpressionDropOff() {
        return getImpressionToDetailDropOff();
    }

    public Integer calculateDetailDropOff() {
        return getDetailToCartDropOff();
    }

    public Integer calculateCartDropOff() {
        return getCartToBookingDropOff();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderListingFunnel that = (ProviderListingFunnel) o;
        return Objects.equals(providerId, that.providerId) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, date);
    }

    @Override
    public String toString() {
        return "ProviderListingFunnel{" +
                "id=" + id +
                ", providerId='" + providerId + '\'' +
                ", date=" + date +
                ", impressions=" + impressions +
                ", detailViews=" + detailViews +
                ", addToCart=" + addToCart +
                ", bookings=" + bookings +
                '}';
    }
}
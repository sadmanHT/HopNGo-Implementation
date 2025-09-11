package com.hopngo.analytics.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "provider_bookings_daily", 
    indexes = {
        @Index(name = "idx_provider_bookings_daily_provider", columnList = "providerId"),
        @Index(name = "idx_provider_bookings_daily_date", columnList = "date"),
        @Index(name = "idx_provider_bookings_daily_revenue", columnList = "revenueMinor")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "idx_provider_bookings_daily_unique", columnNames = {"providerId", "date"})
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ProviderBookingsDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false, length = 36)
    @NotBlank(message = "Provider ID is required")
    private String providerId;

    @Column(name = "date", nullable = false)
    @NotNull(message = "Date is required")
    private LocalDate date;

    @Column(name = "bookings", nullable = false)
    @NotNull
    private Integer bookings = 0;

    @Column(name = "cancellations", nullable = false)
    @NotNull
    private Integer cancellations = 0;

    @Column(name = "revenue_minor", nullable = false)
    @NotNull
    private Long revenueMinor = 0L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @NotNull
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private OffsetDateTime updatedAt;

    // Constructors
    public ProviderBookingsDaily() {}

    public ProviderBookingsDaily(String providerId, LocalDate date) {
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

    public Integer getBookings() {
        return bookings;
    }

    public void setBookings(Integer bookings) {
        this.bookings = bookings;
    }

    public Integer getCancellations() {
        return cancellations;
    }

    public void setCancellations(Integer cancellations) {
        this.cancellations = cancellations;
    }

    public Long getRevenueMinor() {
        return revenueMinor;
    }

    public void setRevenueMinor(Long revenueMinor) {
        this.revenueMinor = revenueMinor;
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
    public void incrementBookings() {
        this.bookings++;
    }

    public void incrementCancellations() {
        this.cancellations++;
    }

    public void addRevenue(Long additionalRevenue) {
        this.revenueMinor += additionalRevenue;
    }

    public Double getCancellationRate() {
        if (bookings == 0) return 0.0;
        return (cancellations.doubleValue() / bookings.doubleValue()) * 100.0;
    }

    public Double getRevenueInMajorUnits() {
        return revenueMinor / 100.0; // Convert cents to dollars
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderBookingsDaily that = (ProviderBookingsDaily) o;
        return Objects.equals(providerId, that.providerId) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, date);
    }

    @Override
    public String toString() {
        return "ProviderBookingsDaily{" +
                "id=" + id +
                ", providerId='" + providerId + '\'' +
                ", date=" + date +
                ", bookings=" + bookings +
                ", cancellations=" + cancellations +
                ", revenueMinor=" + revenueMinor +
                '}';
    }
}
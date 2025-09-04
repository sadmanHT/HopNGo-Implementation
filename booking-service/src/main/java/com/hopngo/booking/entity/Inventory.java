package com.hopngo.booking.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"listing_id", "date"})
})
public class Inventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity = 1;
    
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;
    
    @Column(name = "price_override", precision = 10, scale = 2)
    private BigDecimal priceOverride;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public Inventory() {}
    
    public Inventory(Listing listing, LocalDate date, Integer availableQuantity) {
        this.listing = listing;
        this.date = date;
        this.availableQuantity = availableQuantity;
    }
    
    // Business methods
    public boolean isAvailable(Integer requestedQuantity) {
        return (availableQuantity - reservedQuantity) >= requestedQuantity;
    }
    
    public void reserve(Integer quantity) {
        if (!isAvailable(quantity)) {
            throw new IllegalStateException("Not enough inventory available for date: " + date);
        }
        this.reservedQuantity += quantity;
    }
    
    public void release(Integer quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release more than reserved quantity");
        }
        this.reservedQuantity -= quantity;
    }
    
    public BigDecimal getEffectivePrice() {
        return priceOverride != null ? priceOverride : listing.getBasePrice();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Listing getListing() {
        return listing;
    }
    
    public void setListing(Listing listing) {
        this.listing = listing;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
    
    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
    
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    
    public BigDecimal getPriceOverride() {
        return priceOverride;
    }
    
    public void setPriceOverride(BigDecimal priceOverride) {
        this.priceOverride = priceOverride;
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
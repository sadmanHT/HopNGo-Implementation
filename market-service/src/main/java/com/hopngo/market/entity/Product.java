package com.hopngo.market.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "rental_price_per_day", precision = 12, scale = 2)
    private BigDecimal rentalPricePerDay;
    
    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String currency = "USD";
    
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String category;
    
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String brand;
    
    @NotNull
    @Min(0)
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;
    
    @NotNull
    @Min(0)
    @Column(name = "rental_stock_quantity", nullable = false)
    private Integer rentalStockQuantity;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @NotNull
    @Column(name = "is_available_for_purchase", nullable = false)
    private Boolean isAvailableForPurchase = true;
    
    @NotNull
    @Column(name = "is_available_for_rental", nullable = false)
    private Boolean isAvailableForRental = true;
    
    @Column(name = "weight_kg", precision = 8, scale = 2)
    private BigDecimal weightKg;
    
    @Column(columnDefinition = "TEXT")
    private String specifications;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public Product() {}
    
    public Product(String name, String description, BigDecimal price, BigDecimal rentalPricePerDay,
                   String category, String brand, Integer stockQuantity, Integer rentalStockQuantity) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.rentalPricePerDay = rentalPricePerDay;
        this.category = category;
        this.brand = brand;
        this.stockQuantity = stockQuantity;
        this.rentalStockQuantity = rentalStockQuantity;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public BigDecimal getRentalPricePerDay() {
        return rentalPricePerDay;
    }
    
    public void setRentalPricePerDay(BigDecimal rentalPricePerDay) {
        this.rentalPricePerDay = rentalPricePerDay;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public Integer getStockQuantity() {
        return stockQuantity;
    }
    
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
    
    public Integer getRentalStockQuantity() {
        return rentalStockQuantity;
    }
    
    public void setRentalStockQuantity(Integer rentalStockQuantity) {
        this.rentalStockQuantity = rentalStockQuantity;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public Boolean getIsAvailableForPurchase() {
        return isAvailableForPurchase;
    }
    
    public void setIsAvailableForPurchase(Boolean isAvailableForPurchase) {
        this.isAvailableForPurchase = isAvailableForPurchase;
    }
    
    public Boolean getIsAvailableForRental() {
        return isAvailableForRental;
    }
    
    public void setIsAvailableForRental(Boolean isAvailableForRental) {
        this.isAvailableForRental = isAvailableForRental;
    }
    
    public BigDecimal getWeightKg() {
        return weightKg;
    }
    
    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }
    
    public String getSpecifications() {
        return specifications;
    }
    
    public void setSpecifications(String specifications) {
        this.specifications = specifications;
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
    
    // Business methods
    public boolean isInStock() {
        return stockQuantity > 0;
    }
    
    public boolean isAvailableForRent() {
        return rentalStockQuantity > 0;
    }
    
    public void decreaseStock(int quantity) {
        if (stockQuantity < quantity) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        this.stockQuantity -= quantity;
    }
    
    public void decreaseRentalStock(int quantity) {
        if (rentalStockQuantity < quantity) {
            throw new IllegalArgumentException("Insufficient rental stock");
        }
        this.rentalStockQuantity -= quantity;
    }
    
    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
    }
    
    public void increaseRentalStock(int quantity) {
        this.rentalStockQuantity += quantity;
    }
}
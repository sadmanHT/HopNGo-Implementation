package com.hopngo.booking.entity;

import com.hopngo.booking.entity.base.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Entity
@Table(name = "vendor_responses")
public class VendorResponse extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;
    
    @Column(name = "vendor_user_id", nullable = false)
    private String vendorUserId;
    
    @NotBlank(message = "Response message cannot be blank")
    @Size(max = 2000, message = "Response message cannot exceed 2000 characters")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    // Constructors
    public VendorResponse() {}
    
    public VendorResponse(Review review, String vendorUserId, String message) {
        this.review = review;
        this.vendorUserId = vendorUserId;
        this.message = message;
    }
    
    // Business methods
    public boolean isOwnedByVendor(String vendorUserId) {
        return this.vendorUserId.equals(vendorUserId);
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Review getReview() {
        return review;
    }
    
    public void setReview(Review review) {
        this.review = review;
    }
    
    public String getVendorUserId() {
        return vendorUserId;
    }
    
    public void setVendorUserId(String vendorUserId) {
        this.vendorUserId = vendorUserId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
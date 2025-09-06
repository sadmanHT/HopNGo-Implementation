package com.hopngo.emergency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "emergency_contacts")
public class EmergencyContact {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;
    
    @NotBlank
    @Size(max = 20)
    @Column(name = "phone", nullable = false)
    private String phone;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "relation", nullable = false)
    private String relation;
    
    @NotNull
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // Default constructor
    public EmergencyContact() {}
    
    // Constructor
    public EmergencyContact(String userId, String name, String phone, String relation, Boolean isPrimary) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.relation = relation;
        this.isPrimary = isPrimary != null ? isPrimary : false;
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getRelation() {
        return relation;
    }
    
    public void setRelation(String relation) {
        this.relation = relation;
    }
    
    public Boolean getIsPrimary() {
        return isPrimary;
    }
    
    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary != null ? isPrimary : false;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmergencyContact that = (EmergencyContact) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "EmergencyContact{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", relation='" + relation + '\'' +
                ", isPrimary=" + isPrimary +
                ", createdAt=" + createdAt +
                '}';
    }
}
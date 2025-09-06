package com.hopngo.emergency.dto;

import com.hopngo.emergency.entity.EmergencyContact;

import java.time.Instant;

public class EmergencyContactResponse {
    
    private Long id;
    private String name;
    private String phone;
    private String relation;
    private Boolean isPrimary;
    private Instant createdAt;
    
    // Default constructor
    public EmergencyContactResponse() {}
    
    // Constructor from entity
    public EmergencyContactResponse(EmergencyContact contact) {
        this.id = contact.getId();
        this.name = contact.getName();
        this.phone = contact.getPhone();
        this.relation = contact.getRelation();
        this.isPrimary = contact.getIsPrimary();
        this.createdAt = contact.getCreatedAt();
    }
    
    // Constructor
    public EmergencyContactResponse(Long id, String name, String phone, String relation, Boolean isPrimary, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.relation = relation;
        this.isPrimary = isPrimary;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
        this.isPrimary = isPrimary;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "EmergencyContactResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", relation='" + relation + '\'' +
                ", isPrimary=" + isPrimary +
                ", createdAt=" + createdAt +
                '}';
    }
}
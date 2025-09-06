package com.hopngo.emergency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmergencyContactRequest {
    
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    
    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
    
    @NotBlank(message = "Relation is required")
    @Size(max = 100, message = "Relation must not exceed 100 characters")
    private String relation;
    
    private Boolean isPrimary = false;
    
    // Default constructor
    public EmergencyContactRequest() {}
    
    // Constructor
    public EmergencyContactRequest(String name, String phone, String relation, Boolean isPrimary) {
        this.name = name;
        this.phone = phone;
        this.relation = relation;
        this.isPrimary = isPrimary != null ? isPrimary : false;
    }
    
    // Getters and Setters
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
    
    @Override
    public String toString() {
        return "EmergencyContactRequest{" +
                "name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", relation='" + relation + '\'' +
                ", isPrimary=" + isPrimary +
                '}';
    }
}
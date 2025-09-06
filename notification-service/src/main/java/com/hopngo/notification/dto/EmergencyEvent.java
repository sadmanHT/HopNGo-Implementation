package com.hopngo.notification.dto;

import java.time.Instant;
import java.util.List;

public class EmergencyEvent {
    
    private String userId;
    private String userName;
    private Location location;
    private String note;
    private List<EmergencyContact> contacts;
    private Instant triggeredAt;
    
    // Default constructor
    public EmergencyEvent() {}
    
    // Constructor
    public EmergencyEvent(String userId, String userName, Location location, String note, List<EmergencyContact> contacts, Instant triggeredAt) {
        this.userId = userId;
        this.userName = userName;
        this.location = location;
        this.note = note;
        this.contacts = contacts;
        this.triggeredAt = triggeredAt;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public void setLocation(Location location) {
        this.location = location;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public List<EmergencyContact> getContacts() {
        return contacts;
    }
    
    public void setContacts(List<EmergencyContact> contacts) {
        this.contacts = contacts;
    }
    
    public Instant getTriggeredAt() {
        return triggeredAt;
    }
    
    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }
    
    @Override
    public String toString() {
        return "EmergencyEvent{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", location=" + location +
                ", note='" + note + '\'' +
                ", contacts=" + contacts +
                ", triggeredAt=" + triggeredAt +
                '}';
    }
    
    // Inner class for Location
    public static class Location {
        private Double lat;
        private Double lng;
        
        public Location() {}
        
        public Location(Double lat, Double lng) {
            this.lat = lat;
            this.lng = lng;
        }
        
        public Double getLat() {
            return lat;
        }
        
        public void setLat(Double lat) {
            this.lat = lat;
        }
        
        public Double getLng() {
            return lng;
        }
        
        public void setLng(Double lng) {
            this.lng = lng;
        }
        
        @Override
        public String toString() {
            return "Location{lat=" + lat + ", lng=" + lng + "}";
        }
    }
    
    // Inner class for Emergency Contact
    public static class EmergencyContact {
        private String name;
        private String phone;
        private String relation;
        private Boolean isPrimary;
        
        public EmergencyContact() {}
        
        public EmergencyContact(String name, String phone, String relation, Boolean isPrimary) {
            this.name = name;
            this.phone = phone;
            this.relation = relation;
            this.isPrimary = isPrimary;
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
        
        @Override
        public String toString() {
            return "EmergencyContact{" +
                    "name='" + name + '\'' +
                    ", phone='" + phone + '\'' +
                    ", relation='" + relation + '\'' +
                    ", isPrimary=" + isPrimary +
                    '}';
        }
    }
}
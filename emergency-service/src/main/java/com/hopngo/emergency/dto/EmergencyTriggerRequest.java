package com.hopngo.emergency.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EmergencyTriggerRequest {
    
    @NotNull(message = "Location is required")
    @Valid
    private Location location;
    
    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
    
    // Default constructor
    public EmergencyTriggerRequest() {}
    
    // Constructor
    public EmergencyTriggerRequest(Location location, String note) {
        this.location = location;
        this.note = note;
    }
    
    // Getters and Setters
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
    
    @Override
    public String toString() {
        return "EmergencyTriggerRequest{" +
                "location=" + location +
                ", note='" + note + '\'' +
                '}';
    }
    
    // Inner class for Location
    public static class Location {
        
        @NotNull(message = "Latitude is required")
        private Double lat;
        
        @NotNull(message = "Longitude is required")
        private Double lng;
        
        // Default constructor
        public Location() {}
        
        // Constructor
        public Location(Double lat, Double lng) {
            this.lat = lat;
            this.lng = lng;
        }
        
        // Getters and Setters
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
            return "Location{" +
                    "lat=" + lat +
                    ", lng=" + lng +
                    '}';
        }
    }
}
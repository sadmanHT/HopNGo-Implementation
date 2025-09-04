package com.hopngo.social.dto;

import java.util.List;

public class HeatmapResponse {
    
    private List<HeatmapPoint> points;
    
    // Constructors
    public HeatmapResponse() {}
    
    public HeatmapResponse(List<HeatmapPoint> points) {
        this.points = points;
    }
    
    // Getters and Setters
    public List<HeatmapPoint> getPoints() {
        return points;
    }
    
    public void setPoints(List<HeatmapPoint> points) {
        this.points = points;
    }
    
    // Nested HeatmapPoint class
    public static class HeatmapPoint {
        private double lat;
        private double lng;
        private int count;
        private String geohash;
        
        public HeatmapPoint() {}
        
        public HeatmapPoint(double lat, double lng, int count, String geohash) {
            this.lat = lat;
            this.lng = lng;
            this.count = count;
            this.geohash = geohash;
        }
        
        public double getLat() {
            return lat;
        }
        
        public void setLat(double lat) {
            this.lat = lat;
        }
        
        public double getLng() {
            return lng;
        }
        
        public void setLng(double lng) {
            this.lng = lng;
        }
        
        public int getCount() {
            return count;
        }
        
        public void setCount(int count) {
            this.count = count;
        }
        
        public String getGeohash() {
            return geohash;
        }
        
        public void setGeohash(String geohash) {
            this.geohash = geohash;
        }
    }
}
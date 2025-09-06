package com.hopngo.social.dto;

import java.util.List;
import java.util.ArrayList;

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
        private String geohash;
        private double lat;
        private double lng;
        private double weight;
        private List<String> tagsTop;
        
        public HeatmapPoint() {
            this.tagsTop = new ArrayList<>();
        }
        
        public HeatmapPoint(String geohash, double lat, double lng, double weight, List<String> tagsTop) {
            this.geohash = geohash;
            this.lat = lat;
            this.lng = lng;
            this.weight = weight;
            this.tagsTop = tagsTop != null ? tagsTop : new ArrayList<>();
        }
        
        public String getGeohash() {
            return geohash;
        }
        
        public void setGeohash(String geohash) {
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
        
        public double getWeight() {
            return weight;
        }
        
        public void setWeight(double weight) {
            this.weight = weight;
        }
        
        public List<String> getTagsTop() {
            return tagsTop;
        }
        
        public void setTagsTop(List<String> tagsTop) {
            this.tagsTop = tagsTop != null ? tagsTop : new ArrayList<>();
        }
    }
}
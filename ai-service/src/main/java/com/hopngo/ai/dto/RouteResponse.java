package com.hopngo.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Response containing route information")
public class RouteResponse {
    
    @Schema(description = "List of calculated routes")
    private List<Route> routes;
    
    @Schema(description = "Status code of the routing request")
    private String code;
    
    @Schema(description = "List of waypoints used in routing")
    private List<Waypoint> waypoints;
    
    // Constructors
    public RouteResponse() {}
    
    public RouteResponse(List<Route> routes, String code) {
        this.routes = routes;
        this.code = code;
    }
    
    // Getters and Setters
    public List<Route> getRoutes() {
        return routes;
    }
    
    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public List<Waypoint> getWaypoints() {
        return waypoints;
    }
    
    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }
    
    @Schema(description = "Individual route information")
    public static class Route {
        
        @Schema(description = "Route geometry (polyline or GeoJSON)")
        private Object geometry;
        
        @Schema(description = "Route legs (segments between waypoints)")
        private List<Leg> legs;
        
        @Schema(description = "Total distance in meters")
        private Double distance;
        
        @Schema(description = "Total duration in seconds")
        private Double duration;
        
        @Schema(description = "Weight of the route (used for optimization)")
        @JsonProperty("weight_name")
        private String weightName;
        
        @Schema(description = "Weight value")
        private Double weight;
        
        // Constructors
        public Route() {}
        
        public Route(Object geometry, Double distance, Double duration) {
            this.geometry = geometry;
            this.distance = distance;
            this.duration = duration;
        }
        
        // Getters and Setters
        public Object getGeometry() {
            return geometry;
        }
        
        public void setGeometry(Object geometry) {
            this.geometry = geometry;
        }
        
        public List<Leg> getLegs() {
            return legs;
        }
        
        public void setLegs(List<Leg> legs) {
            this.legs = legs;
        }
        
        public Double getDistance() {
            return distance;
        }
        
        public void setDistance(Double distance) {
            this.distance = distance;
        }
        
        public Double getDuration() {
            return duration;
        }
        
        public void setDuration(Double duration) {
            this.duration = duration;
        }
        
        public String getWeightName() {
            return weightName;
        }
        
        public void setWeightName(String weightName) {
            this.weightName = weightName;
        }
        
        public Double getWeight() {
            return weight;
        }
        
        public void setWeight(Double weight) {
            this.weight = weight;
        }
        
        // Convenience methods for frontend
        @Schema(description = "Distance in kilometers")
        public Double getDistanceKm() {
            return distance != null ? distance / 1000.0 : null;
        }
        
        @Schema(description = "Duration in minutes")
        public Double getDurationMin() {
            return duration != null ? duration / 60.0 : null;
        }
    }
    
    @Schema(description = "Route leg (segment between waypoints)")
    public static class Leg {
        
        @Schema(description = "List of steps in this leg")
        private List<Step> steps;
        
        @Schema(description = "Distance of this leg in meters")
        private Double distance;
        
        @Schema(description = "Duration of this leg in seconds")
        private Double duration;
        
        @Schema(description = "Summary of the leg")
        private String summary;
        
        // Getters and Setters
        public List<Step> getSteps() {
            return steps;
        }
        
        public void setSteps(List<Step> steps) {
            this.steps = steps;
        }
        
        public Double getDistance() {
            return distance;
        }
        
        public void setDistance(Double distance) {
            this.distance = distance;
        }
        
        public Double getDuration() {
            return duration;
        }
        
        public void setDuration(Double duration) {
            this.duration = duration;
        }
        
        public String getSummary() {
            return summary;
        }
        
        public void setSummary(String summary) {
            this.summary = summary;
        }
    }
    
    @Schema(description = "Individual step in a route leg")
    public static class Step {
        
        @Schema(description = "Step geometry")
        private Object geometry;
        
        @Schema(description = "Maneuver information")
        private Map<String, Object> maneuver;
        
        @Schema(description = "Step mode (driving, walking, etc.)")
        private String mode;
        
        @Schema(description = "Driving side")
        @JsonProperty("driving_side")
        private String drivingSide;
        
        @Schema(description = "Step name/instruction")
        private String name;
        
        @Schema(description = "Intersections in this step")
        private List<Map<String, Object>> intersections;
        
        @Schema(description = "Weight of this step")
        private Double weight;
        
        @Schema(description = "Duration of this step in seconds")
        private Double duration;
        
        @Schema(description = "Distance of this step in meters")
        private Double distance;
        
        // Getters and Setters
        public Object getGeometry() {
            return geometry;
        }
        
        public void setGeometry(Object geometry) {
            this.geometry = geometry;
        }
        
        public Map<String, Object> getManeuver() {
            return maneuver;
        }
        
        public void setManeuver(Map<String, Object> maneuver) {
            this.maneuver = maneuver;
        }
        
        public String getMode() {
            return mode;
        }
        
        public void setMode(String mode) {
            this.mode = mode;
        }
        
        public String getDrivingSide() {
            return drivingSide;
        }
        
        public void setDrivingSide(String drivingSide) {
            this.drivingSide = drivingSide;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<Map<String, Object>> getIntersections() {
            return intersections;
        }
        
        public void setIntersections(List<Map<String, Object>> intersections) {
            this.intersections = intersections;
        }
        
        public Double getWeight() {
            return weight;
        }
        
        public void setWeight(Double weight) {
            this.weight = weight;
        }
        
        public Double getDuration() {
            return duration;
        }
        
        public void setDuration(Double duration) {
            this.duration = duration;
        }
        
        public Double getDistance() {
            return distance;
        }
        
        public void setDistance(Double distance) {
            this.distance = distance;
        }
    }
    
    @Schema(description = "Waypoint information")
    public static class Waypoint {
        
        @Schema(description = "Waypoint hint (used for snapping)")
        private String hint;
        
        @Schema(description = "Distance from input coordinate to snapped point")
        private Double distance;
        
        @Schema(description = "Name of the waypoint")
        private String name;
        
        @Schema(description = "Snapped coordinate [longitude, latitude]")
        private Double[] location;
        
        // Getters and Setters
        public String getHint() {
            return hint;
        }
        
        public void setHint(String hint) {
            this.hint = hint;
        }
        
        public Double getDistance() {
            return distance;
        }
        
        public void setDistance(Double distance) {
            this.distance = distance;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Double[] getLocation() {
            return location;
        }
        
        public void setLocation(Double[] location) {
            this.location = location;
        }
    }
}
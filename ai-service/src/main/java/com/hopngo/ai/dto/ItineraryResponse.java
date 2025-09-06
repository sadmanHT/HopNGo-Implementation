package com.hopngo.ai.dto;

import java.util.List;
import java.util.Map;

public class ItineraryResponse {
    
    private String id;
    private String title;
    private String description;
    private int totalDays;
    private int estimatedBudget;
    private List<DayPlan> dayPlans;
    private Map<String, Object> metadata;
    
    // Constructors
    public ItineraryResponse() {}
    
    public ItineraryResponse(String id, String title, String description, int totalDays, 
                           int estimatedBudget, List<DayPlan> dayPlans, Map<String, Object> metadata) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.totalDays = totalDays;
        this.estimatedBudget = estimatedBudget;
        this.dayPlans = dayPlans;
        this.metadata = metadata;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getTotalDays() {
        return totalDays;
    }
    
    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }
    
    public int getEstimatedBudget() {
        return estimatedBudget;
    }
    
    public void setEstimatedBudget(int estimatedBudget) {
        this.estimatedBudget = estimatedBudget;
    }
    
    public List<DayPlan> getDayPlans() {
        return dayPlans;
    }
    
    public void setDayPlans(List<DayPlan> dayPlans) {
        this.dayPlans = dayPlans;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    // Inner class for day plans
    public static class DayPlan {
        private int day;
        private String location;
        private String title;
        private List<Activity> activities;
        private int dailyBudget;
        
        // Constructors
        public DayPlan() {}
        
        public DayPlan(int day, String location, String title, List<Activity> activities, int dailyBudget) {
            this.day = day;
            this.location = location;
            this.title = title;
            this.activities = activities;
            this.dailyBudget = dailyBudget;
        }
        
        // Getters and Setters
        public int getDay() {
            return day;
        }
        
        public void setDay(int day) {
            this.day = day;
        }
        
        public String getLocation() {
            return location;
        }
        
        public void setLocation(String location) {
            this.location = location;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public List<Activity> getActivities() {
            return activities;
        }
        
        public void setActivities(List<Activity> activities) {
            this.activities = activities;
        }
        
        public int getDailyBudget() {
            return dailyBudget;
        }
        
        public void setDailyBudget(int dailyBudget) {
            this.dailyBudget = dailyBudget;
        }
    }
    
    // Inner class for activities
    public static class Activity {
        private String name;
        private String description;
        private String time;
        private String type;
        private int cost;
        private String location;
        
        // Constructors
        public Activity() {}
        
        public Activity(String name, String description, String time, String type, int cost, String location) {
            this.name = name;
            this.description = description;
            this.time = time;
            this.type = type;
            this.cost = cost;
            this.location = location;
        }
        
        // Getters and Setters
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
        
        public String getTime() {
            return time;
        }
        
        public void setTime(String time) {
            this.time = time;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public int getCost() {
            return cost;
        }
        
        public void setCost(int cost) {
            this.cost = cost;
        }
        
        public String getLocation() {
            return location;
        }
        
        public void setLocation(String location) {
            this.location = location;
        }
    }
}
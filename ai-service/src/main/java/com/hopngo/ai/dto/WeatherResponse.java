package com.hopngo.ai.dto;

import java.util.List;

public class WeatherResponse {
    
    private CurrentWeather current;
    private List<DailyForecast> forecast;
    private String location;
    private String timezone;
    private String lastUpdated;
    
    // Constructors
    public WeatherResponse() {}
    
    public WeatherResponse(CurrentWeather current, List<DailyForecast> forecast, 
                          String location, String timezone, String lastUpdated) {
        this.current = current;
        this.forecast = forecast;
        this.location = location;
        this.timezone = timezone;
        this.lastUpdated = lastUpdated;
    }
    
    // Getters and Setters
    public CurrentWeather getCurrent() {
        return current;
    }
    
    public void setCurrent(CurrentWeather current) {
        this.current = current;
    }
    
    public List<DailyForecast> getForecast() {
        return forecast;
    }
    
    public void setForecast(List<DailyForecast> forecast) {
        this.forecast = forecast;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public String getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    // Inner class for current weather
    public static class CurrentWeather {
        private double temperature;
        private double feelsLike;
        private int humidity;
        private double windSpeed;
        private String windDirection;
        private String condition;
        private String icon;
        private double visibility;
        private double uvIndex;
        
        // Constructors
        public CurrentWeather() {}
        
        public CurrentWeather(double temperature, double feelsLike, int humidity, double windSpeed, 
                            String windDirection, String condition, String icon, double visibility, double uvIndex) {
            this.temperature = temperature;
            this.feelsLike = feelsLike;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.windDirection = windDirection;
            this.condition = condition;
            this.icon = icon;
            this.visibility = visibility;
            this.uvIndex = uvIndex;
        }
        
        // Getters and Setters
        public double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
        
        public double getFeelsLike() {
            return feelsLike;
        }
        
        public void setFeelsLike(double feelsLike) {
            this.feelsLike = feelsLike;
        }
        
        public int getHumidity() {
            return humidity;
        }
        
        public void setHumidity(int humidity) {
            this.humidity = humidity;
        }
        
        public double getWindSpeed() {
            return windSpeed;
        }
        
        public void setWindSpeed(double windSpeed) {
            this.windSpeed = windSpeed;
        }
        
        public String getWindDirection() {
            return windDirection;
        }
        
        public void setWindDirection(String windDirection) {
            this.windDirection = windDirection;
        }
        
        public String getCondition() {
            return condition;
        }
        
        public void setCondition(String condition) {
            this.condition = condition;
        }
        
        public String getIcon() {
            return icon;
        }
        
        public void setIcon(String icon) {
            this.icon = icon;
        }
        
        public double getVisibility() {
            return visibility;
        }
        
        public void setVisibility(double visibility) {
            this.visibility = visibility;
        }
        
        public double getUvIndex() {
            return uvIndex;
        }
        
        public void setUvIndex(double uvIndex) {
            this.uvIndex = uvIndex;
        }
    }
    
    // Inner class for daily forecast
    public static class DailyForecast {
        private String date;
        private double maxTemp;
        private double minTemp;
        private String condition;
        private String icon;
        private int chanceOfRain;
        private double windSpeed;
        
        // Constructors
        public DailyForecast() {}
        
        public DailyForecast(String date, double maxTemp, double minTemp, String condition, 
                           String icon, int chanceOfRain, double windSpeed) {
            this.date = date;
            this.maxTemp = maxTemp;
            this.minTemp = minTemp;
            this.condition = condition;
            this.icon = icon;
            this.chanceOfRain = chanceOfRain;
            this.windSpeed = windSpeed;
        }
        
        // Getters and Setters
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
        
        public double getMaxTemp() {
            return maxTemp;
        }
        
        public void setMaxTemp(double maxTemp) {
            this.maxTemp = maxTemp;
        }
        
        public double getMinTemp() {
            return minTemp;
        }
        
        public void setMinTemp(double minTemp) {
            this.minTemp = minTemp;
        }
        
        public String getCondition() {
            return condition;
        }
        
        public void setCondition(String condition) {
            this.condition = condition;
        }
        
        public String getIcon() {
            return icon;
        }
        
        public void setIcon(String icon) {
            this.icon = icon;
        }
        
        public int getChanceOfRain() {
            return chanceOfRain;
        }
        
        public void setChanceOfRain(int chanceOfRain) {
            this.chanceOfRain = chanceOfRain;
        }
        
        public double getWindSpeed() {
            return windSpeed;
        }
        
        public void setWindSpeed(double windSpeed) {
            this.windSpeed = windSpeed;
        }
    }
}
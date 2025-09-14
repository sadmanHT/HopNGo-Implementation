package com.hopngo.tripplanning.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class JsonHelper {
    private static final ObjectMapper JSON = new ObjectMapper();
    
    public static String toJson(Object value) {
        try { 
            return value != null ? JSON.writeValueAsString(value) : null; 
        }
        catch (Exception e) { 
            throw new RuntimeException("JSON serialization failed", e); 
        }
    }
    
    public static List<Map<String, Object>> jsonToList(String json) {
        try { 
            return (json != null && !json.trim().isEmpty()) ? 
                JSON.readValue(json, new TypeReference<>() {}) : Collections.emptyList(); 
        }
        catch (Exception e) { 
            // Log the error and return empty list instead of throwing exception
            System.err.println("JSON parsing failed for: " + json + ", error: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    public static Map<String, Object> jsonToMap(String json) {
        try { 
            return (json != null && !json.trim().isEmpty()) ? 
                JSON.readValue(json, new TypeReference<>() {}) : Collections.emptyMap(); 
        }
        catch (Exception e) { 
            // Log the error and return empty map instead of throwing exception
            System.err.println("JSON parsing failed for: " + json + ", error: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}
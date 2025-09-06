package com.hopngo.market.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * JPA converter for handling JSONB database columns.
 * Converts between Java Map objects and JSON strings for database storage.
 */
@Converter
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonbConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert Map to JSON string", e);
            throw new RuntimeException("Failed to convert Map to JSON", e);
        }
    }
    
    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            logger.error("Failed to convert JSON string to Map", e);
            throw new RuntimeException("Failed to convert JSON to Map", e);
        }
    }
}
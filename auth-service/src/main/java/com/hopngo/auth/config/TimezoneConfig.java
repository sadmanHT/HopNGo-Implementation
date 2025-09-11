package com.hopngo.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Configuration class for timezone handling in the auth service.
 * Sets default timezone to Asia/Dhaka and configures proper ISO 8601 serialization.
 */
@Configuration
public class TimezoneConfig {

    public static final String DEFAULT_TIMEZONE = "Asia/Dhaka";
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of(DEFAULT_TIMEZONE);
    
    /**
     * Set the default timezone for the JVM to Asia/Dhaka
     */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
        System.setProperty("user.timezone", DEFAULT_TIMEZONE);
    }
    
    /**
     * Configure ObjectMapper to properly serialize dates with timezone information
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register JavaTimeModule for Java 8 time support
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // Configure ZonedDateTime to use ISO 8601 format
        javaTimeModule.addSerializer(java.time.ZonedDateTime.class, 
            new ZonedDateTimeSerializer(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        
        mapper.registerModule(javaTimeModule);
        
        // Disable writing dates as timestamps
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}
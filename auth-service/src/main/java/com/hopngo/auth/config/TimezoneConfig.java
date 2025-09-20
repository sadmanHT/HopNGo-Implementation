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
    
    // ObjectMapper configuration moved to JacksonConfig.java to avoid bean conflicts
}
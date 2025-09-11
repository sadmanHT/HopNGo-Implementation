package com.hopngo.booking.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Utility class for date and time operations with proper timezone handling.
 * All methods ensure dates are handled with Asia/Dhaka timezone by default.
 */
public class DateTimeUtil {

    public static final String DEFAULT_TIMEZONE = "Asia/Dhaka";
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of(DEFAULT_TIMEZONE);
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
    /**
     * Get current date/time in Bangladesh timezone
     */
    public static ZonedDateTime nowInBangladesh() {
        return ZonedDateTime.now(DEFAULT_ZONE_ID);
    }
    
    /**
     * Convert LocalDateTime to ZonedDateTime with Bangladesh timezone
     */
    public static ZonedDateTime toZonedDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(DEFAULT_ZONE_ID);
    }
    
    /**
     * Convert LocalDateTime to ISO 8601 string with timezone
     */
    public static String toISOString(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return toZonedDateTime(localDateTime).format(ISO_FORMATTER);
    }
    
    /**
     * Convert ZonedDateTime to ISO 8601 string
     */
    public static String toISOString(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.format(ISO_FORMATTER);
    }
    
    /**
     * Parse ISO 8601 string to ZonedDateTime
     */
    public static ZonedDateTime fromISOString(String isoString) {
        if (isoString == null || isoString.trim().isEmpty()) {
            return null;
        }
        return ZonedDateTime.parse(isoString, ISO_FORMATTER);
    }
    
    /**
     * Convert any temporal object to ZonedDateTime with Bangladesh timezone
     */
    public static ZonedDateTime toZonedDateTime(TemporalAccessor temporal) {
        if (temporal == null) {
            return null;
        }
        
        if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).withZoneSameInstant(DEFAULT_ZONE_ID);
        } else if (temporal instanceof LocalDateTime) {
            return ((LocalDateTime) temporal).atZone(DEFAULT_ZONE_ID);
        } else if (temporal instanceof Instant) {
            return ((Instant) temporal).atZone(DEFAULT_ZONE_ID);
        } else if (temporal instanceof LocalDate) {
            return ((LocalDate) temporal).atStartOfDay(DEFAULT_ZONE_ID);
        }
        
        throw new IllegalArgumentException("Unsupported temporal type: " + temporal.getClass());
    }
    
    /**
     * Convert ZonedDateTime to LocalDateTime in Bangladesh timezone
     */
    public static LocalDateTime toLocalDateTime(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.withZoneSameInstant(DEFAULT_ZONE_ID).toLocalDateTime();
    }
    
    /**
     * Check if a date is today in Bangladesh timezone
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalDate today = LocalDate.now(DEFAULT_ZONE_ID);
        LocalDate checkDate = dateTime.toLocalDate();
        return today.equals(checkDate);
    }
    
    /**
     * Check if a date is today in Bangladesh timezone
     */
    public static boolean isToday(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalDate today = LocalDate.now(DEFAULT_ZONE_ID);
        LocalDate checkDate = dateTime.withZoneSameInstant(DEFAULT_ZONE_ID).toLocalDate();
        return today.equals(checkDate);
    }
    
    /**
     * Get start of day in Bangladesh timezone
     */
    public static ZonedDateTime startOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(DEFAULT_ZONE_ID);
    }
    
    /**
     * Get end of day in Bangladesh timezone
     */
    public static ZonedDateTime endOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atTime(23, 59, 59, 999_999_999).atZone(DEFAULT_ZONE_ID);
    }
    
    /**
     * Format ZonedDateTime for display purposes
     */
    public static String formatForDisplay(ZonedDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTime.withZoneSameInstant(DEFAULT_ZONE_ID).format(formatter);
    }
    
    /**
     * Format LocalDateTime for display purposes (assumes Bangladesh timezone)
     */
    public static String formatForDisplay(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTime.format(formatter);
    }
}
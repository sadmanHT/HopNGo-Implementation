package com.hopngo.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for handling financial calculations with proper precision,
 * rounding, and timezone management.
 */
@Component
public class FinancialCalculationUtil {

    // Standard financial precision (2 decimal places for most currencies)
    public static final int DEFAULT_SCALE = 2;
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    // High precision for intermediate calculations
    public static final int CALCULATION_SCALE = 10;
    
    // Application timezone (configurable)
    public static final ZoneId APPLICATION_TIMEZONE = ZoneId.of("Asia/Dhaka");
    public static final ZoneId UTC_TIMEZONE = ZoneId.of("UTC");
    
    // Currency-specific scales
    private static final Map<String, Integer> CURRENCY_SCALES = new ConcurrentHashMap<>();
    
    static {
        // Initialize currency scales
        CURRENCY_SCALES.put("USD", 2);
        CURRENCY_SCALES.put("EUR", 2);
        CURRENCY_SCALES.put("GBP", 2);
        CURRENCY_SCALES.put("BDT", 2);
        CURRENCY_SCALES.put("JPY", 0); // Japanese Yen has no decimal places
        CURRENCY_SCALES.put("KRW", 0); // Korean Won has no decimal places
        CURRENCY_SCALES.put("BTC", 8); // Bitcoin has 8 decimal places
        CURRENCY_SCALES.put("ETH", 18); // Ethereum has 18 decimal places
    }
    
    /**
     * Normalize a monetary amount to the appropriate scale for the given currency.
     */
    public static BigDecimal normalizeAmount(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        
        int scale = getCurrencyScale(currencyCode);
        return amount.setScale(scale, DEFAULT_ROUNDING_MODE);
    }
    
    /**
     * Get the appropriate decimal scale for a currency.
     */
    public static int getCurrencyScale(String currencyCode) {
        return CURRENCY_SCALES.getOrDefault(currencyCode.toUpperCase(), DEFAULT_SCALE);
    }
    
    /**
     * Add two monetary amounts with proper precision handling.
     */
    public static BigDecimal addAmounts(BigDecimal amount1, BigDecimal amount2, String currencyCode) {
        if (amount1 == null) amount1 = BigDecimal.ZERO;
        if (amount2 == null) amount2 = BigDecimal.ZERO;
        
        // Perform calculation with high precision
        BigDecimal result = amount1.add(amount2);
        
        // Normalize to currency precision
        return normalizeAmount(result, currencyCode);
    }
    
    /**
     * Subtract two monetary amounts with proper precision handling.
     */
    public static BigDecimal subtractAmounts(BigDecimal amount1, BigDecimal amount2, String currencyCode) {
        if (amount1 == null) amount1 = BigDecimal.ZERO;
        if (amount2 == null) amount2 = BigDecimal.ZERO;
        
        BigDecimal result = amount1.subtract(amount2);
        return normalizeAmount(result, currencyCode);
    }
    
    /**
     * Multiply two amounts with proper precision handling.
     */
    public static BigDecimal multiplyAmounts(BigDecimal amount1, BigDecimal amount2, String currencyCode) {
        if (amount1 == null) amount1 = BigDecimal.ZERO;
        if (amount2 == null) amount2 = BigDecimal.ZERO;
        
        BigDecimal result = amount1.multiply(amount2);
        return normalizeAmount(result, currencyCode);
    }
    
    /**
     * Divide two amounts with proper precision handling.
     */
    public static BigDecimal divideAmounts(BigDecimal dividend, BigDecimal divisor, String currencyCode) {
        if (dividend == null) dividend = BigDecimal.ZERO;
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero or null divisor");
        }
        
        // Use high precision for division, then normalize
        BigDecimal result = dividend.divide(divisor, CALCULATION_SCALE, DEFAULT_ROUNDING_MODE);
        return normalizeAmount(result, currencyCode);
    }
    
    /**
     * Calculate percentage of an amount.
     */
    public static BigDecimal calculatePercentage(BigDecimal amount, BigDecimal percentage, String currencyCode) {
        if (amount == null) amount = BigDecimal.ZERO;
        if (percentage == null) percentage = BigDecimal.ZERO;
        
        BigDecimal percentageDecimal = percentage.divide(new BigDecimal("100"), CALCULATION_SCALE, DEFAULT_ROUNDING_MODE);
        BigDecimal result = amount.multiply(percentageDecimal);
        return normalizeAmount(result, currencyCode);
    }
    
    /**
     * Calculate compound interest.
     */
    public static BigDecimal calculateCompoundInterest(BigDecimal principal, BigDecimal rate, 
                                                      int periods, String currencyCode) {
        if (principal == null) principal = BigDecimal.ZERO;
        if (rate == null) rate = BigDecimal.ZERO;
        
        // A = P(1 + r)^n
        BigDecimal rateDecimal = rate.divide(new BigDecimal("100"), CALCULATION_SCALE, DEFAULT_ROUNDING_MODE);
        BigDecimal onePlusRate = BigDecimal.ONE.add(rateDecimal);
        
        // Calculate (1 + r)^n using BigDecimal.pow() for integer exponents
        BigDecimal compound = onePlusRate.pow(periods);
        BigDecimal result = principal.multiply(compound);
        
        return normalizeAmount(result, currencyCode);
    }
    
    /**
     * Calculate simple interest.
     */
    public static BigDecimal calculateSimpleInterest(BigDecimal principal, BigDecimal rate, 
                                                    int periods, String currencyCode) {
        if (principal == null) principal = BigDecimal.ZERO;
        if (rate == null) rate = BigDecimal.ZERO;
        
        // I = P * r * t
        BigDecimal rateDecimal = rate.divide(new BigDecimal("100"), CALCULATION_SCALE, DEFAULT_ROUNDING_MODE);
        BigDecimal periodsDecimal = new BigDecimal(periods);
        BigDecimal interest = principal.multiply(rateDecimal).multiply(periodsDecimal);
        
        return normalizeAmount(interest, currencyCode);
    }
    
    /**
     * Compare two monetary amounts with tolerance for floating-point precision issues.
     */
    public static boolean areAmountsEqual(BigDecimal amount1, BigDecimal amount2, String currencyCode) {
        if (amount1 == null && amount2 == null) return true;
        if (amount1 == null || amount2 == null) return false;
        
        BigDecimal normalized1 = normalizeAmount(amount1, currencyCode);
        BigDecimal normalized2 = normalizeAmount(amount2, currencyCode);
        
        return normalized1.compareTo(normalized2) == 0;
    }
    
    /**
     * Check if an amount is within a tolerance range.
     */
    public static boolean isWithinTolerance(BigDecimal amount1, BigDecimal amount2, 
                                          BigDecimal tolerance, String currencyCode) {
        if (amount1 == null || amount2 == null) return false;
        
        BigDecimal difference = subtractAmounts(amount1, amount2, currencyCode).abs();
        BigDecimal normalizedTolerance = normalizeAmount(tolerance, currencyCode);
        
        return difference.compareTo(normalizedTolerance) <= 0;
    }
    
    /**
     * Convert amount from cents/smallest unit to standard currency unit.
     */
    public static BigDecimal fromCents(long cents, String currencyCode) {
        int scale = getCurrencyScale(currencyCode);
        if (scale == 0) {
            // For currencies without decimal places (JPY, KRW)
            return new BigDecimal(cents);
        }
        
        BigDecimal divisor = BigDecimal.TEN.pow(scale);
        return new BigDecimal(cents).divide(divisor, scale, DEFAULT_ROUNDING_MODE);
    }
    
    /**
     * Convert amount to cents/smallest unit.
     */
    public static long toCents(BigDecimal amount, String currencyCode) {
        if (amount == null) return 0L;
        
        int scale = getCurrencyScale(currencyCode);
        if (scale == 0) {
            // For currencies without decimal places
            return amount.longValue();
        }
        
        BigDecimal multiplier = BigDecimal.TEN.pow(scale);
        return amount.multiply(multiplier).setScale(0, DEFAULT_ROUNDING_MODE).longValue();
    }
    
    /**
     * Format amount for display with proper currency formatting.
     */
    public static String formatAmount(BigDecimal amount, String currencyCode) {
        if (amount == null) return "0.00";
        
        BigDecimal normalized = normalizeAmount(amount, currencyCode);
        int scale = getCurrencyScale(currencyCode);
        
        if (scale == 0) {
            return String.format("%,.0f", normalized);
        } else {
            return String.format("%,." + scale + "f", normalized);
        }
    }
    
    // ==================== TIMEZONE UTILITIES ====================
    
    /**
     * Convert UTC timestamp to application timezone.
     */
    public static LocalDateTime utcToApplicationTime(LocalDateTime utcDateTime) {
        if (utcDateTime == null) return null;
        
        ZonedDateTime utcZoned = utcDateTime.atZone(UTC_TIMEZONE);
        return utcZoned.withZoneSameInstant(APPLICATION_TIMEZONE).toLocalDateTime();
    }
    
    /**
     * Convert application timezone to UTC.
     */
    public static LocalDateTime applicationTimeToUtc(LocalDateTime appDateTime) {
        if (appDateTime == null) return null;
        
        ZonedDateTime appZoned = appDateTime.atZone(APPLICATION_TIMEZONE);
        return appZoned.withZoneSameInstant(UTC_TIMEZONE).toLocalDateTime();
    }
    
    /**
     * Get current time in application timezone.
     */
    public static LocalDateTime getCurrentApplicationTime() {
        return LocalDateTime.now(APPLICATION_TIMEZONE);
    }
    
    /**
     * Get current time in UTC.
     */
    public static LocalDateTime getCurrentUtcTime() {
        return LocalDateTime.now(UTC_TIMEZONE);
    }
    
    /**
     * Convert Unix timestamp to LocalDateTime in application timezone.
     */
    public static LocalDateTime fromUnixTimestamp(long unixTimestamp) {
        Instant instant = Instant.ofEpochSecond(unixTimestamp);
        return LocalDateTime.ofInstant(instant, APPLICATION_TIMEZONE);
    }
    
    /**
     * Convert LocalDateTime to Unix timestamp (assumes application timezone).
     */
    public static long toUnixTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) return 0L;
        
        ZonedDateTime zoned = dateTime.atZone(APPLICATION_TIMEZONE);
        return zoned.toEpochSecond();
    }
    
    /**
     * Get start of day in application timezone.
     */
    public static LocalDateTime getStartOfDay(LocalDate date) {
        return date.atStartOfDay(APPLICATION_TIMEZONE).toLocalDateTime();
    }
    
    /**
     * Get end of day in application timezone.
     */
    public static LocalDateTime getEndOfDay(LocalDate date) {
        return date.atTime(23, 59, 59, 999_999_999);
    }
    
    /**
     * Check if two dates are the same day in application timezone.
     */
    public static boolean isSameDay(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) return false;
        
        LocalDate date1 = dateTime1.atZone(APPLICATION_TIMEZONE).toLocalDate();
        LocalDate date2 = dateTime2.atZone(APPLICATION_TIMEZONE).toLocalDate();
        
        return date1.equals(date2);
    }
    
    /**
     * Calculate business days between two dates (excluding weekends).
     */
    public static long calculateBusinessDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return 0L;
        if (startDate.isAfter(endDate)) return 0L;
        
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        long businessDays = 0;
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                businessDays++;
            }
            current = current.plusDays(1);
        }
        
        return businessDays;
    }
    
    /**
     * Format datetime for financial reports.
     */
    public static String formatForReport(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }
    
    /**
     * Format date for financial reports.
     */
    public static String formatDateForReport(LocalDate date) {
        if (date == null) return "";
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return date.format(formatter);
    }
    
    // ==================== VALIDATION UTILITIES ====================
    
    /**
     * Validate that an amount is positive.
     */
    public static boolean isPositiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Validate that an amount is non-negative.
     */
    public static boolean isNonNegativeAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Validate currency code.
     */
    public static boolean isValidCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return false;
        }
        
        try {
            Currency.getInstance(currencyCode.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            // Check if it's a cryptocurrency or custom currency
            return CURRENCY_SCALES.containsKey(currencyCode.toUpperCase());
        }
    }
    
    /**
     * Validate that an amount doesn't exceed maximum precision.
     */
    public static boolean hasValidPrecision(BigDecimal amount, String currencyCode) {
        if (amount == null) return true;
        
        int maxScale = getCurrencyScale(currencyCode);
        return amount.scale() <= maxScale;
    }
    
    /**
     * Validate amount range (not too large to cause overflow).
     */
    public static boolean isWithinValidRange(BigDecimal amount) {
        if (amount == null) return true;
        
        // Check for reasonable financial limits (adjust as needed)
        BigDecimal maxAmount = new BigDecimal("999999999999.99"); // ~1 trillion
        BigDecimal minAmount = new BigDecimal("-999999999999.99");
        
        return amount.compareTo(maxAmount) <= 0 && amount.compareTo(minAmount) >= 0;
    }
    
    // ==================== ROUNDING UTILITIES ====================
    
    /**
     * Round amount using banker's rounding (round half to even).
     */
    public static BigDecimal roundBankers(BigDecimal amount, String currencyCode) {
        if (amount == null) return BigDecimal.ZERO;
        
        int scale = getCurrencyScale(currencyCode);
        return amount.setScale(scale, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Round amount up to next currency unit.
     */
    public static BigDecimal roundUp(BigDecimal amount, String currencyCode) {
        if (amount == null) return BigDecimal.ZERO;
        
        int scale = getCurrencyScale(currencyCode);
        return amount.setScale(scale, RoundingMode.CEILING);
    }
    
    /**
     * Round amount down to currency unit.
     */
    public static BigDecimal roundDown(BigDecimal amount, String currencyCode) {
        if (amount == null) return BigDecimal.ZERO;
        
        int scale = getCurrencyScale(currencyCode);
        return amount.setScale(scale, RoundingMode.FLOOR);
    }
    
    /**
     * Apply custom rounding mode.
     */
    public static BigDecimal roundWithMode(BigDecimal amount, String currencyCode, RoundingMode roundingMode) {
        if (amount == null) return BigDecimal.ZERO;
        
        int scale = getCurrencyScale(currencyCode);
        return amount.setScale(scale, roundingMode);
    }
    
    // ==================== CONVERSION UTILITIES ====================
    
    /**
     * Convert between currencies (requires exchange rate).
     */
    public static BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, 
                                           String toCurrency, BigDecimal exchangeRate) {
        if (amount == null || exchangeRate == null) return BigDecimal.ZERO;
        
        BigDecimal converted = amount.multiply(exchangeRate);
        return normalizeAmount(converted, toCurrency);
    }
    
    /**
     * Calculate exchange rate from two amounts.
     */
    public static BigDecimal calculateExchangeRate(BigDecimal fromAmount, BigDecimal toAmount) {
        if (fromAmount == null || toAmount == null || fromAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Invalid amounts for exchange rate calculation");
        }
        
        return toAmount.divide(fromAmount, CALCULATION_SCALE, DEFAULT_ROUNDING_MODE);
    }
    
    // ==================== AGGREGATION UTILITIES ====================
    
    /**
     * Sum a list of amounts with proper precision.
     */
    public static BigDecimal sumAmounts(java.util.List<BigDecimal> amounts, String currencyCode) {
        if (amounts == null || amounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = amounts.stream()
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        return normalizeAmount(sum, currencyCode);
    }
    
    /**
     * Calculate average of amounts.
     */
    public static BigDecimal averageAmounts(java.util.List<BigDecimal> amounts, String currencyCode) {
        if (amounts == null || amounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = sumAmounts(amounts, currencyCode);
        BigDecimal count = new BigDecimal(amounts.size());
        
        return divideAmounts(sum, count, currencyCode);
    }
    
    /**
     * Find maximum amount in list.
     */
    public static BigDecimal maxAmount(java.util.List<BigDecimal> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return amounts.stream()
            .filter(java.util.Objects::nonNull)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Find minimum amount in list.
     */
    public static BigDecimal minAmount(java.util.List<BigDecimal> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return amounts.stream()
            .filter(java.util.Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }
}
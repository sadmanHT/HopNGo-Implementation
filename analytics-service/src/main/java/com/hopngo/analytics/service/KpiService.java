package com.hopngo.analytics.service;

import com.hopngo.analytics.dto.KpiResponse;
import com.hopngo.analytics.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KpiService {

    private static final Logger logger = LoggerFactory.getLogger(KpiService.class);

    private final EventRepository eventRepository;

    @Autowired
    public KpiService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public KpiResponse getDailyActiveUsers(LocalDateTime startDate, LocalDateTime endDate) {
        OffsetDateTime start = startDate.atOffset(ZoneOffset.UTC);
        OffsetDateTime end = endDate.atOffset(ZoneOffset.UTC);
        
        Long count = eventRepository.countDistinctUsersByEventTypeAndTimestampBetween(
                "user_login", start, end);
        
        Map<String, Object> data = new HashMap<>();
        data.put("activeUsers", count);
        
        return new KpiResponse("DAU", "daily", startDate, endDate, data);
    }

    public KpiResponse getBookingConversionMetrics(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating booking conversion metrics for period: {} to {}", startDate, endDate);
        
        LocalDateTime start = getStartDateTime(startDate, 7);
        LocalDateTime end = getEndDateTime(endDate);
        OffsetDateTime offsetStart = start.atOffset(ZoneOffset.UTC);
        OffsetDateTime offsetEnd = end.atOffset(ZoneOffset.UTC);
        
        Map<String, Object> data = new HashMap<>();
        
        // Get booking funnel metrics
        Long searchEvents = eventRepository.countByEventTypeAndTimestampBetween(
            "search_performed", offsetStart, offsetEnd);
        Long viewEvents = eventRepository.countByEventTypeAndTimestampBetween(
            "listing_viewed", offsetStart, offsetEnd);
        Long bookingAttempts = eventRepository.countByEventTypeAndTimestampBetween(
            "booking_initiated", offsetStart, offsetEnd);
        Long completedBookings = eventRepository.countByEventTypeAndTimestampBetween(
            "booking_completed", offsetStart, offsetEnd);
        
        data.put("search_events", searchEvents != null ? searchEvents : 0L);
        data.put("view_events", viewEvents != null ? viewEvents : 0L);
        data.put("booking_attempts", bookingAttempts != null ? bookingAttempts : 0L);
        data.put("completed_bookings", completedBookings != null ? completedBookings : 0L);
        
        // Calculate conversion rates
        if (searchEvents != null && searchEvents > 0) {
            data.put("search_to_view_rate", viewEvents != null ? (double) viewEvents / searchEvents : 0.0);
            data.put("search_to_booking_rate", bookingAttempts != null ? (double) bookingAttempts / searchEvents : 0.0);
            data.put("search_to_completion_rate", completedBookings != null ? (double) completedBookings / searchEvents : 0.0);
        }
        
        if (bookingAttempts != null && bookingAttempts > 0) {
            data.put("booking_completion_rate", completedBookings != null ? (double) completedBookings / bookingAttempts : 0.0);
        }
        
        return new KpiResponse("booking_conversion", "7d", start, end, data);
    }

    public KpiResponse getPaymentConversionMetrics(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating payment conversion metrics for period: {} to {}", startDate, endDate);
        
        LocalDateTime start = getStartDateTime(startDate, 7);
        LocalDateTime end = getEndDateTime(endDate);
        OffsetDateTime offsetStart = start.atOffset(ZoneOffset.UTC);
        OffsetDateTime offsetEnd = end.atOffset(ZoneOffset.UTC);
        
        Map<String, Object> data = new HashMap<>();
        
        // Get payment metrics
        Long paymentAttempts = eventRepository.countByEventTypeAndTimestampBetween(
            "payment_initiated", offsetStart, offsetEnd);
        Long successfulPayments = eventRepository.countByEventTypeAndTimestampBetween(
            "payment_completed", offsetStart, offsetEnd);
        Long failedPayments = eventRepository.countByEventTypeAndTimestampBetween(
            "payment_failed", offsetStart, offsetEnd);
        
        data.put("payment_attempts", paymentAttempts != null ? paymentAttempts : 0L);
        data.put("successful_payments", successfulPayments != null ? successfulPayments : 0L);
        data.put("failed_payments", failedPayments != null ? failedPayments : 0L);
        
        // Calculate success rate
        if (paymentAttempts != null && paymentAttempts > 0) {
            data.put("payment_success_rate", successfulPayments != null ? (double) successfulPayments / paymentAttempts : 0.0);
            data.put("payment_failure_rate", failedPayments != null ? (double) failedPayments / paymentAttempts : 0.0);
        }
        
        return new KpiResponse("payment_conversion", "7d", start, end, data);
    }

    public KpiResponse getChatEngagementMetrics(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating chat engagement metrics for period: {} to {}", startDate, endDate);
        
        LocalDateTime start = getStartDateTime(startDate, 7);
        LocalDateTime end = getEndDateTime(endDate);
        OffsetDateTime offsetStart = start.atOffset(ZoneOffset.UTC);
        OffsetDateTime offsetEnd = end.atOffset(ZoneOffset.UTC);
        
        Map<String, Object> data = new HashMap<>();
        
        // Get chat metrics
        Long chatSessions = eventRepository.countByEventTypeAndTimestampBetween(
            "chat_session_started", offsetStart, offsetEnd);
        Long messagesExchanged = eventRepository.countByEventTypeAndTimestampBetween(
            "message_sent", offsetStart, offsetEnd);
        Long activeUsers = eventRepository.countDistinctUsersByEventTypeAndTimestampBetween(
            "message_sent", offsetStart, offsetEnd);
        
        data.put("chat_sessions", chatSessions != null ? chatSessions : 0L);
        data.put("messages_exchanged", messagesExchanged != null ? messagesExchanged : 0L);
        data.put("active_chat_users", activeUsers != null ? activeUsers : 0L);
        
        // Calculate engagement metrics
        if (chatSessions != null && chatSessions > 0) {
            data.put("avg_messages_per_session", messagesExchanged != null ? (double) messagesExchanged / chatSessions : 0.0);
        }
        
        if (activeUsers != null && activeUsers > 0) {
            data.put("avg_messages_per_user", messagesExchanged != null ? (double) messagesExchanged / activeUsers : 0.0);
        }
        
        return new KpiResponse("chat_engagement", "7d", start, end, data);
    }

    public KpiResponse getRevenueMetrics(LocalDate startDate, LocalDate endDate, String groupBy) {
        logger.debug("Calculating revenue metrics for period: {} to {}, grouped by: {}", startDate, endDate, groupBy);
        
        LocalDateTime start = getStartDateTime(startDate, 30);
        LocalDateTime end = getEndDateTime(endDate);
        OffsetDateTime offsetStart = start.atOffset(ZoneOffset.UTC);
        OffsetDateTime offsetEnd = end.atOffset(ZoneOffset.UTC);
        
        Map<String, Object> data = new HashMap<>();
        
        // This would typically involve more complex queries to sum revenue from payment events
        // For now, we'll provide a basic structure
        Long totalTransactions = eventRepository.countByEventTypeAndTimestampBetween(
            "payment_completed", offsetStart, offsetEnd);
        
        data.put("total_transactions", totalTransactions != null ? totalTransactions : 0L);
        data.put("grouping", groupBy);
        
        // Add placeholder for revenue calculation
        // In a real implementation, you'd extract amount from event metadata
        data.put("total_revenue", 0.0);
        data.put("avg_transaction_value", 0.0);
        
        return new KpiResponse("revenue", "30d", start, end, data);
    }

    public KpiResponse getRetentionMetrics(LocalDate startDate, Integer periods) {
        logger.debug("Calculating retention metrics starting from: {} for {} periods", startDate, periods);
        
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        
        Map<String, Object> data = new HashMap<>();
        
        // Placeholder for cohort retention analysis
        // This would require more complex queries to track user return behavior
        data.put("cohort_start_date", start.toLocalDate());
        data.put("periods_analyzed", periods);
        data.put("retention_rates", new HashMap<String, Double>());
        
        return new KpiResponse("retention", periods + "_periods", start, start.plusDays(periods * 7), data);
    }

    public Map<String, Object> getKpiOverview(LocalDate startDate, LocalDate endDate) {
        logger.debug("Generating KPI overview for period: {} to {}", startDate, endDate);
        
        Map<String, Object> overview = new HashMap<>();
        
        // Get all major KPIs
        KpiResponse activeUsers = getDailyActiveUsers(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        KpiResponse bookingConversion = getBookingConversionMetrics(startDate, endDate);
        KpiResponse paymentConversion = getPaymentConversionMetrics(startDate, endDate);
        KpiResponse chatEngagement = getChatEngagementMetrics(startDate, endDate);
        
        overview.put("active_users", activeUsers.getData());
        overview.put("booking_conversion", bookingConversion.getData());
        overview.put("payment_conversion", paymentConversion.getData());
        overview.put("chat_engagement", chatEngagement.getData());
        overview.put("generated_at", LocalDateTime.now());
        
        return overview;
    }

    public KpiResponse getTrendMetrics(String metric, String period, String interval) {
        logger.debug("Calculating trend metrics for: {} over {} grouped by {}", metric, period, interval);
        
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = calculateStartFromPeriod(end, period);
        
        Map<String, Object> data = new HashMap<>();
        data.put("metric", metric);
        data.put("period", period);
        data.put("interval", interval);
        
        // Placeholder for trend calculation
        // This would involve time-series queries grouped by the specified interval
        data.put("trend_data", new HashMap<String, Object>());
        
        return new KpiResponse("trends_" + metric, period, start, end, data);
    }

    private LocalDateTime getStartDateTime(LocalDate startDate, int defaultDays) {
        return startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(defaultDays);
    }

    private LocalDateTime getEndDateTime(LocalDate endDate) {
        return endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
    }

    private LocalDateTime calculateStartFromPeriod(LocalDateTime end, String period) {
        return switch (period) {
            case "7d" -> end.minusDays(7);
            case "30d" -> end.minusDays(30);
            case "90d" -> end.minusDays(90);
            case "1y" -> end.minusYears(1);
            default -> end.minusDays(30);
        };
    }
}
package com.hopngo.analytics.service;

import com.hopngo.analytics.entity.*;
import com.hopngo.analytics.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProviderAnalyticsService {

    @Autowired
    private ProviderBookingsDailyRepository bookingsDailyRepository;

    @Autowired
    private ProviderResponseTimesRepository responseTimesRepository;

    @Autowired
    private ProviderListingFunnelRepository listingFunnelRepository;

    @Autowired
    private ProviderSlaConfigRepository slaConfigRepository;

    /**
     * Get comprehensive provider analytics summary
     */
    public Map<String, Object> getProviderSummary(String providerId, LocalDate fromDate, LocalDate toDate) {
        Map<String, Object> summary = new HashMap<>();
        
        // Bookings and revenue data
        List<ProviderBookingsDaily> bookingsData = bookingsDailyRepository.findByProviderIdAndDateBetween(
                providerId, fromDate, toDate);
        summary.put("bookingsData", formatBookingsData(bookingsData));
        summary.put("bookingsStats", calculateBookingsStats(bookingsData));
        
        // Response times data
        Optional<ProviderResponseTimes> responseTimesOpt = responseTimesRepository.findByProviderId(providerId);
        if (responseTimesOpt.isPresent()) {
            ProviderResponseTimes responseTimes = responseTimesOpt.get();
            summary.put("responseTimesData", formatResponseTimesData(responseTimes));
            summary.put("responseTimesStats", calculateResponseTimesStats(responseTimes));
        }
        
        // Funnel conversion data
        Optional<ProviderListingFunnel> funnelOpt = listingFunnelRepository.findByProviderId(providerId);
        if (funnelOpt.isPresent()) {
            ProviderListingFunnel funnel = funnelOpt.get();
            summary.put("funnelData", formatFunnelData(funnel));
            summary.put("funnelStats", calculateFunnelStats(funnel));
        }
        
        // SLA configuration and performance
        Optional<ProviderSlaConfig> slaConfigOpt = slaConfigRepository.findByProviderId(providerId);
        if (slaConfigOpt.isPresent()) {
            ProviderSlaConfig slaConfig = slaConfigOpt.get();
            summary.put("slaConfig", formatSlaConfig(slaConfig));
            summary.put("slaPerformance", calculateSlaPerformance(providerId, slaConfig, bookingsData, responseTimesOpt.orElse(null)));
        }
        
        return summary;
    }

    /**
     * Format bookings data for charts
     */
    private Map<String, Object> formatBookingsData(List<ProviderBookingsDaily> bookingsData) {
        Map<String, Object> formatted = new HashMap<>();
        
        List<String> dates = bookingsData.stream()
                .map(b -> b.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .collect(Collectors.toList());
        
        List<Integer> bookings = bookingsData.stream()
                .map(ProviderBookingsDaily::getBookings)
                .collect(Collectors.toList());
        
        List<Integer> cancellations = bookingsData.stream()
                .map(ProviderBookingsDaily::getCancellations)
                .collect(Collectors.toList());
        
        List<BigDecimal> revenue = bookingsData.stream()
                .map(b -> BigDecimal.valueOf(b.getRevenueMinor()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .collect(Collectors.toList());
        
        formatted.put("dates", dates);
        formatted.put("bookings", bookings);
        formatted.put("cancellations", cancellations);
        formatted.put("revenue", revenue);
        
        return formatted;
    }

    /**
     * Calculate bookings statistics
     */
    private Map<String, Object> calculateBookingsStats(List<ProviderBookingsDaily> bookingsData) {
        Map<String, Object> stats = new HashMap<>();
        
        if (bookingsData.isEmpty()) {
            stats.put("totalBookings", 0);
            stats.put("totalCancellations", 0);
            stats.put("totalRevenue", BigDecimal.ZERO);
            stats.put("avgBookingsPerDay", BigDecimal.ZERO);
            stats.put("cancellationRate", BigDecimal.ZERO);
            return stats;
        }
        
        int totalBookings = bookingsData.stream().mapToInt(ProviderBookingsDaily::getBookings).sum();
        int totalCancellations = bookingsData.stream().mapToInt(ProviderBookingsDaily::getCancellations).sum();
        long totalRevenueMinor = bookingsData.stream().mapToLong(ProviderBookingsDaily::getRevenueMinor).sum();
        
        BigDecimal totalRevenue = BigDecimal.valueOf(totalRevenueMinor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal avgBookingsPerDay = BigDecimal.valueOf(totalBookings).divide(BigDecimal.valueOf(bookingsData.size()), 2, RoundingMode.HALF_UP);
        BigDecimal cancellationRate = totalBookings > 0 ? 
                BigDecimal.valueOf(totalCancellations).divide(BigDecimal.valueOf(totalBookings), 4, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        
        stats.put("totalBookings", totalBookings);
        stats.put("totalCancellations", totalCancellations);
        stats.put("totalRevenue", totalRevenue);
        stats.put("avgBookingsPerDay", avgBookingsPerDay);
        stats.put("cancellationRate", cancellationRate);
        
        return stats;
    }

    /**
     * Format response times data
     */
    private Map<String, Object> formatResponseTimesData(ProviderResponseTimes responseTimes) {
        Map<String, Object> formatted = new HashMap<>();
        
        formatted.put("avgFirstReplySeconds", responseTimes.getAvgFirstReplySeconds());
        formatted.put("avgConversationReplySeconds", responseTimes.getAvgConversationReplySeconds());
        formatted.put("totalConversations", responseTimes.getTotalConversations());
        formatted.put("lastUpdated", responseTimes.getUpdatedAt());
        
        return formatted;
    }

    /**
     * Calculate response times statistics
     */
    private Map<String, Object> calculateResponseTimesStats(ProviderResponseTimes responseTimes) {
        Map<String, Object> stats = new HashMap<>();
        
        // Convert seconds to minutes for better readability
        BigDecimal avgFirstReplyMinutes = BigDecimal.valueOf(responseTimes.getAvgFirstReplySeconds())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal avgConversationReplyMinutes = BigDecimal.valueOf(responseTimes.getAvgConversationReplySeconds())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        
        stats.put("avgFirstReplyMinutes", avgFirstReplyMinutes);
        stats.put("avgConversationReplyMinutes", avgConversationReplyMinutes);
        stats.put("totalConversations", responseTimes.getTotalConversations());
        
        return stats;
    }

    /**
     * Format funnel data
     */
    private Map<String, Object> formatFunnelData(ProviderListingFunnel funnel) {
        Map<String, Object> formatted = new HashMap<>();
        
        formatted.put("impressions", funnel.getImpressions());
        formatted.put("detailViews", funnel.getDetailViews());
        formatted.put("addToCarts", funnel.getAddToCarts());
        formatted.put("bookings", funnel.getBookings());
        formatted.put("lastUpdated", funnel.getUpdatedAt());
        
        return formatted;
    }

    /**
     * Calculate funnel statistics
     */
    private Map<String, Object> calculateFunnelStats(ProviderListingFunnel funnel) {
        Map<String, Object> stats = new HashMap<>();
        
        Double impressionToDetailRate = funnel.calculateImpressionToDetailRate();
        Double detailToCartRate = funnel.calculateDetailToCartRate();
        Double cartToBookingRate = funnel.calculateCartToBookingRate();
        Double overallConversionRate = funnel.calculateOverallConversionRate();
        
        stats.put("impressionToDetailRate", impressionToDetailRate);
        stats.put("detailToCartRate", detailToCartRate);
        stats.put("cartToBookingRate", cartToBookingRate);
        stats.put("overallConversionRate", overallConversionRate);
        
        // Calculate drop-off rates
        stats.put("impressionDropOff", funnel.calculateImpressionDropOff());
        stats.put("detailDropOff", funnel.calculateDetailDropOff());
        stats.put("cartDropOff", funnel.calculateCartDropOff());
        
        return stats;
    }

    /**
     * Format SLA configuration
     */
    private Map<String, Object> formatSlaConfig(ProviderSlaConfig slaConfig) {
        Map<String, Object> formatted = new HashMap<>();
        
        formatted.put("targetResponseTimeSeconds", slaConfig.getTargetResponseTimeSec());
        formatted.put("targetResponseTimeMinutes", slaConfig.getTargetResponseTimeMinutes());
        formatted.put("targetBookingConversionRate", slaConfig.getTargetBookingConversionRate());
        formatted.put("targetMonthlyRevenue", slaConfig.getTargetMonthlyRevenue());
        
        return formatted;
    }

    /**
     * Calculate SLA performance
     */
    private Map<String, Object> calculateSlaPerformance(String providerId, ProviderSlaConfig slaConfig, 
                                                        List<ProviderBookingsDaily> bookingsData, 
                                                        ProviderResponseTimes responseTimes) {
        Map<String, Object> performance = new HashMap<>();
        
        // Response time SLA performance
        if (responseTimes != null) {
            boolean responseTimeMet = responseTimes.getAvgFirstReplySeconds() <= slaConfig.getTargetResponseTimeSec();
            String responseTimePerformance = slaConfig.calculateResponseTimePerformance(responseTimes.getAvgFirstReplySeconds());
            
            performance.put("responseTimeMet", responseTimeMet);
            performance.put("responseTimePerformance", responseTimePerformance);
        }
        
        // Booking conversion SLA performance (if funnel data available)
        Optional<ProviderListingFunnel> funnelOpt = listingFunnelRepository.findByProviderId(providerId);
        if (funnelOpt.isPresent()) {
            ProviderListingFunnel funnel = funnelOpt.get();
            Double actualConversionRate = funnel.calculateOverallConversionRate();
            boolean conversionRateMet = BigDecimal.valueOf(actualConversionRate).compareTo(slaConfig.getTargetBookingConversionRate()) >= 0;
            String conversionPerformance = slaConfig.calculateConversionRatePerformance(BigDecimal.valueOf(actualConversionRate));
            
            performance.put("conversionRateMet", conversionRateMet);
            performance.put("conversionRatePerformance", conversionPerformance);
            performance.put("actualConversionRate", actualConversionRate);
        }
        
        // Monthly revenue SLA performance
        if (!bookingsData.isEmpty()) {
            long totalRevenueMinor = bookingsData.stream().mapToLong(ProviderBookingsDaily::getRevenueMinor).sum();
            boolean revenueMet = totalRevenueMinor >= slaConfig.getTargetMonthlyRevenueMinor();
            String revenuePerformance = slaConfig.calculateRevenuePerformance(totalRevenueMinor);
            
            performance.put("revenueMet", revenueMet);
            performance.put("revenuePerformance", revenuePerformance);
        }
        
        // Overall SLA status
        String overallStatus = slaConfig.calculateSLAStatus(
                responseTimes != null ? responseTimes.getAvgFirstReplySeconds() : null,
                funnelOpt.map(ProviderListingFunnel::calculateOverallConversionRate).orElse(null),
                bookingsData.stream().mapToLong(ProviderBookingsDaily::getRevenueMinor).sum()
        );
        performance.put("overallStatus", overallStatus);
        
        return performance;
    }

    /**
     * Get provider performance trends
     */
    public Map<String, Object> getProviderTrends(String providerId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        List<ProviderBookingsDaily> recentData = bookingsDailyRepository.findByProviderIdAndDateBetween(
                providerId, startDate, endDate);
        
        Map<String, Object> trends = new HashMap<>();
        
        if (recentData.size() >= 2) {
            // Calculate trends
            ProviderBookingsDaily latest = recentData.get(recentData.size() - 1);
            ProviderBookingsDaily previous = recentData.get(recentData.size() - 2);
            
            trends.put("bookingsTrend", calculateTrend(previous.getBookings(), latest.getBookings()));
            trends.put("revenueTrend", calculateTrend(previous.getRevenueMinor(), latest.getRevenueMinor()));
            trends.put("cancellationsTrend", calculateTrend(previous.getCancellations(), latest.getCancellations()));
        }
        
        return trends;
    }

    /**
     * Calculate trend percentage
     */
    private BigDecimal calculateTrend(Number previous, Number current) {
        if (previous.doubleValue() == 0) {
            return current.doubleValue() > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        
        double change = (current.doubleValue() - previous.doubleValue()) / previous.doubleValue() * 100;
        return BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Record booking event for analytics
     */
    public void recordBookingEvent(String providerId, LocalDate date, boolean isCancellation, long revenueMinor) {
        ProviderBookingsDaily daily = bookingsDailyRepository.findByProviderIdAndDate(providerId, date)
                .orElse(new ProviderBookingsDaily(providerId, date));
        
        if (isCancellation) {
            daily.incrementCancellations();
        } else {
            daily.incrementBookings();
            daily.addRevenue(revenueMinor);
        }
        
        bookingsDailyRepository.save(daily);
    }

    /**
     * Record response time event
     */
    public void recordResponseTime(String providerId, int responseTimeSeconds, boolean isFirstReply) {
        ProviderResponseTimes responseTimes = responseTimesRepository.findByProviderId(providerId)
                .orElse(new ProviderResponseTimes(providerId));
        
        if (isFirstReply) {
            responseTimes.addFirstReplyTime(responseTimeSeconds);
        } else {
            responseTimes.addConversationResponseTime(responseTimeSeconds);
        }
        
        responseTimesRepository.save(responseTimes);
    }

    /**
     * Record funnel event
     */
    public void recordFunnelEvent(String providerId, String eventType) {
        ProviderListingFunnel funnel = listingFunnelRepository.findByProviderId(providerId)
                .orElse(new ProviderListingFunnel(providerId));
        
        switch (eventType.toLowerCase()) {
            case "impression":
                funnel.incrementImpressions();
                break;
            case "detail_view":
                funnel.incrementDetailViews();
                break;
            case "add_to_cart":
                funnel.incrementAddToCarts();
                break;
            case "booking":
                funnel.incrementBookings();
                break;
        }
        
        listingFunnelRepository.save(funnel);
    }
}
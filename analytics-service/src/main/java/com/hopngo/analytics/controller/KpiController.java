package com.hopngo.analytics.controller;

import com.hopngo.analytics.dto.KpiResponse;
import com.hopngo.analytics.service.KpiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics/kpi")
@Tag(name = "KPI Analytics", description = "Key Performance Indicators and metrics endpoints")
@Validated
public class KpiController {

    private final KpiService kpiService;

    @Autowired
    public KpiController(KpiService kpiService) {
        this.kpiService = kpiService;
    }

    @GetMapping("/users/active")
    @Operation(summary = "Get active users metrics", description = "Retrieve DAU, WAU, and MAU metrics")
    public ResponseEntity<KpiResponse> getActiveUsers(
            @Parameter(description = "Start date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        KpiResponse response = kpiService.getDailyActiveUsers(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversion/booking")
    @Operation(summary = "Get booking conversion rates", description = "Retrieve conversion funnel metrics for booking process")
    public ResponseEntity<KpiResponse> getBookingConversion(
            @Parameter(description = "Start date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        KpiResponse response = kpiService.getBookingConversionMetrics(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversion/payment")
    @Operation(summary = "Get payment conversion rates", description = "Retrieve payment success and failure metrics")
    public ResponseEntity<KpiResponse> getPaymentConversion(
            @Parameter(description = "Start date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        KpiResponse response = kpiService.getPaymentConversionMetrics(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/engagement/chat")
    @Operation(summary = "Get chat engagement metrics", description = "Retrieve chat usage and engagement statistics")
    public ResponseEntity<KpiResponse> getChatEngagement(
            @Parameter(description = "Start date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        KpiResponse response = kpiService.getChatEngagementMetrics(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get revenue metrics", description = "Retrieve revenue analytics and trends")
    public ResponseEntity<KpiResponse> getRevenueMetrics(
            @Parameter(description = "Start date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Grouping period: daily, weekly, monthly")
            @RequestParam(defaultValue = "daily") String groupBy) {
        
        KpiResponse response = kpiService.getRevenueMetrics(startDate, endDate, groupBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/retention")
    @Operation(summary = "Get user retention metrics", description = "Retrieve user retention rates and cohort analysis")
    public ResponseEntity<KpiResponse> getRetentionMetrics(
            @Parameter(description = "Start date for the cohort analysis")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "Number of periods to analyze (1-12)")
            @RequestParam(defaultValue = "6") 
            @Min(1) @Max(12) Integer periods) {
        
        KpiResponse response = kpiService.getRetentionMetrics(startDate, periods);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/overview")
    @Operation(summary = "Get KPI overview dashboard", description = "Retrieve a comprehensive overview of all key metrics")
    public ResponseEntity<Map<String, Object>> getKpiOverview(
            @Parameter(description = "Start date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date for the analysis period")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> overview = kpiService.getKpiOverview(startDate, endDate);
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/trends")
    @Operation(summary = "Get trending metrics", description = "Retrieve trending data for various metrics over time")
    public ResponseEntity<KpiResponse> getTrends(
            @Parameter(description = "Metric type: users, bookings, revenue, engagement")
            @RequestParam String metric,
            
            @Parameter(description = "Time period: 7d, 30d, 90d, 1y")
            @RequestParam(defaultValue = "30d") String period,
            
            @Parameter(description = "Grouping interval: hour, day, week, month")
            @RequestParam(defaultValue = "day") String interval) {
        
        KpiResponse response = kpiService.getTrendMetrics(metric, period, interval);
        return ResponseEntity.ok(response);
    }
}
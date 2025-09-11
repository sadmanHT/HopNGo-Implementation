package com.hopngo.analytics.controller;

import com.hopngo.analytics.service.ProviderAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotNull;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.PrintWriter;

@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "*")
public class ProviderAnalyticsController {

    @Autowired
    private ProviderAnalyticsService providerAnalyticsService;

    /**
     * Get comprehensive provider analytics summary
     * GET /analytics/provider/{providerId}/summary?from=2024-01-01&to=2024-01-31
     */
    @GetMapping("/provider/{providerId}/summary")
    public ResponseEntity<Map<String, Object>> getProviderSummary(
            @PathVariable String providerId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        try {
            // Validate date range
            if (fromDate.isAfter(toDate)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid date range",
                    "message", "From date must be before or equal to to date"
                ));
            }
            
            // Limit date range to prevent excessive data
            if (fromDate.isBefore(LocalDate.now().minusYears(1))) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Date range too old",
                    "message", "Date range cannot exceed 1 year from current date"
                ));
            }
            
            Map<String, Object> summary = providerAnalyticsService.getProviderSummary(providerId, fromDate, toDate);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "providerId", providerId,
                "dateRange", Map.of(
                    "from", fromDate.toString(),
                    "to", toDate.toString()
                ),
                "data", summary
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal server error",
                "message", "Failed to retrieve provider analytics: " + e.getMessage()
            ));
        }
    }

    /**
     * Get provider performance trends
     * GET /analytics/provider/{providerId}/trends?days=30
     */
    @GetMapping("/provider/{providerId}/trends")
    public ResponseEntity<Map<String, Object>> getProviderTrends(
            @PathVariable String providerId,
            @RequestParam(value = "days", defaultValue = "30") int days) {
        
        try {
            // Validate days parameter
            if (days < 1 || days > 365) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid days parameter",
                    "message", "Days must be between 1 and 365"
                ));
            }
            
            Map<String, Object> trends = providerAnalyticsService.getProviderTrends(providerId, days);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "providerId", providerId,
                "days", days,
                "data", trends
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal server error",
                "message", "Failed to retrieve provider trends: " + e.getMessage()
            ));
        }
    }

    /**
     * Record booking event for analytics
     * POST /analytics/provider/{providerId}/events/booking
     */
    @PostMapping("/provider/{providerId}/events/booking")
    public ResponseEntity<Map<String, Object>> recordBookingEvent(
            @PathVariable String providerId,
            @RequestBody Map<String, Object> eventData) {
        
        try {
            // Extract event data
            String dateStr = (String) eventData.get("date");
            Boolean isCancellation = (Boolean) eventData.getOrDefault("isCancellation", false);
            Number revenueMinor = (Number) eventData.getOrDefault("revenueMinor", 0);
            
            // Validate required fields
            if (dateStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required field",
                    "message", "Date field is required"
                ));
            }
            
            LocalDate date = LocalDate.parse(dateStr);
            
            providerAnalyticsService.recordBookingEvent(
                providerId, 
                date, 
                isCancellation, 
                revenueMinor.longValue()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Booking event recorded successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal server error",
                "message", "Failed to record booking event: " + e.getMessage()
            ));
        }
    }

    /**
     * Record response time event for analytics
     * POST /analytics/provider/{providerId}/events/response-time
     */
    @PostMapping("/provider/{providerId}/events/response-time")
    public ResponseEntity<Map<String, Object>> recordResponseTimeEvent(
            @PathVariable String providerId,
            @RequestBody Map<String, Object> eventData) {
        
        try {
            // Extract event data
            Number responseTimeSeconds = (Number) eventData.get("responseTimeSeconds");
            Boolean isFirstReply = (Boolean) eventData.getOrDefault("isFirstReply", true);
            
            // Validate required fields
            if (responseTimeSeconds == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required field",
                    "message", "responseTimeSeconds field is required"
                ));
            }
            
            providerAnalyticsService.recordResponseTime(
                providerId, 
                responseTimeSeconds.intValue(), 
                isFirstReply
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Response time event recorded successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal server error",
                "message", "Failed to record response time event: " + e.getMessage()
            ));
        }
    }

    /**
     * Record funnel event for analytics
     * POST /analytics/provider/{providerId}/events/funnel
     */
    @PostMapping("/provider/{providerId}/events/funnel")
    public ResponseEntity<Map<String, Object>> recordFunnelEvent(
            @PathVariable String providerId,
            @RequestBody Map<String, Object> eventData) {
        
        try {
            // Extract event data
            String eventType = (String) eventData.get("eventType");
            
            // Validate required fields
            if (eventType == null || eventType.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required field",
                    "message", "eventType field is required"
                ));
            }
            
            // Validate event type
            String[] validEventTypes = {"impression", "detail_view", "add_to_cart", "booking"};
            boolean isValidEventType = false;
            for (String validType : validEventTypes) {
                if (validType.equalsIgnoreCase(eventType.trim())) {
                    isValidEventType = true;
                    break;
                }
            }
            
            if (!isValidEventType) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid event type",
                    "message", "eventType must be one of: impression, detail_view, add_to_cart, booking"
                ));
            }
            
            providerAnalyticsService.recordFunnelEvent(providerId, eventType.trim());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Funnel event recorded successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal server error",
                "message", "Failed to record funnel event: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     * GET /analytics/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "provider-analytics",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Get analytics metadata
     * GET /analytics/metadata
     */
    @GetMapping("/metadata")
    public ResponseEntity<Map<String, Object>> getAnalyticsMetadata() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of(
                "supportedEventTypes", new String[]{"impression", "detail_view", "add_to_cart", "booking"},
                "maxDateRangeDays", 365,
                "defaultTrendsDays", 30,
                "slaTargets", Map.of(
                    "defaultResponseTimeSeconds", 1800,
                    "defaultConversionRate", 0.05,
                    "defaultMonthlyRevenue", 0
                )
            )
        ));
    }

    /**
     * Export analytics data
     * GET /analytics/provider/{providerId}/export?format=csv&startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59
     */
    @GetMapping("/provider/{providerId}/export")
    public void exportAnalyticsData(
            @PathVariable String providerId,
            @RequestParam @NotNull String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String metrics,
            HttpServletResponse response) throws IOException {
        
        try {
            // Validate format
            if (!"csv".equalsIgnoreCase(format) && !"json".equalsIgnoreCase(format)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Unsupported format. Supported formats: csv, json");
                return;
            }
            
            // Get analytics data
            LocalDate fromDate = startDate.toLocalDate();
            LocalDate toDate = endDate.toLocalDate();
            Map<String, Object> summary = providerAnalyticsService.getProviderSummary(providerId, fromDate, toDate);
            
            // Set response headers
            String filename = String.format("provider_%s_analytics_%s_to_%s.%s", 
                providerId, 
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                format.toLowerCase());
            
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            
            if ("csv".equalsIgnoreCase(format)) {
                exportAsCSV(response, summary);
            } else {
                exportAsJSON(response, summary);
            }
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error exporting data: " + e.getMessage());
        }
    }
    
    private void exportAsCSV(HttpServletResponse response, Map<String, Object> summary) throws IOException {
        response.setContentType("text/csv");
        PrintWriter writer = response.getWriter();
        
        // Write summary section
        writer.println("# Provider Analytics Summary");
        writer.println("Metric,Value");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) summary;
        if (data != null) {
            data.forEach((key, value) -> {
                writer.printf("%s,%s%n", key, value != null ? value.toString() : "");
            });
        }
        
        writer.flush();
    }
    
    private void exportAsJSON(HttpServletResponse response, Map<String, Object> summary) throws IOException {
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        
        // Create export object
        writer.println("{");
        writer.println("  \"exportTimestamp\": \"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\",");
        writer.println("  \"summary\": {");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) summary;
        if (data != null) {
            int count = 0;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                writer.printf("    \"%s\": %s", entry.getKey(), 
                    entry.getValue() instanceof String ? "\"" + entry.getValue() + "\"" : entry.getValue());
                if (++count < data.size()) writer.print(",");
                writer.println();
            }
        }
        
        writer.println("  }");
        writer.println("}");
        writer.flush();
    }
}
package com.hopngo.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "KPI response containing metrics and metadata")
public class KpiResponse {

    @Schema(description = "The type of KPI metric", example = "active_users")
    private String metricType;

    @Schema(description = "Time period for the metric", example = "30d")
    private String period;

    @Schema(description = "Start date of the analysis period")
    private LocalDateTime startDate;

    @Schema(description = "End date of the analysis period")
    private LocalDateTime endDate;

    @Schema(description = "The main metric data")
    private Map<String, Object> data;

    @Schema(description = "Additional metadata about the metric")
    private Map<String, Object> metadata;

    @Schema(description = "Timestamp when the metric was calculated")
    private LocalDateTime calculatedAt;

    public KpiResponse() {
        this.calculatedAt = LocalDateTime.now();
    }

    public KpiResponse(String metricType, String period, Map<String, Object> data) {
        this();
        this.metricType = metricType;
        this.period = period;
        this.data = data;
    }

    public KpiResponse(String metricType, String period, LocalDateTime startDate, LocalDateTime endDate, Map<String, Object> data) {
        this(metricType, period, data);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
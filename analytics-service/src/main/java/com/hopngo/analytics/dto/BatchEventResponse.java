package com.hopngo.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Batch event tracking response")
public class BatchEventResponse {

    @Schema(description = "Batch identifier", example = "batch_123e4567-e89b-12d3-a456-426614174000")
    private String batchId;

    @Schema(description = "Total number of events in the batch", example = "10")
    private int totalEvents;

    @Schema(description = "Number of successfully processed events", example = "8")
    private int successCount;

    @Schema(description = "Number of duplicate events", example = "1")
    private int duplicateCount;

    @Schema(description = "Number of failed events", example = "1")
    private int failedCount;

    @Schema(description = "Number of filtered events", example = "0")
    private int filteredCount;

    @Schema(description = "Individual event responses")
    private List<EventResponse> results;

    @Schema(description = "Server timestamp when batch was processed", example = "2024-01-15T10:30:00.123Z")
    private OffsetDateTime processedAt;

    @Schema(description = "Processing duration in milliseconds", example = "150")
    private long processingTimeMs;

    // Constructors
    public BatchEventResponse() {
        this.processedAt = OffsetDateTime.now();
    }

    public BatchEventResponse(String batchId, List<EventResponse> results) {
        this.batchId = batchId;
        this.results = results;
        this.processedAt = OffsetDateTime.now();
        calculateCounts();
    }

    // Calculate counts from results
    private void calculateCounts() {
        if (results == null) {
            return;
        }
        
        this.totalEvents = results.size();
        this.successCount = (int) results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        this.duplicateCount = (int) results.stream().filter(r -> "DUPLICATE".equals(r.getStatus())).count();
        this.failedCount = (int) results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        this.filteredCount = (int) results.stream().filter(r -> "FILTERED".equals(r.getStatus())).count();
    }

    // Getters and Setters
    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getFilteredCount() {
        return filteredCount;
    }

    public void setFilteredCount(int filteredCount) {
        this.filteredCount = filteredCount;
    }

    public List<EventResponse> getResults() {
        return results;
    }

    public void setResults(List<EventResponse> results) {
        this.results = results;
        calculateCounts();
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    @Override
    public String toString() {
        return "BatchEventResponse{" +
                "batchId='" + batchId + '\'' +
                ", totalEvents=" + totalEvents +
                ", successCount=" + successCount +
                ", duplicateCount=" + duplicateCount +
                ", failedCount=" + failedCount +
                ", filteredCount=" + filteredCount +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}
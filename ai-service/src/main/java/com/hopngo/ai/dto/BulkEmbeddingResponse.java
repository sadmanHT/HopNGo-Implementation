package com.hopngo.ai.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class BulkEmbeddingResponse {
    
    private String jobId; // Unique identifier for the bulk operation
    
    private String status; // "started", "in_progress", "completed", "failed"
    
    private int totalItems; // Total number of items to process
    
    private int processedItems; // Number of items processed so far
    
    private int successfulItems; // Number of items processed successfully
    
    private int failedItems; // Number of items that failed processing
    
    private double progressPercentage; // Progress as percentage (0-100)
    
    private LocalDateTime startTime; // When the operation started
    
    private LocalDateTime endTime; // When the operation completed (if finished)
    
    private String processingTime; // Total processing time
    
    private List<String> errors; // List of error messages
    
    private Map<String, Object> statistics; // Additional statistics
    
    private String message; // Human-readable status message
    
    // Constructors
    public BulkEmbeddingResponse() {
        this.startTime = LocalDateTime.now();
        this.status = "started";
    }
    
    public BulkEmbeddingResponse(String jobId, int totalItems) {
        this();
        this.jobId = jobId;
        this.totalItems = totalItems;
        this.progressPercentage = 0.0;
    }
    
    // Getters and Setters
    public String getJobId() {
        return jobId;
    }
    
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public int getTotalItems() {
        return totalItems;
    }
    
    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
        updateProgress();
    }
    
    public int getProcessedItems() {
        return processedItems;
    }
    
    public void setProcessedItems(int processedItems) {
        this.processedItems = processedItems;
        updateProgress();
    }
    
    public int getSuccessfulItems() {
        return successfulItems;
    }
    
    public void setSuccessfulItems(int successfulItems) {
        this.successfulItems = successfulItems;
    }
    
    public int getFailedItems() {
        return failedItems;
    }
    
    public void setFailedItems(int failedItems) {
        this.failedItems = failedItems;
    }
    
    public double getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public String getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(String processingTime) {
        this.processingTime = processingTime;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public Map<String, Object> getStatistics() {
        return statistics;
    }
    
    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    // Helper methods
    private void updateProgress() {
        if (totalItems > 0) {
            this.progressPercentage = (double) processedItems / totalItems * 100.0;
        }
    }
    
    public void incrementProcessed() {
        this.processedItems++;
        updateProgress();
    }
    
    public void incrementSuccessful() {
        this.successfulItems++;
    }
    
    public void incrementFailed() {
        this.failedItems++;
    }
    
    public boolean isCompleted() {
        return "completed".equals(status) || "failed".equals(status);
    }
    
    public void markCompleted() {
        this.status = "completed";
        this.endTime = LocalDateTime.now();
        this.progressPercentage = 100.0;
    }
    
    public void markFailed(String errorMessage) {
        this.status = "failed";
        this.endTime = LocalDateTime.now();
        this.message = errorMessage;
    }
    
    @Override
    public String toString() {
        return "BulkEmbeddingResponse{" +
                "jobId='" + jobId + '\'' +
                ", status='" + status + '\'' +
                ", totalItems=" + totalItems +
                ", processedItems=" + processedItems +
                ", successfulItems=" + successfulItems +
                ", failedItems=" + failedItems +
                ", progressPercentage=" + progressPercentage +
                ", message='" + message + '\'' +
                '}';
    }
}
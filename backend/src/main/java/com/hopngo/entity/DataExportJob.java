package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_export_jobs")
public class DataExportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ExportStatus status = ExportStatus.PENDING;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "download_url")
    private String downloadUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "export_type")
    @Enumerated(EnumType.STRING)
    private ExportType exportType = ExportType.FULL;

    @Column(name = "include_profile")
    private Boolean includeProfile = true;

    @Column(name = "include_bookings")
    private Boolean includeBookings = true;

    @Column(name = "include_orders")
    private Boolean includeOrders = true;

    @Column(name = "include_messages")
    private Boolean includeMessages = true;

    @Column(name = "include_activity_logs")
    private Boolean includeActivityLogs = true;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public DataExportJob() {}

    public DataExportJob(User user) {
        this.user = user;
        this.status = ExportStatus.PENDING;
        this.exportType = ExportType.FULL;
        // Set expiry to 7 days from creation
        this.expiresAt = LocalDateTime.now().plusDays(7);
    }

    public DataExportJob(User user, ExportType exportType) {
        this(user);
        this.exportType = exportType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ExportStatus getStatus() {
        return status;
    }

    public void setStatus(ExportStatus status) {
        this.status = status;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ExportType getExportType() {
        return exportType;
    }

    public void setExportType(ExportType exportType) {
        this.exportType = exportType;
    }

    public Boolean getIncludeProfile() {
        return includeProfile;
    }

    public void setIncludeProfile(Boolean includeProfile) {
        this.includeProfile = includeProfile;
    }

    public Boolean getIncludeBookings() {
        return includeBookings;
    }

    public void setIncludeBookings(Boolean includeBookings) {
        this.includeBookings = includeBookings;
    }

    public Boolean getIncludeOrders() {
        return includeOrders;
    }

    public void setIncludeOrders(Boolean includeOrders) {
        this.includeOrders = includeOrders;
    }

    public Boolean getIncludeMessages() {
        return includeMessages;
    }

    public void setIncludeMessages(Boolean includeMessages) {
        this.includeMessages = includeMessages;
    }

    public Boolean getIncludeActivityLogs() {
        return includeActivityLogs;
    }

    public void setIncludeActivityLogs(Boolean includeActivityLogs) {
        this.includeActivityLogs = includeActivityLogs;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isCompleted() {
        return status == ExportStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == ExportStatus.FAILED;
    }

    public boolean isProcessing() {
        return status == ExportStatus.PROCESSING;
    }

    public boolean isPending() {
        return status == ExportStatus.PENDING;
    }

    public void markAsStarted() {
        this.status = ExportStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
    }

    public void markAsCompleted(String filePath, String downloadUrl, Long fileSize) {
        this.status = ExportStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.filePath = filePath;
        this.downloadUrl = downloadUrl;
        this.fileSizeBytes = fileSize;
    }

    public void markAsFailed(String errorMessage) {
        this.status = ExportStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public String getFormattedFileSize() {
        if (fileSizeBytes == null) return "Unknown";
        
        long bytes = fileSizeBytes;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    @Override
    public String toString() {
        return "DataExportJob{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", status=" + status +
                ", exportType=" + exportType +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }

    // Enums
    public enum ExportStatus {
        PENDING("Pending", "Export request is queued for processing"),
        PROCESSING("Processing", "Export is currently being generated"),
        COMPLETED("Completed", "Export has been completed and is ready for download"),
        FAILED("Failed", "Export failed due to an error"),
        EXPIRED("Expired", "Export file has expired and is no longer available");

        private final String displayName;
        private final String description;

        ExportStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum ExportType {
        FULL("Full Export", "Complete user data export including all categories"),
        PROFILE_ONLY("Profile Only", "User profile information only"),
        BOOKINGS_ONLY("Bookings Only", "Booking history and related data only"),
        ORDERS_ONLY("Orders Only", "Order history and transaction data only"),
        CUSTOM("Custom", "Custom selection of data categories");

        private final String displayName;
        private final String description;

        ExportType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}
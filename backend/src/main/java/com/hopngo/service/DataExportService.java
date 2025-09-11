package com.hopngo.service;

import com.hopngo.entity.DataExportJob;
import com.hopngo.entity.DataExportJob.ExportStatus;
import com.hopngo.entity.DataExportJob.ExportType;
import com.hopngo.entity.User;
import com.hopngo.repository.DataExportJobRepository;
import com.hopngo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class DataExportService {

    private static final Logger logger = LoggerFactory.getLogger(DataExportService.class);

    @Autowired
    private DataExportJobRepository exportJobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.data-export.storage-path:/tmp/exports}")
    private String exportStoragePath;

    @Value("${app.data-export.max-concurrent-jobs:5}")
    private int maxConcurrentJobs;

    @Value("${app.data-export.retention-days:30}")
    private int retentionDays;

    /**
     * Request a new data export for a user
     */
    public DataExportJob requestDataExport(Long userId, ExportType exportType) {
        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check for existing pending/processing jobs
        List<DataExportJob> activeJobs = exportJobRepository.findByUserIdAndStatusIn(
                userId, Arrays.asList(ExportStatus.PENDING, ExportStatus.PROCESSING));
        
        if (!activeJobs.isEmpty()) {
            throw new IllegalStateException("User already has an active export job");
        }

        // Check concurrent job limit
        long processingCount = exportJobRepository.countByStatus(ExportStatus.PROCESSING);
        if (processingCount >= maxConcurrentJobs) {
            throw new IllegalStateException("Maximum concurrent export jobs reached. Please try again later.");
        }

        // Create new export job
        DataExportJob job = new DataExportJob();
        job.setUser(user);
        job.setExportType(exportType);
        job.setStatus(ExportStatus.PENDING);
        job.setRequestedAt(LocalDateTime.now());
        
        job = exportJobRepository.save(job);
        logger.info("Created data export job {} for user {}", job.getId(), userId);

        // Start async processing
        processDataExportAsync(job.getId());
        
        return job;
    }

    /**
     * Get export job status
     */
    public Optional<DataExportJob> getExportJob(Long jobId, Long userId) {
        return exportJobRepository.findByIdAndUserId(jobId, userId);
    }

    /**
     * Get user's export history
     */
    public List<DataExportJob> getUserExportHistory(Long userId, int limit) {
        return exportJobRepository.findByUserIdOrderByRequestedAtDesc(userId, limit);
    }

    /**
     * Download export file
     */
    public Resource downloadExportFile(Long jobId, Long userId) throws IOException {
        DataExportJob job = exportJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Export job not found"));

        if (job.getStatus() != ExportStatus.COMPLETED) {
            throw new IllegalStateException("Export job is not completed");
        }

        if (job.getFilePath() == null) {
            throw new IllegalStateException("Export file not available");
        }

        Path filePath = Paths.get(job.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("Export file no longer exists");
        }

        return new UrlResource(filePath.toUri());
    }

    /**
     * Process data export asynchronously
     */
    @Async
    public CompletableFuture<Void> processDataExportAsync(Long jobId) {
        try {
            DataExportJob job = exportJobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Export job not found"));

            // Update status to processing
            exportJobRepository.markAsStarted(jobId, LocalDateTime.now());
            logger.info("Started processing export job {}", jobId);

            // Create export directory if it doesn't exist
            Path exportDir = Paths.get(exportStoragePath);
            Files.createDirectories(exportDir);

            // Generate export file
            String fileName = generateExportFileName(job);
            Path filePath = exportDir.resolve(fileName);
            
            generateExportFile(job, filePath);

            // Update job with file path and completion
            exportJobRepository.markAsCompleted(jobId, filePath.toString(), LocalDateTime.now());
            logger.info("Completed export job {} with file {}", jobId, fileName);

        } catch (Exception e) {
            logger.error("Failed to process export job {}", jobId, e);
            exportJobRepository.markAsFailed(jobId, e.getMessage(), LocalDateTime.now());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Generate export file based on job type
     */
    private void generateExportFile(DataExportJob job, Path filePath) throws IOException {
        User user = job.getUser();
        Map<String, Object> exportData = new HashMap<>();

        // Basic user information
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("phoneNumber", user.getPhoneNumber());
        userInfo.put("dateOfBirth", user.getDateOfBirth());
        userInfo.put("createdAt", user.getCreatedAt());
        userInfo.put("updatedAt", user.getUpdatedAt());
        userInfo.put("isEmailVerified", user.getIsEmailVerified());
        userInfo.put("isPhoneVerified", user.getIsPhoneVerified());
        userInfo.put("role", user.getRole());
        exportData.put("user", userInfo);

        // Export metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("exportType", job.getExportType());
        metadata.put("exportedAt", LocalDateTime.now());
        metadata.put("jobId", job.getId());
        exportData.put("metadata", metadata);

        // TODO: Add more data based on export type
        switch (job.getExportType()) {
            case FULL:
                // Add bookings, reviews, preferences, etc.
                exportData.put("bookings", Collections.emptyList()); // Placeholder
                exportData.put("reviews", Collections.emptyList()); // Placeholder
                exportData.put("preferences", Collections.emptyMap()); // Placeholder
                break;
            case PERSONAL_DATA_ONLY:
                // Only personal information (already included above)
                break;
            case ACTIVITY_DATA_ONLY:
                // Add activity data
                exportData.put("bookings", Collections.emptyList()); // Placeholder
                exportData.put("searchHistory", Collections.emptyList()); // Placeholder
                break;
        }

        // Write to JSON file
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, exportData);
        }
    }

    /**
     * Generate unique export file name
     */
    private String generateExportFileName(DataExportJob job) {
        return String.format("user_%d_export_%s_%d.json", 
                job.getUser().getId(), 
                job.getExportType().name().toLowerCase(),
                job.getId());
    }

    /**
     * Clean up expired export files
     */
    @Transactional
    public void cleanupExpiredExports() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<DataExportJob> expiredJobs = exportJobRepository.findExpiredJobs(cutoffDate);
        
        for (DataExportJob job : expiredJobs) {
            try {
                // Delete file if exists
                if (job.getFilePath() != null) {
                    Path filePath = Paths.get(job.getFilePath());
                    Files.deleteIfExists(filePath);
                }
                
                // Delete job record
                exportJobRepository.delete(job);
                logger.info("Cleaned up expired export job {}", job.getId());
                
            } catch (Exception e) {
                logger.error("Failed to cleanup export job {}", job.getId(), e);
            }
        }
    }

    /**
     * Get export statistics
     */
    public Map<String, Object> getExportStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", exportJobRepository.count());
        stats.put("pendingJobs", exportJobRepository.countByStatus(ExportStatus.PENDING));
        stats.put("processingJobs", exportJobRepository.countByStatus(ExportStatus.PROCESSING));
        stats.put("completedJobs", exportJobRepository.countByStatus(ExportStatus.COMPLETED));
        stats.put("failedJobs", exportJobRepository.countByStatus(ExportStatus.FAILED));
        return stats;
    }

    /**
     * Cancel pending export job
     */
    public boolean cancelExportJob(Long jobId, Long userId) {
        Optional<DataExportJob> jobOpt = exportJobRepository.findByIdAndUserId(jobId, userId);
        if (jobOpt.isPresent()) {
            DataExportJob job = jobOpt.get();
            if (job.getStatus() == ExportStatus.PENDING) {
                exportJobRepository.markAsFailed(jobId, "Cancelled by user", LocalDateTime.now());
                return true;
            }
        }
        return false;
    }
}
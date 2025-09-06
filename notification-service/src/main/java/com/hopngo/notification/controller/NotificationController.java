package com.hopngo.notification.controller;

import com.hopngo.notification.entity.Notification;
import com.hopngo.notification.entity.NotificationStatus;
import com.hopngo.notification.entity.NotificationType;
import com.hopngo.notification.repository.NotificationRepository;
import com.hopngo.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification management and testing endpoints")
public class NotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @PostMapping("/test")
    @Operation(summary = "Send test notification", description = "Send a test email notification to verify the system is working")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @Valid @RequestBody TestNotificationRequest request) {
        
        logger.info("Received test notification request for email: {}", request.getEmail());
        
        try {
            notificationService.sendTestNotification(request.getEmail(), request.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test notification sent successfully");
            response.put("email", request.getEmail());
            response.put("timestamp", LocalDateTime.now());
            
            logger.info("Test notification sent successfully to: {}", request.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to send test notification to: {}", request.getEmail(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send test notification: " + e.getMessage());
            response.put("email", request.getEmail());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping
    @Operation(summary = "Get notifications", description = "Retrieve notifications with optional filtering")
    public ResponseEntity<Page<Notification>> getNotifications(
            @Parameter(description = "Recipient ID filter") @RequestParam(required = false) String recipientId,
            @Parameter(description = "Recipient email filter") @RequestParam(required = false) String recipientEmail,
            @Parameter(description = "Notification status filter") @RequestParam(required = false) NotificationStatus status,
            @Parameter(description = "Notification type filter") @RequestParam(required = false) NotificationType type,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Notification> notifications;
        
        if (recipientId != null) {
            notifications = notificationRepository.findByRecipientId(recipientId, pageable);
        } else if (recipientEmail != null) {
            notifications = notificationRepository.findByRecipientEmail(recipientEmail, pageable);
        } else if (status != null) {
            notifications = notificationRepository.findByStatus(status, pageable);
        } else if (type != null) {
            notifications = notificationRepository.findByType(type, pageable);
        } else {
            notifications = notificationRepository.findAll(pageable);
        }
        
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID", description = "Retrieve a specific notification by its ID")
    public ResponseEntity<Notification> getNotificationById(
            @Parameter(description = "Notification ID") @PathVariable Long id) {
        
        Optional<Notification> notification = notificationRepository.findById(id);
        
        if (notification.isPresent()) {
            return ResponseEntity.ok(notification.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get notification statistics", description = "Retrieve notification statistics and metrics")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        
        Map<String, Object> stats = new HashMap<>();
        
        // Count by status
        Map<String, Long> statusCounts = new HashMap<>();
        for (NotificationStatus status : NotificationStatus.values()) {
            long count = notificationRepository.countByStatus(status);
            statusCounts.put(status.name(), count);
        }
        stats.put("statusCounts", statusCounts);
        
        // Count by type
        Map<String, Long> typeCounts = new HashMap<>();
        for (NotificationType type : NotificationType.values()) {
            long count = notificationRepository.countByType(type);
            typeCounts.put(type.name(), count);
        }
        stats.put("typeCounts", typeCounts);
        
        // Recent activity (last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long recentCount = notificationRepository.countByCreatedAtAfter(yesterday);
        stats.put("recentNotifications", recentCount);
        
        // Failed notifications that need attention
        long failedCount = notificationRepository.countByStatus(NotificationStatus.FAILED);
        stats.put("failedNotifications", failedCount);
        
        // Pending retries
        long retryCount = notificationRepository.countByStatus(NotificationStatus.RETRY);
        stats.put("pendingRetries", retryCount);
        
        stats.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(stats);
    }
    
    @DeleteMapping("/cleanup")
    @Operation(summary = "Cleanup old notifications", description = "Remove old processed notifications to free up space")
    public ResponseEntity<Map<String, Object>> cleanupOldNotifications(
            @Parameter(description = "Days to keep (default: 30)") @RequestParam(defaultValue = "30") int daysToKeep) {
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        try {
            long deletedCount = notificationRepository.countByStatusAndCreatedAtBefore(
                NotificationStatus.SENT, cutoffDate);
            notificationRepository.deleteByStatusAndCreatedAtBefore(
                NotificationStatus.SENT, cutoffDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("cutoffDate", cutoffDate);
            response.put("timestamp", LocalDateTime.now());
            
            logger.info("Cleaned up {} old notifications older than {}", deletedCount, cutoffDate);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to cleanup old notifications", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to cleanup notifications: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // DTO for test notification request
    public static class TestNotificationRequest {
        
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;
        
        @NotBlank(message = "Message is required")
        private String message;
        
        // Constructors
        public TestNotificationRequest() {}
        
        public TestNotificationRequest(String email, String message) {
            this.email = email;
            this.message = message;
        }
        
        // Getters and setters
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
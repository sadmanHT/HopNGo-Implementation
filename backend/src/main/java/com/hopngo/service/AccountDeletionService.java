package com.hopngo.service;

import com.hopngo.entity.User;
import com.hopngo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class AccountDeletionService {

    private static final Logger logger = LoggerFactory.getLogger(AccountDeletionService.class);

    @Autowired
    private UserRepository userRepository;

    @Value("${app.account-deletion.grace-period-days:30}")
    private int gracePeriodDays;

    @Value("${app.account-deletion.hard-delete-after-days:90}")
    private int hardDeleteAfterDays;

    /**
     * Request account deletion (soft delete with grace period)
     */
    public boolean requestAccountDeletion(Long userId, String reason) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("Attempted to delete non-existent user: {}", userId);
            return false;
        }

        User user = userOpt.get();
        
        // Check if already scheduled for deletion
        if (user.getScheduledForDeletionAt() != null) {
            logger.info("User {} is already scheduled for deletion", userId);
            return true;
        }

        // Schedule for deletion after grace period
        LocalDateTime scheduledDeletionTime = LocalDateTime.now().plusDays(gracePeriodDays);
        user.setScheduledForDeletionAt(scheduledDeletionTime);
        user.setDeletionReason(reason);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        logger.info("User {} scheduled for deletion on {}", userId, scheduledDeletionTime);
        
        // TODO: Send confirmation email to user
        // TODO: Notify relevant services about pending deletion
        
        return true;
    }

    /**
     * Cancel account deletion request (during grace period)
     */
    public boolean cancelAccountDeletion(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        
        // Check if scheduled for deletion and still in grace period
        if (user.getScheduledForDeletionAt() == null) {
            logger.info("User {} is not scheduled for deletion", userId);
            return false;
        }

        if (user.getScheduledForDeletionAt().isBefore(LocalDateTime.now())) {
            logger.warn("Cannot cancel deletion for user {} - grace period expired", userId);
            return false;
        }

        // Cancel deletion
        user.setScheduledForDeletionAt(null);
        user.setDeletionReason(null);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        logger.info("Account deletion cancelled for user {}", userId);
        
        // TODO: Send cancellation confirmation email
        
        return true;
    }

    /**
     * Process pending account deletions (soft delete)
     */
    @Transactional
    public void processPendingDeletions() {
        LocalDateTime now = LocalDateTime.now();
        List<User> usersToDelete = userRepository.findUsersScheduledForDeletion(now);
        
        logger.info("Processing {} pending account deletions", usersToDelete.size());
        
        for (User user : usersToDelete) {
            try {
                softDeleteUser(user);
            } catch (Exception e) {
                logger.error("Failed to soft delete user {}", user.getId(), e);
            }
        }
    }

    /**
     * Soft delete user account (anonymize PII but keep record)
     */
    private void softDeleteUser(User user) {
        Long userId = user.getId();
        
        // Anonymize personal information
        user.setEmail("deleted_" + userId + "@deleted.local");
        user.setFirstName("[DELETED]");
        user.setLastName("[DELETED]");
        user.setPhoneNumber(null);
        user.setDateOfBirth(null);
        user.setProfilePictureUrl(null);
        
        // Mark as deleted
        user.setDeletedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // Clear sensitive flags
        user.setIsEmailVerified(false);
        user.setIsPhoneVerified(false);
        
        userRepository.save(user);
        
        logger.info("Soft deleted user {} - PII anonymized", userId);
        
        // TODO: Notify other services about user deletion
        // TODO: Cancel active bookings
        // TODO: Anonymize reviews and ratings
        // TODO: Clear user preferences and settings
        
        // Start async cleanup of related data
        cleanupUserDataAsync(userId);
    }

    /**
     * Hard delete users after retention period
     */
    @Transactional
    public void processHardDeletions() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(hardDeleteAfterDays);
        List<User> usersToHardDelete = userRepository.findUsersForHardDeletion(cutoffDate);
        
        logger.info("Processing {} hard deletions", usersToHardDelete.size());
        
        for (User user : usersToHardDelete) {
            try {
                hardDeleteUser(user);
            } catch (Exception e) {
                logger.error("Failed to hard delete user {}", user.getId(), e);
            }
        }
    }

    /**
     * Hard delete user record completely
     */
    private void hardDeleteUser(User user) {
        Long userId = user.getId();
        
        // TODO: Delete all related data from other services
        // TODO: Delete bookings, reviews, messages, etc.
        
        // Delete user record
        userRepository.delete(user);
        
        logger.info("Hard deleted user {} - all data removed", userId);
    }

    /**
     * Cleanup user-related data asynchronously
     */
    @Async
    public CompletableFuture<Void> cleanupUserDataAsync(Long userId) {
        try {
            logger.info("Starting async cleanup for user {}", userId);
            
            // TODO: Implement cleanup of related data
            // - Cancel active bookings
            // - Anonymize reviews and ratings
            // - Clear search history
            // - Remove from recommendation systems
            // - Clear cached data
            // - Notify analytics services
            
            Thread.sleep(1000); // Simulate cleanup work
            
            logger.info("Completed async cleanup for user {}", userId);
            
        } catch (Exception e) {
            logger.error("Failed async cleanup for user {}", userId, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get account deletion status
     */
    public AccountDeletionStatus getAccountDeletionStatus(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return new AccountDeletionStatus(false, null, null, null);
        }

        User user = userOpt.get();
        
        if (user.getDeletedAt() != null) {
            return new AccountDeletionStatus(true, user.getDeletedAt(), null, "Account has been deleted");
        }
        
        if (user.getScheduledForDeletionAt() != null) {
            boolean canCancel = user.getScheduledForDeletionAt().isAfter(LocalDateTime.now());
            return new AccountDeletionStatus(false, null, user.getScheduledForDeletionAt(), 
                    canCancel ? "Account scheduled for deletion - can be cancelled" : "Account deletion in progress");
        }
        
        return new AccountDeletionStatus(false, null, null, "Account is active");
    }

    /**
     * Get deletion statistics
     */
    public DeletionStatistics getDeletionStatistics() {
        long scheduledCount = userRepository.countUsersScheduledForDeletion();
        long softDeletedCount = userRepository.countSoftDeletedUsers();
        long totalActiveUsers = userRepository.countActiveUsers();
        
        return new DeletionStatistics(scheduledCount, softDeletedCount, totalActiveUsers);
    }

    /**
     * Account deletion status DTO
     */
    public static class AccountDeletionStatus {
        private final boolean isDeleted;
        private final LocalDateTime deletedAt;
        private final LocalDateTime scheduledForDeletionAt;
        private final String status;

        public AccountDeletionStatus(boolean isDeleted, LocalDateTime deletedAt, 
                                   LocalDateTime scheduledForDeletionAt, String status) {
            this.isDeleted = isDeleted;
            this.deletedAt = deletedAt;
            this.scheduledForDeletionAt = scheduledForDeletionAt;
            this.status = status;
        }

        public boolean isDeleted() { return isDeleted; }
        public LocalDateTime getDeletedAt() { return deletedAt; }
        public LocalDateTime getScheduledForDeletionAt() { return scheduledForDeletionAt; }
        public String getStatus() { return status; }
    }

    /**
     * Deletion statistics DTO
     */
    public static class DeletionStatistics {
        private final long scheduledForDeletion;
        private final long softDeleted;
        private final long activeUsers;

        public DeletionStatistics(long scheduledForDeletion, long softDeleted, long activeUsers) {
            this.scheduledForDeletion = scheduledForDeletion;
            this.softDeleted = softDeleted;
            this.activeUsers = activeUsers;
        }

        public long getScheduledForDeletion() { return scheduledForDeletion; }
        public long getSoftDeleted() { return softDeleted; }
        public long getActiveUsers() { return activeUsers; }
    }
}
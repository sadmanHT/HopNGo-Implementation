package com.hopngo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
@EnableAsync
@ConfigurationProperties(prefix = "app.data-retention")
public class DataRetentionConfig {

    private boolean enabled = true;
    private int exportFilesRetentionDays = 30;
    private int auditLogsRetentionDays = 365;
    private int sessionDataRetentionDays = 90;
    private int analyticsDataRetentionDays = 730;
    private int backupRetentionDays = 2555; // ~7 years
    private int maxConcurrentCleanupTasks = 3;
    private int cleanupThreadPoolSize = 5;

    /**
     * Task scheduler for scheduled cleanup jobs
     */
    @Bean(name = "dataRetentionTaskScheduler")
    public ThreadPoolTaskScheduler dataRetentionTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(cleanupThreadPoolSize);
        scheduler.setThreadNamePrefix("data-retention-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Task executor for async cleanup operations
     */
    @Bean(name = "dataRetentionTaskExecutor")
    public Executor dataRetentionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(maxConcurrentCleanupTasks);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("data-retention-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    // Getters and setters for configuration properties
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getExportFilesRetentionDays() {
        return exportFilesRetentionDays;
    }

    public void setExportFilesRetentionDays(int exportFilesRetentionDays) {
        this.exportFilesRetentionDays = exportFilesRetentionDays;
    }

    public int getAuditLogsRetentionDays() {
        return auditLogsRetentionDays;
    }

    public void setAuditLogsRetentionDays(int auditLogsRetentionDays) {
        this.auditLogsRetentionDays = auditLogsRetentionDays;
    }

    public int getSessionDataRetentionDays() {
        return sessionDataRetentionDays;
    }

    public void setSessionDataRetentionDays(int sessionDataRetentionDays) {
        this.sessionDataRetentionDays = sessionDataRetentionDays;
    }

    public int getAnalyticsDataRetentionDays() {
        return analyticsDataRetentionDays;
    }

    public void setAnalyticsDataRetentionDays(int analyticsDataRetentionDays) {
        this.analyticsDataRetentionDays = analyticsDataRetentionDays;
    }

    public int getBackupRetentionDays() {
        return backupRetentionDays;
    }

    public void setBackupRetentionDays(int backupRetentionDays) {
        this.backupRetentionDays = backupRetentionDays;
    }

    public int getMaxConcurrentCleanupTasks() {
        return maxConcurrentCleanupTasks;
    }

    public void setMaxConcurrentCleanupTasks(int maxConcurrentCleanupTasks) {
        this.maxConcurrentCleanupTasks = maxConcurrentCleanupTasks;
    }

    public int getCleanupThreadPoolSize() {
        return cleanupThreadPoolSize;
    }

    public void setCleanupThreadPoolSize(int cleanupThreadPoolSize) {
        this.cleanupThreadPoolSize = cleanupThreadPoolSize;
    }
}
package com.hopngo.notification.channel;

import com.hopngo.notification.entity.Notification;

/**
 * Interface for notification channels (Email, SMS, Push, etc.)
 */
public interface NotificationChannel {
    
    /**
     * Check if this channel supports the given channel type
     * @param channel the channel type (EMAIL, SMS, PUSH, etc.)
     * @return true if supported, false otherwise
     */
    boolean supports(String channel);
    
    /**
     * Send a notification through this channel
     * @param notification the notification to send
     * @throws Exception if sending fails
     */
    void send(Notification notification) throws Exception;
    
    /**
     * Get the name of this channel
     * @return channel name
     */
    String getChannelName();
    
    /**
     * Check if this channel is currently available
     * @return true if available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Get the maximum number of retries for this channel
     * @return max retries
     */
    int getMaxRetries();
    
    /**
     * Get the retry delay in milliseconds
     * @return retry delay
     */
    long getRetryDelayMs();
}
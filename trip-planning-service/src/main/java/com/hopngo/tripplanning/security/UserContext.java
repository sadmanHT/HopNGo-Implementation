package com.hopngo.tripplanning.security;

/**
 * Thread-local storage for user context information.
 * This class provides a way to store and retrieve the current user's ID
 * throughout the request processing lifecycle.
 */
public class UserContext {

    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();

    /**
     * Set the current user ID for the current thread
     * @param userId the user ID to set
     */
    public static void setUserId(String userId) {
        userIdHolder.set(userId);
    }

    /**
     * Get the current user ID for the current thread
     * @return the current user ID, or null if not set
     */
    public static String getUserId() {
        return userIdHolder.get();
    }

    /**
     * Check if a user ID is set for the current thread
     * @return true if user ID is set, false otherwise
     */
    public static boolean hasUserId() {
        return userIdHolder.get() != null;
    }

    /**
     * Clear the user context for the current thread.
     * This should be called at the end of request processing to prevent memory leaks.
     */
    public static void clear() {
        userIdHolder.remove();
    }

    /**
     * Get the current user ID and throw an exception if not set
     * @return the current user ID
     * @throws IllegalStateException if no user ID is set
     */
    public static String requireUserId() {
        String userId = getUserId();
        if (userId == null) {
            throw new IllegalStateException("No user ID found in context. This indicates a security configuration issue.");
        }
        return userId;
    }
}
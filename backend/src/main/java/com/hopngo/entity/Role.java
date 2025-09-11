package com.hopngo.entity;

/**
 * User roles in the HopNGo system
 */
public enum Role {
    USER("ROLE_USER", "Regular user"),
    PROVIDER("ROLE_PROVIDER", "Service provider"),
    ADMIN("ROLE_ADMIN", "System administrator"),
    MODERATOR("ROLE_MODERATOR", "Content moderator"),
    SUPPORT("ROLE_SUPPORT", "Customer support");

    private final String authority;
    private final String description;

    Role(String authority, String description) {
        this.authority = authority;
        this.description = description;
    }

    public String getAuthority() {
        return authority;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return authority;
    }
}
package com.hopngo.market.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an account in the ledger system.
 */
@Entity
@Table(name = "accounts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_type", "owner_id", "currency"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;
    
    @Column(name = "owner_id")
    private UUID ownerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type")
    private OwnerType ownerType;
    
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "BDT";
    
    @Column(name = "balance_minor", nullable = false)
    @Builder.Default
    private Long balanceMinor = 0L;
    
    @Column(name = "available_balance_minor", nullable = false)
    @Builder.Default
    private Long availableBalanceMinor = 0L;
    
    @Column(name = "reserved_balance_minor", nullable = false)
    @Builder.Default
    private Long reservedBalanceMinor = 0L;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Get balance as decimal for display.
     */
    public BigDecimal getBalanceDecimal() {
        return new BigDecimal(balanceMinor).divide(new BigDecimal("100"));
    }
    
    /**
     * Get available balance as decimal for display.
     */
    public BigDecimal getAvailableBalanceDecimal() {
        return new BigDecimal(availableBalanceMinor).divide(new BigDecimal("100"));
    }
    
    /**
     * Get reserved balance as decimal for display.
     */
    public BigDecimal getReservedBalanceDecimal() {
        return new BigDecimal(reservedBalanceMinor).divide(new BigDecimal("100"));
    }
    
    /**
     * Set balance from decimal amount.
     */
    public void setBalanceFromDecimal(BigDecimal amount) {
        this.balanceMinor = amount.multiply(new BigDecimal("100")).longValue();
    }
    
    /**
     * Set available balance from decimal amount.
     */
    public void setAvailableBalanceFromDecimal(BigDecimal amount) {
        this.availableBalanceMinor = amount.multiply(new BigDecimal("100")).longValue();
    }
    
    /**
     * Set reserved balance from decimal amount.
     */
    public void setReservedBalanceFromDecimal(BigDecimal amount) {
        this.reservedBalanceMinor = amount.multiply(new BigDecimal("100")).longValue();
    }
    
    /**
     * Credit the account (increase balance).
     */
    public void credit(long amountMinor) {
        this.balanceMinor += amountMinor;
        this.availableBalanceMinor += amountMinor;
    }
    
    /**
     * Debit the account (decrease balance).
     */
    public void debit(long amountMinor) {
        if (this.availableBalanceMinor < amountMinor) {
            throw new IllegalArgumentException("Insufficient available balance");
        }
        this.balanceMinor -= amountMinor;
        this.availableBalanceMinor -= amountMinor;
    }
    
    /**
     * Reserve funds (move from available to reserved).
     */
    public void reserve(long amountMinor) {
        if (this.availableBalanceMinor < amountMinor) {
            throw new IllegalArgumentException("Insufficient available balance to reserve");
        }
        this.availableBalanceMinor -= amountMinor;
        this.reservedBalanceMinor += amountMinor;
    }
    
    /**
     * Release reserved funds (move from reserved to available).
     */
    public void releaseReserved(long amountMinor) {
        if (this.reservedBalanceMinor < amountMinor) {
            throw new IllegalArgumentException("Insufficient reserved balance to release");
        }
        this.reservedBalanceMinor -= amountMinor;
        this.availableBalanceMinor += amountMinor;
    }
    
    /**
     * Capture reserved funds (remove from reserved and total balance).
     */
    public void captureReserved(long amountMinor) {
        if (this.reservedBalanceMinor < amountMinor) {
            throw new IllegalArgumentException("Insufficient reserved balance to capture");
        }
        this.reservedBalanceMinor -= amountMinor;
        this.balanceMinor -= amountMinor;
    }
    
    /**
     * Check if account has sufficient available balance.
     */
    public boolean hasSufficientBalance(long amountMinor) {
        return this.availableBalanceMinor >= amountMinor;
    }
    
    /**
     * Check if account is active.
     */
    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(this.status);
    }
    
    /**
     * Check if account belongs to a provider.
     */
    public boolean isProviderAccount() {
        return AccountType.PROVIDER.equals(this.accountType);
    }
    
    /**
     * Check if account belongs to a user.
     */
    public boolean isUserAccount() {
        return AccountType.USER.equals(this.accountType);
    }
    
    /**
     * Check if account is a platform account.
     */
    public boolean isPlatformAccount() {
        return AccountType.PLATFORM.equals(this.accountType);
    }
    
    /**
     * Validate balance consistency.
     */
    public boolean isBalanceConsistent() {
        return balanceMinor.equals(availableBalanceMinor + reservedBalanceMinor);
    }
    
    @PrePersist
    @PreUpdate
    private void validateAccount() {
        if (currency != null) {
            currency = currency.toUpperCase();
            if (currency.length() != 3) {
                throw new IllegalArgumentException("Currency code must be exactly 3 characters");
            }
        }
        
        if (balanceMinor < 0) {
            throw new IllegalArgumentException("Account balance cannot be negative");
        }
        
        if (availableBalanceMinor < 0) {
            throw new IllegalArgumentException("Available balance cannot be negative");
        }
        
        if (reservedBalanceMinor < 0) {
            throw new IllegalArgumentException("Reserved balance cannot be negative");
        }
        
        if (!isBalanceConsistent()) {
            throw new IllegalArgumentException("Balance consistency check failed: balance != available + reserved");
        }
    }
    
    /**
     * Account type enumeration.
     */
    public enum AccountType {
        PLATFORM,   // Platform revenue account
        PROVIDER,   // Service provider account
        USER,       // User account (for refunds, credits)
        ESCROW,     // Escrow account for holding funds
        RESERVE     // Reserve account for platform reserves
    }
    
    /**
     * Owner type enumeration.
     */
    public enum OwnerType {
        PLATFORM,   // Platform-owned account
        PROVIDER,   // Provider-owned account
        USER        // User-owned account
    }
    
    /**
     * Account status enumeration.
     */
    public enum AccountStatus {
        ACTIVE,     // Account is active and can be used
        SUSPENDED,  // Account is temporarily suspended
        CLOSED      // Account is permanently closed
    }
}
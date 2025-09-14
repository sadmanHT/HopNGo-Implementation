package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @NotNull
    @Column(name = "order_number", unique = true)
    private String orderNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status = OrderStatus.PENDING;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "platform_fee", precision = 19, scale = 4)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "provider_fee", precision = 19, scale = 4)
    private BigDecimal providerFee = BigDecimal.ZERO;

    @NotNull
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Constructors
    public Order() {}

    public Order(User user, String orderNumber, BigDecimal totalAmount, String currency) {
        this.user = user;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.currency = currency;
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

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        if (status == OrderStatus.COMPLETED && completedAt == null) {
            completedAt = LocalDateTime.now();
        } else if (status == OrderStatus.CANCELLED && cancelledAt == null) {
            cancelledAt = LocalDateTime.now();
        }
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getProviderFee() {
        return providerFee;
    }

    public void setProviderFee(BigDecimal providerFee) {
        this.providerFee = providerFee;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    // Helper methods
    public BigDecimal getNetAmount() {
        return totalAmount.subtract(platformFee).subtract(providerFee);
    }

    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public enum OrderStatus {
        PENDING("Pending", "Order is awaiting payment"),
        PROCESSING("Processing", "Order is being processed"),
        COMPLETED("Completed", "Order has been completed successfully"),
        CANCELLED("Cancelled", "Order has been cancelled"),
        REFUNDED("Refunded", "Order has been refunded"),
        DISPUTED("Disputed", "Order is under dispute");

        private final String displayName;
        private final String description;

        OrderStatus(String displayName, String description) {
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

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
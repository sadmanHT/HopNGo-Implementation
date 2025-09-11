package com.hopngo.analytics.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "points_ledger")
public class PointsLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    @Column(name = "points_amount", nullable = false)
    private Integer pointsAmount;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "source_id", length = 36)
    private String sourceId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public PointsLedger() {}

    public PointsLedger(String userId, TransactionType transactionType, Integer pointsAmount, 
                       Integer balanceAfter, String source) {
        this.userId = userId;
        this.transactionType = transactionType;
        this.pointsAmount = pointsAmount;
        this.balanceAfter = balanceAfter;
        this.source = source;
    }

    public PointsLedger(String userId, TransactionType transactionType, Integer pointsAmount, 
                       Integer balanceAfter, String source, String sourceId, String description) {
        this(userId, transactionType, pointsAmount, balanceAfter, source);
        this.sourceId = sourceId;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getPointsAmount() {
        return pointsAmount;
    }

    public void setPointsAmount(Integer pointsAmount) {
        this.pointsAmount = pointsAmount;
    }

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Business methods
    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isEarning() {
        return transactionType == TransactionType.EARNED && pointsAmount > 0;
    }

    public boolean isSpending() {
        return transactionType == TransactionType.REDEEMED && pointsAmount < 0;
    }

    public Integer getAbsoluteAmount() {
        return Math.abs(pointsAmount);
    }

    // Static factory methods
    public static PointsLedger createEarning(String userId, Integer points, Integer newBalance, 
                                           String source, String sourceId, String description) {
        PointsLedger ledger = new PointsLedger(userId, TransactionType.EARNED, points, newBalance, source, sourceId, description);
        return ledger;
    }

    public static PointsLedger createRedemption(String userId, Integer points, Integer newBalance, 
                                              String source, String sourceId, String description) {
        PointsLedger ledger = new PointsLedger(userId, TransactionType.REDEEMED, -Math.abs(points), newBalance, source, sourceId, description);
        return ledger;
    }

    public static PointsLedger createExpiration(String userId, Integer points, Integer newBalance, 
                                              String sourceId, String description) {
        PointsLedger ledger = new PointsLedger(userId, TransactionType.EXPIRED, -Math.abs(points), newBalance, "expiration", sourceId, description);
        return ledger;
    }

    public static PointsLedger createAdjustment(String userId, Integer points, Integer newBalance, 
                                              String reason, String description) {
        PointsLedger ledger = new PointsLedger(userId, TransactionType.ADJUSTED, points, newBalance, "adjustment");
        ledger.setSourceId(reason);
        ledger.setDescription(description);
        return ledger;
    }

    // Enum for transaction types
    public enum TransactionType {
        EARNED,
        REDEEMED,
        EXPIRED,
        ADJUSTED,
        REFERRAL_BONUS
    }

    @Override
    public String toString() {
        return "PointsLedger{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", transactionType=" + transactionType +
                ", pointsAmount=" + pointsAmount +
                ", balanceAfter=" + balanceAfter +
                ", source='" + source + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
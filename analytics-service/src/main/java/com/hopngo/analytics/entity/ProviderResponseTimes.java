package com.hopngo.analytics.entity;

import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "provider_response_times", 
    indexes = {
        @Index(name = "idx_provider_response_times_provider", columnList = "providerId"),
        @Index(name = "idx_provider_response_times_avg_reply", columnList = "avgFirstReplySec"),
        @Index(name = "idx_provider_response_times_updated", columnList = "updatedAt")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ProviderResponseTimes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false, unique = true, length = 36)
    @NotBlank(message = "Provider ID is required")
    private String providerId;

    @Column(name = "avg_first_reply_sec", nullable = false)
    @NotNull
    private Integer avgFirstReplySec = 0;

    @Column(name = "total_conversations", nullable = false)
    @NotNull
    private Integer totalConversations = 0;

    @Column(name = "total_response_time_sec", nullable = false)
    @NotNull
    private Long totalResponseTimeSec = 0L;

    @Column(name = "last_calculated_at")
    private OffsetDateTime lastCalculatedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @NotNull
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private OffsetDateTime updatedAt;

    // Constructors
    public ProviderResponseTimes() {}

    public ProviderResponseTimes(String providerId) {
        this.providerId = providerId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Integer getAvgFirstReplySec() {
        return avgFirstReplySec;
    }

    public Integer getAvgFirstReplySeconds() {
        return avgFirstReplySec;
    }

    public Integer getAvgConversationReplySeconds() {
        return avgFirstReplySec; // Using same metric for now
    }

    public void setAvgFirstReplySec(Integer avgFirstReplySec) {
        this.avgFirstReplySec = avgFirstReplySec;
    }

    public Integer getTotalConversations() {
        return totalConversations;
    }

    public void setTotalConversations(Integer totalConversations) {
        this.totalConversations = totalConversations;
    }

    public Long getTotalResponseTimeSec() {
        return totalResponseTimeSec;
    }

    public void setTotalResponseTimeSec(Long totalResponseTimeSec) {
        this.totalResponseTimeSec = totalResponseTimeSec;
    }

    public OffsetDateTime getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public void setLastCalculatedAt(OffsetDateTime lastCalculatedAt) {
        this.lastCalculatedAt = lastCalculatedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    public void addConversationResponseTime(Integer responseTimeSec) {
        this.totalConversations++;
        this.totalResponseTimeSec += responseTimeSec;
        this.avgFirstReplySec = (int) (this.totalResponseTimeSec / this.totalConversations);
        this.lastCalculatedAt = OffsetDateTime.now();
    }

    public void recalculateAverage() {
        if (totalConversations > 0) {
            this.avgFirstReplySec = (int) (this.totalResponseTimeSec / this.totalConversations);
        } else {
            this.avgFirstReplySec = 0;
        }
        this.lastCalculatedAt = OffsetDateTime.now();
    }

    public Double getAvgFirstReplyMinutes() {
        return avgFirstReplySec / 60.0;
    }

    public Double getAvgFirstReplyHours() {
        return avgFirstReplySec / 3600.0;
    }

    public boolean isWithinSLA(Integer targetResponseTimeSec) {
        return avgFirstReplySec <= targetResponseTimeSec;
    }

    public Double getSLAPerformancePercent(Integer targetResponseTimeSec) {
        if (targetResponseTimeSec == 0) return 100.0;
        return Math.max(0.0, ((targetResponseTimeSec.doubleValue() - avgFirstReplySec.doubleValue()) / targetResponseTimeSec.doubleValue()) * 100.0);
    }

    public void addFirstReplyTime(int responseTimeSec) {
        addConversationResponseTime(responseTimeSec);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderResponseTimes that = (ProviderResponseTimes) o;
        return Objects.equals(providerId, that.providerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId);
    }

    @Override
    public String toString() {
        return "ProviderResponseTimes{" +
                "id=" + id +
                ", providerId='" + providerId + '\'' +
                ", avgFirstReplySec=" + avgFirstReplySec +
                ", totalConversations=" + totalConversations +
                ", lastCalculatedAt=" + lastCalculatedAt +
                '}';
    }
}
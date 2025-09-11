package com.hopngo.analytics.entity;

import java.math.BigDecimal;
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
@Table(name = "provider_sla_config", 
    indexes = {
        @Index(name = "idx_provider_sla_config_provider", columnList = "providerId"),
        @Index(name = "idx_provider_sla_config_response_time", columnList = "targetResponseTimeSec")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ProviderSlaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false, unique = true, length = 36)
    @NotBlank(message = "Provider ID is required")
    private String providerId;

    @Column(name = "target_response_time_sec", nullable = false)
    @NotNull
    private Integer targetResponseTimeSec = 1800; // 30 minutes default

    @Column(name = "target_booking_conversion_rate", precision = 5, scale = 4)
    private BigDecimal targetBookingConversionRate = new BigDecimal("0.0500"); // 5% default

    @Column(name = "target_monthly_revenue_minor")
    private Long targetMonthlyRevenueMinor = 0L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @NotNull
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private OffsetDateTime updatedAt;

    // Constructors
    public ProviderSlaConfig() {}

    public ProviderSlaConfig(String providerId) {
        this.providerId = providerId;
    }

    public ProviderSlaConfig(String providerId, Integer targetResponseTimeSec, BigDecimal targetBookingConversionRate, Long targetMonthlyRevenueMinor) {
        this.providerId = providerId;
        this.targetResponseTimeSec = targetResponseTimeSec;
        this.targetBookingConversionRate = targetBookingConversionRate;
        this.targetMonthlyRevenueMinor = targetMonthlyRevenueMinor;
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

    public Integer getTargetResponseTimeSec() {
        return targetResponseTimeSec;
    }

    public void setTargetResponseTimeSec(Integer targetResponseTimeSec) {
        this.targetResponseTimeSec = targetResponseTimeSec;
    }

    public BigDecimal getTargetBookingConversionRate() {
        return targetBookingConversionRate;
    }

    public void setTargetBookingConversionRate(BigDecimal targetBookingConversionRate) {
        this.targetBookingConversionRate = targetBookingConversionRate;
    }

    public Long getTargetMonthlyRevenueMinor() {
        return targetMonthlyRevenueMinor;
    }

    public void setTargetMonthlyRevenueMinor(Long targetMonthlyRevenueMinor) {
        this.targetMonthlyRevenueMinor = targetMonthlyRevenueMinor;
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
    public Double getTargetResponseTimeMinutes() {
        return targetResponseTimeSec / 60.0;
    }

    public Double getTargetResponseTimeHours() {
        return targetResponseTimeSec / 3600.0;
    }

    public Double getTargetMonthlyRevenueInMajorUnits() {
        return targetMonthlyRevenueMinor / 100.0; // Convert cents to dollars
    }

    public void setTargetResponseTimeMinutes(Double minutes) {
        this.targetResponseTimeSec = (int) (minutes * 60);
    }

    public void setTargetResponseTimeHours(Double hours) {
        this.targetResponseTimeSec = (int) (hours * 3600);
    }

    public void setTargetMonthlyRevenueInMajorUnits(Double revenue) {
        this.targetMonthlyRevenueMinor = (long) (revenue * 100); // Convert dollars to cents
    }

    public String getSlaStatus(Integer actualResponseTimeSec) {
        if (actualResponseTimeSec <= targetResponseTimeSec) {
            return "MEETING";
        } else if (actualResponseTimeSec <= (targetResponseTimeSec * 1.2)) {
            return "WARNING";
        } else {
            return "FAILING";
        }
    }

    public Double getSlaPerformancePercent(Integer actualResponseTimeSec) {
        if (targetResponseTimeSec == 0) return 100.0;
        return Math.max(0.0, ((targetResponseTimeSec.doubleValue() - actualResponseTimeSec.doubleValue()) / targetResponseTimeSec.doubleValue()) * 100.0);
    }

    public Long getTargetMonthlyRevenue() {
        return targetMonthlyRevenueMinor;
    }

    public String calculateConversionRatePerformance(BigDecimal actualConversionRate) {
        if (actualConversionRate.compareTo(targetBookingConversionRate) >= 0) {
            return "MEETING";
        } else if (actualConversionRate.compareTo(targetBookingConversionRate.multiply(new BigDecimal("0.8"))) >= 0) {
            return "WARNING";
        } else {
            return "FAILING";
        }
    }

    public String calculateRevenuePerformance(long actualRevenue) {
        if (actualRevenue >= targetMonthlyRevenueMinor) {
            return "MEETING";
        } else if (actualRevenue >= (targetMonthlyRevenueMinor * 0.8)) {
            return "WARNING";
        } else {
            return "FAILING";
        }
    }

    public String calculateResponseTimePerformance(Integer actualResponseTimeSec) {
        if (actualResponseTimeSec <= targetResponseTimeSec) {
            return "MEETING";
        } else if (actualResponseTimeSec <= (targetResponseTimeSec * 1.2)) {
            return "WARNING";
        } else {
            return "FAILING";
        }
    }

    public String calculateSLAStatus(Integer responseTimeSec, Double conversionRate, long revenue) {
        int meetingCount = 0;
        int totalChecks = 0;
        
        if (responseTimeSec != null) {
            totalChecks++;
            if (responseTimeSec <= targetResponseTimeSec) meetingCount++;
        }
        
        if (conversionRate != null) {
            totalChecks++;
            if (BigDecimal.valueOf(conversionRate).compareTo(targetBookingConversionRate) >= 0) meetingCount++;
        }
        
        if (revenue > 0) {
            totalChecks++;
            if (revenue >= targetMonthlyRevenueMinor) meetingCount++;
        }
        
        if (totalChecks == 0) return "UNKNOWN";
        
        double percentage = (double) meetingCount / totalChecks;
        if (percentage >= 0.8) return "MEETING";
        else if (percentage >= 0.5) return "WARNING";
        else return "FAILING";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderSlaConfig that = (ProviderSlaConfig) o;
        return Objects.equals(providerId, that.providerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId);
    }

    @Override
    public String toString() {
        return "ProviderSlaConfig{" +
                "id=" + id +
                ", providerId='" + providerId + '\'' +
                ", targetResponseTimeSec=" + targetResponseTimeSec +
                ", targetBookingConversionRate=" + targetBookingConversionRate +
                ", targetMonthlyRevenueMinor=" + targetMonthlyRevenueMinor +
                '}';
    }
}
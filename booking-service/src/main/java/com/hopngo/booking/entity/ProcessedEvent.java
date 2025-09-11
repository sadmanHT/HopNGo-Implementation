package com.hopngo.booking.entity;

import com.hopngo.booking.entity.base.Auditable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ProcessedEvent extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "message_id", nullable = false, unique = true, length = 255)
    private String messageId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
    
    public ProcessedEvent(String messageId, String eventType) {
        this.messageId = messageId;
        this.eventType = eventType;
        this.processedAt = LocalDateTime.now();
    }
    
    // Manual getters/setters for Lombok fallback
    public String getMessageId() { return messageId; }
    public String getEventType() { return eventType; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
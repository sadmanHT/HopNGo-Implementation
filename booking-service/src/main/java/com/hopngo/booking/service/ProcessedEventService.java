package com.hopngo.booking.service;

import com.hopngo.booking.entity.ProcessedEvent;
import com.hopngo.booking.repository.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
public class ProcessedEventService {
    
    private final ProcessedEventRepository processedEventRepository;
    
    @Autowired
    public ProcessedEventService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }
    
    @Transactional(readOnly = true)
    public boolean isEventProcessed(String messageId) {
        return processedEventRepository.existsByMessageId(messageId);
    }
    
    public void markEventAsProcessed(String messageId, String eventType) {
        if (isEventProcessed(messageId)) {
            log.debug("Event {} already marked as processed", messageId);
            return;
        }
        
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setMessageId(messageId);
        processedEvent.setEventType(eventType);
        processedEvent.setProcessedAt(LocalDateTime.now());
        
        processedEventRepository.save(processedEvent);
        log.debug("Marked event {} as processed", messageId);
    }
    
    @Transactional(readOnly = true)
    public List<ProcessedEvent> findProcessedEventsOlderThan(LocalDateTime cutoffDate) {
        return processedEventRepository.findByProcessedAtBefore(cutoffDate);
    }
    
    public int cleanupOldProcessedEvents(LocalDateTime cutoffDate) {
        List<ProcessedEvent> oldEvents = findProcessedEventsOlderThan(cutoffDate);
        if (!oldEvents.isEmpty()) {
            processedEventRepository.deleteAll(oldEvents);
            log.info("Cleaned up {} old processed events", oldEvents.size());
        }
        return oldEvents.size();
    }
    
    @Transactional(readOnly = true)
    public long countProcessedEvents() {
        return processedEventRepository.count();
    }
}
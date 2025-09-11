package com.hopngo.analytics.service;

import com.hopngo.analytics.dto.BatchEventRequest;
import com.hopngo.analytics.dto.BatchEventResponse;
import com.hopngo.analytics.dto.EventRequest;
import com.hopngo.analytics.dto.EventResponse;
import com.hopngo.analytics.entity.Event;
import com.hopngo.analytics.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final PrivacyFilterService privacyFilterService;

    @Value("${analytics.privacy.enabled:true}")
    private boolean privacyFilterEnabled;

    @Value("${analytics.batch.max-size:100}")
    private int maxBatchSize;

    @Value("${analytics.deduplication.enabled:true}")
    private boolean deduplicationEnabled;

    @Autowired
    public EventService(EventRepository eventRepository, PrivacyFilterService privacyFilterService) {
        this.eventRepository = eventRepository;
        this.privacyFilterService = privacyFilterService;
    }

    /**
     * Process a single event
     */
    public EventResponse processEvent(EventRequest request, String clientIp) {
        try {
            logger.debug("Processing event: {}", request.getEventId());

            // Validate event request
            List<String> validationErrors = validateEventRequest(request);
            if (!validationErrors.isEmpty()) {
                return EventResponse.failed(request.getEventId(), "Validation failed", validationErrors);
            }

            // Check for duplicates if deduplication is enabled
            if (deduplicationEnabled && eventRepository.existsByEventId(request.getEventId())) {
                logger.debug("Duplicate event detected: {}", request.getEventId());
                return EventResponse.duplicate(request.getEventId());
            }

            // Apply privacy filters
            if (privacyFilterEnabled) {
                String filterReason = privacyFilterService.shouldFilterEvent(request, clientIp);
                if (filterReason != null) {
                    logger.debug("Event filtered: {} - {}", request.getEventId(), filterReason);
                    return EventResponse.filtered(request.getEventId(), filterReason);
                }
            }

            // Convert request to entity
            Event event = convertToEntity(request, clientIp);

            // Save event
            Event savedEvent = eventRepository.save(event);
            logger.debug("Event saved successfully: {}", savedEvent.getEventId());

            return EventResponse.success(request.getEventId());

        } catch (Exception e) {
            logger.error("Error processing event: {}", request.getEventId(), e);
            return EventResponse.failed(request.getEventId(), "Internal processing error: " + e.getMessage());
        }
    }

    /**
     * Process a batch of events
     */
    public BatchEventResponse processBatchEvents(BatchEventRequest batchRequest, String clientIp) {
        long startTime = System.currentTimeMillis();
        String batchId = batchRequest.getBatchId() != null ? batchRequest.getBatchId() : UUID.randomUUID().toString();
        
        logger.info("Processing batch: {} with {} events", batchId, batchRequest.getEvents().size());

        try {
            // Validate batch size
            if (batchRequest.getEvents().size() > maxBatchSize) {
                throw new IllegalArgumentException("Batch size exceeds maximum allowed: " + maxBatchSize);
            }

            // Process events in parallel for better performance
            List<CompletableFuture<EventResponse>> futures = batchRequest.getEvents().stream()
                    .map(eventRequest -> CompletableFuture.supplyAsync(() -> processEvent(eventRequest, clientIp)))
                    .collect(Collectors.toList());

            // Wait for all events to be processed
            List<EventResponse> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            BatchEventResponse response = new BatchEventResponse(batchId, results);
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            logger.info("Batch processed: {} - Success: {}, Duplicates: {}, Failed: {}, Filtered: {}", 
                    batchId, response.getSuccessCount(), response.getDuplicateCount(), 
                    response.getFailedCount(), response.getFilteredCount());

            return response;

        } catch (Exception e) {
            logger.error("Error processing batch: {}", batchId, e);
            throw new RuntimeException("Batch processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate event request
     */
    private List<String> validateEventRequest(EventRequest request) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(request.getEventId())) {
            errors.add("Event ID is required");
        }

        if (!StringUtils.hasText(request.getEventType())) {
            errors.add("Event type is required");
        }

        if (!StringUtils.hasText(request.getEventCategory())) {
            errors.add("Event category is required");
        }

        // Validate event type
        if (StringUtils.hasText(request.getEventType()) && !isValidEventType(request.getEventType())) {
            errors.add("Invalid event type: " + request.getEventType());
        }

        // Validate event category
        if (StringUtils.hasText(request.getEventCategory()) && !isValidEventCategory(request.getEventCategory())) {
            errors.add("Invalid event category: " + request.getEventCategory());
        }

        // Validate timestamp format if provided
        if (StringUtils.hasText(request.getTimestamp())) {
            try {
                OffsetDateTime.parse(request.getTimestamp(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (DateTimeParseException e) {
                errors.add("Invalid timestamp format. Use ISO 8601 format.");
            }
        }

        return errors;
    }

    /**
     * Convert EventRequest to Event entity
     */
    private Event convertToEntity(EventRequest request, String clientIp) {
        Event event = new Event();
        event.setEventId(request.getEventId());
        event.setEventType(request.getEventType());
        event.setEventCategory(request.getEventCategory());
        event.setUserId(request.getUserId());
        event.setSessionId(request.getSessionId());
        event.setPageUrl(request.getPageUrl());
        event.setReferrer(request.getReferrer());
        event.setUserAgent(request.getUserAgent());
        event.setEventData(request.getEventData());
        event.setMetadata(request.getMetadata());
        
        // Set IP address from client or request
        event.setIpAddress(clientIp);
        
        // Set timestamp - use client timestamp if provided, otherwise server timestamp
        if (StringUtils.hasText(request.getTimestamp())) {
            try {
                event.setCreatedAt(OffsetDateTime.parse(request.getTimestamp(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } catch (DateTimeParseException e) {
                // Fall back to server timestamp if client timestamp is invalid
                event.setCreatedAt(OffsetDateTime.now());
            }
        } else {
            event.setCreatedAt(OffsetDateTime.now());
        }

        return event;
    }

    /**
     * Validate event type
     */
    private boolean isValidEventType(String eventType) {
        Set<String> validTypes = Set.of(
            "page_view", "click", "form_submit", "purchase", "signup", "login", "logout",
            "search", "booking_start", "booking_complete", "payment_success", "payment_failed",
            "error", "custom"
        );
        return validTypes.contains(eventType.toLowerCase());
    }

    /**
     * Validate event category
     */
    private boolean isValidEventCategory(String eventCategory) {
        Set<String> validCategories = Set.of(
            "user_interaction", "navigation", "conversion", "engagement", "error", "system"
        );
        return validCategories.contains(eventCategory.toLowerCase());
    }

    /**
     * Get event by ID
     */
    @Transactional(readOnly = true)
    public Event getEventById(String eventId) {
        return eventRepository.findByEventId(eventId).orElse(null);
    }

    /**
     * Check if event exists
     */
    @Transactional(readOnly = true)
    public boolean eventExists(String eventId) {
        return eventRepository.existsByEventId(eventId);
    }
}
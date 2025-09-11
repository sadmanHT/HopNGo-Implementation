package com.hopngo.analytics.service;

import com.hopngo.analytics.dto.EventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PrivacyFilterService {

    private static final Logger logger = LoggerFactory.getLogger(PrivacyFilterService.class);

    @Value("${analytics.privacy.filter-bots:true}")
    private boolean filterBots;

    @Value("${analytics.privacy.filter-internal-ips:true}")
    private boolean filterInternalIps;

    @Value("${analytics.privacy.filter-test-events:true}")
    private boolean filterTestEvents;

    @Value("${analytics.privacy.require-user-consent:false}")
    private boolean requireUserConsent;

    // Common bot user agents patterns
    private static final Set<Pattern> BOT_PATTERNS = Set.of(
        Pattern.compile(".*bot.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*crawler.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*spider.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*scraper.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*googlebot.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*bingbot.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*slurp.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*duckduckbot.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*baiduspider.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*yandexbot.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*facebookexternalhit.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*twitterbot.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*linkedinbot.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*whatsapp.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*telegram.*", Pattern.CASE_INSENSITIVE)
    );

    // Internal/private IP ranges
    private static final Set<String> INTERNAL_IP_RANGES = Set.of(
        "127.0.0.1",
        "localhost",
        "10.",
        "192.168.",
        "172.16.", "172.17.", "172.18.", "172.19.", "172.20.",
        "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
        "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31."
    );

    // Test event patterns
    private static final Set<Pattern> TEST_EVENT_PATTERNS = Set.of(
        Pattern.compile(".*test.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*debug.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*dev.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*staging.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*localhost.*", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Determine if an event should be filtered based on privacy rules
     * 
     * @param request The event request
     * @param clientIp The client IP address
     * @return Filter reason if event should be filtered, null otherwise
     */
    public String shouldFilterEvent(EventRequest request, String clientIp) {
        try {
            // Filter bot traffic
            if (filterBots && isBotTraffic(request.getUserAgent())) {
                return "Bot traffic detected";
            }

            // Filter internal IP addresses
            if (filterInternalIps && isInternalIp(clientIp)) {
                return "Internal IP address";
            }

            // Filter test events
            if (filterTestEvents && isTestEvent(request)) {
                return "Test event detected";
            }

            // Check user consent (if required)
            if (requireUserConsent && !hasUserConsent(request)) {
                return "User consent required";
            }

            // Filter events with suspicious patterns
            if (hasSuspiciousPatterns(request)) {
                return "Suspicious event patterns";
            }

            return null; // Event should not be filtered

        } catch (Exception e) {
            logger.warn("Error in privacy filtering for event {}: {}", request.getEventId(), e.getMessage());
            return null; // Don't filter on errors, but log them
        }
    }

    /**
     * Check if the user agent indicates bot traffic
     */
    private boolean isBotTraffic(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return false;
        }

        return BOT_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(userAgent).matches());
    }

    /**
     * Check if the IP address is internal/private
     */
    private boolean isInternalIp(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return false;
        }

        // Check for localhost and private ranges
        return INTERNAL_IP_RANGES.stream()
                .anyMatch(range -> ipAddress.startsWith(range));
    }

    /**
     * Check if this is a test event
     */
    private boolean isTestEvent(EventRequest request) {
        // Check event ID
        if (StringUtils.hasText(request.getEventId()) && 
            TEST_EVENT_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(request.getEventId()).matches())) {
            return true;
        }

        // Check page URL
        if (StringUtils.hasText(request.getPageUrl()) && 
            TEST_EVENT_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(request.getPageUrl()).matches())) {
            return true;
        }

        // Check user ID
        if (StringUtils.hasText(request.getUserId()) && 
            TEST_EVENT_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(request.getUserId()).matches())) {
            return true;
        }

        // Check event data for test indicators
        if (request.getEventData() != null && request.getEventData().containsKey("test")) {
            return true;
        }

        return false;
    }

    /**
     * Check if user has given consent for tracking
     */
    private boolean hasUserConsent(EventRequest request) {
        // Check metadata for consent flag
        if (request.getMetadata() != null && request.getMetadata().containsKey("consent")) {
            Object consent = request.getMetadata().get("consent");
            return consent instanceof Boolean ? (Boolean) consent : false;
        }

        // Check event data for consent
        if (request.getEventData() != null && request.getEventData().containsKey("consent")) {
            Object consent = request.getEventData().get("consent");
            return consent instanceof Boolean ? (Boolean) consent : false;
        }

        // If consent is required but not provided, assume no consent
        return false;
    }

    /**
     * Check for suspicious event patterns that might indicate abuse
     */
    private boolean hasSuspiciousPatterns(EventRequest request) {
        // Check for extremely long event IDs (potential injection attempts)
        if (StringUtils.hasText(request.getEventId()) && request.getEventId().length() > 100) {
            return true;
        }

        // Check for suspicious characters in event type
        if (StringUtils.hasText(request.getEventType()) && 
            (request.getEventType().contains("<") || request.getEventType().contains(">") || 
             request.getEventType().contains("script") || request.getEventType().contains("javascript"))) {
            return true;
        }

        // Check for suspicious URLs
        if (StringUtils.hasText(request.getPageUrl()) && 
            (request.getPageUrl().contains("javascript:") || request.getPageUrl().contains("data:") ||
             request.getPageUrl().contains("<script"))) {
            return true;
        }

        return false;
    }

    /**
     * Anonymize sensitive data in event request
     */
    public EventRequest anonymizeEvent(EventRequest request) {
        // Create a copy to avoid modifying the original
        EventRequest anonymized = new EventRequest();
        anonymized.setEventId(request.getEventId());
        anonymized.setEventType(request.getEventType());
        anonymized.setEventCategory(request.getEventCategory());
        anonymized.setSessionId(request.getSessionId());
        anonymized.setTimestamp(request.getTimestamp());
        
        // Anonymize or remove sensitive fields
        anonymized.setUserId(anonymizeUserId(request.getUserId()));
        anonymized.setPageUrl(anonymizeUrl(request.getPageUrl()));
        anonymized.setReferrer(anonymizeUrl(request.getReferrer()));
        anonymized.setUserAgent(anonymizeUserAgent(request.getUserAgent()));
        
        // Keep event data and metadata as-is for now
        // In production, you might want to filter sensitive fields
        anonymized.setEventData(request.getEventData());
        anonymized.setMetadata(request.getMetadata());
        
        return anonymized;
    }

    private String anonymizeUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return userId;
        }
        // Hash or pseudonymize user ID
        return "user_" + Math.abs(userId.hashCode());
    }

    private String anonymizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        // Remove query parameters that might contain sensitive data
        int queryIndex = url.indexOf('?');
        return queryIndex > 0 ? url.substring(0, queryIndex) : url;
    }

    private String anonymizeUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return userAgent;
        }
        // Keep only browser and OS info, remove detailed version numbers
        // This is a simplified implementation
        return userAgent.replaceAll("\\d+\\.\\d+\\.\\d+", "x.x.x");
    }
}
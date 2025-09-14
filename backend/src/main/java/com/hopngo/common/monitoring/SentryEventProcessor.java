package com.hopngo.common.monitoring;

import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.SentryStackFrame;
import io.sentry.protocol.SentryStackTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom Sentry event processor to add context and implement rate limiting
 */
@Component
public class SentryEventProcessor implements EventProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(SentryEventProcessor.class);
    
    private final AtomicLong eventCounter = new AtomicLong(0);
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute
    private static final long MAX_EVENTS_PER_WINDOW = 100;
    
    @Override
    public SentryEvent process(SentryEvent event, Hint hint) {
        try {
            // Apply rate limiting
            if (!shouldProcessEvent()) {
                logger.debug("Sentry event dropped due to rate limiting");
                return null;
            }
            
            // Add custom tags
            addCustomTags(event);
            
            // Add environment context
            addEnvironmentContext(event);
            
            // Filter sensitive data
            filterSensitiveData(event);
            
            // Enhance stack traces
            enhanceStackTraces(event);
            
            // Set fingerprinting for better grouping
            setCustomFingerprint(event);
            
            return event;
            
        } catch (Exception e) {
            logger.error("Error processing Sentry event", e);
            return event; // Return original event if processing fails
        }
    }
    
    private boolean shouldProcessEvent() {
        long currentTime = System.currentTimeMillis();
        long lastReset = lastResetTime.get();
        
        // Reset counter if window has passed
        if (currentTime - lastReset > RATE_LIMIT_WINDOW_MS) {
            if (lastResetTime.compareAndSet(lastReset, currentTime)) {
                eventCounter.set(0);
            }
        }
        
        // Check if we're under the rate limit
        return eventCounter.incrementAndGet() <= MAX_EVENTS_PER_WINDOW;
    }
    
    private void addCustomTags(SentryEvent event) {
        event.setTag("service", "hopngo-backend");
        event.setTag("component", "api");
        event.setTag("version", getClass().getPackage().getImplementationVersion());
        
        // Add JVM information
        Runtime runtime = Runtime.getRuntime();
        event.setExtra("jvm.memory.used", runtime.totalMemory() - runtime.freeMemory());
        event.setExtra("jvm.memory.total", runtime.totalMemory());
        event.setExtra("jvm.memory.max", runtime.maxMemory());
        event.setExtra("jvm.processors", runtime.availableProcessors());
    }
    
    private void addEnvironmentContext(SentryEvent event) {
        // Add system properties (non-sensitive)
        event.setExtra("java.version", System.getProperty("java.version"));
        event.setExtra("os.name", System.getProperty("os.name"));
        event.setExtra("os.arch", System.getProperty("os.arch"));
        
        // Add thread information
        Thread currentThread = Thread.currentThread();
        event.setExtra("thread.name", currentThread.getName());
        event.setExtra("thread.id", currentThread.getId());
        event.setExtra("thread.priority", currentThread.getPriority());
    }
    
    private void filterSensitiveData(SentryEvent event) {
        // Filter sensitive data from extra context
        if (event.getExtras() != null) {
            event.getExtras().entrySet().removeIf(entry -> 
                isSensitiveKey(entry.getKey()) || isSensitiveValue(entry.getValue())
            );
        }
        
        // Filter sensitive data from tags
        if (event.getTags() != null) {
            event.getTags().entrySet().removeIf(entry -> 
                isSensitiveKey(entry.getKey()) || isSensitiveValue(entry.getValue())
            );
        }
    }
    
    private boolean isSensitiveKey(String key) {
        if (key == null) return false;
        
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("token") ||
               lowerKey.contains("key") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("credential") ||
               lowerKey.contains("auth");
    }
    
    private boolean isSensitiveValue(Object value) {
        if (value == null) return false;
        
        String stringValue = value.toString().toLowerCase();
        
        // Check for patterns that might be sensitive
        return stringValue.matches(".*[a-f0-9]{32,}.*") || // Hex strings (tokens, hashes)
               stringValue.matches(".*[A-Za-z0-9+/]{20,}={0,2}.*") || // Base64 strings
               stringValue.length() > 100; // Very long strings might contain sensitive data
    }
    
    private void enhanceStackTraces(SentryEvent event) {
        List<SentryException> exceptions = event.getExceptions();
        if (exceptions != null) {
            for (SentryException exception : exceptions) {
                SentryStackTrace stackTrace = exception.getStacktrace();
                if (stackTrace != null && stackTrace.getFrames() != null) {
                    for (SentryStackFrame frame : stackTrace.getFrames()) {
                        // Mark frames as in-app if they belong to our package
                        if (frame.getModule() != null && frame.getModule().startsWith("com.hopngo")) {
                            frame.setInApp(true);
                        }
                    }
                }
            }
        }
    }
    
    private void setCustomFingerprint(SentryEvent event) {
        List<SentryException> exceptions = event.getExceptions();
        if (exceptions != null && !exceptions.isEmpty()) {
            SentryException firstException = exceptions.get(0);
            String exceptionType = firstException.getType();
            
            // Create custom fingerprint for better grouping
            if (exceptionType != null) {
                // Group by exception type and first in-app stack frame
                SentryStackTrace stackTrace = firstException.getStacktrace();
                if (stackTrace != null && stackTrace.getFrames() != null) {
                    for (SentryStackFrame frame : stackTrace.getFrames()) {
                        if (frame.isInApp() != null && frame.isInApp()) {
                            event.setFingerprints(List.of(
                                exceptionType,
                                frame.getModule(),
                                frame.getFunction()
                            ));
                            break;
                        }
                    }
                }
            }
        }
    }
}
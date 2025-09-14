package com.hopngo.common.monitoring;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
import io.sentry.protocol.User;
import io.sentry.spring.boot.EnableSentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableSentry
public class SentryConfig {

    private static final Logger logger = LoggerFactory.getLogger(SentryConfig.class);

    @Value("${sentry.dsn:}")
    private String sentryDsn;

    @Value("${sentry.environment:development}")
    private String environment;

    @Value("${sentry.release:unknown}")
    private String release;

    @Value("${sentry.sample-rate:1.0}")
    private Double sampleRate;

    @Value("${sentry.traces-sample-rate:0.1}")
    private Double tracesSampleRate;

    @Value("${spring.application.name:hopngo-service}")
    private String serviceName;

    @PostConstruct
    public void initSentry() {
        if (sentryDsn == null || sentryDsn.isEmpty()) {
            logger.warn("Sentry DSN not configured. Error monitoring will be disabled.");
            return;
        }

        try {
            Sentry.init(options -> {
                options.setDsn(sentryDsn);
                options.setEnvironment(environment);
                options.setRelease(release);
                options.setSampleRate(sampleRate);
                options.setTracesSampleRate(tracesSampleRate);
                options.setServerName(serviceName);
                options.setAttachStacktrace(true);
                options.setAttachThreads(true);
                options.setSendDefaultPii(false); // Don't send PII by default
                
                // Configure tags
                options.setTag("service", serviceName);
                options.setTag("environment", environment);
                
                // Configure before send callback to add context
                options.setBeforeSend((event, hint) -> {
                    addRequestContext(event);
                    return event;
                });
                
                // Configure before breadcrumb callback
                options.setBeforeBreadcrumb((breadcrumb, hint) -> {
                    // Filter out noisy breadcrumbs
                    if (breadcrumb.getCategory() != null && 
                        (breadcrumb.getCategory().contains("health") || 
                         breadcrumb.getCategory().contains("metrics"))) {
                        return null;
                    }
                    return breadcrumb;
                });
            });
            
            logger.info("Sentry initialized successfully for service: {}, environment: {}", 
                       serviceName, environment);
        } catch (Exception e) {
            logger.error("Failed to initialize Sentry", e);
        }
    }

    /**
     * Add request context to Sentry event
     */
    private void addRequestContext(io.sentry.SentryEvent event) {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Add request context
                Map<String, Object> requestContext = new HashMap<>();
                requestContext.put("method", request.getMethod());
                requestContext.put("url", request.getRequestURL().toString());
                requestContext.put("query_string", request.getQueryString());
                requestContext.put("user_agent", request.getHeader("User-Agent"));
                requestContext.put("remote_addr", getClientIpAddress(request));
                
                event.setContext("request", requestContext);
                
                // Add trace context if available
                String traceId = request.getHeader("X-Trace-Id");
                if (traceId != null) {
                    event.setTag("trace_id", traceId);
                }
                
                // Add user context (hashed)
                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    User user = new User();
                    user.setId(hashUserId(userId));
                    event.setUser(user);
                }
                
                // Add session context
                String sessionId = request.getHeader("X-Session-Id");
                if (sessionId != null) {
                    event.setTag("session_id", hashUserId(sessionId));
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to add request context to Sentry event", e);
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Hash user ID for privacy
     */
    private String hashUserId(String userId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(userId.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString().substring(0, 16); // First 16 chars
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Failed to hash user ID", e);
            return "anonymous";
        }
    }

    /**
     * Utility method to capture exception with additional context
     */
    public static void captureException(Throwable throwable, Map<String, Object> extra) {
        Sentry.withScope(scope -> {
            if (extra != null) {
                extra.forEach(scope::setExtra);
            }
            Sentry.captureException(throwable);
        });
    }

    /**
     * Utility method to capture message with level
     */
    public static void captureMessage(String message, io.sentry.SentryLevel level, Map<String, Object> extra) {
        Sentry.withScope(scope -> {
            if (extra != null) {
                extra.forEach(scope::setExtra);
            }
            scope.setLevel(level);
            Sentry.captureMessage(message);
        });
    }

    /**
     * Add breadcrumb for tracking user actions
     */
    public static void addBreadcrumb(String message, String category, io.sentry.SentryLevel level) {
        Sentry.addBreadcrumb(message, category, level);
    }

    /**
     * Set user context for current scope
     */
    public static void setUserContext(String userId, String email, String ipAddress) {
        Sentry.configureScope(scope -> {
            User user = new User();
            user.setId(userId != null ? hashUserIdStatic(userId) : null);
            user.setEmail(email);
            user.setIpAddress(ipAddress);
            scope.setUser(user);
        });
    }

    private static String hashUserIdStatic(String userId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(userId.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString().substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "anonymous";
        }
    }
}
package com.hopngo.ai.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
public class ResilienceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ResilienceConfig.class);

    @Value("${ai.performance.circuit-breaker.failure-rate-threshold:50}")
    private float failureRateThreshold;

    @Value("${ai.performance.circuit-breaker.wait-duration-in-open-state:30s}")
    private Duration waitDurationInOpenState;

    @Value("${ai.performance.circuit-breaker.sliding-window-size:10}")
    private int slidingWindowSize;

    @Value("${ai.performance.request-timeout:30s}")
    private Duration requestTimeout;

    @Bean
    public CircuitBreaker aiServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(waitDurationInOpenState)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(
                    TimeoutException.class,
                    org.springframework.web.client.ResourceAccessException.class,
                    org.springframework.web.client.HttpServerErrorException.class
                )
                .ignoreExceptions(
                    IllegalArgumentException.class,
                    org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                    org.springframework.web.client.HttpClientErrorException.Unauthorized.class
                )
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("aiService", config);
        
        // Add event listeners for monitoring
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    logger.info("Circuit breaker state transition: {} -> {}", 
                        event.getStateTransition().getFromState(), 
                        event.getStateTransition().getToState()))
                .onCallNotPermitted(event -> 
                    logger.warn("Circuit breaker call not permitted: {}", event.getCircuitBreakerName()))
                .onError(event -> 
                    logger.error("Circuit breaker recorded error: {} - Duration: {}ms", 
                        event.getThrowable().getMessage(), 
                        event.getElapsedDuration().toMillis()))
                .onSuccess(event -> 
                    logger.debug("Circuit breaker recorded success - Duration: {}ms", 
                        event.getElapsedDuration().toMillis()));

        return circuitBreaker;
    }

    @Bean
    public Retry aiServiceRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(500), 2.0))
                .retryOnException(throwable -> 
                    throwable instanceof TimeoutException ||
                    throwable instanceof org.springframework.web.client.ResourceAccessException ||
                    (throwable instanceof org.springframework.web.client.HttpServerErrorException &&
                     ((org.springframework.web.client.HttpServerErrorException) throwable).getStatusCode().is5xxServerError())
                )
                .ignoreExceptions(
                    IllegalArgumentException.class,
                    org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                    org.springframework.web.client.HttpClientErrorException.Unauthorized.class,
                    org.springframework.web.client.HttpClientErrorException.TooManyRequests.class
                )
                .build();

        Retry retry = Retry.of("aiService", config);
        
        // Add event listeners for monitoring
        retry.getEventPublisher()
                .onRetry(event -> 
                    logger.warn("Retry attempt {} for operation: {}", 
                        event.getNumberOfRetryAttempts(), 
                        event.getName()))
                .onError(event -> 
                    logger.error("Retry failed after {} attempts: {}", 
                        event.getNumberOfRetryAttempts(), 
                        event.getLastThrowable().getMessage()))
                .onSuccess(event -> 
                    logger.info("Retry succeeded after {} attempts", 
                        event.getNumberOfRetryAttempts()));

        return retry;
    }

    @Bean
    public TimeLimiter aiServiceTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(requestTimeout)
                .cancelRunningFuture(true)
                .build();

        TimeLimiter timeLimiter = TimeLimiter.of("aiService", config);
        
        // Add event listeners for monitoring
        timeLimiter.getEventPublisher()
                .onTimeout(event -> 
                    logger.warn("Time limiter timeout for {}", 
                        event.getTimeLimiterName()))
                .onSuccess(event -> 
                    logger.debug("Time limiter success for {}", 
                        event.getTimeLimiterName()));

        return timeLimiter;
    }

    @Bean
    public CircuitBreaker imageSearchCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(60) // Higher threshold for image processing
                .waitDurationInOpenState(Duration.ofSeconds(45))
                .slidingWindowSize(8)
                .minimumNumberOfCalls(3)
                .permittedNumberOfCallsInHalfOpenState(2)
                .recordExceptions(
                    TimeoutException.class,
                    org.springframework.web.client.ResourceAccessException.class,
                    org.springframework.web.client.HttpServerErrorException.class
                )
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("imageSearch", config);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    logger.info("Image search circuit breaker state transition: {} -> {}", 
                        event.getStateTransition().getFromState(), 
                        event.getStateTransition().getToState()));

        return circuitBreaker;
    }

    @Bean
    public CircuitBreaker chatbotCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(40) // Lower threshold for chatbot (more critical)
                .waitDurationInOpenState(Duration.ofSeconds(20))
                .slidingWindowSize(12)
                .minimumNumberOfCalls(4)
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(
                    TimeoutException.class,
                    org.springframework.web.client.ResourceAccessException.class,
                    org.springframework.web.client.HttpServerErrorException.class
                )
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("chatbot", config);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    logger.info("Chatbot circuit breaker state transition: {} -> {}", 
                        event.getStateTransition().getFromState(), 
                        event.getStateTransition().getToState()));

        return circuitBreaker;
    }
}
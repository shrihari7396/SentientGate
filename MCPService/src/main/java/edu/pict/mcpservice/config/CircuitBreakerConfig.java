package edu.pict.mcpservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerConfig {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerConfig.class);

    /**
     * Configure the circuit breaker for AI Service Feign Client. - Failure rate threshold: 50% -
     * Minimum number of calls: 5 - Wait duration in open state: 10 seconds - Permitted number of
     * calls in half-open state: 3
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

        // Register metrics
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry)
                .bindTo(meterRegistry);

        // Add event consumer for logging
        circuitBreakerRegistry
                .getEventPublisher()
                .onEntryAdded(
                        event ->
                                logger.info(
                                        "CircuitBreaker added: {}",
                                        event.getAddedEntry().getName()))
                .onEntryRemoved(
                        event ->
                                logger.info(
                                        "CircuitBreaker removed: {}",
                                        event.getRemovedEntry().getName()));

        return circuitBreakerRegistry;
    }

    @Bean
    public CircuitBreaker aiServiceCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config =
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .failureRateThreshold(50.0f) // Open circuit when failure rate exceeds 50%
                        .slowCallRateThreshold(50.0f) // Slow call threshold
                        .slowCallDurationThreshold(
                                Duration.ofSeconds(
                                        2)) // Calls taking more than 2 seconds are considered slow
                        .waitDurationInOpenState(
                                Duration.ofSeconds(10)) // Wait 10 seconds before trying again
                        .permittedNumberOfCallsInHalfOpenState(
                                3) // Allow 3 calls in half-open state
                        .minimumNumberOfCalls(
                                5) // Minimum 5 calls before evaluating the failure rate
                        .automaticTransitionFromOpenToHalfOpenEnabled(
                                true) // Automatically transition to half-open
                        .recordExceptions(Exception.class) // Record all exceptions
                        .ignoreExceptions() // Don't ignore any exceptions by default
                        .build();

        return circuitBreakerRegistry.circuitBreaker("ai-service-circuit-breaker", config);
    }
}

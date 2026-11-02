package com.ieltspath.commonresilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Applies retry, circuit breaking, and an optional semaphore bulkhead to synchronous HTTP calls. */
public class ResilientCall {
    private final CircuitBreakerRegistry breakers;
    private final RetryRegistry retries;
    private final BulkheadRegistry bulkheads;

    public ResilientCall(CircuitBreakerRegistry breakers, RetryRegistry retries, BulkheadRegistry bulkheads) {
        this.breakers = Objects.requireNonNull(breakers);
        this.retries = Objects.requireNonNull(retries);
        this.bulkheads = Objects.requireNonNull(bulkheads);
    }

    /** Creates an isolated instance for direct adapter tests that do not load Spring configuration. */
    public static ResilientCall isolatedDefault() {
        CircuitBreakerConfig breaker = CircuitBreakerConfig.custom()
                .recordException(ResilientCall::isTransientFailure).build();
        RetryConfig retry = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(200))
                .retryOnException(ResilientCall::isTransientFailure)
                .build();
        return new ResilientCall(CircuitBreakerRegistry.of(breaker), RetryRegistry.of(retry),
                BulkheadRegistry.ofDefaults());
    }

    public <T> T call(String target, CallKind kind, Supplier<T> request) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(request);
        CircuitBreaker breaker = breakers.circuitBreaker(target);
        Supplier<T> guarded = request;
        if ("llm".equals(target)
                || bulkheads.getAllBulkheads().stream().anyMatch(bulkhead -> bulkhead.getName().equals(target))) {
            Bulkhead bulkhead = bulkheads.bulkhead(target);
            guarded = Bulkhead.decorateSupplier(bulkhead, guarded);
        }
        Supplier<T> protectedRequest = CircuitBreaker.decorateSupplier(breaker, guarded);
        try {
            if (kind == CallKind.WRITE) return protectedRequest.get();
            Retry retry = retries.retry(target);
            return Retry.decorateSupplier(retry, protectedRequest).get();
        } catch (CallNotPermittedException exception) {
            throw new DownstreamUnavailableException(target, "circuit-open", exception);
        } catch (BulkheadFullException exception) {
            throw new DownstreamUnavailableException(target, "bulkhead-full", exception);
        }
    }

    public void run(String target, CallKind kind, Runnable request) {
        call(target, kind, () -> {
            request.run();
            return null;
        });
    }

    public static boolean isTransientFailure(Throwable exception) {
        return exception instanceof ResourceAccessException
                || exception instanceof RestClientResponseException response
                && response.getStatusCode().is5xxServerError();
    }

    /** Spring configuration predicate shared by circuit-breaker and retry instances. */
    public static final class TransientHttpFailure implements Predicate<Throwable> {
        @Override
        public boolean test(Throwable exception) {
            return isTransientFailure(exception);
        }
    }
}

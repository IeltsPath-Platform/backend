package com.ieltspath.commonresilience;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResilientCallTest {
    @Test
    void isolatedDefaultMatchesRetrySettingsInApplicationYaml() throws IOException {
        Path applicationYaml = findApplicationYaml();
        var properties = new YamlPropertySourceLoader()
                .load("application", new FileSystemResource(applicationYaml)).get(0);
        int expectedMaxAttempts = Integer.parseInt(String.valueOf(
                properties.getProperty("resilience4j.retry.configs.default.max-attempts")));
        Duration expectedWait = DurationStyle.detectAndParse(String.valueOf(
                properties.getProperty("resilience4j.retry.configs.default.wait-duration")));

        ResilientCall call = ResilientCall.isolatedDefault();
        RetryRegistry retries = (RetryRegistry) ReflectionTestUtils.getField(call, "retries");
        AtomicReference<Duration> observedWait = new AtomicReference<>();
        retries.retry("yaml-config-check").getEventPublisher()
                .onRetry(event -> observedWait.set(event.getWaitInterval()));
        AtomicInteger attempts = new AtomicInteger();
        assertThrows(ResourceAccessException.class, () -> call.call("yaml-config-check", CallKind.READ, () -> {
            attempts.incrementAndGet();
            throw connectionFailure();
        }));

        assertEquals(expectedMaxAttempts, attempts.get());
        assertEquals(expectedWait, observedWait.get());
    }

    private static Path findApplicationYaml() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve("infra/config-server/config-repo/application.yaml");
            if (Files.isRegularFile(candidate)) return candidate;
            directory = directory.getParent();
        }
        throw new AssertionError("Could not locate infra/config-server/config-repo/application.yaml");
    }

    @Test
    void opensAfterFailureThreshold() {
        var fixture = fixture(2, 1, Duration.ofSeconds(30));
        for (int i = 0; i < 2; i++) assertThrows(ResourceAccessException.class,
                () -> fixture.call.call("content", CallKind.WRITE, () -> { throw connectionFailure(); }));
        assertEquals(CircuitBreaker.State.OPEN, fixture.breakers.circuitBreaker("content").getState());
    }

    @Test
    void doesNotCallDownstreamWhileOpen() {
        var fixture = fixture(2, 1, Duration.ofSeconds(30));
        open(fixture);
        AtomicInteger calls = new AtomicInteger();
        var failure = assertThrows(DownstreamUnavailableException.class,
                () -> fixture.call.call("content", CallKind.READ, calls::incrementAndGet));
        assertEquals("circuit-open", failure.reason());
        assertEquals(0, calls.get());
    }

    @Test
    void closesAfterSuccessfulHalfOpenCalls() throws InterruptedException {
        var fixture = fixture(2, 1, Duration.ofMillis(1));
        open(fixture);
        Thread.sleep(10);
        for (int i = 0; i < 3; i++) fixture.call.call("content", CallKind.READ, () -> "ok");
        assertEquals(CircuitBreaker.State.CLOSED, fixture.breakers.circuitBreaker("content").getState());
    }

    @Test
    void clientErrorsDoNotOpenBreaker() {
        var fixture = fixture(2, 1, Duration.ofSeconds(30));
        for (int i = 0; i < 12; i++) assertThrows(HttpClientErrorException.class,
                () -> fixture.call.call("content", CallKind.READ, () -> { throw notFound(); }));
        assertEquals(CircuitBreaker.State.CLOSED, fixture.breakers.circuitBreaker("content").getState());
    }

    @Test
    void retriesReadOnServerError() {
        var fixture = fixture(100, 2, Duration.ofSeconds(30));
        AtomicInteger calls = new AtomicInteger();
        assertThrows(HttpServerErrorException.class, () -> fixture.call.call("content", CallKind.READ, () -> {
            calls.incrementAndGet();
            throw serverFailure();
        }));
        assertEquals(2, calls.get());
    }

    @Test
    void doesNotRetryWrite() {
        var fixture = fixture(100, 2, Duration.ofSeconds(30));
        AtomicInteger calls = new AtomicInteger();
        assertThrows(HttpServerErrorException.class, () -> fixture.call.call("llm", CallKind.WRITE, () -> {
            calls.incrementAndGet();
            throw serverFailure();
        }));
        assertEquals(1, calls.get());
    }

    @Test
    void retriesIdempotentWrite() {
        var fixture = fixture(100, 2, Duration.ofSeconds(30));
        AtomicInteger calls = new AtomicInteger();
        assertThrows(HttpServerErrorException.class, () -> fixture.call.call("access", CallKind.IDEMPOTENT_WRITE, () -> {
            calls.incrementAndGet();
            throw serverFailure();
        }));
        assertEquals(2, calls.get());
    }

    @Test
    void rejectsImmediatelyWhenBulkheadFull() throws InterruptedException {
        var fixture = fixture(100, 1, Duration.ofSeconds(30));
        fixture.bulkheads = BulkheadRegistry.of(BulkheadConfig.custom().maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO).build());
        fixture.bulkheads.bulkhead("llm");
        fixture.call = new ResilientCall(fixture.breakers, fixture.retries, fixture.bulkheads);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread held = Thread.ofPlatform().start(() -> fixture.call.call("llm", CallKind.WRITE, () -> {
            entered.countDown();
            try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "done";
        }));
        entered.await();
        try {
            var failure = assertThrows(DownstreamUnavailableException.class,
                    () -> fixture.call.call("llm", CallKind.WRITE, () -> "unexpected"));
            assertEquals("bulkhead-full", failure.reason());
        } finally {
            release.countDown();
            held.join();
        }
    }

    @Test
    void rethrowsOriginalExceptionAfterRetriesSoClientMappingStillWorks() {
        var fixture = fixture(100, 2, Duration.ofSeconds(30));
        var original = serverFailure();
        var thrown = assertThrows(HttpServerErrorException.class,
                () -> fixture.call.call("content", CallKind.READ, () -> { throw original; }));
        assertSame(original, thrown);
    }

    @Test
    void fourHundredFourIsNeitherCountedNorRetried() {
        var fixture = fixture(2, 2, Duration.ofSeconds(30));
        AtomicInteger calls = new AtomicInteger();
        for (int i = 0; i < 10; i++) assertThrows(HttpClientErrorException.class,
                () -> fixture.call.call("content", CallKind.READ, () -> {
                    calls.incrementAndGet();
                    throw notFound();
                }));
        assertEquals(10, calls.get());
        assertEquals(CircuitBreaker.State.CLOSED, fixture.breakers.circuitBreaker("content").getState());
    }

    @Test
    void doesNotRetryWhenBreakerIsOpen() {
        var fixture = fixture(2, 2, Duration.ofSeconds(30));
        open(fixture);
        AtomicInteger calls = new AtomicInteger();
        var failure = assertThrows(DownstreamUnavailableException.class,
                () -> fixture.call.call("content", CallKind.READ, calls::incrementAndGet));
        assertEquals("circuit-open", failure.reason());
        assertEquals(0, calls.get());
    }

    @Test
    void postMarkedReadIsRetried() {
        var fixture = fixture(100, 2, Duration.ofSeconds(30));
        AtomicInteger calls = new AtomicInteger();
        assertThrows(HttpServerErrorException.class, () -> fixture.call.call("content", CallKind.READ, () -> {
            calls.incrementAndGet();
            throw serverFailure();
        }));
        assertEquals(2, calls.get());
    }

    @Test
    void readTimeoutCountsAsFailure() {
        var fixture = fixture(2, 1, Duration.ofSeconds(30));
        for (int i = 0; i < 2; i++) assertThrows(ResourceAccessException.class,
                () -> fixture.call.call("content", CallKind.WRITE,
                        () -> { throw new ResourceAccessException("read timed out"); }));
        assertEquals(CircuitBreaker.State.OPEN, fixture.breakers.circuitBreaker("content").getState());
    }

    @Test
    void businessExceptionFromSupplierPassesThrough() {
        var fixture = fixture(2, 2, Duration.ofSeconds(30));
        var original = new IllegalStateException("business failure");
        var thrown = assertThrows(IllegalStateException.class,
                () -> fixture.call.call("content", CallKind.READ, () -> { throw original; }));
        assertSame(original, thrown);
    }

    private static Fixture fixture(int minimumCalls, int maxAttempts, Duration openDuration) {
        CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.custom().slidingWindowSize(20)
                .minimumNumberOfCalls(minimumCalls).failureRateThreshold(50).waitDurationInOpenState(openDuration)
                .permittedNumberOfCallsInHalfOpenState(3).automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordException(ResilientCall::isTransientFailure).build();
        RetryConfig retryConfig = RetryConfig.custom().maxAttempts(maxAttempts).waitDuration(Duration.ZERO)
                .retryOnException(ResilientCall::isTransientFailure).build();
        CircuitBreakerRegistry breakers = CircuitBreakerRegistry.of(breakerConfig);
        RetryRegistry retries = RetryRegistry.of(retryConfig);
        BulkheadRegistry bulkheads = BulkheadRegistry.ofDefaults();
        return new Fixture(breakers, retries, bulkheads, new ResilientCall(breakers, retries, bulkheads));
    }

    private static void open(Fixture fixture) {
        for (int i = 0; i < 2; i++) assertThrows(ResourceAccessException.class,
                () -> fixture.call.call("content", CallKind.WRITE, () -> { throw connectionFailure(); }));
    }

    private static ResourceAccessException connectionFailure() {
        return new ResourceAccessException("connection refused");
    }

    private static HttpServerErrorException serverFailure() {
        return HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "server error",
                HttpHeaders.EMPTY, new byte[0], null);
    }

    private static HttpClientErrorException notFound() {
        return HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not found",
                HttpHeaders.EMPTY, new byte[0], null);
    }

    private static final class Fixture {
        private final CircuitBreakerRegistry breakers;
        private final RetryRegistry retries;
        private BulkheadRegistry bulkheads;
        private ResilientCall call;
        private Fixture(CircuitBreakerRegistry breakers, RetryRegistry retries, BulkheadRegistry bulkheads,
                        ResilientCall call) {
            this.breakers = breakers; this.retries = retries; this.bulkheads = bulkheads; this.call = call;
        }
    }
}

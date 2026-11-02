package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.learning.application.exception.LearningRequestException;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiCompatibleLlmClientBulkheadTest {
    @Test
    void fifthConcurrentCallIsRejectedAsBusy() throws InterruptedException {
        var bulkheads = BulkheadRegistry.of(BulkheadConfig.custom().maxConcurrentCalls(4)
                .maxWaitDuration(Duration.ZERO).build());
        bulkheads.bulkhead("llm");
        var resilience = new ResilientCall(CircuitBreakerRegistry.ofDefaults(), RetryRegistry.ofDefaults(), bulkheads);
        var client = new OpenAiCompatibleLlmClient(RestClient.builder().baseUrl("http://unused").build(),
                "key", "model", "", resilience);
        CountDownLatch entered = new CountDownLatch(4);
        CountDownLatch release = new CountDownLatch(1);
        List<Thread> held = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Thread thread = Thread.ofPlatform().start(() -> resilience.call("llm", CallKind.WRITE, () -> {
                entered.countDown();
                try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return "held";
            }));
            held.add(thread);
        }
        entered.await();
        try {
            var failure = assertThrows(LearningRequestException.class, () -> client.completeJson("system", "essay"));
            assertEquals(503, failure.getStatus());
            assertEquals("AI_BUSY", failure.getCode());
        } finally {
            release.countDown();
            for (Thread thread : held) thread.join();
        }
    }
}

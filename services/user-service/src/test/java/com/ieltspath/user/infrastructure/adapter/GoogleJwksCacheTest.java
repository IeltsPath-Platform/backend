package com.ieltspath.user.infrastructure.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleJwksCacheTest {
    @Test
    void aSlowLoadDoesNotBlockAnotherLookup() throws Exception {
        GoogleJwksCache cache = new GoogleJwksCache();
        cache.put("cached", "value");
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> slow = executor.submit(() -> cache.get("slow", () -> {
                loading.countDown();
                release.await(5, TimeUnit.SECONDS);
                return "loaded";
            }));
            loading.await(5, TimeUnit.SECONDS);

            // The first thread is still inside its network call; this lookup must not wait for it.
            assertEquals("value", cache.get("cached", () -> "unexpected"));

            release.countDown();
            assertEquals("loaded", slow.get(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void aFailedLoadIsNotRepeatedForEveryRequest() {
        GoogleJwksCache cache = new GoogleJwksCache();
        AtomicInteger calls = new AtomicInteger();

        assertThrows(Cache.ValueRetrievalException.class, () -> cache.get("keys", () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("Google is down");
        }));
        assertThrows(Cache.ValueRetrievalException.class, () -> cache.get("keys", () -> {
            calls.incrementAndGet();
            return "loaded";
        }));

        assertEquals(1, calls.get(), "the second request must not call Google again right after a failure");
    }
}

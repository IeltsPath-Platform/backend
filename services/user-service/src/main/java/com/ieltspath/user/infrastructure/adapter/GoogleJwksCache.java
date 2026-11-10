package com.ieltspath.user.infrastructure.adapter;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;

/** A small time-limited cache for the remote Google JWK set. */
final class GoogleJwksCache implements Cache {
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration FAILURE_TTL = Duration.ofSeconds(5);
    private final ConcurrentHashMap<Object, Entry> entries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Object, Failure> failures = new ConcurrentHashMap<>();

    @Override public String getName() { return "google-jwks"; }
    @Override public Object getNativeCache() { return entries; }

    @Override
    public ValueWrapper get(Object key) {
        Entry entry = entries.get(key);
        if (entry == null) return null;
        if (entry.expiresAtNanos() <= System.nanoTime()) {
            entries.remove(key, entry);
            return null;
        }
        return new SimpleValueWrapper(entry.value());
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper value = get(key);
        if (value == null) return null;
        Object cached = value.get();
        if (type != null && cached != null && !type.isInstance(cached)) {
            throw new IllegalStateException("Cached value is not of the requested type");
        }
        @SuppressWarnings("unchecked") T cast = (T) cached;
        return cast;
    }

    /**
     * No lock is held while the loader makes its network call, so a slow Google response never blocks other sign-ins.
     * The key set is public and stable, so a few parallel loads are harmless. After a failure the next loads are refused
     * for a few seconds instead of calling Google again for every request.
     */
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper cached = get(key);
        if (cached != null) {
            @SuppressWarnings("unchecked") T value = (T) cached.get();
            return value;
        }
        Failure recent = failures.get(key);
        if (recent != null) {
            if (recent.untilNanos() > System.nanoTime()) {
                throw new Cache.ValueRetrievalException(key, valueLoader, recent.cause());
            }
            failures.remove(key, recent);
        }
        try {
            T value = valueLoader.call();
            put(key, value);
            failures.remove(key);
            return value;
        } catch (Exception failure) {
            failures.put(key, new Failure(failure, System.nanoTime() + FAILURE_TTL.toNanos()));
            throw new Cache.ValueRetrievalException(key, valueLoader, failure);
        }
    }

    @Override public void put(Object key, Object value) {
        entries.put(key, new Entry(value, System.nanoTime() + TTL.toNanos()));
    }
    @Override public void evict(Object key) { entries.remove(key); }
    @Override public void clear() { entries.clear(); failures.clear(); }

    private record Entry(Object value, long expiresAtNanos) { }

    private record Failure(Throwable cause, long untilNanos) { }
}

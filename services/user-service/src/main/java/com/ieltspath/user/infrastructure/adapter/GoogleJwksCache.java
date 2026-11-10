package com.ieltspath.user.infrastructure.adapter;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;

/** A small time-limited cache for the remote Google JWK set. */
final class GoogleJwksCache implements Cache {
    private static final Duration TTL = Duration.ofMinutes(10);
    private final ConcurrentHashMap<Object, Entry> entries = new ConcurrentHashMap<>();

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

    @Override
    public synchronized <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper cached = get(key);
        if (cached != null) {
            @SuppressWarnings("unchecked") T value = (T) cached.get();
            return value;
        }
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception failure) {
            throw new Cache.ValueRetrievalException(key, valueLoader, failure);
        }
    }

    @Override public void put(Object key, Object value) {
        entries.put(key, new Entry(value, System.nanoTime() + TTL.toNanos()));
    }
    @Override public void evict(Object key) { entries.remove(key); }
    @Override public void clear() { entries.clear(); }

    private record Entry(Object value, long expiresAtNanos) { }
}

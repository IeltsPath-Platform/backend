package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Published placement exams of every pool, read from Content in one call and cached for a short time. The Content call
 * runs without any lock held; two requests that find the cache expired at the same moment may both read it, which is
 * harmless because the result is a replaceable snapshot.
 */
@Component
public class PlacementCatalog {
    private final LearningContentClient content;
    private final Clock clock;
    private final Duration ttl;
    private volatile Snapshot snapshot;

    @Autowired
    public PlacementCatalog(LearningContentClient content,
                            @Value("${learning.placement.catalog-cache-seconds:30}") long ttlSeconds) {
        this(content, Clock.systemUTC(), ttlSeconds);
    }

    public PlacementCatalog(LearningContentClient content, Clock clock, long ttlSeconds) {
        this.content = content; this.clock = clock; this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
    }

    public List<LearningContentClient.PlacementPackage> packages(PlacementPool pool) {
        return current().get(pool);
    }

    public int count(PlacementPool pool) { return packages(pool).size(); }

    public void evict() { snapshot = null; }

    private Map<PlacementPool, List<LearningContentClient.PlacementPackage>> current() {
        Snapshot cached = snapshot;
        if (cached != null && cached.expiresAt().isAfter(clock.instant())) return cached.byPool();
        List<LearningContentClient.PlacementPackage> response = content.getPlacementPackages();
        Map<PlacementPool, List<LearningContentClient.PlacementPackage>> byPool = new EnumMap<>(PlacementPool.class);
        for (PlacementPool pool : PlacementPool.values()) {
            byPool.put(pool, response == null ? List.of() : response.stream()
                    .filter(item -> item != null && pool == item.pool() && item.packageId() != null
                            && item.packageVersionId() != null).toList());
        }
        snapshot = new Snapshot(byPool, clock.instant().plus(ttl));
        return byPool;
    }

    private record Snapshot(Map<PlacementPool, List<LearningContentClient.PlacementPackage>> byPool, Instant expiresAt) {}
}

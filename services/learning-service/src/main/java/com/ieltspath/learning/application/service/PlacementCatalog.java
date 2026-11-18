package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
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

@Component
public class PlacementCatalog {
    private final LearningContentClient content;
    private final Clock clock;
    private final Duration ttl;
    private final Map<PlacementPool, Entry> entries = new EnumMap<>(PlacementPool.class);

    @Autowired
    public PlacementCatalog(LearningContentClient content,
                            @Value("${learning.placement.catalog-cache-seconds:30}") long ttlSeconds) {
        this(content, Clock.systemUTC(), ttlSeconds);
    }

    public PlacementCatalog(LearningContentClient content, Clock clock, long ttlSeconds) {
        this.content = content; this.clock = clock; this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
    }

    public synchronized List<LearningContentClient.PlacementPackage> packages(PlacementPool pool) {
        Entry cached = entries.get(pool);
        if (cached != null && cached.expiresAt().isAfter(clock.instant())) return cached.packages();
        List<LearningContentClient.PlacementPackage> response = content.getPlacementPackages(pool);
        if (response == null) response = List.of();
        List<LearningContentClient.PlacementPackage> safe = response.stream()
                .filter(item -> item != null && pool == item.pool() && item.packageId() != null
                        && item.packageVersionId() != null).toList();
        entries.put(pool, new Entry(safe, clock.instant().plus(ttl)));
        return safe;
    }

    public int count(PlacementPool pool) { return packages(pool).size(); }
    public void evict() { entries.clear(); }

    private record Entry(List<LearningContentClient.PlacementPackage> packages, Instant expiresAt) {}
}

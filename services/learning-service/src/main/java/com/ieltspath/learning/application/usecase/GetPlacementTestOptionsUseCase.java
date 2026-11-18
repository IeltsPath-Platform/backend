package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.result.PlacementAssignmentResult;
import com.ieltspath.learning.application.result.PlacementTestOptionsResult;
import com.ieltspath.learning.application.service.PlacementCatalog;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.repository.PlacementAssignmentRepository;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
public class GetPlacementTestOptionsUseCase {
    private final PlacementAssignmentRepository assignments;
    private final LearnerPlacementRepository placements;
    private final PlacementCatalog catalog;
    private final Clock clock;
    private final int limit;

    @Autowired
    public GetPlacementTestOptionsUseCase(PlacementAssignmentRepository assignments,
                                          LearnerPlacementRepository placements, PlacementCatalog catalog,
                                          @Value("${learning.placement.limit-per-24h:3}") int limit) {
        this(assignments, placements, catalog, Clock.systemUTC(), limit);
    }
    GetPlacementTestOptionsUseCase(PlacementAssignmentRepository assignments, LearnerPlacementRepository placements,
                                   PlacementCatalog catalog, Clock clock, int limit) {
        this.assignments = assignments; this.placements = placements; this.catalog = catalog; this.clock = clock;
        this.limit = limit;
    }

    public PlacementTestOptionsResult execute(UUID userId) {
        Instant now = clock.instant();
        Instant since = now.minus(java.time.Duration.ofHours(24));
        int used = assignments.countSince(userId, since);
        Instant next = used >= limit ? assignments.oldestSince(userId, since)
                .map(t -> t.plus(java.time.Duration.ofHours(24)))
                .orElse(null) : null;
        var current = placements.find(userId);
        var open = assignments.findOpen(userId)
                .map(a -> new PlacementAssignmentResult(a.id(), a.pool(), a.packageId(), a.packageVersionId(),
                        Math.max(0, limit - used), next)).orElse(null);
        return new PlacementTestOptionsResult(Arrays.stream(PlacementPool.values())
                .map(pool -> new PlacementTestOptionsResult.PoolOption(pool, catalog.count(pool))).toList(),
                limit, Math.max(0, limit - used), next, current.isPresent(),
                current.map(p -> p.band().value().doubleValue()).orElse(null), open);
    }
}

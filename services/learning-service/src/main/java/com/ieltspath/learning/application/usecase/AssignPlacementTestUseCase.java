package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.PlacementAssignmentResult;
import com.ieltspath.learning.application.service.PlacementCatalog;
import com.ieltspath.learning.domain.repository.PlacementAssignmentRepository;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AssignPlacementTestUseCase {
    private final PlacementAssignmentRepository assignments;
    private final PlacementCatalog catalog;
    private final CreatePlacementAssignment creator;
    private final Clock clock;
    private final int limit;
    private final Duration reuseWindow;

    @Autowired
    public AssignPlacementTestUseCase(PlacementAssignmentRepository assignments, PlacementCatalog catalog,
                                     CreatePlacementAssignment creator,
                                     @Value("${learning.placement.limit-per-24h:3}") int limit,
                                     @Value("${learning.placement.reuse-window-minutes:15}") long reuseMinutes) {
        this(assignments, catalog, creator, Clock.systemUTC(), limit, reuseMinutes);
    }

    AssignPlacementTestUseCase(PlacementAssignmentRepository assignments, PlacementCatalog catalog,
                               CreatePlacementAssignment creator, Clock clock, int limit, long reuseMinutes) {
        this.assignments = assignments; this.catalog = catalog; this.creator = creator; this.clock = clock;
        this.limit = limit; this.reuseWindow = Duration.ofMinutes(reuseMinutes);
    }

    public PlacementAssignmentResult execute(UUID userId, PlacementPool pool) {
        Instant now = clock.instant();
        var reusable = assignments.findReusable(userId, pool, now.minus(reuseWindow));
        if (reusable.isPresent()) return result(reusable.get(), remaining(userId, now), nextRetake(userId, now));
        if (assignments.countSince(userId, now.minus(Duration.ofHours(24))) >= limit) throw limitReached();
        var candidates = catalog.packages(pool);
        if (candidates.isEmpty()) throw new LearningRequestException(404, "NO_PLACEMENT_TEST",
                "No published placement test is available");
        var created = creator.create(userId, pool, candidates, now, now.minus(reuseWindow),
                now.minus(Duration.ofHours(24)), limit);
        return result(created, remaining(userId, now), nextRetake(userId, now));
    }

    private int remaining(UUID userId, Instant now) {
        return Math.max(0, limit - assignments.countSince(userId, now.minus(Duration.ofHours(24))));
    }
    private Instant nextRetake(UUID userId, Instant now) {
        if (assignments.countSince(userId, now.minus(Duration.ofHours(24))) < limit) return null;
        return assignments.oldestSince(userId, now.minus(Duration.ofHours(24))).map(t -> t.plus(Duration.ofHours(24)))
                .orElse(null);
    }
    private static PlacementAssignmentResult result(com.ieltspath.learning.domain.aggregate.PlacementAssignment a,
                                                     int left, Instant next) {
        return new PlacementAssignmentResult(a.id(), a.pool(), a.packageId(), a.packageVersionId(), left, next);
    }
    private static LearningRequestException limitReached() {
        return new LearningRequestException(429, "PLACEMENT_LIMIT_REACHED", "Placement limit reached");
    }
}

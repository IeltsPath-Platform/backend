package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.PlacementPicker;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.aggregate.PlacementAssignment;
import com.ieltspath.learning.domain.repository.PlacementAssignmentRepository;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CreatePlacementAssignment {
    private final PlacementAssignmentRepository assignments;
    private final LearnerLock lock;
    private final PlacementPicker picker;

    public CreatePlacementAssignment(PlacementAssignmentRepository assignments, LearnerLock lock,
                                    PlacementPicker picker) {
        this.assignments = assignments; this.lock = lock; this.picker = picker;
    }

    @Transactional(noRollbackFor = LearningRequestException.class)
    public PlacementAssignment create(UUID userId, PlacementPool pool,
                                      List<LearningContentClient.PlacementPackage> packages,
                                      Instant now, Instant reuseSince, Instant limitSince, int limit) {
        lock.lock(userId);
        var reusable = assignments.findReusable(userId, pool, reuseSince);
        if (reusable.isPresent()) return reusable.get();
        if (assignments.countSince(userId, limitSince) >= limit) throw limitReached();
        List<LearningContentClient.PlacementPackage> candidates = packages.stream()
                .filter(p -> p.pool() == pool).toList();
        if (candidates.isEmpty()) throw noTest();
        Map<UUID, Instant> latest = assignments.latestByPackage(userId);
        List<LearningContentClient.PlacementPackage> unused = candidates.stream()
                .filter(p -> !latest.containsKey(p.packageId())).toList();
        var chosen = unused.isEmpty()
                ? candidates.stream().min(java.util.Comparator.comparing(p -> latest.get(p.packageId()))).orElseThrow()
                : unused.get(picker.choose(unused.size()));
        var assignment = PlacementAssignment.assign(UUID.randomUUID(), userId, chosen.packageId(),
                chosen.packageVersionId(), pool, now);
        assignments.save(assignment);
        return assignment;
    }

    private static LearningRequestException limitReached() {
        return new LearningRequestException(429, "PLACEMENT_LIMIT_REACHED", "Placement limit reached");
    }
    private static LearningRequestException noTest() {
        return new LearningRequestException(404, "NO_PLACEMENT_TEST", "No published placement test is available");
    }
}

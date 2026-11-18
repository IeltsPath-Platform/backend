package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.PlacementAssignment;
import com.ieltspath.learning.domain.vo.PlacementPool;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PlacementAssignmentRepository {
    int countSince(UUID userId, Instant since);
    Optional<Instant> oldestSince(UUID userId, Instant since);
    Optional<PlacementAssignment> findReusable(UUID userId, PlacementPool pool, Instant since);
    Optional<PlacementAssignment> findOpen(UUID userId);
    Map<UUID, Instant> latestByPackage(UUID userId);
    List<PlacementAssignment> findByUser(UUID userId);
    Optional<PlacementAssignment> findOwnedOpen(UUID userId, UUID assignmentId);
    Optional<PlacementAssignment> findOpenForResult(UUID userId, UUID packageVersionId, Instant completedAt);
    void save(PlacementAssignment assignment);
}

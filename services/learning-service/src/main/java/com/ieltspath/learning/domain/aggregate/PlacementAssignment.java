package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.vo.PlacementPool;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PlacementAssignment {
    private final UUID id;
    private final UUID userId;
    private final UUID packageId;
    private final UUID packageVersionId;
    private final PlacementPool pool;
    private final Instant assignedAt;
    private Instant completedAt;
    private Instant releasedAt;
    private UUID attemptId;

    private PlacementAssignment(UUID id, UUID userId, UUID packageId, UUID packageVersionId, PlacementPool pool,
                                Instant assignedAt, Instant completedAt, Instant releasedAt, UUID attemptId) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.packageId = Objects.requireNonNull(packageId);
        this.packageVersionId = Objects.requireNonNull(packageVersionId);
        this.pool = Objects.requireNonNull(pool);
        this.assignedAt = Objects.requireNonNull(assignedAt);
        this.completedAt = completedAt;
        this.releasedAt = releasedAt;
        this.attemptId = attemptId;
    }

    public static PlacementAssignment assign(UUID id, UUID userId, UUID packageId, UUID packageVersionId,
                                             PlacementPool pool, Instant assignedAt) {
        return new PlacementAssignment(id, userId, packageId, packageVersionId, pool, assignedAt, null, null, null);
    }

    public static PlacementAssignment restore(UUID id, UUID userId, UUID packageId, UUID packageVersionId,
                                              PlacementPool pool, Instant assignedAt, Instant completedAt,
                                              Instant releasedAt, UUID attemptId) {
        return new PlacementAssignment(id, userId, packageId, packageVersionId, pool, assignedAt, completedAt,
                releasedAt, attemptId);
    }

    public boolean release(Instant at) {
        if (completedAt != null || releasedAt != null) return false;
        releasedAt = Objects.requireNonNull(at);
        return true;
    }

    public boolean complete(UUID completedAttemptId, Instant at) {
        if (releasedAt != null || completedAt != null) return false;
        attemptId = Objects.requireNonNull(completedAttemptId);
        completedAt = Objects.requireNonNull(at);
        return true;
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID packageId() { return packageId; }
    public UUID packageVersionId() { return packageVersionId; }
    public PlacementPool pool() { return pool; }
    public Instant assignedAt() { return assignedAt; }
    public Instant completedAt() { return completedAt; }
    public Instant releasedAt() { return releasedAt; }
    public UUID attemptId() { return attemptId; }
    public boolean isReleased() { return releasedAt != null; }
    public boolean isCompleted() { return completedAt != null; }
}

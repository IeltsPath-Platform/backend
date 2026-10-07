package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.vo.BandLevel;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** The latest placement estimate; it recommends courses without changing learner progress. */
public final class LearnerPlacement {
    private final UUID userId;
    private BandLevel band;
    private UUID attemptId;
    private Instant completedAt;
    private Instant updatedAt;

    private LearnerPlacement(UUID userId, BandLevel band, UUID attemptId, Instant completedAt, Instant updatedAt) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.band = Objects.requireNonNull(band, "band");
        this.attemptId = Objects.requireNonNull(attemptId, "attemptId");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static LearnerPlacement create(UUID userId, BandLevel band, UUID attemptId, Instant completedAt) {
        return new LearnerPlacement(userId, band, attemptId, completedAt, Instant.now());
    }

    public static LearnerPlacement restore(UUID userId, BandLevel band, UUID attemptId,
                                           Instant completedAt, Instant updatedAt) {
        return new LearnerPlacement(userId, band, attemptId, completedAt, updatedAt);
    }

    /** A newer attempt or regrade of the current attempt may replace the estimate. */
    public boolean record(BandLevel band, UUID attemptId, Instant completedAt) {
        Objects.requireNonNull(band, "band");
        Objects.requireNonNull(attemptId, "attemptId");
        Objects.requireNonNull(completedAt, "completedAt");
        if (!this.attemptId.equals(attemptId) && !completedAt.isAfter(this.completedAt)) return false;
        this.band = band;
        this.attemptId = attemptId;
        this.completedAt = completedAt;
        this.updatedAt = Instant.now();
        return true;
    }

    public UUID userId() { return userId; }
    public BandLevel band() { return band; }
    public UUID attemptId() { return attemptId; }
    public Instant completedAt() { return completedAt; }
    public Instant updatedAt() { return updatedAt; }
}

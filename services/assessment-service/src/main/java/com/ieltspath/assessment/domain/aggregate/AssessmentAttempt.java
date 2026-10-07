package com.ieltspath.assessment.domain.aggregate;

import com.ieltspath.assessment.domain.exception.AttemptExpiredException;
import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;
import com.ieltspath.assessment.domain.vo.*;

import java.time.Instant;
import java.util.UUID;

public final class AssessmentAttempt {
    private final UUID id;
    private final UUID userId;
    private final UUID packageVersionId;
    private final AttemptType attemptType;
    private final AttemptMode mode;
    private final AttemptChannel channel;
    private AttemptStatus status;
    private final Instant startedAt;
    private Instant submittedAt;
    private final Instant expiresAt;
    private long rowVersion;
    private final Instant createdAt;
    private Instant updatedAt;
    // Kept only for attempts started before goals stopped being captured; new attempts store null and
    // AssessmentCompleted.v2 always announces a null goal.
    private final UUID learningGoalId;

    public AssessmentAttempt(UUID id, UUID userId, UUID packageVersionId, AttemptType attemptType,
                             AttemptMode mode, AttemptChannel channel, AttemptStatus status,
                             Instant startedAt, Instant submittedAt, Instant expiresAt,
                             long rowVersion, Instant createdAt, Instant updatedAt) {
        this(id, userId, packageVersionId, attemptType, mode, channel, status, startedAt, submittedAt, expiresAt,
                rowVersion, createdAt, updatedAt, null);
    }

    public AssessmentAttempt(UUID id, UUID userId, UUID packageVersionId, AttemptType attemptType,
                             AttemptMode mode, AttemptChannel channel, AttemptStatus status,
                             Instant startedAt, Instant submittedAt, Instant expiresAt,
                             long rowVersion, Instant createdAt, Instant updatedAt, UUID learningGoalId) {
        this.id = id;
        this.userId = userId;
        this.packageVersionId = packageVersionId;
        this.attemptType = attemptType;
        this.mode = mode;
        this.channel = channel;
        this.status = status;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.expiresAt = expiresAt;
        this.rowVersion = rowVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.learningGoalId = learningGoalId;
    }

    public static AssessmentAttempt start(UUID userId, UUID packageVersionId, AttemptType type,
                                          AttemptMode mode, AttemptChannel channel, Instant expiresAt) {
        Instant now = Instant.now();
        return new AssessmentAttempt(UUID.randomUUID(), userId, packageVersionId, type, mode, channel,
                AttemptStatus.IN_PROGRESS, now, null, expiresAt, 0, now, now, null);
    }

    /**
     * A submit at or after the deadline moves the attempt to EXPIRED before throwing, so the caller can persist the
     * expiry rather than lose it with the failed submit.
     */
    public void submit(Instant now) {
        if (status == AttemptStatus.SUBMITTED) return;
        if (status == AttemptStatus.EXPIRED) {
            throw new AttemptExpiredException();
        }
        if (status != AttemptStatus.IN_PROGRESS) {
            throw new InvalidAssessmentStateException("Only an in-progress attempt can be submitted");
        }
        if (expiresAt != null && !now.isBefore(expiresAt)) {
            expire(now);
            throw new AttemptExpiredException();
        }
        status = AttemptStatus.SUBMITTED;
        submittedAt = now;
        updatedAt = now;
    }

    public void expire(Instant now) {
        if (status == AttemptStatus.EXPIRED) return;
        if (status != AttemptStatus.IN_PROGRESS) {
            throw new InvalidAssessmentStateException("Only an in-progress attempt can expire");
        }
        status = AttemptStatus.EXPIRED;
        updatedAt = now;
    }

    public boolean belongsTo(UUID currentUserId) { return userId.equals(currentUserId); }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getPackageVersionId() { return packageVersionId; }
    public AttemptType getAttemptType() { return attemptType; }
    public AttemptMode getMode() { return mode; }
    public AttemptChannel getChannel() { return channel; }
    public AttemptStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public long getRowVersion() { return rowVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getLearningGoalId() { return learningGoalId; }
}

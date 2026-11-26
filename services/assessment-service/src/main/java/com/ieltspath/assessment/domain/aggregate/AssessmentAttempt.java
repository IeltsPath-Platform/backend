package com.ieltspath.assessment.domain.aggregate;

import com.ieltspath.assessment.domain.exception.AttemptExpiredException;
import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;
import com.ieltspath.assessment.domain.vo.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class AssessmentAttempt {
    private static final Duration PRACTICE_GRACE = Duration.ofSeconds(30);

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
    private final UUID packageId;
    private final AttemptScope scope;
    private final Skill skill;
    private final PlacementPool placementPool;

    public AssessmentAttempt(UUID id, UUID userId, UUID packageVersionId, AttemptType attemptType,
                             AttemptMode mode, AttemptChannel channel, AttemptStatus status,
                             Instant startedAt, Instant submittedAt, Instant expiresAt,
                             long rowVersion, Instant createdAt, Instant updatedAt) {
        this(id, userId, packageVersionId, attemptType, mode, channel, status, startedAt, submittedAt, expiresAt,
                rowVersion, createdAt, updatedAt, null, null, AttemptScope.FULL, null, null);
    }

    public AssessmentAttempt(UUID id, UUID userId, UUID packageVersionId, AttemptType attemptType,
                             AttemptMode mode, AttemptChannel channel, AttemptStatus status,
                             Instant startedAt, Instant submittedAt, Instant expiresAt,
                             long rowVersion, Instant createdAt, Instant updatedAt, UUID learningGoalId) {
        this(id, userId, packageVersionId, attemptType, mode, channel, status, startedAt, submittedAt, expiresAt,
                rowVersion, createdAt, updatedAt, learningGoalId, null, AttemptScope.FULL, null, null);
    }

    public AssessmentAttempt(UUID id, UUID userId, UUID packageVersionId, AttemptType attemptType,
                             AttemptMode mode, AttemptChannel channel, AttemptStatus status,
                             Instant startedAt, Instant submittedAt, Instant expiresAt,
                             long rowVersion, Instant createdAt, Instant updatedAt, UUID learningGoalId,
                             UUID packageId, AttemptScope scope, Skill skill) {
        this(id, userId, packageVersionId, attemptType, mode, channel, status, startedAt, submittedAt, expiresAt,
                rowVersion, createdAt, updatedAt, learningGoalId, packageId, scope, skill, null);
    }

    public AssessmentAttempt(UUID id, UUID userId, UUID packageVersionId, AttemptType attemptType,
                             AttemptMode mode, AttemptChannel channel, AttemptStatus status,
                             Instant startedAt, Instant submittedAt, Instant expiresAt, long rowVersion,
                             Instant createdAt, Instant updatedAt, UUID learningGoalId, UUID packageId,
                             AttemptScope scope, Skill skill, PlacementPool placementPool) {
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
        this.packageId = packageId;
        this.scope = scope == null ? AttemptScope.FULL : scope;
        this.skill = skill;
        this.placementPool = placementPool;
    }

    public static AssessmentAttempt start(UUID userId, UUID packageVersionId, AttemptType type,
                                          AttemptMode mode, AttemptChannel channel, Instant expiresAt) {
        return start(userId, packageVersionId, null, type, mode, channel, expiresAt, AttemptScope.FULL, null);
    }

    public static AssessmentAttempt start(UUID userId, UUID packageVersionId, UUID packageId, AttemptType type,
                                          AttemptMode mode, AttemptChannel channel, Instant expiresAt,
                                          AttemptScope scope, Skill skill) {
        return start(userId, packageVersionId, packageId, type, mode, channel, expiresAt, scope, skill, null);
    }

    public static AssessmentAttempt start(UUID userId, UUID packageVersionId, UUID packageId, AttemptType type,
                                          AttemptMode mode, AttemptChannel channel, Instant expiresAt,
                                          AttemptScope scope, Skill skill, PlacementPool placementPool) {
        Instant now = Instant.now();
        return new AssessmentAttempt(UUID.randomUUID(), userId, packageVersionId, type, mode, channel,
                AttemptStatus.IN_PROGRESS, now, null, expiresAt, 0, now, now, null, packageId, scope, skill,
                type == AttemptType.PLACEMENT ? placementPool : null);
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
            throw new InvalidAssessmentStateException("Chỉ có thể nộp bài khi lượt làm bài đang diễn ra.");
        }
        if (isPastDeadline(now)) {
            expire(now);
            throw new AttemptExpiredException();
        }
        status = AttemptStatus.SUBMITTED;
        submittedAt = now;
        updatedAt = now;
    }

    /**
     * Whether the server clock has run out. A practice test gets a short grace after {@code expiresAt}, because its
     * client submits on the deadline and the request needs time to arrive.
     */
    public boolean isPastDeadline(Instant now) {
        if (expiresAt == null) return false;
        Instant deadline = attemptType == AttemptType.OFFICIAL_PRACTICE ? expiresAt.plus(PRACTICE_GRACE) : expiresAt;
        return !now.isBefore(deadline);
    }

    public void expire(Instant now) {
        if (status == AttemptStatus.EXPIRED) return;
        if (status != AttemptStatus.IN_PROGRESS) {
            throw new InvalidAssessmentStateException("Chỉ có thể hết hạn lượt làm bài đang diễn ra.");
        }
        status = AttemptStatus.EXPIRED;
        updatedAt = now;
    }

    public void cancel(Instant now) {
        if (attemptType != AttemptType.PLACEMENT || status != AttemptStatus.IN_PROGRESS) {
            throw new InvalidAssessmentStateException("Chỉ có thể hủy bài kiểm tra đầu vào đang làm dở.");
        }
        status = AttemptStatus.CANCELLED;
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
    public UUID getPackageId() { return packageId; }
    public AttemptScope getScope() { return scope; }
    public Skill getSkill() { return skill; }
    public PlacementPool getPlacementPool() { return placementPool; }
}

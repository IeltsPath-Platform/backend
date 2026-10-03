package com.group01.learning.domain.aggregate;

import com.group01.learning.domain.exception.PracticeAttemptAlreadySubmittedException;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.PracticeSubmission;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PracticeAttempt {
    private final UUID id;
    private final UUID userId;
    private final UUID lessonId;
    private final LearningSkill skill;
    private final UUID packageId;
    private final UUID packageVersionId;
    private final Instant startedAt;
    private Instant submittedAt;
    private UUID requestId;
    private PracticeSubmission response;

    private PracticeAttempt(UUID id, UUID userId, UUID lessonId, LearningSkill skill, UUID packageId,
                            UUID packageVersionId, Instant startedAt, Instant submittedAt, UUID requestId,
                            PracticeSubmission response) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.lessonId = Objects.requireNonNull(lessonId);
        this.skill = Objects.requireNonNull(skill);
        this.packageId = Objects.requireNonNull(packageId);
        this.packageVersionId = Objects.requireNonNull(packageVersionId);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.submittedAt = submittedAt;
        this.requestId = requestId;
        this.response = response;
    }

    public static PracticeAttempt start(UUID id, UUID userId, UUID lessonId, LearningSkill skill,
                                        UUID packageId, UUID versionId, Instant now) {
        return new PracticeAttempt(id, userId, lessonId, skill, packageId, versionId, now, null, null, null);
    }

    public static PracticeAttempt restore(UUID id, UUID userId, UUID lessonId, LearningSkill skill, UUID packageId,
                                          UUID versionId, Instant startedAt, Instant submittedAt, UUID requestId,
                                          PracticeSubmission response) {
        return new PracticeAttempt(id, userId, lessonId, skill, packageId, versionId, startedAt, submittedAt,
                requestId, response);
    }

    /** True for a new submission; the same request id replays the saved response. */
    public boolean acceptsAnswers(UUID submittedRequestId) {
        Objects.requireNonNull(submittedRequestId);
        if (requestId == null) return true;
        if (requestId.equals(submittedRequestId)) return false;
        throw new PracticeAttemptAlreadySubmittedException();
    }

    public void recordResult(UUID submittedRequestId, PracticeSubmission result, Instant now) {
        if (!acceptsAnswers(submittedRequestId)) return;
        if (!id.equals(result.attemptId()) || result.total() <= 0 || result.correct() < 0
                || result.correct() > result.total()) throw new IllegalArgumentException("Invalid practice result");
        requestId = submittedRequestId;
        response = Objects.requireNonNull(result);
        submittedAt = Objects.requireNonNull(now);
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID lessonId() { return lessonId; }
    public LearningSkill skill() { return skill; }
    public UUID packageId() { return packageId; }
    public UUID packageVersionId() { return packageVersionId; }
    public Instant startedAt() { return startedAt; }
    public Instant submittedAt() { return submittedAt; }
    public UUID requestId() { return requestId; }
    public PracticeSubmission response() { return response; }
    public boolean submitted() { return submittedAt != null; }
}

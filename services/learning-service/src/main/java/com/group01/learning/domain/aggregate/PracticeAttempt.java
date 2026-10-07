package com.group01.learning.domain.aggregate;

import com.group01.learning.domain.exception.PracticeAttemptAlreadySubmittedException;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.PracticeSubmission;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class PracticeAttempt {
    private final UUID id;
    private final UUID userId;
    private final UUID lessonId;
    private final Set<LearningSkill> skills;
    private final UUID packageId;
    private final UUID packageVersionId;
    private final Instant startedAt;
    private Instant submittedAt;
    private UUID requestId;
    private PracticeSubmission response;

    private PracticeAttempt(UUID id, UUID userId, UUID lessonId, Set<LearningSkill> skills, UUID packageId,
                            UUID packageVersionId, Instant startedAt, Instant submittedAt, UUID requestId,
                            PracticeSubmission response) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.lessonId = Objects.requireNonNull(lessonId);
        EnumSet<LearningSkill> copy = EnumSet.noneOf(LearningSkill.class);
        Objects.requireNonNull(skills).forEach(skill -> copy.add(Objects.requireNonNull(skill)));
        if (copy.isEmpty()) throw new IllegalArgumentException("A practice attempt needs at least one skill");
        this.skills = Collections.unmodifiableSet(copy);
        this.packageId = Objects.requireNonNull(packageId);
        this.packageVersionId = Objects.requireNonNull(packageVersionId);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.submittedAt = submittedAt;
        this.requestId = requestId;
        this.response = response;
    }

    public static PracticeAttempt start(UUID id, UUID userId, UUID lessonId, LearningSkill skill,
                                        UUID packageId, UUID versionId, Instant now) {
        return start(id, userId, lessonId, Set.of(Objects.requireNonNull(skill)), packageId, versionId, now);
    }

    /** {@code skills} are the skills of the practice set's questions. */
    public static PracticeAttempt start(UUID id, UUID userId, UUID lessonId, Set<LearningSkill> skills,
                                        UUID packageId, UUID versionId, Instant now) {
        return new PracticeAttempt(id, userId, lessonId, skills, packageId, versionId, now, null, null, null);
    }

    public static PracticeAttempt restore(UUID id, UUID userId, UUID lessonId, LearningSkill skill, UUID packageId,
                                          UUID versionId, Instant startedAt, Instant submittedAt, UUID requestId,
                                          PracticeSubmission response) {
        return restore(id, userId, lessonId, Set.of(Objects.requireNonNull(skill)), packageId, versionId, startedAt,
                submittedAt, requestId, response);
    }

    public static PracticeAttempt restore(UUID id, UUID userId, UUID lessonId, Set<LearningSkill> skills,
                                          UUID packageId, UUID versionId, Instant startedAt, Instant submittedAt,
                                          UUID requestId, PracticeSubmission response) {
        return new PracticeAttempt(id, userId, lessonId, skills, packageId, versionId, startedAt, submittedAt,
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
    /** The one skill of the set, or null for a set that mixes skills. */
    public LearningSkill skill() { return skills.size() == 1 ? skills.iterator().next() : null; }
    public Set<LearningSkill> skills() { return skills; }

    /** Skills that reached the pass mark in the recorded submission; empty before submission. */
    public Set<LearningSkill> passedSkills() {
        if (response == null) return Set.of();
        if (response.skillScores().isEmpty()) return response.passed() ? skills : Set.of();
        EnumSet<LearningSkill> passed = EnumSet.noneOf(LearningSkill.class);
        response.skillScores().stream().filter(PracticeSubmission.SkillScore::passed)
                .forEach(score -> passed.add(score.skill()));
        return Collections.unmodifiableSet(passed);
    }
    public UUID packageId() { return packageId; }
    public UUID packageVersionId() { return packageVersionId; }
    public Instant startedAt() { return startedAt; }
    public Instant submittedAt() { return submittedAt; }
    public UUID requestId() { return requestId; }
    public PracticeSubmission response() { return response; }
    public boolean submitted() { return submittedAt != null; }
}

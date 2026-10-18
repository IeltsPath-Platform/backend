package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One learner answer to one question, in one submission. {@code durationMs} is clamped to [0, two hours] because the
 * client reports it and a stalled tab can send a huge value. {@code correct} is null for items that are not judged
 * right or wrong. {@code attemptId} is set only for assessment events, so a regrade can replace them.
 * {@code answeredAt} is when the learner submitted the answer; an assessment event has none, because its result is
 * finalised when grading ends, which can be days after the learner answered.
 */
public record QuestionAttemptEvent(UUID userId, UUID questionVersionId, List<UUID> knowledgePointIds, Boolean correct,
                                   BigDecimal score, BigDecimal maxScore, Long durationMs, String errorCode,
                                   QuestionEventSource source, UUID sourceReferenceId, UUID attemptId,
                                   boolean firstSubmission, Instant answeredAt) {

    public static final long MAX_DURATION_MS = 2L * 60 * 60 * 1000;

    public QuestionAttemptEvent {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(questionVersionId, "questionVersionId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sourceReferenceId, "sourceReferenceId");
        knowledgePointIds = List.copyOf(new LinkedHashSet<>(knowledgePointIds));
        durationMs = durationMs == null ? null : Math.max(0L, Math.min(durationMs, MAX_DURATION_MS));
        if ((source == QuestionEventSource.ASSESSMENT) != (attemptId != null)) {
            throw new IllegalArgumentException("Only assessment events carry an attempt id");
        }
        if ((source == QuestionEventSource.ASSESSMENT) != (answeredAt == null)) {
            throw new IllegalArgumentException("Only assessment events have no answer time");
        }
    }

    /** An event for a source that has no formal attempt, answered now. */
    public static QuestionAttemptEvent of(UUID userId, UUID questionVersionId, List<UUID> knowledgePointIds,
                                          Boolean correct, Long durationMs, String errorCode,
                                          QuestionEventSource source, UUID sourceReferenceId,
                                          boolean firstSubmission) {
        return new QuestionAttemptEvent(userId, questionVersionId, knowledgePointIds, correct, null, null, durationMs,
                errorCode, source, sourceReferenceId, null, firstSubmission, Instant.now());
    }
}

package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.LearningSkill;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/**
 * {@code set} is null once the review is no longer pending and at the theory stage. {@code theory} holds the TEXT
 * blocks that teach the knowledge point ({@code theoryScope = KNOWLEDGE_POINT}) or, when the lesson tags none, every
 * TEXT block ({@code LESSON_FALLBACK}). {@code quickCheck} is non-empty only at the theory stage.
 *
 * <p>{@code state} is {@code DIAGNOSING} while the learner has probes of the prerequisites to answer,
 * {@code WAITING_ON_PREREQUISITES} while the reviews of the gap points are open, otherwise {@code ACTIVE}; in the first
 * two the review gives no set. {@code diagnosticSessionId} is set once a diagnosis was opened for the review.
 */
public record ReviewResult(UUID reviewId, String reviewStatus, UUID lessonId, List<String> theory, ReviewSet set,
                           UUID knowledgePointId, LearningSkill skill, String stage, String theoryReason,
                           String theoryScope, int failedSets, Integer maxFailedSets,
                           List<LessonResult.Question> quickCheck, String state, UUID diagnosticSessionId,
                           boolean blocking, List<UUID> waitingOnReviewIds,
                           @JsonInclude(JsonInclude.Include.NON_NULL) ErrorTrapResult errorTrap) {
    public static final String ACTIVE = "ACTIVE";
    public static final String DIAGNOSING = "DIAGNOSING";
    public static final String WAITING_ON_PREREQUISITES = "WAITING_ON_PREREQUISITES";

    public ReviewResult(UUID reviewId, String reviewStatus, UUID lessonId, List<String> theory, ReviewSet set,
                        UUID knowledgePointId, LearningSkill skill, String stage, String theoryReason,
                        String theoryScope, int failedSets, Integer maxFailedSets,
                        List<LessonResult.Question> quickCheck) {
        this(reviewId, reviewStatus, lessonId, theory, set, knowledgePointId, skill, stage, theoryReason, theoryScope,
                failedSets, maxFailedSets, quickCheck, ACTIVE, null, true, List.of(), null);
    }

    public ReviewResult(UUID reviewId, String reviewStatus, UUID lessonId, List<String> theory, ReviewSet set,
                        UUID knowledgePointId, LearningSkill skill, String stage, String theoryReason,
                        String theoryScope, int failedSets, Integer maxFailedSets,
                        List<LessonResult.Question> quickCheck, String state, UUID diagnosticSessionId,
                        List<UUID> waitingOnReviewIds) {
        this(reviewId, reviewStatus, lessonId, theory, set, knowledgePointId, skill, stage, theoryReason, theoryScope,
                failedSets, maxFailedSets, quickCheck, state, diagnosticSessionId, true, waitingOnReviewIds, null);
    }

    public ReviewResult(UUID reviewId, String reviewStatus, UUID lessonId, List<String> theory, ReviewSet set,
                        UUID knowledgePointId, LearningSkill skill, String stage, String theoryReason,
                        String theoryScope, int failedSets, Integer maxFailedSets,
                        List<LessonResult.Question> quickCheck, String state, UUID diagnosticSessionId,
                        List<UUID> waitingOnReviewIds, ErrorTrapResult errorTrap) {
        this(reviewId, reviewStatus, lessonId, theory, set, knowledgePointId, skill, stage, theoryReason, theoryScope,
                failedSets, maxFailedSets, quickCheck, state, diagnosticSessionId, true, waitingOnReviewIds, errorTrap);
    }

    public record ReviewSet(UUID reviewSetId, UUID packageId, UUID packageVersionId, String passage, Audio audio,
                            List<LessonResult.Question> questions) {}

    /** Review audio never carries the transcript before the set is answered. */
    public record Audio(String mediaUrl, Integer durationSeconds) {}
}

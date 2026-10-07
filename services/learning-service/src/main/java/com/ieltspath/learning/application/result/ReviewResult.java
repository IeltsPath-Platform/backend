package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.LearningSkill;

import java.util.List;
import java.util.UUID;

/**
 * {@code set} is null once the review is no longer pending and at the theory stage. {@code theory} holds the TEXT
 * blocks that teach the knowledge point ({@code theoryScope = KNOWLEDGE_POINT}) or, when the lesson tags none, every
 * TEXT block ({@code LESSON_FALLBACK}). {@code quickCheck} is non-empty only at the theory stage.
 */
public record ReviewResult(UUID reviewId, String reviewStatus, UUID lessonId, List<String> theory, ReviewSet set,
                           UUID knowledgePointId, LearningSkill skill, String stage, String theoryReason,
                           String theoryScope, int failedSets, int maxFailedSets,
                           List<LessonResult.Question> quickCheck) {
    public record ReviewSet(UUID reviewSetId, UUID packageId, UUID packageVersionId, String passage, Audio audio,
                            List<LessonResult.Question> questions) {}

    /** Review audio never carries the transcript before the set is answered. */
    public record Audio(String mediaUrl, Integer durationSeconds) {}
}

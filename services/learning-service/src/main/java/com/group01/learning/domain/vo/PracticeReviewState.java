package com.group01.learning.domain.vo;

import java.util.UUID;

/** {@code skill} is the skill of the review's knowledge point, null for older reviews. */
public record PracticeReviewState(UUID lessonId, UUID knowledgePointId, ReviewStatus status, LearningSkill skill) {
    public PracticeReviewState(UUID lessonId, UUID knowledgePointId, ReviewStatus status) {
        this(lessonId, knowledgePointId, status, null);
    }
}

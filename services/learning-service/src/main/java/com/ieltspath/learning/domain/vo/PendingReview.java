package com.ieltspath.learning.domain.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * A review the learner has not finished. Only a {@code blocking} one holds back lessons, Practice and topic tests; an
 * optional one (picked from the review basket, or from a free practice test) never does.
 */
public record PendingReview(UUID reviewId, UUID lessonId, UUID knowledgePointId, LearningSkill skill,
                            boolean blocking, String triggerKind) {
    public PendingReview(UUID reviewId, UUID lessonId, UUID knowledgePointId, LearningSkill skill, boolean blocking) {
        this(reviewId, lessonId, knowledgePointId, skill, blocking, null);
    }
    public PendingReview(UUID reviewId, UUID lessonId, UUID knowledgePointId, LearningSkill skill) {
        this(reviewId, lessonId, knowledgePointId, skill, true, null);
    }
    public PendingReview(UUID reviewId, UUID lessonId, UUID knowledgePointId) {
        this(reviewId, lessonId, knowledgePointId, null, true, null);
    }
    public PendingReview {
        Objects.requireNonNull(reviewId, "reviewId");
        Objects.requireNonNull(lessonId, "lessonId");
        Objects.requireNonNull(knowledgePointId, "knowledgePointId");
    }
}

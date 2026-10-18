package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code sourcePackageId} is the Practice set whose attempt created the review; null for reviews from assessments.
 * {@code blocking} is false for an optional review (review basket, free practice test).
 */
public record ReviewListEntry(UUID reviewId, UUID knowledgePointId, UUID lessonId,
                              LearningSkill skill, ReviewStage stage, Instant createdAt, UUID sourcePackageId,
                              boolean blocking) {
    public ReviewListEntry(UUID reviewId, UUID knowledgePointId, UUID lessonId, LearningSkill skill,
                           ReviewStage stage, Instant createdAt) {
        this(reviewId, knowledgePointId, lessonId, skill, stage, createdAt, null, true);
    }

    public ReviewListEntry(UUID reviewId, UUID knowledgePointId, UUID lessonId, LearningSkill skill,
                           ReviewStage stage, Instant createdAt, boolean blocking) {
        this(reviewId, knowledgePointId, lessonId, skill, stage, createdAt, null, blocking);
    }
}

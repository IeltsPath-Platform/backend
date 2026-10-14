package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.UUID;

/** {@code sourcePackageId} is the Practice set whose attempt created the review; null for reviews from assessments. */
public record ReviewListEntry(UUID reviewId, UUID knowledgePointId, UUID lessonId,
                              LearningSkill skill, ReviewStage stage, Instant createdAt, UUID sourcePackageId) {
    public ReviewListEntry(UUID reviewId, UUID knowledgePointId, UUID lessonId, LearningSkill skill,
                           ReviewStage stage, Instant createdAt) {
        this(reviewId, knowledgePointId, lessonId, skill, stage, createdAt, null);
    }
}

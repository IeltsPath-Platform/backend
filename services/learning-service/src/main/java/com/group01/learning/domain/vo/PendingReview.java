package com.group01.learning.domain.vo;

import java.util.Objects;
import java.util.UUID;

public record PendingReview(UUID reviewId, UUID lessonId, UUID knowledgePointId) {
    public PendingReview {
        Objects.requireNonNull(reviewId, "reviewId");
        Objects.requireNonNull(lessonId, "lessonId");
        Objects.requireNonNull(knowledgePointId, "knowledgePointId");
    }
}

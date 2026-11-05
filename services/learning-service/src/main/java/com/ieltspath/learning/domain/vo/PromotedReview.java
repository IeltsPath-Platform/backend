package com.ieltspath.learning.domain.vo;

import java.util.UUID;

/** A suggested review that a repeat weakness just made blocking. */
public record PromotedReview(UUID reviewId, UUID knowledgePointId, ReviewStage stage) {}

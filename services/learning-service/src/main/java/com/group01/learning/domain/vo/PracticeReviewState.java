package com.group01.learning.domain.vo;

import java.util.UUID;

public record PracticeReviewState(UUID lessonId, UUID knowledgePointId, ReviewStatus status) {}

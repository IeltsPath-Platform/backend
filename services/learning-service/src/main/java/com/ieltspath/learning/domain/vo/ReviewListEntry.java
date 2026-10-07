package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.UUID;

public record ReviewListEntry(UUID reviewId, UUID knowledgePointId, UUID lessonId,
                              LearningSkill skill, ReviewStage stage, Instant createdAt) {}

package com.group01.learning.domain.vo;

import java.util.UUID;

public record PracticeReviewCandidate(UUID reviewId, UUID lessonId, UUID knowledgePointId, LearningSkill skill,
                                      UUID sourceAttemptId, double kpPercent) {}

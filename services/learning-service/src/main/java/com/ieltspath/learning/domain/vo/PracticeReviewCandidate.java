package com.ieltspath.learning.domain.vo;

import java.util.UUID;

/** {@code theoryReason} is set when the review starts at {@link ReviewStage#THEORY}. */
public record PracticeReviewCandidate(UUID reviewId, UUID lessonId, UUID knowledgePointId, LearningSkill skill,
                                      UUID sourceAttemptId, double kpPercent, ReviewStage stage,
                                      TheoryReason theoryReason) {}

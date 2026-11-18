package com.ieltspath.learning.domain.vo;

import java.util.UUID;

/** {@code skill} is the skill of the review's knowledge point, null for older reviews. */
public record PracticeReviewState(UUID lessonId, UUID knowledgePointId, ReviewStatus status, LearningSkill skill,
                                  boolean blocking, boolean unblocked) {
    public PracticeReviewState(UUID lessonId, UUID knowledgePointId, ReviewStatus status, LearningSkill skill,
                               boolean blocking) {
        this(lessonId, knowledgePointId, status, skill, blocking, false);
    }
    public PracticeReviewState(UUID lessonId, UUID knowledgePointId, ReviewStatus status, LearningSkill skill) {
        this(lessonId, knowledgePointId, status, skill, true, false);
    }
    public PracticeReviewState(UUID lessonId, UUID knowledgePointId, ReviewStatus status) {
        this(lessonId, knowledgePointId, status, null, true, false);
    }
}

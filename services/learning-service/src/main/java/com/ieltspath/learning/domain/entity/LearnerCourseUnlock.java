package com.ieltspath.learning.domain.entity;

import com.ieltspath.learning.domain.vo.LearningSkill;

import java.util.UUID;

public record LearnerCourseUnlock(UUID id, UUID userId, UUID courseId, LearningSkill skill, int pointCost,
                                  UUID requestId, Status status, UUID ledgerEntryId) {
    public boolean combo() { return skill == null; }

    public enum Status { PENDING, ACTIVE, CANCELLED }
}

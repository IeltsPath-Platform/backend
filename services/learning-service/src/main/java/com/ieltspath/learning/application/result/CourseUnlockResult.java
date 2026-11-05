package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;

import java.util.UUID;

public record CourseUnlockResult(UUID courseId, String target, int pointCost, String status, UUID ledgerEntryId) {
    public static CourseUnlockResult from(LearnerCourseUnlock unlock) {
        return new CourseUnlockResult(unlock.courseId(), unlock.skill() == null ? "COMBO" : unlock.skill().name(),
                unlock.pointCost(), unlock.status().name(), unlock.ledgerEntryId());
    }
}

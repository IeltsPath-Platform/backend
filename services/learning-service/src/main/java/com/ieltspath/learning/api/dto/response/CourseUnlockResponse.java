package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.CourseUnlockResult;

import java.util.UUID;

public record CourseUnlockResponse(UUID courseId, String target, int pointCost, String status, UUID ledgerEntryId) {
    public static CourseUnlockResponse from(CourseUnlockResult result) {
        return new CourseUnlockResponse(result.courseId(), result.target(), result.pointCost(), result.status(),
                result.ledgerEntryId());
    }
}

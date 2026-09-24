package com.group01.learningsupport.application.result;

import com.group01.learningsupport.domain.aggregate.LearningActivity;

import java.time.Instant;
import java.util.UUID;

public record LearningActivityResult(UUID id, UUID userId, String activityType, String sourceType, UUID sourceId,
                                     Instant occurredAt, Integer durationSeconds, Instant verifiedAt) {
    public static LearningActivityResult from(LearningActivity activity) {
        return new LearningActivityResult(activity.getId(), activity.getUserId(), activity.getActivityType(),
                activity.getSourceType(), activity.getSourceId(), activity.getOccurredAt(),
                activity.getDurationSeconds(), activity.getVerifiedAt());
    }
}

package com.group01.content.api.dto.internal;

import com.group01.content.application.result.LessonPracticeSetResult;

import java.util.List;
import java.util.UUID;

/** A practice set of a lesson's Practice; no questions or answers. */
public record LessonPracticeSetResponse(
        UUID packageId,
        UUID packageVersionId,
        String code,
        String title,
        int questionCount,
        List<UUID> knowledgePointIds,
        String requiredFeatureKey
) {
    public static LessonPracticeSetResponse from(LessonPracticeSetResult result) {
        return new LessonPracticeSetResponse(result.packageId(), result.packageVersionId(), result.code(),
                result.title(), result.questionCount(), result.knowledgePointIds(), result.requiredFeatureKey());
    }
}

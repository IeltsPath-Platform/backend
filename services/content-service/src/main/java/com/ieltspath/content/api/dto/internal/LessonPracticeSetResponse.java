package com.ieltspath.content.api.dto.internal;

import com.ieltspath.content.application.result.LessonPracticeSetResult;

import java.util.List;
import java.util.UUID;
import com.ieltspath.content.domain.vo.Skill;

/** A practice set of a lesson's Practice; no questions or answers. */
public record LessonPracticeSetResponse(
        UUID packageId,
        UUID packageVersionId,
        String code,
        String title,
        int questionCount,
        List<UUID> knowledgePointIds,
        String requiredFeatureKey,
        List<Skill> skills
) {
    public static LessonPracticeSetResponse from(LessonPracticeSetResult result) {
        return new LessonPracticeSetResponse(result.packageId(), result.packageVersionId(), result.code(),
                result.title(), result.questionCount(), result.knowledgePointIds(), result.requiredFeatureKey(), result.skills());
    }
}

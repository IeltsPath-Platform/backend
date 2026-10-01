package com.group01.content.api.dto.internal;

import com.group01.content.application.result.LessonSummaryResult;

import java.util.List;
import java.util.UUID;

public record LessonSummaryResponse(
        UUID lessonId,
        UUID topicId,
        String code,
        String title,
        String summary,
        int sortOrder,
        List<UUID> knowledgePointIds,
        List<UUID> exerciseBlockIds
) {
    public static LessonSummaryResponse from(LessonSummaryResult result) {
        return new LessonSummaryResponse(result.lessonId(), result.topicId(), result.code(), result.title(),
                result.summary(), result.sortOrder(), result.knowledgePointIds(), result.exerciseBlockIds());
    }
}

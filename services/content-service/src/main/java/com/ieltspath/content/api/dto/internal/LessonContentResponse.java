package com.ieltspath.content.api.dto.internal;

import com.ieltspath.content.application.result.LessonContentResult;
import com.ieltspath.content.domain.vo.Skill;

import java.util.List;
import java.util.UUID;

/** Internal lesson payload. {@code summaryMarkdown} is {@code null} when the lesson has no summary. */
public record LessonContentResponse(
        UUID lessonId,
        UUID topicId,
        String code,
        String title,
        String summary,
        String summaryMarkdown,
        int sortOrder,
        List<UUID> knowledgePointIds,
        Skill skill,
        List<Skill> skills
) {
    public static LessonContentResponse from(LessonContentResult result) {
        return new LessonContentResponse(result.lessonId(), result.topicId(), result.code(), result.title(),
                result.summary(), result.summaryMarkdown(), result.sortOrder(), result.knowledgePointIds(),
                result.skill(), result.skills());
    }
}

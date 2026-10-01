package com.group01.content.application.result;

import java.util.List;
import java.util.UUID;

/** A published lesson of a topic, with the ids a learner's progress is recorded against. */
public record LessonSummaryResult(
        UUID lessonId,
        UUID topicId,
        String code,
        String title,
        String summary,
        int sortOrder,
        List<UUID> knowledgePointIds,
        List<UUID> exerciseBlockIds
) {}

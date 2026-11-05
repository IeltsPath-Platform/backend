package com.ieltspath.content.application.result;

import java.util.List;
import java.util.UUID;
import com.ieltspath.content.domain.vo.Skill;

/**
 * A published lesson of a topic, with the ids a learner's progress is recorded against. {@code estimatedMinutes} is
 * null when the author has not set a duration.
 */
public record LessonSummaryResult(
        UUID lessonId,
        UUID topicId,
        String code,
        String title,
        String summary,
        int sortOrder,
        List<UUID> knowledgePointIds,
        List<UUID> exerciseBlockIds,
        List<Skill> skills,
        Integer estimatedMinutes,
        UUID courseId,
        Skill skill,
        Integer skillPosition
) {
    public LessonSummaryResult(UUID lessonId, UUID topicId, String code, String title, String summary, int sortOrder,
                               List<UUID> knowledgePointIds, List<UUID> exerciseBlockIds, List<Skill> skills,
                               Integer estimatedMinutes) {
        this(lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, exerciseBlockIds, skills,
                estimatedMinutes, null, skills != null && skills.size() == 1 ? skills.getFirst() : null, null);
    }

    public LessonSummaryResult(UUID lessonId, UUID topicId, String code, String title, String summary, int sortOrder,
                               List<UUID> knowledgePointIds, List<UUID> exerciseBlockIds) {
        this(lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, exerciseBlockIds, List.of(), null,
                null, null, null);
    }
}

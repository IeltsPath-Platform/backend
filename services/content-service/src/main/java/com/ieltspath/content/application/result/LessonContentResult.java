package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.Skill;

import java.util.List;
import java.util.UUID;

/**
 * A published lesson for services. {@code summaryMarkdown} is the lesson's Markdown summary ({@code null} when it has
 * none); its slides and documents are read separately. {@code skills} are the skills of the lesson's knowledge
 * points, and {@code skill} is set when there is exactly one.
 */
public record LessonContentResult(
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
    public LessonContentResult {
        skills = skills.stream().filter(s -> s != Skill.ALL).distinct().sorted().toList();
        skill = skills.size() == 1 ? skills.getFirst() : null;
    }
}

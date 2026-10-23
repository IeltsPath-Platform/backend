package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.Skill;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * What an author asks of the course generator. {@code topics} are phrases matched to knowledge points;
 * {@code knowledgePointIds} name points exactly. At least one of the two is needed.
 */
public record CourseDraftRequest(BigDecimal bandLevel, List<Skill> skills, List<String> topics,
                                 List<UUID> knowledgePointIds, Integer lessonCount, String instructions) {

    public static final int DEFAULT_LESSON_COUNT = 8;

    public CourseDraftRequest {
        skills = skills == null ? List.of() : List.copyOf(skills);
        topics = topics == null ? List.of() : List.copyOf(topics);
        knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
    }

    public int lessons() {
        return lessonCount == null ? DEFAULT_LESSON_COUNT : lessonCount;
    }
}

package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.Skill;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * What an author asks of the course generator. {@code topics} are phrases matched to knowledge points;
 * {@code knowledgePointIds} name points exactly. At least one of the two is needed. {@code includePrerequisites} says
 * whether the generator adds the prerequisites of those points itself; it is on when absent. A client that lets its
 * author pick the prerequisites, and so sends exactly the points to teach, turns it off.
 */
public record CourseDraftRequest(BigDecimal bandLevel, List<Skill> skills, List<String> topics,
                                 List<UUID> knowledgePointIds, Integer lessonCount, String instructions,
                                 Boolean includePrerequisites) {

    public static final int DEFAULT_LESSON_COUNT = 8;

    public CourseDraftRequest {
        skills = skills == null ? List.of() : List.copyOf(skills);
        topics = topics == null ? List.of() : List.copyOf(topics);
        knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
    }

    /** A request that leaves the prerequisites to the generator. */
    public CourseDraftRequest(BigDecimal bandLevel, List<Skill> skills, List<String> topics,
                              List<UUID> knowledgePointIds, Integer lessonCount, String instructions) {
        this(bandLevel, skills, topics, knowledgePointIds, lessonCount, instructions, null);
    }

    public boolean withPrerequisites() {
        return includePrerequisites == null || includePrerequisites;
    }

    public int lessons() {
        return lessonCount == null ? DEFAULT_LESSON_COUNT : lessonCount;
    }
}

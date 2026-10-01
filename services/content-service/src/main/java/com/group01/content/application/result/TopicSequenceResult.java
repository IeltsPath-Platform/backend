package com.group01.content.application.result;

import com.group01.content.domain.vo.LearningType;
import com.group01.content.domain.vo.Skill;

import java.util.List;
import java.util.UUID;

/** A topic in the learning order, with the active knowledge points it measures. */
public record TopicSequenceResult(
        UUID topicId,
        String code,
        String name,
        int sortOrder,
        List<KnowledgePointEntry> knowledgePoints
) {
    /** {@code hasPracticeSet}: at least one practice set is eligible for review of this knowledge point. */
    public record KnowledgePointEntry(
            UUID id,
            UUID topicId,
            String code,
            String name,
            LearningType learningType,
            Skill skill,
            String description,
            boolean hasPracticeSet
    ) {}
}

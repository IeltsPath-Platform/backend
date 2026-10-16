package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.KnowledgeTreeNodeResult;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record KnowledgeTreeNodeResponse(UUID id, String code, String name, Skill skill, KnowledgePointKind kind,
                                         LearningType learningType, BigDecimal band, UUID courseId, UUID topicId,
                                         List<UUID> lessonIds, ContentStatus status) {
    public static KnowledgeTreeNodeResponse from(KnowledgeTreeNodeResult result) {
        return new KnowledgeTreeNodeResponse(result.id(), result.code(), result.name(), result.skill(), result.kind(),
                result.learningType(), result.band(), result.courseId(), result.topicId(), result.lessonIds(),
                result.status());
    }
}

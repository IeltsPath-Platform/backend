package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.aggregate.Question;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionDetailResult(
        UUID id,
        QuestionType questionType,
        Skill skill,
        String requiredFeatureKey,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionVersionResult> versions,
        QuestionPurpose purpose
) {
    public static QuestionDetailResult from(Question question) {
        List<QuestionVersionResult> versions = question.getVersions().stream().map(v -> new QuestionVersionResult(
                v.getId(), v.getQuestionId(), v.getVersionNumber(), v.getStem(), v.getOptions(), v.getAnswerSpecJson(),
                v.getExplanation(), v.getHint(), v.getDifficulty(), v.getStatus(), v.getCreatedAt(), v.getUpdatedAt(),
                v.getKnowledgePoints().stream().map(kp -> new QuestionKnowledgePointResult(
                        kp.getQuestionVersionId(), kp.getKnowledgePointId(), kp.getWeight())).toList())).toList();
        return new QuestionDetailResult(question.getId(), question.getQuestionType(), question.getSkill(),
                question.getRequiredFeatureKey(), question.getStatus(), question.getCurrentPublishedVersionId(),
                question.getCreatedAt(), question.getUpdatedAt(), versions, question.getPurpose());
    }

    public QuestionDetailResult(UUID id, QuestionType questionType, Skill skill, String requiredFeatureKey,
                                PublicationStatus status, UUID currentPublishedVersionId, Instant createdAt,
                                Instant updatedAt, List<QuestionVersionResult> versions) {
        this(id, questionType, skill, requiredFeatureKey, status, currentPublishedVersionId, createdAt, updatedAt,
                versions, QuestionPurpose.LEARNING);
    }
}

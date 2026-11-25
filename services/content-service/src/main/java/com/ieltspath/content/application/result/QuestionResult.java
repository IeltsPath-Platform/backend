package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;

import java.time.Instant;
import java.util.UUID;

public record QuestionResult(
        UUID id,
        QuestionType questionType,
        Skill skill,
        String requiredFeatureKey,
        PublicationStatus status,
        UUID currentPublishedVersionId,
        Instant createdAt,
        Instant updatedAt,
        QuestionPurpose purpose,
        String importRef,
        String code
) {
    public QuestionResult(UUID id, QuestionType questionType, Skill skill, String requiredFeatureKey,
                          PublicationStatus status, UUID currentPublishedVersionId, Instant createdAt,
                          Instant updatedAt) {
        this(id, questionType, skill, requiredFeatureKey, status, currentPublishedVersionId, createdAt, updatedAt,
                QuestionPurpose.LEARNING, null, null);
    }

    public QuestionResult(UUID id, QuestionType questionType, Skill skill, String requiredFeatureKey,
                          PublicationStatus status, UUID currentPublishedVersionId, Instant createdAt,
                          Instant updatedAt, QuestionPurpose purpose) {
        this(id, questionType, skill, requiredFeatureKey, status, currentPublishedVersionId, createdAt, updatedAt,
                purpose, null, null);
    }

    public QuestionResult(UUID id, QuestionType questionType, Skill skill, String requiredFeatureKey,
                          PublicationStatus status, UUID currentPublishedVersionId, Instant createdAt,
                          Instant updatedAt, QuestionPurpose purpose, String importRef) {
        this(id, questionType, skill, requiredFeatureKey, status, currentPublishedVersionId, createdAt, updatedAt,
                purpose, importRef, null);
    }

    /** Every reader that returns a question uses this, so the import reference is never dropped on one path. */
    public static QuestionResult of(com.ieltspath.content.domain.aggregate.Question question) {
        return new QuestionResult(question.getId(), question.getQuestionType(), question.getSkill(),
                question.getRequiredFeatureKey(), question.getStatus(), question.getCurrentPublishedVersionId(),
                question.getCreatedAt(), question.getUpdatedAt(), question.getPurpose(), question.getImportRef(),
                question.getCode());
    }
}
